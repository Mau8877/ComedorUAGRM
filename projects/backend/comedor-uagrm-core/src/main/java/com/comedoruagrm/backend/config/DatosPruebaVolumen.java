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
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Datos de prueba con VOLUMEN para medir el sistema completo (B1): postulaciones con sus respuestas del
 * formulario socioeconomico y su puntaje, adjudicados, becas y asistencias.
 *
 * * Solo con el perfil `dev` y por defecto activo; se apaga con `app.seed.volumen.enabled=false`.
 * * Reutiliza los postulantes de prueba que crea {@link DatosPruebaListaPriorizada}, asi que el volumen se fija
 * * con `app.seed.priorizacion.postulaciones` (con 20000 se obtienen 20000 postulaciones, 1000 becas y unas
 * * 18000 asistencias). Corre despues de los demas cargadores y no hace nada si no hay postulantes de prueba.
 * * Crea su propia convocatoria `[PRUEBA] Volumen - Becados y asistencias` (cerrada), para no cambiar el estado
 * * de las postulaciones que usan los otros cargadores. Es idempotente: si esa convocatoria ya existe, no hace nada.
 * * Todo se genera con SQL por conjuntos (no fila por fila): 20000 postulaciones tardan segundos. El azar parte
 * * de una semilla fija, asi que dos cargas dan la misma distribucion.
 * * Los valores salen de los catalogos de {@code OpcionesSocioeconomicas} (incluido el formulario oficial de la
 * * DUBSS); el puntaje de la IA se simula entre 0 y 70 con relacion a la necesidad, pero NO es el de la IA real:
 * * para el puntaje real se reprocesa desde el CU26.
 * ! NUNCA en produccion: crea decisiones, becas y asistencias ficticias.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.volumen.enabled", havingValue = "true", matchIfMissing = true)
