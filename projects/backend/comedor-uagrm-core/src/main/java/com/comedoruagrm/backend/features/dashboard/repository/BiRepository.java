package com.comedoruagrm.backend.features.dashboard.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas del analisis BI del dashboard (CU12) sobre las vistas `mv_bi_*`.
 *
 * * Igual que {@link IndicadoresRepository}: los numeros salen solo de las vistas, nunca de las tablas
 * * transaccionales. Las unicas tablas que se tocan son `convocatoria` y `gestion_academica`, para los codigos
 * * publicos y los nombres.
 * * `convocatoriaId` nulo = todas las convocatorias de la gestion.
 */
@Repository
@RequiredArgsConstructor
public class BiRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Una convocatoria con todas sus cifras de la vista `mv_bi_convocatoria`. */
    public record FilaConvocatoria(long id, UUID codigo, String nombre, Integer cupos, long recibidas,
            long precalificadas, Double puntajePromedio, long finalistas, long adjudicadas, long becasVigentes,
            long becasSuspendidas, long becasBaja, long reemplazos, long becasEnRiesgo) {
    }

    /** Una carrera con su facultad, ya sumada sobre la seleccion. */
    public record FilaCarrera(String facultad, String carrera, long postulaciones, long adjudicadas) {
    }

    /** Un tramo de puntaje (0 a 6, de 10 puntos) con su cantidad. */
    public record FilaTramo(int tramo, long cantidad) {
    }

    /** Asistencia sumada de un dia de la semana (ISO: 1 lunes ... 7 domingo). */
    public record FilaDiaSemana(int dia, long asistieron, long esperados) {
    }

    /** Asistencia total de la seleccion y los dias que tuvieron registro. */
    public record FilaAsistenciaBi(long asistieron, long esperados, long dias) {
    }

    /** Una gestion con sus totales para la serie historica. */
    public record FilaGestion(UUID codigo, String etiqueta, long postulaciones, long adjudicadas) {
    }

    /** Todas las convocatorias de la gestion, de la de apertura mas antigua a la mas reciente. */
    public List<FilaConvocatoria> convocatorias(long gestionId) {
        return jdbc.query("""
                SELECT m.convocatoria_id, c.codigo_convocatoria, m.nombre, m.cupos, m.recibidas, m.precalificadas,
                       m.puntaje_promedio, m.finalistas, m.adjudicadas, m.becas_vigentes, m.becas_suspendidas,
                       m.becas_baja, m.reemplazos, m.becas_en_riesgo
                FROM mv_bi_convocatoria m
                JOIN convocatoria c ON c.id = m.convocatoria_id AND c.eliminado_en IS NULL
                WHERE m.gestion_id = :gestion
                ORDER BY m.fecha_apertura, m.convocatoria_id
                """, new MapSqlParameterSource("gestion", gestionId), (rs, n) -> new FilaConvocatoria(
                rs.getLong("convocatoria_id"), rs.getObject("codigo_convocatoria", UUID.class),
                rs.getString("nombre"), (Integer) rs.getObject("cupos"), rs.getLong("recibidas"),
                rs.getLong("precalificadas"), decimal(rs.getBigDecimal("puntaje_promedio")),
                rs.getLong("finalistas"), rs.getLong("adjudicadas"), rs.getLong("becas_vigentes"),
                rs.getLong("becas_suspendidas"), rs.getLong("becas_baja"), rs.getLong("reemplazos"),
                rs.getLong("becas_en_riesgo")));
    }

    /** Las carreras de la seleccion con su facultad, de la que mas postula a la que menos. */
    public List<FilaCarrera> carreras(long gestionId, Long convocatoriaId) {
        return jdbc.query("""
                SELECT facultad, carrera, sum(postulaciones) AS postulaciones, sum(adjudicadas) AS adjudicadas
                FROM mv_bi_carrera_convocatoria
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                GROUP BY facultad, carrera
                ORDER BY sum(postulaciones) DESC, facultad, carrera
                """, parametros(gestionId, convocatoriaId), (rs, n) -> new FilaCarrera(rs.getString("facultad"),
                rs.getString("carrera"), rs.getLong("postulaciones"), rs.getLong("adjudicadas")));
    }

    /** Los tramos de puntaje con datos, del mas bajo al mas alto. */
    public List<FilaTramo> tramosPuntaje(long gestionId, Long convocatoriaId) {
        return jdbc.query("""
                SELECT tramo, sum(cantidad) AS cantidad
                FROM mv_bi_puntaje_convocatoria
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                GROUP BY tramo
                ORDER BY tramo
                """, parametros(gestionId, convocatoriaId),
                (rs, n) -> new FilaTramo(rs.getInt("tramo"), rs.getLong("cantidad")));
    }

    /** La asistencia agrupada por dia de la semana (solo los dias con registro). */
    public List<FilaDiaSemana> asistenciaPorDiaSemana(long gestionId, Long convocatoriaId) {
        return jdbc.query("""
                SELECT CAST(extract(isodow FROM fecha) AS integer) AS dia, sum(asistieron) AS asistieron,
                       sum(esperados) AS esperados
                FROM mv_bi_asistencia_dia
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                GROUP BY 1
                ORDER BY 1
                """, parametros(gestionId, convocatoriaId), (rs, n) -> new FilaDiaSemana(rs.getInt("dia"),
                rs.getLong("asistieron"), rs.getLong("esperados")));
    }

    /** La asistencia total de la seleccion; todo en cero si nunca se registro asistencia. */
    public FilaAsistenciaBi asistenciaTotal(long gestionId, Long convocatoriaId) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(asistieron), 0) AS asistieron, coalesce(sum(esperados), 0) AS esperados,
                       count(DISTINCT fecha) AS dias
                FROM mv_bi_asistencia_dia
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                """, parametros(gestionId, convocatoriaId), (rs, n) -> new FilaAsistenciaBi(rs.getLong("asistieron"),
                rs.getLong("esperados"), rs.getLong("dias")));
    }

    /** Todas las gestiones, de la mas antigua a la mas reciente, con sus postulaciones y adjudicadas. */
    public List<FilaGestion> gestiones() {
        return jdbc.query("""
                SELECT g.codigo_gestion_academica, g.etiqueta,
                       coalesce(sum(m.recibidas), 0) AS postulaciones, coalesce(sum(m.adjudicadas), 0) AS adjudicadas
                FROM gestion_academica g
                LEFT JOIN mv_bi_convocatoria m ON m.gestion_id = g.id
                WHERE g.eliminado_en IS NULL
                GROUP BY g.id, g.codigo_gestion_academica, g.etiqueta, g.fecha_inicio
                ORDER BY g.fecha_inicio, g.id
                """, (rs, n) -> new FilaGestion(rs.getObject("codigo_gestion_academica", UUID.class),
                rs.getString("etiqueta"), rs.getLong("postulaciones"), rs.getLong("adjudicadas")));
    }

    private static Double decimal(java.math.BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    private static MapSqlParameterSource parametros(long gestionId, Long convocatoriaId) {
        return new MapSqlParameterSource().addValue("gestion", gestionId).addValue("convocatoria", convocatoriaId,
                java.sql.Types.BIGINT);
    }
}
