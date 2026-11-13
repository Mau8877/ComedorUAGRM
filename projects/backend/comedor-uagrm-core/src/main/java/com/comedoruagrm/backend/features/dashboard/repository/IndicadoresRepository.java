package com.comedoruagrm.backend.features.dashboard.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas del dashboard de indicadores (CU12) sobre la vista `mv_indicadores_convocatoria`.
 *
 * * Los numeros salen solo de la vista: nunca se leen `postulacion`, `finalista` ni `adjudicacion` aqui. Las
 * * unicas tablas que se tocan son `gestion_academica` y `convocatoria`, para resolver los codigos publicos.
 * * Cada consulta es una sola sentencia; respeta el borrado logico en las tablas que lee.
 */
@Repository
@RequiredArgsConstructor
public class IndicadoresRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Una gestion academica resuelta por su codigo publico. */
    public record GestionDatos(long id, UUID codigo, String etiqueta) {
    }

    /** Una convocatoria resuelta por su codigo publico, con los datos de su gestion. */
    public record ConvocatoriaDatos(long id, UUID codigo, String nombre, GestionDatos gestion) {
    }

    /** Un mes de la serie; `mes` es el primer dia del mes. */
    public record FilaMensual(LocalDate mes, long postulaciones, long finalistas, long adjudicadas) {
    }

    /** Totales de una gestion o convocatoria. */
    public record TotalesFila(long postulaciones, long finalistas, long adjudicadas) {
    }

    public Optional<GestionDatos> gestion(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_gestion_academica, etiqueta
                FROM gestion_academica
                WHERE codigo_gestion_academica = :codigo AND eliminado_en IS NULL
                """, new MapSqlParameterSource("codigo", codigo), (rs, n) -> new GestionDatos(rs.getLong("id"),
                rs.getObject("codigo_gestion_academica", UUID.class), rs.getString("etiqueta"))).stream().findFirst();
    }

    public Optional<ConvocatoriaDatos> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT c.id, c.codigo_convocatoria, c.nombre,
                       g.id AS gestion_id, g.codigo_gestion_academica, g.etiqueta
                FROM convocatoria c
                JOIN gestion_academica g ON g.id = c.gestion_id AND g.eliminado_en IS NULL
                WHERE c.codigo_convocatoria = :codigo AND c.eliminado_en IS NULL
                """, new MapSqlParameterSource("codigo", codigo), (rs, n) -> convocatoriaDe(rs)).stream()
                .findFirst();
    }

    private static ConvocatoriaDatos convocatoriaDe(ResultSet rs) throws SQLException {
        GestionDatos gestion = new GestionDatos(rs.getLong("gestion_id"),
                rs.getObject("codigo_gestion_academica", UUID.class), rs.getString("etiqueta"));
        return new ConvocatoriaDatos(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                rs.getString("nombre"), gestion);
    }

    /** Los totales de la gestion; con `convocatoriaId` no nulo, solo los de esa convocatoria. */
    public TotalesFila totales(long gestionId, Long convocatoriaId) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(postulaciones_recibidas), 0) AS postulaciones,
                       coalesce(sum(finalistas), 0) AS finalistas,
                       coalesce(sum(adjudicadas), 0) AS adjudicadas
                FROM mv_indicadores_convocatoria
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                """, parametros(gestionId, convocatoriaId), (rs, n) -> new TotalesFila(rs.getLong("postulaciones"),
                rs.getLong("finalistas"), rs.getLong("adjudicadas")));
    }

    /** Las filas por mes, de la mas antigua a la mas reciente, solo de los meses con datos. */
    public List<FilaMensual> serieMensual(long gestionId, Long convocatoriaId) {
        return jdbc.query("""
                SELECT mes, sum(postulaciones_recibidas) AS postulaciones, sum(finalistas) AS finalistas,
                       sum(adjudicadas) AS adjudicadas
                FROM mv_indicadores_convocatoria
                WHERE gestion_id = :gestion AND (CAST(:convocatoria AS bigint) IS NULL OR convocatoria_id = :convocatoria)
                GROUP BY mes
                ORDER BY mes
                """, parametros(gestionId, convocatoriaId), (rs, n) -> new FilaMensual(
                rs.getObject("mes", LocalDate.class), rs.getLong("postulaciones"), rs.getLong("finalistas"),
                rs.getLong("adjudicadas")));
    }

    /** Cuando se calculo la vista por ultima vez; vacio si el lote nunca se ejecuto. */
    public Optional<Instant> ultimaActualizacion() {
        String consulta = "SELECT max(calculado_en) FROM mv_indicadores_convocatoria";
        Timestamp t = jdbc.getJdbcOperations().queryForObject(consulta, Timestamp.class);
        return Optional.ofNullable(t).map(Timestamp::toInstant);
    }

    private static MapSqlParameterSource parametros(long gestionId, Long convocatoriaId) {
        return new MapSqlParameterSource().addValue("gestion", gestionId).addValue("convocatoria", convocatoriaId,
                java.sql.Types.BIGINT);
    }
}