@Order(9)
@RequiredArgsConstructor
public class DatosPruebaVolumen implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaVolumen.class);

    static final String CONVOCATORIA = DatosPruebaListaPriorizada.MARCA + " Volumen - Becados y asistencias";
    static final String GESTION = DatosPruebaListaPriorizada.MARCA + " Volumen";
    static final String CORREO_POSTULANTES = "postulante.%@prueba.local";
    /** Tope de becas: es lo que reparte la convocatoria. */
    static final int CUPOS_MAXIMOS = 1000;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    /** Dias corridos de asistencia desde el inicio de las becas (1 de septiembre) (solo cuentan los dias habiles). */
    @Value("${app.seed.volumen.dias:35}")
    private int dias;

    @Override
    public void run(String... args) {
        Integer existentes = jdbc.queryForObject("SELECT count(*) FROM convocatoria WHERE nombre = ?",
                Integer.class, CONVOCATORIA);
        if (existentes != null && existentes > 0) {
            return;
        }
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        Integer postulantes = jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo LIKE ? AND eliminado_en IS NULL", Integer.class,
                CORREO_POSTULANTES);
        if (administradores.isEmpty() || postulantes == null || postulantes == 0) {
            LOG.warn("No se cargaron los datos de prueba de volumen: falta un Administrador DUBSS o los postulantes de "
                    + "prueba (los crea el cargador de la lista priorizada)");
            return;
        }
        long inicio = System.currentTimeMillis();
        int cupos = Math.min(CUPOS_MAXIMOS, Math.max(1, postulantes / 5));
        long[] cantidades = new long[4];
        transacciones.executeWithoutResult(estado -> cargar(administradores.get(0), cupos, cantidades));
        LOG.info("Datos de prueba de volumen cargados: {} postulaciones, {} adjudicadas, {} becas y {} asistencias en "
                + "{} ms", cantidades[0], cantidades[1], cantidades[2], cantidades[3],
                System.currentTimeMillis() - inicio);
    }

    private void cargar(long administradorId, int cupos, long[] cantidades) {
        // Misma semilla en cada carga: la distribucion de los datos no cambia entre una y otra.
        jdbc.execute("SELECT setseed(0.42)");
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class, GESTION);
        long convocatoria = jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, estado)
                VALUES (?, ?, ?, TIMESTAMPTZ '2026-07-01 00:00+00', TIMESTAMPTZ '2026-08-15 23:59+00', ?, 40,
                        CAST('CERRADA' AS estado_convocatoria))
                RETURNING id""", Long.class, gestion, administradorId, CONVOCATORIA, cupos);

        cantidades[0] = crearPostulaciones(convocatoria);
        crearRespuestas(convocatoria);
        crearPuntajes(convocatoria, administradorId);
        cantidades[1] = crearAdjudicaciones(convocatoria, administradorId, cupos);
        cantidades[2] = crearBecas(convocatoria, administradorId);
        cantidades[3] = crearAsistencias(convocatoria, administradorId);
    }

    /** Una postulacion `calificada` por cada postulante de prueba, enviadas con un minuto de diferencia. */
    private long crearPostulaciones(long convocatoria) {
        return jdbc.update("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                SELECT ?, u.id, CAST('calificada' AS estado_postulacion),
                       TIMESTAMPTZ '2026-07-10 08:00+00' + ROW_NUMBER() OVER (ORDER BY u.id) * INTERVAL '1 minute', 1
                FROM usuario u JOIN postulante p ON p.usuario_id = u.id AND p.eliminado_en IS NULL
                WHERE u.correo LIKE ? AND u.eliminado_en IS NULL
                """, convocatoria, CORREO_POSTULANTES);
    }

    /**
     * Las respuestas del formulario de cada postulacion, con los catalogos de `OpcionesSocioeconomicas`. Los
     * ingresos y los dependientes se reparten sesgados hacia lo bajo (como en la realidad) y los datos del
     * formulario oficial van en `datos_extra`, como los guarda el CU02.
     */
    private void crearRespuestas(long convocatoria) {
        jdbc.update("""
                INSERT INTO respuesta_socioeconomica (postulacion_id, ingresos_familiares, dependientes, procedencia,
                       patrimonio, estado_vulnerabilidad, promedio_academico, datos_extra)
                SELECT po.id,
                       round(CAST(800 + 9200 * power(d.a, 1.7) AS numeric), 2),
                       CAST(floor(d.b * d.c * 8) AS smallint),
                       CASE WHEN d.d < 0.45 THEN 'Capital' WHEN d.d < 0.75 THEN 'Provincia'
                            WHEN d.d < 0.90 THEN 'Otro departamento' ELSE 'Área rural' END,
                       CASE WHEN d.e < 0.35 THEN 'Vivienda propia' WHEN d.e < 0.50 THEN 'Vivienda en anticrético'
                            WHEN d.e < 0.80 THEN 'Vivienda alquilada' ELSE 'Sin vivienda propia' END,
                       CASE WHEN d.f < 0.55 THEN 'Sin situación especial' WHEN d.f < 0.70 THEN 'Hogar monoparental'
                            WHEN d.f < 0.82 THEN 'Ingresos irregulares' WHEN d.f < 0.94 THEN 'Dependiente de otro familiar'
                            ELSE 'Persona con discapacidad en el hogar' END,
                       round(CAST(51 + 49 * d.g AS numeric), 2),
                       jsonb_build_object(
                           'dependenciaEconomica',
                               CASE WHEN d.h < 0.10 THEN 'Independiente' WHEN d.h < 0.25 THEN 'Otro familiar'
                                    WHEN d.h < 0.45 THEN 'Solo de padre o madre' WHEN d.h < 0.50 THEN 'De la pareja'
                                    ELSE 'Ambos padres' END,
                           'ocupacionSosten',
                               CASE WHEN d.i < 0.25 THEN 'Asalariado formal' WHEN d.i < 0.50 THEN 'Asalariado informal'
                                    WHEN d.i < 0.58 THEN 'Comerciante mayorista' WHEN d.i < 0.64 THEN 'Rentista'
                                    WHEN d.i < 0.90 THEN 'Comerciante minorista' ELSE 'Agricultor' END,
                           'tenenciaVivienda',
                               CASE WHEN d.e < 0.15 THEN 'Herencia' WHEN d.e < 0.35 THEN 'De los padres'
                                    WHEN d.e < 0.45 THEN 'Cedida' WHEN d.e < 0.50 THEN 'Anticrético'
                                    ELSE 'Alquiler' END,
                           'tieneHijos', d.j < 0.12,
                           'cantidadHijos', CASE WHEN d.j >= 0.12 THEN NULL WHEN d.k < 0.6 THEN '1' ELSE 'Más de 1' END,
                           'otroBeneficio', d.l < 0.05,
                           'discapacidad', d.m < 0.04)
                FROM postulacion po
                CROSS JOIN LATERAL (SELECT random() AS a, random() AS b, random() AS c, random() AS d, random() AS e,
                                           random() AS f, random() AS g, random() AS h, random() AS i, random() AS j,
                                           random() AS k, random() AS l, random() AS m, po.id AS pid) d
                WHERE po.convocatoria_id = ?
                """, convocatoria);
    }

    /**
     * Un puntaje vigente por postulacion, de 0 a 70 (el tope de la IA), mas alto cuanto menos ingresos y mas
     * dependientes. Es una SIMULACION para tener datos con forma: no es el puntaje de la IA real.
     */
    private void crearPuntajes(long convocatoria, long administradorId) {
        Long modelo = jdbc.query("""
                SELECT id FROM modelo_preseleccion WHERE eliminado_en IS NULL
                ORDER BY (version = 'v1.0') DESC, id LIMIT 1""", rs -> rs.next() ? rs.getLong(1) : null);
        if (modelo == null) {
            modelo = jdbc.queryForObject("""
                    INSERT INTO modelo_preseleccion (creado_por, version, descripcion, activo)
                    VALUES (?, ?, 'Modelo de prueba de volumen', false) RETURNING id""", Long.class, administradorId,
                    DatosPruebaListaPriorizada.MARCA + " v-volumen");
        }
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                SELECT ?, po.id,
                       round(CAST(70 * LEAST(1, GREATEST(0, 0.6 * (0.6 * (1 - LEAST(r.ingresos_familiares, 8000) / 8000)
                                 + 0.4 * LEAST(r.dependientes, 6) / 6.0) + 0.4 * d.x)) AS numeric), 2),
                       TIMESTAMPTZ '2026-08-20 09:00+00', true
                FROM postulacion po
                JOIN respuesta_socioeconomica r ON r.postulacion_id = po.id
                CROSS JOIN LATERAL (SELECT random() AS x, po.id AS pid) d
                WHERE po.convocatoria_id = ?
                """, modelo, convocatoria);
    }

    /**
     * Las decisiones: los primeros `cupos` puestos de la lista priorizada quedan `ADJUDICADA` y la mitad de esa
     * cantidad que sigue queda `NO_ADJUDICADA`; el estado de la postulacion se actualiza igual. El puntaje final
     * suma 10 a 30 puntos de rendimiento academico (los que pone el auditor en el CU07), con tope de 100.
     */
    private long crearAdjudicaciones(long convocatoria, long administradorId, int cupos) {
        int limite = cupos + Math.max(1, cupos / 2);
        jdbc.update("""
                WITH lista AS (
                    SELECT po.id AS postulacion_id, pu.puntaje_total,
                           ROW_NUMBER() OVER (ORDER BY pu.puntaje_total DESC, po.fecha_envio, po.id) AS puesto
                    FROM postulacion po
                    JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente AND pu.eliminado_en IS NULL
                    WHERE po.convocatoria_id = ? AND po.eliminado_en IS NULL)
                INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por, fecha_adjudicacion)
                SELECT postulacion_id,
                       CAST(CASE WHEN puesto <= ? THEN 'ADJUDICADA' ELSE 'NO_ADJUDICADA' END AS resultado_adjudicacion),
                       round(CAST(LEAST(100, puntaje_total + 10 + random() * 20) AS numeric), 2), ?,
                       TIMESTAMPTZ '2026-08-25 12:00+00'
                FROM lista WHERE puesto <= ?
                """, convocatoria, cupos, administradorId, limite);
        jdbc.update("""
                UPDATE postulacion po
                SET estado = CAST(CASE WHEN a.resultado = 'ADJUDICADA' THEN 'adjudicada' ELSE 'no_adjudicada' END
                                  AS estado_postulacion), actualizado_en = now()
                FROM adjudicacion a
                WHERE a.postulacion_id = po.id AND a.eliminado_en IS NULL AND po.convocatoria_id = ?
                """, convocatoria);
        return jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion a JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ? AND a.resultado = 'ADJUDICADA'""", Long.class, convocatoria);
    }

    /**
     * Una beca por cada adjudicada (CU23, alta desde adjudicados): una de cada 12 queda suspendida y una de
     * cada 17 de baja, cada una con su movimiento; el resto vigente. Los periodos son chicos a proposito: asi hay
     * variedad tambien con el volumen por defecto (60 postulantes).
     */
    private long crearBecas(long convocatoria, long administradorId) {
        long creadas = jdbc.update("""
                INSERT INTO beca (convocatoria_id, postulante_id, adjudicacion_id, estado, fecha_inicio, fecha_fin)
                SELECT po.convocatoria_id, po.postulante_id, a.id,
                       CAST(CASE WHEN n % 12 = 0 THEN 'SUSPENDIDA' WHEN n % 17 = 0 THEN 'BAJA'
                                 ELSE 'VIGENTE' END AS estado_beca),
                       DATE '2026-09-01', CASE WHEN n % 12 <> 0 AND n % 17 = 0 THEN DATE '2026-09-15' END
                FROM (SELECT a2.id, a2.postulacion_id, ROW_NUMBER() OVER (ORDER BY a2.id) AS n
                      FROM adjudicacion a2 WHERE a2.resultado = 'ADJUDICADA' AND a2.eliminado_en IS NULL) a
                JOIN postulacion po ON po.id = a.postulacion_id
                WHERE po.convocatoria_id = ?
                """, convocatoria);
        jdbc.update("""
                INSERT INTO movimiento_beca (beca_id, registrado_por, tipo, fecha, motivo)
                SELECT b.id, ?,
                       CAST(CASE WHEN b.estado = 'SUSPENDIDA' THEN 'SUSPENSION' ELSE 'BAJA' END AS tipo_movimiento_beca),
                       CASE WHEN b.estado = 'SUSPENDIDA' THEN DATE '2026-09-10' ELSE DATE '2026-09-15' END,
                       CASE WHEN b.estado = 'SUSPENDIDA' THEN 'Suspensión de prueba por inasistencias'
                            ELSE 'Baja de prueba por retiro del postulante' END
                FROM beca b WHERE b.convocatoria_id = ? AND b.estado <> 'VIGENTE' AND b.eliminado_en IS NULL
                """, administradorId, convocatoria);
        return creadas;
    }

    /**
     * Las asistencias de las becas vigentes en los dias habiles desde el inicio: cada becado asiste en el 80 % de
     * esos dias. Las registra el Personal del Comedor si hay uno; si no, el Administrador.
     */
    private long crearAsistencias(long convocatoria, long administradorId) {
        Long registrador = jdbc.query("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'PERSONAL_COMEDOR' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1""", rs -> rs.next() ? rs.getLong(1) : null);
        return jdbc.update("""
                INSERT INTO asistencia (beca_id, registrado_por, fecha, hora_registro)
                SELECT b.id, ?, d.dia, ((d.dia + TIME '12:00') + random() * INTERVAL '90 minutes')
                                       AT TIME ZONE 'America/La_Paz'
                FROM beca b
                CROSS JOIN LATERAL (SELECT CAST(b.fecha_inicio + g AS date) AS dia FROM generate_series(0, ? - 1) g) d
                WHERE b.convocatoria_id = ? AND b.estado = 'VIGENTE' AND b.eliminado_en IS NULL
                  AND EXTRACT(ISODOW FROM d.dia) < 6 AND random() < 0.8
                """, registrador == null ? administradorId : registrador, dias, convocatoria);
    }
}
