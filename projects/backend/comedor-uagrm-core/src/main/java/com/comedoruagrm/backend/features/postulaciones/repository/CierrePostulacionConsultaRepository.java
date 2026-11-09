package com.comedoruagrm.backend.features.postulaciones.repository;

import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas propias del cierre de una postulacion, archivar o rechazar (CU20).
 *
 * * Se usa JDBC porque cruza finalista, postulacion, usuario y convocatoria sin leer el repository de
 * * otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee: el `service` guarda por JPA.
 */
@Repository
@RequiredArgsConstructor
public class CierrePostulacionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Lo que el `service` necesita saber de un finalista y su postulacion. */
    public record Cabecera(
            long postulacionId,
            UUID postulacionCodigo,
            long finalistaId,
            UUID finalistaCodigo,
            long convocatoriaId,
            String estado,
            UUID postulanteUsuarioCodigo,
            String postulanteNombre,
            String convocatoriaNombre) {
    }

    /** Vacio si el finalista no existe o fue eliminado. */
    public Optional<Cabecera> cabecera(UUID finalista) {
        return jdbc.query("""
                SELECT po.id AS postulacion_id, po.codigo_postulacion, f.id AS finalista_id, f.codigo_finalista,
                       cv.id AS convocatoria_id, CAST(po.estado AS text) AS estado, u.codigo_usuario,
                       u.nombres || ' ' || u.apellidos AS postulante_nombre, cv.nombre AS convocatoria_nombre
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND f.codigo_finalista = CAST(:finalista AS uuid)
                """, new MapSqlParameterSource().addValue("finalista", finalista.toString(), Types.VARCHAR),
                (rs, n) -> new Cabecera(
                        rs.getLong("postulacion_id"), rs.getObject("codigo_postulacion", UUID.class),
                        rs.getLong("finalista_id"), rs.getObject("codigo_finalista", UUID.class),
                        rs.getLong("convocatoria_id"), rs.getString("estado"),
                        rs.getObject("codigo_usuario", UUID.class), rs.getString("postulante_nombre"),
                        rs.getString("convocatoria_nombre")))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la postulacion hasta el fin de la transaccion: dos decisiones simultaneas se
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
     * Si la convocatoria ya tiene sus resultados publicados.
     *
     * TODO: el CU22 (publicar resultados) todavia no existe y no hay donde leer la marca de publicacion;
     * TODO: hasta entonces devuelve siempre `false`. Al implementar el CU22, leerla aqui.
     */
    public boolean resultadosPublicados(long convocatoriaId) {
        return false;
    }
}
