package com.comedoruagrm.backend.config;

import java.util.List;

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
 * Datos de ejemplo para recorrer «Mis postulaciones» (CU04) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.mis-postulaciones.enabled=false`.
 * * Corre despues de los cargadores del CU06 y CU21 y no hace nada si no encuentra al postulante de prueba
 * * (`postulante.finalista@prueba.local`, con contraseña de desarrollo), un Administrador DUBSS o la convocatoria
 * * `[PRUEBA]`. Es idempotente: si ya existe la convocatoria `[PRUEBA] Mis postulaciones - Pendiente`, no hace nada.
 * * Una postulacion por postulante y convocatoria, asi que cada estado vive en su propia convocatoria: pendiente,
 * * devuelta (con observaciones), archivada (con motivo), una decision final SIN publicar (se ve «calificada»), un
 * * finalista con plazo y aviso, una adjudicada con resultados publicados, un rechazo del CU20 (sin fila de
 * * `adjudicacion`, se ve de inmediato) y un borrador (que no se lista). Cada una lleva las filas de
 * * `postulacion_historial` coherentes (fechas distintas). Dos gestiones permiten probar el filtro.
 * ! NUNCA en produccion: crea postulaciones, decisiones y publicaciones ficticias.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.mis-postulaciones.enabled", havingValue = "true", matchIfMissing = true)
@Order(7)
@RequiredArgsConstructor
public class DatosPruebaMisPostulaciones implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaMisPostulaciones.class);

    static final String PREFIJO = DatosPruebaListaPriorizada.MARCA + " Mis postulaciones - ";
    static final String PENDIENTE = PREFIJO + "Pendiente";
    static final String DEVUELTA = PREFIJO + "Devuelta";
    static final String ARCHIVADA = PREFIJO + "Archivada";
    static final String DECIDIDA_SIN_PUBLICAR = PREFIJO + "Decidida sin publicar";
    static final String FINALISTA = PREFIJO + "Finalista";
    static final String PUBLICADA = PREFIJO + "Adjudicada y publicada";
    static final String RECHAZADA = PREFIJO + "Rechazada";
    static final String BORRADOR = PREFIJO + "Borrador";

    /** La gestion mas antigua, para probar el filtro por gestion academica. */
    static final String GESTION_ANTERIOR = "2025-II";

    private static final String TITULO_AVISO_FINALISTA = "Entrega de documentación física - Beca Comedor";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    @Override
    public void run(String... args) {
        List<Long> postulantes = jdbc.queryForList(
                "SELECT id FROM usuario WHERE correo = ? AND eliminado_en IS NULL", Long.class,
                DatosPruebaFinalistas.CORREO);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        List<Long> gestiones = jdbc.queryForList("""
                SELECT gestion_id FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL
                """, Long.class, DatosPruebaAdjudicacion.CONVOCATORIA);
        if (postulantes.isEmpty() || administradores.isEmpty() || gestiones.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de mis postulaciones: falta el postulante de prueba, un "
                    + "Administrador DUBSS o la convocatoria de prueba");
            return;
        }
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Integer.class,
                PENDIENTE);
        if (existentes != null && existentes > 0) {
            return;
        }
        transacciones.executeWithoutResult(
                estado -> cargar(postulantes.get(0), administradores.get(0), gestiones.get(0)));
        LOG.info("Datos de prueba de mis postulaciones cargados para {}", DatosPruebaFinalistas.CORREO);
    }

    private void cargar(long usuario, long admin, long gestion) {
        long anterior = gestionAnterior();

        // Pendiente: enviada, todavia sin calificar.
        long pendiente = postulacion(convocatoria(gestion, admin, PENDIENTE, "CERRADA"), usuario, "pendiente", 10);
        historial(pendiente, null, "borrador", "pendiente", null, 10);

        // Devuelta: la DUBSS pidio corregir (observaciones en el historial).
        long devuelta = postulacion(convocatoria(gestion, admin, DEVUELTA, "CERRADA"), usuario, "devuelta", 20);
        historial(devuelta, null, "borrador", "pendiente", null, 20);
        historial(devuelta, admin, "pendiente", "calificada", null, 18);
        historial(devuelta, admin, "calificada", "devuelta",
                "Falta el certificado de notas legalizado y la última factura de luz.", 16);

        // Archivada: cerrada por la DUBSS con su motivo (CU20); de la gestion anterior.
        long archivada = postulacion(convocatoria(anterior, admin, ARCHIVADA, "CERRADA"), usuario, "archivada", 60);
        historial(archivada, null, "borrador", "pendiente", null, 60);
        historial(archivada, admin, "pendiente", "calificada", null, 58);
        historial(archivada, admin, "calificada", "archivada", "No entregó su documentación física en el plazo.", 50);

        // Decision final sin publicar: adjudicada de verdad, pero el postulante la ve «calificada».
        long decidida = postulacion(convocatoria(gestion, admin, DECIDIDA_SIN_PUBLICAR, "CERRADA"), usuario,
                "adjudicada", 30);
        historial(decidida, null, "borrador", "pendiente", null, 30);
        historial(decidida, admin, "pendiente", "calificada", null, 28);
        historial(decidida, admin, "calificada", "adjudicada", null, 5);
        adjudicacion(decidida, admin, "ADJUDICADA", "88.00");

        // Finalista: con plazo, aviso con los documentos a presentar y documentacion aun sin recibir.
        long finalista = postulacion(convocatoria(gestion, admin, FINALISTA, "CERRADA"), usuario, "calificada", 25);
        historial(finalista, null, "borrador", "pendiente", null, 25);
        historial(finalista, admin, "pendiente", "calificada", null, 23);
        jdbc.update("""
                INSERT INTO finalista (postulacion_id, posicion, plazo_entrega, notificado_en)
                VALUES (?, 1, current_date + 7, now() - interval '2 days')
                """, finalista);
        avisoDeFinalista(usuario, finalista);

        // Adjudicada con resultados publicados: se ve «adjudicada» y el boton hacia los resultados.
        long publicada = convocatoria(anterior, admin, PUBLICADA, "PUBLICADA");
        long adjudicada = postulacion(publicada, usuario, "adjudicada", 70);
        historial(adjudicada, null, "borrador", "pendiente", null, 70);
        historial(adjudicada, admin, "pendiente", "calificada", null, 68);
        historial(adjudicada, admin, "calificada", "adjudicada", null, 40);
        adjudicacion(adjudicada, admin, "ADJUDICADA", "91.50");
        jdbc.update("""
                INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en)
                VALUES (?, ?, now() - interval '35 days')
                """, publicada, admin);

        // Rechazada (CU20): «no adjudicada» de inmediato, con su motivo y sin fila de adjudicacion.
        long rechazada = postulacion(convocatoria(gestion, admin, RECHAZADA, "CERRADA"), usuario, "no_adjudicada", 35);
        historial(rechazada, null, "borrador", "pendiente", null, 35);
        historial(rechazada, admin, "pendiente", "calificada", null, 33);
        historial(rechazada, admin, "calificada", "no_adjudicada", "Los documentos presentados no coinciden.", 8);

        // Borrador: no se lista en «Mis postulaciones».
        postulacion(convocatoria(gestion, admin, BORRADOR, "CERRADA"), usuario, "borrador", 0);
    }

    private long gestionAnterior() {
        List<Long> existente = jdbc.queryForList(
                "SELECT id FROM gestion_academica WHERE etiqueta = ? AND eliminado_en IS NULL", Long.class,
                GESTION_ANTERIOR);
        if (!existente.isEmpty()) {
            return existente.get(0);
        }
        return jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, DATE '2025-07-01', DATE '2025-12-31') RETURNING id
                """, Long.class, GESTION_ANTERIOR);
    }

    private long convocatoria(long gestion, long admin, String nombre, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, cantidad_finalistas, estado)
                VALUES (?, ?, ?, CAST('2026-07-01T00:00:00Z' AS timestamptz),
                        CAST('2026-08-15T23:59:59Z' AS timestamptz), 5, CAST(? AS numeric), 10,
                        CAST(? AS estado_convocatoria)) RETURNING id
                """, Long.class, gestion, admin, nombre, DatosPruebaAdjudicacion.PUNTAJE_MINIMO, estado);
    }

    /** La postulacion del usuario, enviada hace `diasAtras` dias (los borradores no tienen fecha de envio). */
    private long postulacion(long convocatoria, long usuario, String estado, int diasAtras) {
        return jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion),
                        CASE WHEN ? > 0 THEN now() - (? * interval '1 day') END, ?)
                RETURNING id
                """, Long.class, convocatoria, usuario, estado, diasAtras, diasAtras,
                "borrador".equals(estado) ? 0 : 1);
    }

    private void historial(long postulacion, Long actor, String anterior, String nuevo, String motivo,
            int diasAtras) {
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                   motivo, creado_en)
                VALUES (?, ?, CAST(? AS estado_postulacion), CAST(? AS estado_postulacion), ?,
                        now() - (? * interval '1 day'))
                """, postulacion, actor, anterior, nuevo, motivo, diasAtras);
    }

    private void adjudicacion(long postulacion, long admin, String resultado, String puntajeFinal) {
        jdbc.update("""
                INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?, now() - interval '5 days')
                """, postulacion, resultado, puntajeFinal, admin);
    }

    /** El aviso de finalista (CU21) con el mismo titulo y formato que arma la plantilla, ligado a la postulacion. */
    private void avisoDeFinalista(long usuario, long postulacion) {
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, postulacion_id, titulo, mensaje, tipo, leida, enviada_en)
                SELECT ?, ?, ?,
                       'Hola, quedaste como finalista de la Beca Comedor. Debes entregar tu documentación física '
                       || 'hasta el ' || to_char(current_date + 7, 'DD/MM/YYYY')
                       || '. Lugar y horario: DUBSS, oficina 3, lunes a viernes de 8:00 a 12:00. '
                       || 'Documentos a presentar: carnet de identidad, certificado de notas y última factura de luz.',
                       'INFO', false, now() - interval '2 days'
                """, usuario, postulacion, TITULO_AVISO_FINALISTA);
    }
}
