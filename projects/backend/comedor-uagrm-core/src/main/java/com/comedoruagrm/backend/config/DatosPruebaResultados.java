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
 * Datos de ejemplo para ver y probar la publicacion de resultados (CU22) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.resultados.enabled=false`.
 * * Corre despues del cargador del CU05 (que crea la convocatoria `[PRUEBA]` con sus finalistas) y no hace
 * * nada si no la encuentra. Es idempotente: si ya existe la convocatoria `[PRUEBA] Con pendientes`, no hace nada.
 * * Deja tres escenarios:
 * * `[PRUEBA] Beca Comedor 2026-II` con todos sus finalistas decididos (adjudicados y no adjudicados) y SIN
 * * publicar, lista para publicar (`puedePublicar = true`); `[PRUEBA] Con pendientes`, cerrada y con un finalista en
 * * «calificada» (el resumen da `puedePublicar = false` y publicar da `ERR_PUB_01`); y `[PRUEBA] Publicada`, con su
 * * fila en `publicacion_resultado`, la convocatoria «publicada» y los avisos ya creados, para probar la lista
 * * publica y el resultado de cada postulante.
 * * Decide los finalistas de `[PRUEBA]` que seguian en «calificada»: ahi ya no queda nada por adjudicar a mano
 * * (los casos de decision pendiente del CU05 siguen en sus dos convocatorias de casos limite).
 * ! NUNCA en produccion: crea decisiones de adjudicacion y publicaciones ficticias.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.resultados.enabled", havingValue = "true", matchIfMissing = true)
@Order(6)
@RequiredArgsConstructor
public class DatosPruebaResultados implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaResultados.class);

    static final String CON_PENDIENTES = DatosPruebaListaPriorizada.MARCA + " Con pendientes";
    static final String PUBLICADA = DatosPruebaListaPriorizada.MARCA + " Publicada";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    @Override
    public void run(String... args) {
        List<Map<String, Object>> convocatorias = jdbc.queryForList(
                "SELECT id, gestion_id, cupos FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL",
                DatosPruebaAdjudicacion.CONVOCATORIA);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        List<Long> modelos = jdbc.queryForList(
                "SELECT id FROM modelo_preseleccion WHERE eliminado_en IS NULL ORDER BY id LIMIT 1", Long.class);
        if (convocatorias.isEmpty() || administradores.isEmpty() || modelos.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de resultados: falta la convocatoria de prueba, un "
                    + "Administrador DUBSS o el modelo de preseleccion de prueba");
            return;
        }
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Integer.class,
                CON_PENDIENTES);
        if (existentes != null && existentes > 0) {
            return;
        }
        Map<String, Object> prueba = convocatorias.get(0);
        long convocatoria = ((Number) prueba.get("id")).longValue();
        long gestion = ((Number) prueba.get("gestion_id")).longValue();
        int cupos = prueba.get("cupos") == null ? 0 : ((Number) prueba.get("cupos")).intValue();
        transacciones.executeWithoutResult(estado -> cargar(convocatoria, gestion, cupos, administradores.get(0),
                modelos.get(0)));
        LOG.info("Datos de prueba de resultados cargados");
    }

    private void cargar(long convocatoria, long gestion, int cupos, long admin, long modelo) {
        decidirLosPendientes(convocatoria, cupos, admin);

        // Con pendientes: un adjudicado, un no adjudicado y uno que sigue «calificada».
        long conPendientes = convocatoriaCerrada(gestion, admin, CON_PENDIENTES, "CERRADA", 2);
        List<Long> usuarios = postulantes(0, 3);
        if (usuarios.size() == 3) {
            decidido(conPendientes, usuarios.get(0), modelo, admin, "ADJUDICADA", "88.00", 1);
            decidido(conPendientes, usuarios.get(1), modelo, admin, "NO_ADJUDICADA", "52.00", 2);
            sinDecidir(conPendientes, usuarios.get(2), modelo, admin, 3);
        } else {
            LOG.warn("No hay postulantes de prueba suficientes para la convocatoria con pendientes");
        }

        // Publicada: dos adjudicados, un no adjudicado y uno archivado (sin adjudicacion, no se avisa).
        long publicada = convocatoriaCerrada(gestion, admin, PUBLICADA, "PUBLICADA", 2);
        List<Long> otros = postulantes(3, 4);
        if (otros.size() == 4) {
            decidido(publicada, otros.get(0), modelo, admin, "ADJUDICADA", "93.50", 1);
            decidido(publicada, otros.get(1), modelo, admin, "ADJUDICADA", "87.25", 2);
            decidido(publicada, otros.get(2), modelo, admin, "NO_ADJUDICADA", "61.00", 3);
            sinDecidirConEstado(publicada, otros.get(3), modelo, admin, "archivada", 4);
            jdbc.update("""
                    INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en)
                    VALUES (?, ?, now() - interval '1 day')
                    """, publicada, admin);
            avisarResultados(publicada);
        } else {
            LOG.warn("No hay postulantes de prueba suficientes para la convocatoria publicada");
        }
    }

    /**
     * Los finalistas de `[PRUEBA]` que seguian en «calificada» quedan decididos: los primeros ocupan los cupos que
     * faltan (adjudicados, con auditoria validada) y el resto queda no adjudicado.
     */
    private void decidirLosPendientes(long convocatoria, int cupos, long admin) {
        Integer adjudicadas = jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = 'ADJUDICADA' AND a.eliminado_en IS NULL
                """, Integer.class, convocatoria);
        int libres = Math.max(0, cupos - (adjudicadas == null ? 0 : adjudicadas));
        List<long[]> pendientes = jdbc.query("""
                SELECT po.id AS postulacion_id, f.id AS finalista_id
                FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = ? AND po.estado = 'calificada'
                  AND NOT EXISTS (SELECT 1 FROM adjudicacion a WHERE a.postulacion_id = po.id
                                  AND a.eliminado_en IS NULL)
                ORDER BY f.posicion, f.id
                """, (rs, n) -> new long[] {rs.getLong("postulacion_id"), rs.getLong("finalista_id")}, convocatoria);
        for (int i = 0; i < pendientes.size(); i++) {
            long postulacion = pendientes.get(i)[0];
            if (i < libres) {
                auditoria(pendientes.get(i)[1], admin, "VALIDADO", "30.00");
                decidir(postulacion, admin, "ADJUDICADA", "adjudicada", "75.00");
            } else {
                decidir(postulacion, admin, "NO_ADJUDICADA", "no_adjudicada", "NULL");
            }
        }
    }

    private long convocatoriaCerrada(long gestion, long admin, String nombre, String estado, int cupos) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, cantidad_finalistas, estado)
                VALUES (?, ?, ?, CAST('2026-07-01T00:00:00Z' AS timestamptz),
                        CAST('2026-08-15T23:59:59Z' AS timestamptz), ?, CAST(? AS numeric), 4,
                        CAST(? AS estado_convocatoria)) RETURNING id
                """, Long.class, gestion, admin, nombre, cupos, DatosPruebaAdjudicacion.PUNTAJE_MINIMO, estado);
    }

    private List<Long> postulantes(int desplazamiento, int cantidad) {
        return jdbc.queryForList("""
                SELECT id FROM usuario WHERE correo LIKE 'postulante.%@prueba.local' AND eliminado_en IS NULL
                ORDER BY ci OFFSET ? LIMIT ?
                """, Long.class, desplazamiento, cantidad);
    }

    /** Un finalista con su decision ya tomada (adjudicacion, estado, historial y auditoria validada). */
    private void decidido(long convocatoria, long usuario, long modelo, long admin, String resultado,
            String puntajeFinal, int posicion) {
        boolean adjudicada = "ADJUDICADA".equals(resultado);
        long postulacion = postulacion(convocatoria, usuario, modelo, "calificada", posicion);
        long finalista = finalista(postulacion, posicion);
        auditoria(finalista, admin, adjudicada ? "VALIDADO" : "OBSERVADO", adjudicada ? "30.00" : null);
        jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?",
                adjudicada ? "adjudicada" : "no_adjudicada", postulacion);
        jdbc.update("""
                INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?, now() - interval '2 days')
                """, postulacion, resultado, puntajeFinal, admin);
        historial(postulacion, admin, adjudicada ? "adjudicada" : "no_adjudicada",
                adjudicada ? null : "El expediente no se pudo completar.");
    }

    /** Un finalista que sigue en «calificada». */
    private void sinDecidir(long convocatoria, long usuario, long modelo, long admin, int posicion) {
        long postulacion = postulacion(convocatoria, usuario, modelo, "calificada", posicion);
        auditoria(finalista(postulacion, posicion), admin, "VALIDADO", "30.00");
    }

    /** Un finalista cuya postulacion ya fue cerrada por el CU20 (sin fila de adjudicacion). */
    private void sinDecidirConEstado(long convocatoria, long usuario, long modelo, long admin, String estado,
            int posicion) {
        long postulacion = postulacion(convocatoria, usuario, modelo, "calificada", posicion);
        finalista(postulacion, posicion);
        jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?", estado, postulacion);
        historial(postulacion, admin, estado, "Cerrada sin adjudicar por no entregar su documentación.");
    }

    private long postulacion(long convocatoria, long usuario, long modelo, String estado, int indice) {
        long postulacion = jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion),
                        CAST('2026-07-10T08:00:00Z' AS timestamptz) + (? * interval '1 minute'), 1)
                RETURNING id
                """, Long.class, convocatoria, usuario, estado, indice);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, ?, now() - interval '30 days', true)
                """, modelo, postulacion, new BigDecimal(60 - indice));
        return postulacion;
    }

    private long finalista(long postulacion, int posicion) {
        return jdbc.queryForObject("""
                INSERT INTO finalista (postulacion_id, posicion, plazo_entrega, notificado_en,
                                       documentacion_recibida_en)
                VALUES (?, ?, current_date + 7, now() - interval '5 days', now() - interval '4 days')
                RETURNING id
                """, Long.class, postulacion, posicion);
    }

    private void auditoria(long finalista, long auditor, String resultado, String puntajeAcademico) {
        jdbc.update("""
                INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones,
                                                  puntaje_academico, fecha_auditoria)
                VALUES (?, ?, CAST(? AS resultado_auditoria), ?, CAST(? AS numeric), now() - interval '3 days')
                """, finalista, auditor, resultado, "OBSERVADO".equals(resultado) ? "Falta el certificado de notas."
                        : null, puntajeAcademico);
    }

    /** La decision ya tomada de un finalista de `[PRUEBA]`: estado, fila de `adjudicacion` y fila del historial. */
    private void decidir(long postulacion, long admin, String resultado, String estado, String puntajeFinal) {
        jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?", estado,
                postulacion);
        if ("NULL".equals(puntajeFinal)) {
            jdbc.update("""
                    INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                    VALUES (?, CAST(? AS resultado_adjudicacion), NULL, ?, now() - interval '2 days')
                    """, postulacion, resultado, admin);
        } else {
            jdbc.update("""
                    INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                    VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?, now() - interval '2 days')
                    """, postulacion, resultado, puntajeFinal, admin);
        }
        historial(postulacion, admin, estado, "no_adjudicada".equals(estado) ? "No cumple los requisitos." : null);
    }

    private void historial(long postulacion, long admin, String estadoNuevo, String motivo) {
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), CAST(? AS estado_postulacion), ?,
                        now() - interval '2 days')
                """, postulacion, admin, estadoNuevo, motivo);
    }

    /** Los avisos de la publicacion ya creados: uno por finalista con adjudicacion, con su resultado. */
    private void avisarResultados(long convocatoria) {
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, postulacion_id, enviada_en)
                SELECT u.id, 'Beca Comedor: resultado de tu postulación',
                       CASE WHEN a.resultado = 'ADJUDICADA'
                            THEN u.nombres || ', ¡felicidades! Tu postulación en ' || cv.nombre
                                 || ' fue adjudicada: obtuviste la Beca Comedor.'
                            ELSE u.nombres || ', tu postulación en ' || cv.nombre
                                 || ' no fue adjudicada. Gracias por participar.' END,
                       CASE WHEN a.resultado = 'ADJUDICADA' THEN 'SUCCESS' ELSE 'INFO' END::tipo_notificacion,
                       po.id, now() - interval '1 day'
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id
                JOIN convocatoria cv ON cv.id = po.convocatoria_id
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = ?
                """, convocatoria);
    }
}
