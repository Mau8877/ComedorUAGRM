package com.comedoruagrm.backend.features.finalistas.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura de los finalistas (CU21), con el estado de su aviso y de su entrega.
 *
 * * El estado de la notificacion se deriva del ULTIMO aviso de la postulacion que no es un comunicado:
 * * sin aviso es SIN_ENVIAR, con `enviada_en` es ENVIADA y sin el es FALLIDA.
 * * Se usa JDBC (como la lista priorizada del CU06) porque cruza finalista, postulacion, usuario,
 * * puntaje y notificacion. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 */
@Repository
@RequiredArgsConstructor
public class FinalistaConsultaRepository {

    /** Zona en la que se calcula el dia de las fechas (Bolivia). */
    public static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    private static final String SELECT = """
            SELECT f.codigo_finalista AS codigo, f.posicion, u.nombres, u.apellidos, u.ci,
                   pu.puntaje_total AS puntaje, f.plazo_entrega, f.notificado_en, f.documentacion_recibida_en,
                   CASE WHEN n.id IS NULL THEN 'SIN_ENVIAR'
                        WHEN n.enviada_en IS NOT NULL THEN 'ENVIADA'
                        ELSE 'FALLIDA' END AS estado_notificacion
            FROM finalista f
            JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
            JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
            JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
            LEFT JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                 AND pu.eliminado_en IS NULL
            LEFT JOIN LATERAL (
                SELECT x.id, x.enviada_en FROM notificacion x
                WHERE x.postulacion_id = po.id AND x.comunicado_id IS NULL AND x.eliminado_en IS NULL
                ORDER BY x.creado_en DESC, x.id DESC LIMIT 1) n ON true
            WHERE f.eliminado_en IS NULL
            """;

    private static final String FILTRO_CONVOCATORIA = """
              AND cv.codigo_convocatoria = CAST(:convocatoria AS uuid)
              AND (CAST(:busqueda AS text) IS NULL
                   OR lower(u.nombres) LIKE :busqueda ESCAPE '\\'
                   OR lower(u.apellidos) LIKE :busqueda ESCAPE '\\'
                   OR lower(u.nombres || ' ' || u.apellidos) LIKE :busqueda ESCAPE '\\'
                   OR u.ci LIKE :busqueda ESCAPE '\\')
            """;

    private static final RowMapper<FinalistaResponse> FILA = FinalistaConsultaRepository::aFila;

    private final NamedParameterJdbcTemplate jdbc;

    /** Si la convocatoria existe y no esta eliminada. */
    public boolean existeConvocatoria(UUID codigo) {
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM convocatoria
                WHERE codigo_convocatoria = CAST(:convocatoria AS uuid) AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("convocatoria", codigo.toString(), Types.VARCHAR),
                Long.class);
        return total != null && total > 0;
    }

    /** Cuantos finalistas tiene la convocatoria con esa busqueda (`busqueda` ya viene como `%texto%` o null). */
    public long contar(UUID convocatoria, String busqueda) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM (" + SELECT + FILTRO_CONVOCATORIA + ") t",
                parametros(convocatoria, busqueda), Long.class);
        return total == null ? 0 : total;
    }

    /** Un tramo de los finalistas, ordenado por posicion. */
    public List<FinalistaResponse> buscar(UUID convocatoria, String busqueda, int limite, long desplazamiento) {
        MapSqlParameterSource parametros = parametros(convocatoria, busqueda)
                .addValue("limite", limite)
                .addValue("desplazamiento", desplazamiento);
        return jdbc.query(SELECT + FILTRO_CONVOCATORIA
                + "ORDER BY f.posicion, f.id LIMIT :limite OFFSET :desplazamiento", parametros, FILA);
    }

    /** Un finalista con sus estados al dia; vacio si no existe. */
    public Optional<FinalistaResponse> buscarUno(UUID finalista) {
        return jdbc.query(SELECT + " AND f.codigo_finalista = CAST(:finalista AS uuid)",
                new MapSqlParameterSource().addValue("finalista", finalista.toString(), Types.VARCHAR), FILA)
                .stream().findFirst();
    }

    /** `codigo` publico del usuario dueño de la postulacion (el postulante a avisar). */
    public Optional<UUID> codigoUsuarioDePostulacion(long postulacionId) {
        return jdbc.query("""
                SELECT u.codigo_usuario FROM postulacion po
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE po.id = :postulacion
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId),
                (rs, n) -> rs.getObject("codigo_usuario", UUID.class)).stream().findFirst();
    }

    private static MapSqlParameterSource parametros(UUID convocatoria, String busqueda) {
        return new MapSqlParameterSource()
                .addValue("convocatoria", convocatoria.toString(), Types.VARCHAR)
                .addValue("busqueda", busqueda, Types.VARCHAR);
    }

    private static FinalistaResponse aFila(ResultSet rs, int fila) throws SQLException {
        OffsetDateTime recibida = rs.getObject("documentacion_recibida_en", OffsetDateTime.class);
        OffsetDateTime notificada = rs.getObject("notificado_en", OffsetDateTime.class);
        Instant notificadoEn = notificada == null ? null : notificada.toInstant();
        return new FinalistaResponse(
                rs.getObject("codigo", UUID.class),
                rs.getInt("posicion"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("ci"),
                rs.getBigDecimal("puntaje"),
                rs.getString("estado_notificacion"),
                rs.getObject("plazo_entrega", LocalDate.class),
                notificadoEn,
                recibida == null ? null : recibida.toInstant().atZone(ZONA).toLocalDate());
    }
}
