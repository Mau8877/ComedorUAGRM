package com.comedoruagrm.backend.features.postulaciones.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas propias de la adjudicacion (CU05): cupos, puntajes, auditorias, lista priorizada y bloqueos.
 *
 * * Se usa JDBC porque cruza convocatoria, postulacion, finalista, adjudicacion, auditoria y puntaje sin
 * * leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee
 * * (salvo los `FOR UPDATE`): los `service` guardan por JPA. Los cupos disponibles se calculan, no se guardan.
 * ! Siempre se bloquea primero la convocatoria y despues la postulacion, nunca al reves: asi dos
 * ! decisiones simultaneas no se bloquean entre si.
 */
@Repository
@RequiredArgsConstructor
public class AdjudicacionConsultaRepository {

    /**
     * Calificadas de la convocatoria con su posicion en la lista priorizada COMPLETA; mismo orden y mismo
     * conjunto que la lista del CU06 (puntaje vigente descendente, fecha de envio ascendente con nulos al
     * final, id ascendente).
     */
    private static final String LISTA = """
            WITH base AS (
                SELECT po.id AS postulacion_id, po.codigo_postulacion,
                       ROW_NUMBER() OVER (ORDER BY pu.puntaje_total DESC, po.fecha_envio ASC NULLS LAST,
                                          po.id ASC) AS posicion,
                       EXISTS (SELECT 1 FROM finalista f
                               WHERE f.postulacion_id = po.id AND f.eliminado_en IS NULL) AS es_finalista
                FROM postulacion po
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id
                     AND pu.vigente AND pu.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = po.postulante_id AND pt.eliminado_en IS NULL
                JOIN usuario u ON u.id = pt.usuario_id AND u.eliminado_en IS NULL
                WHERE po.convocatoria_id = :convocatoria
                  AND po.estado = 'calificada' AND po.eliminado_en IS NULL
            )
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CierrePostulacionConsultaRepository cierreConsulta;

    /** Datos de la convocatoria que necesita el panel, la seleccion y la decision. */
    public record DatosConvocatoria(
            long id,
            UUID codigo,
            String nombre,
            String estado,
            Integer cupos,
            BigDecimal puntajeMinimo,
            Integer cantidadFinalistas) {
    }

    /** Un finalista con su postulacion y convocatoria, y el puntaje vigente de la IA (nulo si no hay). */
    public record CabeceraFinalista(
            long finalistaId,
            UUID finalistaCodigo,
            long postulacionId,
            UUID postulacionCodigo,
            long convocatoriaId,
            UUID convocatoriaCodigo,
            String estado,
            BigDecimal puntajeIa) {
    }

    /** La auditoria mas reciente de un finalista. */
    public record UltimaAuditoria(String resultado, BigDecimal puntajeAcademico) {
    }

    /** Una calificada que aun no es finalista, con su posicion en la lista priorizada completa. */
    public record CandidataFinalista(long postulacionId, UUID postulacionCodigo, long posicion) {
    }

    /** Vacio si la convocatoria no existe o esta eliminada. */
    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_convocatoria, nombre, CAST(estado AS text) AS estado, cupos, puntaje_minimo,
                       cantidad_finalistas
                FROM convocatoria
                WHERE codigo_convocatoria = CAST(:codigo AS uuid) AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"),
                        rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre"),
                        rs.getString("estado"), (Integer) rs.getObject("cupos"),
                        rs.getBigDecimal("puntaje_minimo"), (Integer) rs.getObject("cantidad_finalistas")))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la convocatoria hasta el fin de la transaccion: dos decisiones o selecciones
     * simultaneas se atienden una despues de la otra y no superan los cupos ni repiten finalistas.
     */
    public void bloquearConvocatoria(long convocatoriaId) {
        jdbc.queryForList("SELECT id FROM convocatoria WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", convocatoriaId), Long.class);
    }

    /** Vacio si el finalista no existe o fue eliminado. */
    public Optional<CabeceraFinalista> cabeceraFinalista(UUID finalistaCodigo) {
        return jdbc.query("""
                SELECT f.id AS finalista_id, f.codigo_finalista, po.id AS postulacion_id, po.codigo_postulacion,
                       cv.id AS convocatoria_id, cv.codigo_convocatoria, CAST(po.estado AS text) AS estado,
                       (SELECT pu.puntaje_total FROM puntaje_postulacion pu
                        WHERE pu.postulacion_id = po.id AND pu.vigente AND pu.eliminado_en IS NULL
                        LIMIT 1) AS puntaje_ia
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND f.codigo_finalista = CAST(:finalista AS uuid)
                """, new MapSqlParameterSource().addValue("finalista", finalistaCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new CabeceraFinalista(rs.getLong("finalista_id"),
                        rs.getObject("codigo_finalista", UUID.class), rs.getLong("postulacion_id"),
                        rs.getObject("codigo_postulacion", UUID.class), rs.getLong("convocatoria_id"),
                        rs.getObject("codigo_convocatoria", UUID.class), rs.getString("estado"),
                        rs.getBigDecimal("puntaje_ia")))
                .stream().findFirst();
    }

    /** La auditoria mas reciente del finalista (mismo criterio de «ultima» que el CU07); vacio si no hay. */
    public Optional<UltimaAuditoria> ultimaAuditoria(long finalistaId) {
        return jdbc.query("""
                SELECT CAST(resultado AS text) AS resultado, puntaje_academico
                FROM auditoria_expediente
                WHERE finalista_id = :finalista AND eliminado_en IS NULL
                ORDER BY fecha_auditoria DESC, id DESC
                LIMIT 1
                """, new MapSqlParameterSource("finalista", finalistaId),
                (rs, n) -> new UltimaAuditoria(rs.getString("resultado"), rs.getBigDecimal("puntaje_academico")))
                .stream().findFirst();
    }

    /** Bloquea la fila de la postulacion hasta el fin de la transaccion (siempre despues de la convocatoria). */
    public void bloquearPostulacion(long postulacionId) {
        jdbc.queryForList("SELECT id FROM postulacion WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", postulacionId), Long.class);
    }

    /** El estado actual de la postulacion (se vuelve a leer tras el bloqueo). */
    public String estadoActual(long postulacionId) {
        return jdbc.queryForObject("SELECT CAST(estado AS text) FROM postulacion WHERE id = :id",
                new MapSqlParameterSource("id", postulacionId), String.class);
    }

    /** Adjudicaciones (`ADJUDICADA`) de las postulaciones de la convocatoria. */
    public long contarAdjudicadas(long convocatoriaId) {
        return contarPorResultado(convocatoriaId, "ADJUDICADA");
    }

    /** Decisiones finales de no adjudicar (`NO_ADJUDICADA`) de las postulaciones de la convocatoria. */
    public long contarNoAdjudicadas(long convocatoriaId) {
        return contarPorResultado(convocatoriaId, "NO_ADJUDICADA");
    }

    private long contarPorResultado(long convocatoriaId, String resultado) {
        Long total = jdbc.queryForObject("""
                SELECT count(*)
                FROM adjudicacion a
                JOIN postulacion po ON po.id = a.postulacion_id AND po.eliminado_en IS NULL
                WHERE po.convocatoria_id = :convocatoria AND a.eliminado_en IS NULL
                  AND a.resultado = CAST(:resultado AS resultado_adjudicacion)
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                .addValue("resultado", resultado, Types.VARCHAR), Long.class);
        return total == null ? 0 : total;
    }

    /** Filas de `finalista` (no eliminadas) de las postulaciones de la convocatoria. */
    public long contarFinalistas(long convocatoriaId) {
        Long total = jdbc.queryForObject("""
                SELECT count(*)
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                WHERE po.convocatoria_id = :convocatoria AND f.eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
        return total == null ? 0 : total;
    }

    /** Calificadas de la lista priorizada que todavia no son finalistas. */
    public long contarCalificadasSinFinalista(long convocatoriaId) {
        Long total = jdbc.queryForObject(LISTA + "SELECT count(*) FROM base WHERE NOT es_finalista",
                new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Las primeras `limite` calificadas que aun no son finalistas, con la posicion que ocupan en la lista
     * priorizada completa (no en la pagina), ordenadas por posicion.
     */
    public List<CandidataFinalista> siguientesSinFinalista(long convocatoriaId, int limite) {
        return jdbc.query(LISTA + """
                SELECT postulacion_id, codigo_postulacion, posicion FROM base
                WHERE NOT es_finalista ORDER BY posicion LIMIT :limite
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                .addValue("limite", limite),
                (rs, n) -> new CandidataFinalista(rs.getLong("postulacion_id"),
                        rs.getObject("codigo_postulacion", UUID.class), rs.getLong("posicion")));
    }

    /**
     * Si la convocatoria ya tiene sus resultados publicados. Reutiliza el criterio del CU20, que lee la marca
     * real del CU22 (la fila de `publicacion_resultado`), una sola vez.
     */
    public boolean resultadosPublicados(long convocatoriaId) {
        return cierreConsulta.resultadosPublicados(convocatoriaId);
    }
}
