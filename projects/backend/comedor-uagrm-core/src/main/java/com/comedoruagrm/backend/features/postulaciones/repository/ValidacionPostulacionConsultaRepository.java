package com.comedoruagrm.backend.features.postulaciones.repository;

import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura de la validacion de elegibilidad y duplicados (CU18).
 *
 * * Se usa JDBC porque cruza usuario, postulante, convocatoria, gestion y postulacion sin leer el
 * * repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 */
@Repository
@RequiredArgsConstructor
public class ValidacionPostulacionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** El postulante de una cuenta (`postulante.usuario_id` es el `id` del usuario). */
    public record DatosPostulante(long id, boolean activo) {
    }

    public record DatosConvocatoria(long id, long gestionId, String gestionEtiqueta, String estado,
            Instant fechaApertura, Instant fechaCierre) {
    }

    public record PostulacionExistente(long id, UUID codigo, long convocatoriaId, String estado,
            Instant fechaEnvio) {
    }

    /** Vacio si la cuenta no existe, esta eliminada o no es de un postulante (no tiene fila en `postulante`). */
    public Optional<DatosPostulante> postulanteDe(UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT u.id, u.activo
                FROM usuario u
                JOIN postulante p ON p.usuario_id = u.id AND p.eliminado_en IS NULL
                WHERE u.eliminado_en IS NULL AND u.codigo_usuario = CAST(:codigo AS uuid)
                """, new MapSqlParameterSource().addValue("codigo", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosPostulante(rs.getLong("id"), rs.getBoolean("activo")))
                .stream().findFirst();
    }

    /** Vacio si la convocatoria no existe o fue eliminada. */
    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT c.id, c.gestion_id, g.etiqueta, CAST(c.estado AS text) AS estado,
                       c.fecha_apertura, c.fecha_cierre
                FROM convocatoria c
                JOIN gestion_academica g ON g.id = c.gestion_id
                WHERE c.eliminado_en IS NULL AND c.codigo_convocatoria = CAST(:codigo AS uuid)
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getLong("gestion_id"),
                        rs.getString("etiqueta"), rs.getString("estado"),
                        rs.getTimestamp("fecha_apertura").toInstant(), rs.getTimestamp("fecha_cierre").toInstant()))
                .stream().findFirst();
    }

    /** Todas las postulaciones (de cualquier estado) del postulante en las convocatorias de la gestion. */
    public List<PostulacionExistente> postulacionesEnLaGestion(long postulanteId, long gestionId) {
        return jdbc.query("""
                SELECT po.id, po.codigo_postulacion, po.convocatoria_id, CAST(po.estado AS text) AS estado,
                       po.fecha_envio
                FROM postulacion po
                JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
                WHERE po.eliminado_en IS NULL AND po.postulante_id = :postulante AND c.gestion_id = :gestion
                ORDER BY po.fecha_envio NULLS LAST, po.id
                """, new MapSqlParameterSource().addValue("postulante", postulanteId)
                        .addValue("gestion", gestionId),
                (rs, n) -> {
                    var envio = rs.getTimestamp("fecha_envio");
                    return new PostulacionExistente(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class),
                            rs.getLong("convocatoria_id"), rs.getString("estado"),
                            envio == null ? null : envio.toInstant());
                });
    }
}
