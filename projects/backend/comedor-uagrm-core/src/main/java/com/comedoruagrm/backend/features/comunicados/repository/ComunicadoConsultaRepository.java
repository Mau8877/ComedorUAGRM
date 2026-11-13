package com.comedoruagrm.backend.features.comunicados.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.comunicados.dto.ComunicadoResponse;
import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura de comunicados (CU21): la convocatoria, quienes son los destinatarios y el
 * listado de lo publicado. JDBC porque cruza usuario, postulacion, finalista y notificacion; todo respeta
 * el borrado logico.
 */
@Repository
@RequiredArgsConstructor
public class ComunicadoConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** `id` interno de la convocatoria con ese codigo publico; vacio si no existe o esta eliminada. */
    public Optional<Long> buscarConvocatoriaId(UUID codigo) {
        return jdbc.query("""
                SELECT id FROM convocatoria
                WHERE codigo_convocatoria = CAST(:convocatoria AS uuid) AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("convocatoria", codigo.toString(), Types.VARCHAR),
                (rs, n) -> rs.getLong("id")).stream().findFirst();
    }

    /**
     * `id` de los usuarios activos que reciben el comunicado. `TODOS`: las cuentas de Postulante (si hay
     * convocatoria, solo quienes tienen una postulacion enviada en ella). `FINALISTAS`: los finalistas de
     * la convocatoria (que no puede ser null). `ADJUDICADOS`: los finalistas con adjudicacion `ADJUDICADA` de la
     * convocatoria (que tampoco puede ser null); existen desde que se decide, no hace falta que se haya publicado.
     */
    public List<Long> destinatarios(DestinatariosComunicado tipo, Long convocatoriaId) {
        MapSqlParameterSource parametros = new MapSqlParameterSource().addValue("convocatoria", convocatoriaId);
        return switch (tipo) {
            case TODOS -> jdbc.queryForList("""
                    SELECT u.id FROM usuario u
                    JOIN rol r ON r.id = u.rol_id AND r.nombre = 'POSTULANTE' AND r.eliminado_en IS NULL
                    JOIN postulante pt ON pt.usuario_id = u.id AND pt.eliminado_en IS NULL
                    WHERE u.activo AND u.eliminado_en IS NULL
                      AND (CAST(:convocatoria AS bigint) IS NULL OR EXISTS (
                            SELECT 1 FROM postulacion po
                            WHERE po.postulante_id = u.id AND po.convocatoria_id = :convocatoria
                              AND po.estado <> 'borrador' AND po.eliminado_en IS NULL))
                    ORDER BY u.id
                    """, parametros, Long.class);
            case FINALISTAS -> jdbc.queryForList("""
                    SELECT DISTINCT u.id FROM finalista f
                    JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                    JOIN usuario u ON u.id = po.postulante_id AND u.activo AND u.eliminado_en IS NULL
                    WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                    ORDER BY u.id
                    """, parametros, Long.class);
            case ADJUDICADOS -> jdbc.queryForList("""
                    SELECT DISTINCT u.id FROM finalista f
                    JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                    JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                         AND a.resultado = 'ADJUDICADA'
                    JOIN usuario u ON u.id = po.postulante_id AND u.activo AND u.eliminado_en IS NULL
                    WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                    ORDER BY u.id
                    """, parametros, Long.class);
        };
    }

    public long contar() {
        Long total = jdbc.getJdbcTemplate().queryForObject(
                "SELECT count(*) FROM comunicado WHERE eliminado_en IS NULL", Long.class);
        return total == null ? 0 : total;
    }

    /** Un tramo de los comunicados publicados, el mas reciente primero. */
    public List<ComunicadoResponse> listar(int limite, long desplazamiento) {
        return jdbc.query("""
                SELECT c.codigo_comunicado, cv.codigo_convocatoria, c.titulo, c.contenido,
                       c.destinatarios::text AS destinatarios, c.publicado_en,
                       (SELECT count(*) FROM notificacion n
                        WHERE n.comunicado_id = c.id AND n.eliminado_en IS NULL) AS total
                FROM comunicado c
                LEFT JOIN convocatoria cv ON cv.id = c.convocatoria_id
                WHERE c.eliminado_en IS NULL
                ORDER BY c.publicado_en DESC, c.id DESC
                LIMIT :limite OFFSET :desplazamiento
                """, new MapSqlParameterSource().addValue("limite", limite)
                        .addValue("desplazamiento", desplazamiento),
                (rs, n) -> aFila(rs));
    }

    private static ComunicadoResponse aFila(ResultSet rs) throws SQLException {
        OffsetDateTime publicado = rs.getObject("publicado_en", OffsetDateTime.class);
        return new ComunicadoResponse(
                rs.getObject("codigo_comunicado", UUID.class),
                rs.getObject("codigo_convocatoria", UUID.class),
                rs.getString("titulo"),
                rs.getString("contenido"),
                DestinatariosComunicado.valueOf(rs.getString("destinatarios")),
                publicado.toInstant(),
                rs.getLong("total"));
    }
}
