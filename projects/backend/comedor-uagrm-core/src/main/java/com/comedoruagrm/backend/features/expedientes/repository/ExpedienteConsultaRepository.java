package com.comedoruagrm.backend.features.expedientes.repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.DocumentoExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse.RespuestaSocioeconomicaResponse;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura del expediente de un finalista (CU07).
 *
 * * Se usa JDBC porque cruza finalista, postulacion, postulante, carrera, puntaje, documentos y auditorias.
 * * Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * ! Nunca devuelve la ruta de almacenamiento (`storage_path`).
 */
@Repository
@RequiredArgsConstructor
public class ExpedienteConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Datos base del expediente: finalista, postulante, convocatoria, estado y puntaje de la IA. */
    public record Cabecera(
            long finalistaId,
            long postulacionId,
            long convocatoriaId,
            UUID finalistaCodigo,
            int posicion,
            LocalDate plazoEntrega,
            OffsetDateTime entregaRecibidaEn,
            String nombres,
            String apellidos,
            String ci,
            String carrera,
            UUID convocatoriaCodigo,
            String convocatoriaNombre,
            String estadoPostulacion,
            java.math.BigDecimal puntajeIa,
            String desgloseJson) {
    }

    public Optional<Cabecera> cabecera(UUID finalista) {
        return jdbc.query("""
                SELECT f.id AS finalista_id, po.id AS postulacion_id, cv.id AS convocatoria_id,
                       f.codigo_finalista, f.posicion, f.plazo_entrega, f.documentacion_recibida_en,
                       u.nombres, u.apellidos, u.ci, ca.nombre AS carrera,
                       cv.codigo_convocatoria, cv.nombre AS convocatoria_nombre,
                       CAST(po.estado AS text) AS estado_postulacion,
                       pu.puntaje_total, CAST(pu.detalle AS text) AS detalle
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                LEFT JOIN postulante pt ON pt.usuario_id = u.id AND pt.eliminado_en IS NULL
                LEFT JOIN carrera ca ON ca.id = pt.carrera_id
                LEFT JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                     AND pu.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND f.codigo_finalista = CAST(:finalista AS uuid)
                """, new MapSqlParameterSource().addValue("finalista", finalista.toString(), Types.VARCHAR),
                (rs, n) -> new Cabecera(
                        rs.getLong("finalista_id"), rs.getLong("postulacion_id"), rs.getLong("convocatoria_id"),
                        rs.getObject("codigo_finalista", UUID.class), rs.getInt("posicion"),
                        rs.getObject("plazo_entrega", LocalDate.class),
                        rs.getObject("documentacion_recibida_en", OffsetDateTime.class),
                        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"),
                        rs.getString("carrera"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("convocatoria_nombre"), rs.getString("estado_postulacion"),
                        rs.getBigDecimal("puntaje_total"), rs.getString("detalle")))
                .stream().findFirst();
    }

    /** `codigo` publico del usuario dueño de la postulacion (el postulante a avisar). */
    public Optional<UUID> codigoPostulante(long postulacionId) {
        return jdbc.query("""
                SELECT u.codigo_usuario FROM postulacion po
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE po.id = :postulacion
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId),
                (rs, n) -> rs.getObject("codigo_usuario", UUID.class)).stream().findFirst();
    }

    /** Ruta de almacenamiento de un documento SOLO si pertenece a esa postulacion; para firmar su acceso. */
    public Optional<String> rutaDocumento(long postulacionId, UUID documento) {
        return jdbc.query("""
                SELECT storage_path FROM documento_adjunto
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                  AND codigo_documento_adjunto = CAST(:documento AS uuid)
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId)
                        .addValue("documento", documento.toString(), Types.VARCHAR),
                (rs, n) -> rs.getString("storage_path")).stream().findFirst();
    }

    public Optional<RespuestaSocioeconomicaResponse> respuestaSocioeconomica(long postulacionId) {
        return jdbc.query("""
                SELECT ingresos_familiares, dependientes, patrimonio, procedencia, promedio_academico,
                       estado_vulnerabilidad
                FROM respuesta_socioeconomica
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId),
                (rs, n) -> new RespuestaSocioeconomicaResponse(
                        rs.getBigDecimal("ingresos_familiares"), rs.getObject("dependientes", Short.class),
                        rs.getString("patrimonio"), rs.getString("procedencia"),
                        rs.getBigDecimal("promedio_academico"), rs.getString("estado_vulnerabilidad")))
                .stream().findFirst();
    }

    /**
     * Un renglon por cada tipo que la convocatoria exige (RECIBIDO o FALTA) mas los adjuntos de tipos que no
     * exige. De cada tipo se toma el adjunto mas reciente.
     */
    public List<DocumentoExpedienteResponse> documentos(long postulacionId, long convocatoriaId) {
        return jdbc.query("""
                SELECT td.nombre AS tipo, COALESCE(cd.obligatorio, false) AS obligatorio,
                       da.codigo_documento_adjunto, da.nombre_archivo, da.mime_type, da.subido_en
                FROM tipo_documento td
                LEFT JOIN convocatoria_documento cd ON cd.tipo_documento_id = td.id
                     AND cd.convocatoria_id = :convocatoria AND cd.eliminado_en IS NULL
                LEFT JOIN LATERAL (
                    SELECT x.codigo_documento_adjunto, x.nombre_archivo, x.mime_type, x.subido_en
                    FROM documento_adjunto x
                    WHERE x.postulacion_id = :postulacion AND x.tipo_documento_id = td.id
                      AND x.eliminado_en IS NULL
                    ORDER BY x.subido_en DESC, x.id DESC LIMIT 1) da ON true
                WHERE td.eliminado_en IS NULL AND (cd.tipo_documento_id IS NOT NULL OR da.codigo_documento_adjunto IS NOT NULL)
                ORDER BY COALESCE(cd.obligatorio, false) DESC, td.nombre, td.id
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId)
                        .addValue("convocatoria", convocatoriaId),
                (rs, n) -> {
                    UUID codigo = rs.getObject("codigo_documento_adjunto", UUID.class);
                    OffsetDateTime subido = rs.getObject("subido_en", OffsetDateTime.class);
                    return new DocumentoExpedienteResponse(codigo, rs.getString("tipo"),
                            rs.getBoolean("obligatorio"), codigo == null ? "FALTA" : "RECIBIDO",
                            rs.getString("nombre_archivo"), rs.getString("mime_type"),
                            subido == null ? null : subido.toInstant());
                });
    }

    /** Auditorias del finalista, de la mas reciente a la mas antigua. */
    public List<AuditoriaResponse> auditorias(long finalistaId) {
        return jdbc.query("""
                SELECT a.codigo_auditoria_expediente, CAST(a.resultado AS text) AS resultado, a.observaciones,
                       a.puntaje_academico, a.fecha_auditoria, u.nombres || ' ' || u.apellidos AS auditor
                FROM auditoria_expediente a
                JOIN usuario u ON u.id = a.auditor_id
                WHERE a.finalista_id = :finalista AND a.eliminado_en IS NULL
                ORDER BY a.fecha_auditoria DESC, a.id DESC
                """, new MapSqlParameterSource().addValue("finalista", finalistaId),
                (rs, n) -> new AuditoriaResponse(
                        rs.getObject("codigo_auditoria_expediente", UUID.class),
                        ResultadoAuditoria.valueOf(rs.getString("resultado")), rs.getString("observaciones"),
                        rs.getBigDecimal("puntaje_academico"),
                        rs.getObject("fecha_auditoria", OffsetDateTime.class).toInstant(),
                        rs.getString("auditor")));
    }

    /**
     * Bloquea la fila del finalista hasta el fin de la transaccion: dos auditorias del mismo finalista
     * (o dos reintentos con la misma clave) se atienden una despues de la otra.
     */
    public void bloquearFinalista(long finalistaId) {
        jdbc.queryForList("SELECT id FROM finalista WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", finalistaId), Long.class);
    }
}
