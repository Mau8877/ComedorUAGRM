package com.comedoruagrm.backend.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Datos de ejemplo para ver y probar el CU05 (seleccionar finalistas y adjudicar o no adjudicar) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.adjudicacion.enabled=false`.
 * * Corre despues de los cargadores del CU06, CU21 y CU07 (que crean la convocatoria `[PRUEBA]`, sus 10
 * * finalistas y sus expedientes) y no hace nada si no los encuentra. Es idempotente: si ya existe la
 * * convocatoria de casos limite, no hace nada. No toca los finalistas que cargaron esos CU.
 * * En la convocatoria `[PRUEBA]` define cupos (5), puntaje minimo (70) y cantidad de finalistas (15), valores
 * * de EJEMPLO (los reales los define la DUBSS; la cantidad la escribira el CU13), y agrega cuatro finalistas
 * * mas: uno «Validado» con puntaje total por debajo del minimo, uno «Validado» con puntaje suficiente, uno ya adjudicado y uno ya «no adjudicado» por
 * * decision final (con fila en `adjudicacion`; distinto del rechazo del CU20, que no la tiene). Quedan
 * * calificadas sin seleccionar para probar la seleccion.
 * * Agrega dos convocatorias de casos limite: una cerrada con un solo cupo libre y varios finalistas validados
 * * compitiendo por el, y una cerrada sin cupos ni puntaje minimo definidos (`ERR_ADJ_05`). La convocatoria con
 * * resultados ya publicados la carga `DatosPruebaResultados` (CU22), que corre despues de este cargador.
 * ! NUNCA en produccion: crea decisiones de adjudicacion ficticias.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.adjudicacion.enabled", havingValue = "true", matchIfMissing = true)
@Order(5)
@RequiredArgsConstructor
public class DatosPruebaAdjudicacion implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaAdjudicacion.class);

    static final String CONVOCATORIA = DatosPruebaListaPriorizada.MARCA + " Beca Comedor 2026-II";
    static final String CASI_AGOTADA = DatosPruebaListaPriorizada.MARCA + " Adjudicación: un cupo libre";
    static final String SIN_CUPOS = DatosPruebaListaPriorizada.MARCA + " Adjudicación: sin cupos ni mínimo";

    /** Valores de ejemplo, no son los de la DUBSS. */
    static final int CUPOS = 5;
    static final String PUNTAJE_MINIMO = "70";
    static final int CANTIDAD_FINALISTAS = 15;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    @Override
    public void run(String... args) {
        List<Map<String, Object>> convocatorias = jdbc.queryForList(
                "SELECT id, gestion_id FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", CONVOCATORIA);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        if (convocatorias.isEmpty() || administradores.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de adjudicacion: falta la convocatoria de prueba o un "
                    + "Administrador DUBSS");
            return;
        }
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Integer.class,
                CASI_AGOTADA);
        if (existentes != null && existentes > 0) {
            return;
        }
        long convocatoria = ((Number) convocatorias.get(0).get("id")).longValue();
        long gestion = ((Number) convocatorias.get(0).get("gestion_id")).longValue();
        List<Long> modelos = jdbc.queryForList(
                "SELECT id FROM modelo_preseleccion WHERE eliminado_en IS NULL ORDER BY id LIMIT 1", Long.class);
        if (modelos.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de adjudicacion: falta el modelo de preseleccion de prueba");
            return;
        }
        transacciones.executeWithoutResult(estado -> cargar(convocatoria, gestion, administradores.get(0),
                modelos.get(0)));
        LOG.info("Datos de prueba de adjudicacion cargados");
    }

    private void cargar(long convocatoria, long gestion, long admin, long modelo) {
        jdbc.update("UPDATE convocatoria SET cupos = ?, puntaje_minimo = CAST(? AS numeric), "
                + "cantidad_finalistas = ? WHERE id = ?", CUPOS, PUNTAJE_MINIMO, CANTIDAD_FINALISTAS, convocatoria);

        // Los cuatro siguientes puestos de la lista priorizada pasan a ser finalistas (los 10 primeros ya lo son).
        List<long[]> siguientes = jdbc.query("""
                SELECT id, posicion FROM (
                    SELECT po.id, ROW_NUMBER() OVER (ORDER BY pu.puntaje_total DESC, po.fecha_envio ASC NULLS LAST,
                                                     po.id ASC) AS posicion,
                           EXISTS (SELECT 1 FROM finalista f WHERE f.postulacion_id = po.id
                                   AND f.eliminado_en IS NULL) AS es_finalista
                    FROM postulacion po
                    JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                         AND pu.eliminado_en IS NULL
                    WHERE po.convocatoria_id = ? AND po.estado = 'calificada' AND po.eliminado_en IS NULL) lista
                WHERE NOT es_finalista ORDER BY posicion LIMIT 4
                """, (rs, n) -> new long[] {rs.getLong("id"), rs.getLong("posicion")}, convocatoria);
        if (siguientes.size() == 4) {
            // Validado, pero con el puntaje academico en 0 el total no alcanza el minimo.
            long bajo = finalista(siguientes.get(0)[0], (int) siguientes.get(0)[1]);
            auditoria(bajo, admin, "VALIDADO", null, "0.00");
            // Validado con 30 de puntaje academico: el total alcanza el minimo y todavia se puede decidir.
            long suficiente = finalista(siguientes.get(1)[0], (int) siguientes.get(1)[1]);
            auditoria(suficiente, admin, "VALIDADO", null, "30.00");
            // Adjudicado: validado con 30 de puntaje academico.
            long adjudicado = finalista(siguientes.get(2)[0], (int) siguientes.get(2)[1]);
            auditoria(adjudicado, admin, "VALIDADO", null, "30.00");
            decidir(siguientes.get(2)[0], admin, "ADJUDICADA", "adjudicada",
                    "(SELECT puntaje_total + 30 FROM puntaje_postulacion WHERE postulacion_id = ? AND vigente "
                            + "AND eliminado_en IS NULL LIMIT 1)", null);
            // No adjudicado por decision final: auditoria observada, motivo y sin puntaje final.
            long noAdjudicado = finalista(siguientes.get(3)[0], (int) siguientes.get(3)[1]);
            auditoria(noAdjudicado, admin, "OBSERVADO", "Falta el certificado de notas.", null);
            decidir(siguientes.get(3)[0], admin, "NO_ADJUDICADA", "no_adjudicada", "NULL",
                    "El expediente no se pudo completar.");
        } else {
            LOG.warn("La convocatoria de prueba no tiene suficientes calificadas sin finalista para el CU05");
        }

        cargarCasoLimite(gestion, admin, modelo, CASI_AGOTADA, 2, 3, 0, 4, 4, true);
        cargarCasoLimite(gestion, admin, modelo, SIN_CUPOS, null, null, 4, 3, 1, false);
    }

    /**
     * Una convocatoria cerrada con `postulantes` postulaciones calificadas (la primera con el mayor puntaje); las
     * primeras `finalistas` pasan a ser finalistas «Validado» con 25 de puntaje academico. Con `adjudicarUno`, el
     * primero ya esta adjudicado (deja un solo cupo libre con 2 cupos). Las demas quedan sin seleccionar.
     */
    private void cargarCasoLimite(long gestion, long admin, long modelo, String nombre, Integer cupos,
            Integer cantidad, int desplazamiento, int postulantes, int finalistas, boolean adjudicarUno) {
        long convocatoria = jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, cantidad_finalistas, estado)
                VALUES (?, ?, ?, CAST('2026-07-01T00:00:00Z' AS timestamptz),
                        CAST('2026-08-15T23:59:59Z' AS timestamptz), ?, CAST(? AS numeric), ?,
                        CAST('CERRADA' AS estado_convocatoria)) RETURNING id
                """, Long.class, gestion, admin, nombre, cupos, cupos == null ? null : PUNTAJE_MINIMO, cantidad);
        List<Long> usuarios = jdbc.queryForList("""
                SELECT id FROM usuario WHERE correo LIKE 'postulante.%@prueba.local' AND eliminado_en IS NULL
                ORDER BY ci OFFSET ? LIMIT ?
                """, Long.class, desplazamiento, postulantes);
        int[] puntajes = {60, 58, 55, 50};
        for (int i = 0; i < usuarios.size(); i++) {
            long postulacion = jdbc.queryForObject("""
                    INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                    VALUES (?, ?, CAST('calificada' AS estado_postulacion),
                            CAST('2026-07-10T08:00:00Z' AS timestamptz) + (? * interval '1 minute'), 1)
                    RETURNING id
                    """, Long.class, convocatoria, usuarios.get(i), i);
            jdbc.update("""
                    INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                    VALUES (?, ?, ?, now() - interval '30 days', true)
                    """, modelo, postulacion, new BigDecimal(puntajes[i % puntajes.length]));
            if (i < finalistas) {
                auditoria(finalista(postulacion, i + 1), admin, "VALIDADO", null, "25.00");
            }
            if (adjudicarUno && i == 0) {
                decidir(postulacion, admin, "ADJUDICADA", "adjudicada",
                        "(SELECT puntaje_total + 25 FROM puntaje_postulacion WHERE postulacion_id = ? AND vigente "
                                + "AND eliminado_en IS NULL LIMIT 1)", null);
            }
        }
    }

    /** El finalista con la entrega fisica ya registrada. */
    private long finalista(long postulacion, int posicion) {
        return jdbc.queryForObject("""
                INSERT INTO finalista (postulacion_id, posicion, plazo_entrega, notificado_en,
                                       documentacion_recibida_en)
                VALUES (?, ?, current_date + 7, now() - interval '3 days', now() - interval '1 day')
                RETURNING id
                """, Long.class, postulacion, posicion);
    }

    private void auditoria(long finalista, long auditor, String resultado, String observaciones,
            String puntajeAcademico) {
        jdbc.update("""
                INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones,
                                                  puntaje_academico, fecha_auditoria)
                VALUES (?, ?, CAST(? AS resultado_auditoria), ?, CAST(? AS numeric), now() - interval '12 hours')
                """, finalista, auditor, resultado, observaciones, puntajeAcademico);
    }

    /**
     * La decision ya tomada: el estado, la fila de `adjudicacion` (con el puntaje final que da la expresion SQL,
     * que recibe el id de la postulacion) y la del historial.
     */
    private void decidir(long postulacion, long admin, String resultado, String estado, String puntajeFinal,
            String motivo) {
        jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?", estado,
                postulacion);
        if ("NULL".equals(puntajeFinal)) {
            jdbc.update("""
                    INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                    VALUES (?, CAST(? AS resultado_adjudicacion), NULL, ?, now() - interval '6 hours')
                    """, postulacion, resultado, admin);
        } else {
            jdbc.update("INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, "
                    + "fecha_adjudicacion) VALUES (?, CAST(? AS resultado_adjudicacion), " + puntajeFinal
                    + ", ?, now() - interval '6 hours')", postulacion, resultado, postulacion, admin);
        }
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), CAST(? AS estado_postulacion), ?,
                        now() - interval '6 hours')
                """, postulacion, admin, estado, motivo);
    }
}
