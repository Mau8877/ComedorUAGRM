package com.comedoruagrm.backend.features.postulaciones.repository;

import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas propias de la devolucion de una postulacion (CU19).
 *
 * * Se usa JDBC porque cruza finalista, postulacion, usuario, convocatoria, auditoria e historial
 * * sin leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 */
@Repository
@RequiredArgsConstructor
public class DevolucionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Lo que el `service` necesita saber de un finalista y su postulacion. */
    public record Cabecera(
            long finalistaId,
            UUID finalistaCodigo,
            long postulacionId,
            UUID postulacionCodigo,
            String estado,
            UUID postulanteCodigo,
            String postulanteNombre,
            String convocatoriaNombre) {
    }

    /** Vacio si el finalista no existe o fue eliminado. */
    public Optional<Cabecera> cabecera(UUID finalista) {
        return jdbc.query("""
                SELECT f.id AS finalista_id, f.codigo_finalista, po.id AS postulacion_id,
                       po.codigo_postulacion, CAST(po.estado AS text) AS estado,
                       u.codigo_usuario, u.nombres || ' ' || u.apellidos AS postulante_nombre,
                       cv.nombre AS convocatoria_nombre
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND f.codigo_finalista = CAST(:finalista AS uuid)
                """, new MapSqlParameterSource().addValue("finalista", finalista.toString(), Types.VARCHAR),
                (rs, n) -> new Cabecera(
                        rs.getLong("finalista_id"), rs.getObject("codigo_finalista", UUID.class),
                        rs.getLong("postulacion_id"), rs.getObject("codigo_postulacion", UUID.class),
                        rs.getString("estado"), rs.getObject("codigo_usuario", UUID.class),
                        rs.getString("postulante_nombre"), rs.getString("convocatoria_nombre")))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la postulacion hasta el fin de la transaccion: dos devoluciones simultaneas se
     * atienden una despues de la otra y la segunda ve el estado ya cambiado.
     */
    public void bloquearPostulacion(long postulacionId) {
        jdbc.queryForList("SELECT id FROM postulacion WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", postulacionId), Long.class);
    }

    /** El estado actual de la postulacion (se vuelve a leer tras el bloqueo). */
    public String estadoActual(long postulacionId) {
        return jdbc.queryForObject("SELECT CAST(estado AS text) FROM postulacion WHERE id = :id",
                new MapSqlParameterSource("id", postulacionId), String.class);
    }

    /**
     * Si el finalista tiene al menos una auditoria y, cuando la postulacion ya se devolvio antes, alguna
     * es posterior a la fecha de la ultima devolucion.
     */
    /** Si la convocatoria de la postulacion ya tiene sus resultados publicados (fila de `publicacion_resultado`, CU22). */
    public boolean resultadosPublicadosDe(long postulacionId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM postulacion po
                               JOIN publicacion_resultado pr ON pr.convocatoria_id = po.convocatoria_id
                                    AND pr.eliminado_en IS NULL
                               WHERE po.id = :id)
                """, new MapSqlParameterSource("id", postulacionId), Boolean.class));
    }

    public boolean hayAuditoriaPosteriorALaUltimaDevolucion(long finalistaId, long postulacionId) {
        Boolean hay = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM auditoria_expediente a
                    WHERE a.finalista_id = :finalista AND a.eliminado_en IS NULL
                      AND a.fecha_auditoria > COALESCE(
                          (SELECT max(h.creado_en) FROM postulacion_historial h
                           WHERE h.postulacion_id = :postulacion AND h.eliminado_en IS NULL
                             AND h.estado_nuevo = CAST('devuelta' AS estado_postulacion)),
                          '-infinity'::timestamptz))
                """, new MapSqlParameterSource().addValue("finalista", finalistaId)
                .addValue("postulacion", postulacionId), Boolean.class);
        return Boolean.TRUE.equals(hay);
    }
}
