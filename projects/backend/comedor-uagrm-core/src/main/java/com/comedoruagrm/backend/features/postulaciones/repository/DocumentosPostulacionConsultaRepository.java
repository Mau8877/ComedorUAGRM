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
 * Consultas de los documentos de una postulacion (CU16).
 *
 * * Se usa JDBC porque cruza postulacion, usuario, convocatoria, tipo de documento y documento adjunto sin
 * * leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * * La postulacion se busca SIEMPRE junto con el usuario del token: una ajena responde igual que una inexistente.
 */
@Repository
@RequiredArgsConstructor
public class DocumentosPostulacionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** La postulacion del postulante y el estado de su convocatoria. */
    public record Cabecera(long postulacionId, UUID postulacionCodigo, String estado, long convocatoriaId,
            String convocatoriaEstado, Instant convocatoriaCierre) {
    }

    /** Un documento que exige la convocatoria. */
    public record Requerido(long tipoDocumentoId, UUID tipoDocumentoCodigo, String nombre, String descripcion,
            boolean obligatorio) {
    }

    /** Un archivo ya adjuntado (sin su ruta de almacenamiento). */
    public record Adjunto(UUID codigo, long tipoDocumentoId, String nombreArchivo, String mimeType, long tamanoBytes,
            Instant subidoEn) {
    }

    /** Vacio si la postulacion no existe, esta eliminada o no es de ese usuario. */
    public Optional<Cabecera> cabecera(UUID postulacionCodigo, UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT po.id, po.codigo_postulacion, CAST(po.estado AS text) AS estado, c.id AS convocatoria_id,
                       CAST(c.estado AS text) AS convocatoria_estado, c.fecha_cierre
                FROM postulacion po
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
                WHERE po.eliminado_en IS NULL
                  AND po.codigo_postulacion = CAST(:postulacion AS uuid)
                  AND u.codigo_usuario = CAST(:usuario AS uuid)
                """, new MapSqlParameterSource().addValue("postulacion", postulacionCodigo.toString(), Types.VARCHAR)
                        .addValue("usuario", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new Cabecera(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class),
                        rs.getString("estado"), rs.getLong("convocatoria_id"), rs.getString("convocatoria_estado"),
                        rs.getTimestamp("fecha_cierre").toInstant()))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la postulacion hasta el fin de la transaccion: dos subidas simultaneas de la misma
     * postulacion se atienden una despues de la otra y la segunda ve el conteo ya actualizado.
     */
    public void bloquearPostulacion(long postulacionId) {
        jdbc.queryForList("SELECT id FROM postulacion WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", postulacionId), Long.class);
    }

    /** Los documentos que exige la convocatoria, ordenados por nombre. */
    public List<Requerido> requeridos(long convocatoriaId) {
        return jdbc.query("""
                SELECT td.id, td.codigo_tipo_documento, td.nombre, td.descripcion, cd.obligatorio
                FROM convocatoria_documento cd
                JOIN tipo_documento td ON td.id = cd.tipo_documento_id AND td.eliminado_en IS NULL
                WHERE cd.eliminado_en IS NULL AND cd.convocatoria_id = :convocatoria
                ORDER BY td.nombre, td.id
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Requerido(rs.getLong("id"), rs.getObject("codigo_tipo_documento", UUID.class),
                        rs.getString("nombre"), rs.getString("descripcion"), rs.getBoolean("obligatorio")));
    }

    /** El id interno del tipo de documento si la convocatoria lo exige; vacio si no existe o no figura en su lista. */
    public Optional<Long> tipoRequerido(long convocatoriaId, UUID tipoDocumentoCodigo) {
        return jdbc.queryForList("""
                SELECT td.id
                FROM convocatoria_documento cd
                JOIN tipo_documento td ON td.id = cd.tipo_documento_id AND td.eliminado_en IS NULL
                WHERE cd.eliminado_en IS NULL AND cd.convocatoria_id = :convocatoria
                  AND td.codigo_tipo_documento = CAST(:tipo AS uuid)
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                        .addValue("tipo", tipoDocumentoCodigo.toString(), Types.VARCHAR), Long.class)
                .stream().findFirst();
    }

    /** Los archivos vigentes de la postulacion, del mas antiguo al mas reciente. */
    public List<Adjunto> adjuntos(long postulacionId) {
        return jdbc.query("""
                SELECT codigo_documento_adjunto, tipo_documento_id, nombre_archivo, mime_type, tamano_bytes, subido_en
                FROM documento_adjunto
                WHERE eliminado_en IS NULL AND postulacion_id = :postulacion
                ORDER BY subido_en, id
                """, new MapSqlParameterSource("postulacion", postulacionId),
                (rs, n) -> new Adjunto(rs.getObject("codigo_documento_adjunto", UUID.class),
                        rs.getLong("tipo_documento_id"), rs.getString("nombre_archivo"), rs.getString("mime_type"),
                        rs.getLong("tamano_bytes"), rs.getTimestamp("subido_en").toInstant()));
    }
}
