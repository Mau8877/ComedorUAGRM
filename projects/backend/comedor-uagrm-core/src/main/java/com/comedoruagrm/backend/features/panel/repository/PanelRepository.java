package com.comedoruagrm.backend.features.panel.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Conteos de las pantallas de inicio (Administrador DUBSS y Personal del Comedor). Solo lee y solo cuenta: no
 * devuelve ningún dato personal. Respeta el borrado lógico (`eliminado_en IS NULL`).
 */
@Repository
@RequiredArgsConstructor
public class PanelRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** La convocatoria abierta que cierra primero. */
    public record ConvocatoriaAbiertaFila(UUID codigo, String nombre, Instant fechaCierre, Integer cupos,
            long postulaciones) {
    }

    /** Asistencias de una fecha. */
    public record AsistenciaFecha(LocalDate fecha, long asistencias) {
    }

    /** El estado de la postulación del postulante en la convocatoria abierta, si ya empezó una. */
    public Optional<String> estadoEnConvocatoriaAbierta(long postulanteId) {
        return jdbc.query("""
                SELECT p.estado::text AS estado
                FROM postulacion p
                JOIN convocatoria c ON c.id = p.convocatoria_id AND c.estado = 'ABIERTA' AND c.eliminado_en IS NULL
                WHERE p.postulante_id = :postulante AND p.eliminado_en IS NULL
                ORDER BY p.id DESC
                LIMIT 1
                """, new MapSqlParameterSource("postulante", postulanteId), (rs, n) -> rs.getString("estado"))
                .stream().findFirst();
    }

    public long convocatoriasAbiertas() {
        return jdbc.getJdbcTemplate().queryForObject(
                "SELECT count(*) FROM convocatoria WHERE estado = 'ABIERTA' AND eliminado_en IS NULL", Long.class);
    }

    public long postulacionesEnEstado(String estado) {
        return jdbc.queryForObject("""
                SELECT count(*)
                FROM postulacion p
                JOIN convocatoria c ON c.id = p.convocatoria_id AND c.eliminado_en IS NULL
                WHERE p.estado = CAST(:estado AS estado_postulacion) AND p.eliminado_en IS NULL
                """, new MapSqlParameterSource("estado", estado), Long.class);
    }

    public long becasEnEstado(String estado) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM beca
                WHERE estado = CAST(:estado AS estado_beca) AND eliminado_en IS NULL
                """, new MapSqlParameterSource("estado", estado), Long.class);
    }

    public long asistenciasDelDia(LocalDate fecha) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM asistencia WHERE fecha = :fecha AND eliminado_en IS NULL
                """, new MapSqlParameterSource("fecha", fecha), Long.class);
    }

    public long asistenciasDelDiaRegistradasPor(LocalDate fecha, long usuarioId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM asistencia
                WHERE fecha = :fecha AND registrado_por = :usuario AND eliminado_en IS NULL
                """, new MapSqlParameterSource("fecha", fecha).addValue("usuario", usuarioId), Long.class);
    }

    public Optional<Instant> ultimoRegistroDelDia(LocalDate fecha) {
        OffsetDateTime ultimo = jdbc.queryForObject("""
                SELECT max(hora_registro) FROM asistencia WHERE fecha = :fecha AND eliminado_en IS NULL
                """, new MapSqlParameterSource("fecha", fecha), OffsetDateTime.class);
        return Optional.ofNullable(ultimo).map(OffsetDateTime::toInstant);
    }

    public Optional<ConvocatoriaAbiertaFila> convocatoriaAbierta() {
        return jdbc.query("""
                SELECT c.codigo_convocatoria, c.nombre, c.fecha_cierre, c.cupos,
                       (SELECT count(*) FROM postulacion p
                        WHERE p.convocatoria_id = c.id AND p.estado <> 'borrador' AND p.eliminado_en IS NULL)
                           AS postulaciones
                FROM convocatoria c
                WHERE c.estado = 'ABIERTA' AND c.eliminado_en IS NULL
                ORDER BY c.fecha_cierre
                LIMIT 1
                """, (rs, n) -> convocatoriaDe(rs)).stream().findFirst();
    }

    /** Asistencias por fecha en el rango (solo las fechas con registros). */
    public List<AsistenciaFecha> asistenciasPorFecha(LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                SELECT fecha, count(*) AS asistencias
                FROM asistencia
                WHERE fecha BETWEEN :desde AND :hasta AND eliminado_en IS NULL
                GROUP BY fecha
                """, new MapSqlParameterSource("desde", desde).addValue("hasta", hasta),
                (rs, n) -> new AsistenciaFecha(rs.getObject("fecha", LocalDate.class), rs.getLong("asistencias")));
    }

    private static ConvocatoriaAbiertaFila convocatoriaDe(ResultSet rs) throws SQLException {
        return new ConvocatoriaAbiertaFila(rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre"),
                rs.getObject("fecha_cierre", OffsetDateTime.class).toInstant(), (Integer) rs.getObject("cupos"),
                rs.getLong("postulaciones"));
    }
}
