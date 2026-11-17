package com.comedoruagrm.backend.features.preseleccion.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Consultas y escrituras del reprocesamiento de puntajes (CU26). JDBC porque cruza `convocatoria`, `postulacion`,
 * `puntaje_postulacion`, `publicacion_resultado` y `lote_reprocesamiento` sin leer repositories de otros modulos.
 * Respeta el borrado logico.
 */
@Repository
@RequiredArgsConstructor
public class ReprocesamientoConsultaRepository {

    private static final String LOTE = """
            SELECT l.id, l.codigo_lote_reprocesamiento, l.convocatoria_id, c.codigo_convocatoria, l.modelo_id, m.version, l.estado,
                   l.total_postulaciones, l.procesadas, l.fallidas, l.iniciado_en, l.terminado_en
            FROM lote_reprocesamiento l
            JOIN convocatoria c ON c.id = l.convocatoria_id
            JOIN modelo_preseleccion m ON m.id = l.modelo_id
            WHERE l.eliminado_en IS NULL""";

    private final NamedParameterJdbcTemplate jdbc;

    /** La convocatoria a reprocesar. */
    public record ConvocatoriaDatos(long id, UUID codigo, String nombre, String gestion) {
    }

    /** Un lote de reprocesamiento con el codigo de su convocatoria y la version del modelo. */
    public record LoteFila(long id, UUID codigo, long convocatoriaId, UUID convocatoriaCodigo, long modeloId, String modeloVersion,
            String estado, int total, int procesadas, int fallidas, Instant iniciadoEn, Instant terminadoEn) {
    }

    private static LoteFila lote(ResultSet rs) throws SQLException {
        var inicio = rs.getTimestamp("iniciado_en");
        var fin = rs.getTimestamp("terminado_en");
        return new LoteFila(rs.getLong("id"), rs.getObject("codigo_lote_reprocesamiento", UUID.class),
                rs.getLong("convocatoria_id"), rs.getObject("codigo_convocatoria", UUID.class), rs.getLong("modelo_id"), rs.getString("version"),
                rs.getString("estado"), rs.getInt("total_postulaciones"), rs.getInt("procesadas"),
                rs.getInt("fallidas"), inicio == null ? null : inicio.toInstant(),
                fin == null ? null : fin.toInstant());
    }

    public Optional<ConvocatoriaDatos> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT c.id, c.codigo_convocatoria, c.nombre, g.etiqueta
                FROM convocatoria c JOIN gestion_academica g ON g.id = c.gestion_id
                WHERE c.eliminado_en IS NULL AND c.codigo_convocatoria = CAST(:codigo AS uuid)
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new ConvocatoriaDatos(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getString("etiqueta"))).stream().findFirst();
    }

    /** Si ya se publicaron los resultados de la convocatoria (CU22): desde ahi no se reprocesa (A3). */
    public boolean resultadosPublicados(long convocatoriaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM publicacion_resultado
                               WHERE convocatoria_id = :id AND eliminado_en IS NULL)
                """, new MapSqlParameterSource("id", convocatoriaId), Boolean.class));
    }

    /** Las postulaciones `calificada` de la convocatoria con respuestas. */
    public int calificadas(long convocatoriaId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM postulacion po
                JOIN respuesta_socioeconomica r ON r.postulacion_id = po.id AND r.eliminado_en IS NULL
                WHERE po.convocatoria_id = :id AND po.eliminado_en IS NULL AND po.estado = 'calificada'
                """, new MapSqlParameterSource("id", convocatoriaId), Integer.class);
        return n == null ? 0 : n;
    }

    /**
     * Las `calificada` que todavia no tienen un puntaje vigente de esa version del modelo, las mas antiguas primero.
     * Es lo que hace idempotente el reprocesamiento: repetirlo toma solo las que faltan (las que fallaron).
     */
    public List<Long> porReprocesar(long convocatoriaId, long modeloId) {
        return jdbc.queryForList("""
                SELECT po.id FROM postulacion po
                JOIN respuesta_socioeconomica r ON r.postulacion_id = po.id AND r.eliminado_en IS NULL
                WHERE po.convocatoria_id = :convocatoria AND po.eliminado_en IS NULL AND po.estado = 'calificada'
                  AND NOT EXISTS (SELECT 1 FROM puntaje_postulacion pp
                                  WHERE pp.postulacion_id = po.id AND pp.vigente AND pp.modelo_id = :modelo
                                    AND pp.eliminado_en IS NULL)
                ORDER BY po.fecha_envio NULLS LAST, po.id
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                        .addValue("modelo", modeloId), Long.class);
    }

    /** Crea el lote `PENDIENTE`. Si ya hay uno en curso para la convocatoria viola el indice unico (A4). */
    public LoteFila crearLote(long convocatoriaId, long modeloId, long solicitadoPor, int total) {
        Long id = jdbc.queryForObject("""
                INSERT INTO lote_reprocesamiento (convocatoria_id, modelo_id, solicitado_por, estado, total_postulaciones)
                VALUES (:convocatoria, :modelo, :solicitadoPor, 'PENDIENTE', :total) RETURNING id
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                        .addValue("modelo", modeloId).addValue("solicitadoPor", solicitadoPor)
                        .addValue("total", total), Long.class);
        return loteDe(id).orElseThrow();
    }

    public Optional<LoteFila> loteDe(long id) {
        return jdbc.query(LOTE + " AND l.id = :id", new MapSqlParameterSource("id", id), (rs, n) -> lote(rs))
                .stream().findFirst();
    }

    public Optional<LoteFila> lote(UUID codigo) {
        return jdbc.query(LOTE + " AND l.codigo_lote_reprocesamiento = CAST(:codigo AS uuid)",
                new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> lote(rs)).stream().findFirst();
    }

    /** El lote `PENDIENTE` o `EN_PROCESO` de la convocatoria, si hay. */
    public Optional<LoteFila> loteEnCurso(long convocatoriaId) {
        return jdbc.query(LOTE + " AND l.convocatoria_id = :id AND l.estado IN ('PENDIENTE', 'EN_PROCESO')",
                new MapSqlParameterSource("id", convocatoriaId), (rs, n) -> lote(rs)).stream().findFirst();
    }

    public void marcarEnProceso(long loteId, int total) {
        jdbc.update("""
                UPDATE lote_reprocesamiento SET estado = 'EN_PROCESO', iniciado_en = now(),
                       total_postulaciones = :total, actualizado_en = now() WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", loteId).addValue("total", total));
    }

    /** Suma una postulacion procesada y, si fallo, una fallida. */
    public void sumarAvance(long loteId, boolean fallida) {
        jdbc.update("""
                UPDATE lote_reprocesamiento SET procesadas = procesadas + 1, fallidas = fallidas + :fallida,
                       actualizado_en = now() WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", loteId).addValue("fallida", fallida ? 1 : 0));
    }

    public void terminar(long loteId, String estado) {
        jdbc.update("""
                UPDATE lote_reprocesamiento SET estado = :estado, terminado_en = now(), actualizado_en = now()
                WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", loteId).addValue("estado", estado));
    }

    /** Al arrancar: un lote que quedo en curso con el backend apagado ya no avanza; se marca `FALLIDO`. */
    public int marcarHuerfanos() {
        return jdbc.update("""
                UPDATE lote_reprocesamiento SET estado = 'FALLIDO', terminado_en = now(), actualizado_en = now()
                WHERE estado IN ('PENDIENTE', 'EN_PROCESO') AND eliminado_en IS NULL
                """, new MapSqlParameterSource());
    }
}
