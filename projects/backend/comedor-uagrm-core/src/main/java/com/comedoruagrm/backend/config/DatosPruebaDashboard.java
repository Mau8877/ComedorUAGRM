package com.comedoruagrm.backend.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.features.dashboard.service.RefrescoIndicadoresService;

import lombok.RequiredArgsConstructor;

/**
 * Datos de ejemplo para ver el dashboard de indicadores (CU12) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.dashboard.enabled=false`.
 * * Crea la cuenta de Directivo de prueba (`directivo@dubss.com`, contraseña de desarrollo) y dos convocatorias
 * * sobre la gestion de `[PRUEBA]`: `[PRUEBA] Dashboard - Publicada` (con sus resultados publicados) y
 * * `[PRUEBA] Dashboard - Sin publicar`. Las postulaciones se reparten en varios meses, hay borradores (que no
 * * cuentan) y finalistas y adjudicaciones en las dos; las de la convocatoria sin publicar NO deben sumar
 * * adjudicadas en el dashboard.
 * * Al terminar refresca la vista de indicadores. Es idempotente: si ya existe la convocatoria
 * * `[PRUEBA] Dashboard - Publicada`, no hace nada. Corre despues de los demas cargadores (necesita sus
 * * postulantes y el Administrador DUBSS).
 * ! NUNCA en produccion: crea cuentas con contraseña de prueba, decisiones y publicaciones ficticias.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.dashboard.enabled", havingValue = "true", matchIfMissing = true)
@Order(8)
@RequiredArgsConstructor
public class DatosPruebaDashboard implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaDashboard.class);

    static final String CORREO_DIRECTIVO = "directivo@dubss.com";
    static final String CI_DIRECTIVO = "0000002";
    static final String PUBLICADA = DatosPruebaListaPriorizada.MARCA + " Dashboard - Publicada";
    static final String SIN_PUBLICAR = DatosPruebaListaPriorizada.MARCA + " Dashboard - Sin publicar";

    private static final int MAXIMO_ENVIADAS = 30;
    private static final int BORRADORES = 3;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final PasswordEncoder passwordEncoder;
    private final RefrescoIndicadoresService refresco;

    @Value("${app.seed.dashboard.contrasena:123123123}")
    private String contrasena;

    @Override
    public void run(String... args) {
        crearDirectivo();
        List<Long> gestiones = jdbc.queryForList(
                "SELECT gestion_id FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Long.class,
                DatosPruebaAdjudicacion.CONVOCATORIA);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        List<Long> postulantes = jdbc.queryForList("""
                SELECT id FROM usuario WHERE correo LIKE 'postulante.%@prueba.local' AND eliminado_en IS NULL
                ORDER BY ci
                """, Long.class);
        if (gestiones.isEmpty() || administradores.isEmpty() || postulantes.size() < 2 * BORRADORES) {
            LOG.warn("No se cargaron los datos de prueba del dashboard: falta la convocatoria de prueba, un "
                    + "Administrador DUBSS o postulantes de prueba suficientes");
            return;
        }
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Integer.class,
                PUBLICADA);
        if (existentes != null && existentes > 0) {
            return;
        }
        transacciones.executeWithoutResult(
                estado -> cargar(gestiones.get(0), administradores.get(0), postulantes));
        // Fuera de la transaccion: la vista solo ve lo que ya esta confirmado.
        refresco.refrescar();
        LOG.info("Datos de prueba del dashboard cargados");
    }

    private void crearDirectivo() {
        Integer existente = jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo = ? OR ci = ?", Integer.class, CORREO_DIRECTIVO,
                CI_DIRECTIVO);
        if (existente != null && existente > 0) {
            return;
        }
        int filas = jdbc.update("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, ?, 'Directivo', 'DUBSS' FROM rol WHERE nombre = 'DIRECTIVO'
                """, CI_DIRECTIVO, CORREO_DIRECTIVO, passwordEncoder.encode(contrasena));
        if (filas == 0) {
            LOG.warn("No se creó el Directivo de prueba: no existe el rol DIRECTIVO");
        } else {
            LOG.info("Directivo de prueba creado para desarrollo: {}", CORREO_DIRECTIVO);
        }
    }

    private void cargar(long gestion, long admin, List<Long> postulantes) {
        int enviadas = Math.min(MAXIMO_ENVIADAS, postulantes.size() - BORRADORES);
        List<Long> conEnvio = postulantes.subList(0, enviadas);
        List<Long> borradores = postulantes.subList(postulantes.size() - BORRADORES, postulantes.size());

        // Publicada: postulaciones de julio a octubre, 6 finalistas (4 adjudicadas y 1 no adjudicada), borradores.
        long publicada = convocatoria(gestion, admin, PUBLICADA, "PUBLICADA");
        List<Long> postulaciones = enviar(publicada, conEnvio, 0);
        for (int i = 0; i < BORRADORES; i++) {
            postulacion(publicada, borradores.get(i), "borrador", null);
        }
        finalistas(postulaciones, admin, 4, 1);
        jdbc.update("""
                INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en)
                VALUES (?, ?, now() - interval '1 day')
                """, publicada, admin);

        // Sin publicar: otra tanda de postulaciones, con adjudicaciones que todavia no cuentan.
        long sinPublicar = convocatoria(gestion, admin, SIN_PUBLICAR, "CERRADA");
        List<Long> otras = enviar(sinPublicar, conEnvio.subList(0, Math.min(20, enviadas)), 3);
        finalistas(otras, admin, 3, 0);
    }

    private long convocatoria(long gestion, long admin, String nombre, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, cantidad_finalistas, estado)
                VALUES (?, ?, ?, CAST('2026-07-01T00:00:00Z' AS timestamptz),
                        CAST('2026-10-31T23:59:59Z' AS timestamptz), 5, CAST(? AS numeric), 6,
                        CAST(? AS estado_convocatoria)) RETURNING id
                """, Long.class, gestion, admin, nombre, DatosPruebaAdjudicacion.PUNTAJE_MINIMO, estado);
    }

    /** Postulaciones enviadas, una cada 4 dias desde el 5 de julio (varios meses); devuelve sus ids en orden. */
    private List<Long> enviar(long convocatoria, List<Long> usuarios, int desplazamientoDias) {
        return java.util.stream.IntStream.range(0, usuarios.size())
                .mapToObj(i -> postulacion(convocatoria, usuarios.get(i), "calificada",
                        "CAST('2026-07-05T15:00:00Z' AS timestamptz) + interval '" + (i * 4 + desplazamientoDias)
                                + " days'"))
                .toList();
    }

    private long postulacion(long convocatoria, long usuario, String estado, String fechaEnvioSql) {
        String envio = fechaEnvioSql == null ? "NULL" : fechaEnvioSql;
        return jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion), %s, %d) RETURNING id
                """.formatted(envio, fechaEnvioSql == null ? 0 : 1), Long.class, convocatoria, usuario, estado);
    }

    /** Los primeros postulantes pasan a finalistas; los primeros quedan adjudicados y luego los no adjudicados. */
    private void finalistas(List<Long> postulaciones, long admin, int adjudicadas, int noAdjudicadas) {
        int total = Math.min(postulaciones.size(), adjudicadas + noAdjudicadas + 2);
        for (int i = 0; i < total; i++) {
            long postulacion = postulaciones.get(i);
            // Los finalistas se crean en septiembre y octubre; las decisiones, en octubre y noviembre.
            jdbc.update("""
                    INSERT INTO finalista (postulacion_id, posicion, creado_en)
                    VALUES (?, ?, CAST('2026-09-15T15:00:00Z' AS timestamptz) + (? * interval '9 days'))
                    """, postulacion, i + 1, i);
            if (i < adjudicadas + noAdjudicadas) {
                boolean adjudicada = i < adjudicadas;
                jdbc.update("UPDATE postulacion SET estado = CAST(? AS estado_postulacion) WHERE id = ?",
                        adjudicada ? "adjudicada" : "no_adjudicada", postulacion);
                jdbc.update("""
                        INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por,
                                                  fecha_adjudicacion)
                        VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?,
                                CAST('2026-10-10T15:00:00Z' AS timestamptz) + (? * interval '10 days'))
                        """, postulacion, adjudicada ? "ADJUDICADA" : "NO_ADJUDICADA",
                        adjudicada ? "80.00" : null, admin, i);
            }
        }
    }
}
