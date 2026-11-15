package com.comedoruagrm.backend.features.becas.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas del padron de becados (CU23): el listado, los conteos, el alta de los adjudicados, y el siguiente
 * postulante disponible para un reemplazo.
 *
 * * Se usa JDBC porque cruza beca, usuario, postulante, carrera, adjudicacion, finalista, auditoria y puntaje sin
 * * leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Los `service`
 * * guardan por JPA; la unica escritura de aqui es el alta masiva (`INSERT ... SELECT`).
 * ! Siempre se bloquea primero la convocatoria y despues la beca, nunca al reves.
 */
@Repository
@RequiredArgsConstructor
public class PadronConsultaRepository {

    /** Las adjudicaciones `ADJUDICADA` de la convocatoria que todavia no tienen beca (ni su postulante la tiene). */
    private static final String ADJUDICADAS_SIN_BECA = """
            FROM adjudicacion ad
            JOIN postulacion po ON po.id = ad.postulacion_id
            WHERE po.convocatoria_id = :convocatoria
              AND CAST(ad.resultado AS text) = 'ADJUDICADA'
              AND ad.eliminado_en IS NULL AND po.eliminado_en IS NULL
              AND NOT EXISTS (SELECT 1 FROM beca b
                              WHERE b.eliminado_en IS NULL
                                AND (b.adjudicacion_id = ad.id
                                     OR (b.postulante_id = po.postulante_id
                                         AND b.convocatoria_id = po.convocatoria_id)))
            """;

    private static final String FILAS = """
            FROM beca b
            JOIN postulante pt ON pt.usuario_id = b.postulante_id AND pt.eliminado_en IS NULL
            JOIN usuario u ON u.id = pt.usuario_id AND u.eliminado_en IS NULL
            JOIN carrera c ON c.id = pt.carrera_id
            """;

    private static final String SELECT_FILA = """
            SELECT b.codigo_beca, pt.codigo_postulante, u.nombres, u.apellidos, u.ci, c.nombre AS carrera,
                   CAST(b.estado AS text) AS estado, b.fecha_inicio, b.fecha_fin
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /** La convocatoria con lo que necesita el padron; el puntaje minimo puede ser nulo. */
    public record DatosConvocatoria(long id, UUID codigo, String nombre, BigDecimal puntajeMinimo) {
    }

    /** Una fila del padron: la beca con el nombre, CI y carrera del becado. */
    public record FilaBeca(UUID codigo, UUID becadoCodigo, String nombres, String apellidos, String ci,
            String carrera, String estado, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    /** Una beca con su convocatoria, para decidir si se le puede cambiar el estado. */
    public record CabeceraBeca(long id, UUID codigo, long convocatoriaId, String estado) {
    }

    /** El siguiente postulante disponible para ocupar un cupo liberado. */
    public record Candidato(long postulanteId, UUID postulanteCodigo, String nombres, String apellidos, String ci,
            String carrera, int posicion, BigDecimal puntajeTotal) {
    }

    // ---- Convocatoria y publicacion ----

    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_convocatoria, nombre, puntaje_minimo FROM convocatoria
                WHERE codigo_convocatoria = :codigo AND eliminado_en IS NULL
                """, new MapSqlParameterSource("codigo", codigo),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getBigDecimal("puntaje_minimo")))
                .stream().findFirst();
    }

    public Optional<DatosConvocatoria> convocatoriaPorId(long id) {
        return jdbc.query("""
                SELECT id, codigo_convocatoria, nombre, puntaje_minimo FROM convocatoria
                WHERE id = :id AND eliminado_en IS NULL
                """, new MapSqlParameterSource("id", id),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getBigDecimal("puntaje_minimo")))
                .stream().findFirst();
    }

    /** Cuando se publicaron los resultados; vacio si todavia no. */
    public Optional<Instant> fechaPublicacion(long convocatoriaId) {
        return jdbc.query("""
                SELECT publicado_en FROM publicacion_resultado
                WHERE convocatoria_id = :convocatoria AND eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> rs.getTimestamp("publicado_en").toInstant()).stream().findFirst();
    }

    /** Bloquea la fila de la convocatoria hasta el fin de la transaccion (siempre antes que la beca). */
    public void bloquearConvocatoria(long convocatoriaId) {
        jdbc.queryForList("SELECT id FROM convocatoria WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", convocatoriaId), Long.class);
    }

    // ---- Conteos ----

    public long contarAdjudicadas(long convocatoriaId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM adjudicacion ad JOIN postulacion po ON po.id = ad.postulacion_id
                WHERE po.convocatoria_id = :convocatoria AND CAST(ad.resultado AS text) = 'ADJUDICADA'
                  AND ad.eliminado_en IS NULL AND po.eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
    }

    public long contarAdjudicadasSinBeca(long convocatoriaId) {
        return jdbc.queryForObject("SELECT count(*) " + ADJUDICADAS_SIN_BECA,
                new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
    }

    /** Las becas de la convocatoria por estado (`VIGENTE`, `SUSPENDIDA`, `BAJA`); lo que no hay no aparece. */
    public Map<String, Long> contarPorEstado(long convocatoriaId) {
        Map<String, Long> conteos = new HashMap<>();
        jdbc.query("""
                SELECT CAST(estado AS text) AS estado, count(*) AS total FROM beca
                WHERE convocatoria_id = :convocatoria AND eliminado_en IS NULL GROUP BY estado
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                rs -> {
                    conteos.put(rs.getString("estado"), rs.getLong("total"));
                });
        return conteos;
    }

    // ---- Alta ----

    /**
     * Crea una beca vigente por cada adjudicada sin beca. Si dos altas corren a la vez, los indices unicos de `beca`
     * y `ON CONFLICT DO NOTHING` evitan duplicar. Devuelve cuantas creo.
     */
    public int crearBecasFaltantes(long convocatoriaId, LocalDate inicio) {
        return jdbc.update("""
                INSERT INTO beca (convocatoria_id, postulante_id, adjudicacion_id, estado, fecha_inicio)
                SELECT po.convocatoria_id, po.postulante_id, ad.id, CAST('VIGENTE' AS estado_beca), :inicio
                """ + ADJUDICADAS_SIN_BECA + " ON CONFLICT DO NOTHING",
                new MapSqlParameterSource("convocatoria", convocatoriaId).addValue("inicio", inicio));
    }

    // ---- Listado ----

    /**
     * Una pagina del padron. `orden` es un fragmento SQL ya validado por el service (nunca texto del cliente);
     * `patron` ya viene en minuscula y con los `%`; `estado` es opcional.
     */
    public List<FilaBeca> listar(long convocatoriaId, String patron, String estado, String orden, int limite,
            long desplazamiento) {
        return jdbc.query(SELECT_FILA + FILAS + " WHERE " + CONDICIONES + " ORDER BY " + orden
                + " LIMIT :limite OFFSET :desplazamiento",
                parametros(convocatoriaId, patron, estado).addValue("limite", limite)
                        .addValue("desplazamiento", desplazamiento),
                (rs, n) -> fila(rs));
    }

    public long contar(long convocatoriaId, String patron, String estado) {
        return jdbc.queryForObject("SELECT count(*) " + FILAS + " WHERE " + CONDICIONES,
                parametros(convocatoriaId, patron, estado), Long.class);
    }

    /** Una beca como fila del padron (despues de un cambio). */
    public Optional<FilaBeca> fila(long becaId) {
        return jdbc.query(SELECT_FILA + FILAS + " WHERE b.id = :id AND b.eliminado_en IS NULL",
                new MapSqlParameterSource("id", becaId), (rs, n) -> fila(rs)).stream().findFirst();
    }

    private static final String CONDICIONES = """
            b.convocatoria_id = :convocatoria AND b.eliminado_en IS NULL
              AND (CAST(:patron AS text) IS NULL OR lower(u.nombres) LIKE :patron OR lower(u.apellidos) LIKE :patron
                   OR lower(u.ci) LIKE :patron)
              AND (CAST(:estado AS text) IS NULL OR CAST(b.estado AS text) = :estado)
            """;

    private static MapSqlParameterSource parametros(long convocatoriaId, String patron, String estado) {
        return new MapSqlParameterSource("convocatoria", convocatoriaId)
                .addValue("patron", patron, Types.VARCHAR).addValue("estado", estado, Types.VARCHAR);
    }

    private static FilaBeca fila(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FilaBeca(rs.getObject("codigo_beca", UUID.class), rs.getObject("codigo_postulante", UUID.class),
                rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"), rs.getString("carrera"),
                rs.getString("estado"), rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class));
    }

    // ---- Una beca ----

    /** Bloquea la beca (despues de la convocatoria) y la devuelve; vacio si no existe. */
    public Optional<CabeceraBeca> bloquearBeca(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_beca, convocatoria_id, CAST(estado AS text) AS estado FROM beca
                WHERE codigo_beca = :codigo AND eliminado_en IS NULL FOR UPDATE
                """, new MapSqlParameterSource("codigo", codigo),
                (rs, n) -> new CabeceraBeca(rs.getLong("id"), rs.getObject("codigo_beca", UUID.class),
                        rs.getLong("convocatoria_id"), rs.getString("estado"))).stream().findFirst();
    }

    /** La beca sin bloquearla (para leer su convocatoria antes de bloquear en el orden correcto). */
    public Optional<CabeceraBeca> cabeceraBeca(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_beca, convocatoria_id, CAST(estado AS text) AS estado FROM beca
                WHERE codigo_beca = :codigo AND eliminado_en IS NULL
                """, new MapSqlParameterSource("codigo", codigo),
                (rs, n) -> new CabeceraBeca(rs.getLong("id"), rs.getObject("codigo_beca", UUID.class),
                        rs.getLong("convocatoria_id"), rs.getString("estado"))).stream().findFirst();
    }

    /** Si la beca liberada ya tiene su movimiento de reemplazo. */
    public boolean yaReemplazada(long becaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM movimiento_beca
                               WHERE beca_id = :beca AND CAST(tipo AS text) = 'REEMPLAZO' AND eliminado_en IS NULL)
                """, new MapSqlParameterSource("beca", becaId), Boolean.class));
    }

    // ---- Reemplazo ----

    /**
     * El siguiente postulante disponible: el finalista de mejor posicion de la lista priorizada cuya ultima
     * auditoria es «Validado», con puntaje total (IA + academico) igual o mayor al minimo, la postulacion todavia
     * «calificada» (sin decision) y sin beca en la convocatoria. Vacio si no hay puntaje minimo definido.
     */
    public Optional<Candidato> siguienteDisponible(long convocatoriaId) {
        return jdbc.query("""
                SELECT pt.usuario_id, pt.codigo_postulante, u.nombres, u.apellidos, u.ci, c.nombre AS carrera,
                       f.posicion, pu.puntaje_total + aud.puntaje_academico AS total
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id
                JOIN convocatoria cv ON cv.id = po.convocatoria_id
                JOIN postulante pt ON pt.usuario_id = po.postulante_id AND pt.eliminado_en IS NULL
                JOIN usuario u ON u.id = pt.usuario_id AND u.eliminado_en IS NULL
                JOIN carrera c ON c.id = pt.carrera_id
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente AND pu.eliminado_en IS NULL
                JOIN LATERAL (SELECT CAST(a.resultado AS text) AS resultado, a.puntaje_academico
                              FROM auditoria_expediente a
                              WHERE a.finalista_id = f.id AND a.eliminado_en IS NULL
                              ORDER BY a.fecha_auditoria DESC, a.id DESC LIMIT 1) aud ON TRUE
                WHERE po.convocatoria_id = :convocatoria AND CAST(po.estado AS text) = 'calificada'
                  AND f.eliminado_en IS NULL AND po.eliminado_en IS NULL
                  AND aud.resultado = 'VALIDADO' AND cv.puntaje_minimo IS NOT NULL
                  AND pu.puntaje_total + aud.puntaje_academico >= cv.puntaje_minimo
                  AND NOT EXISTS (SELECT 1 FROM beca b
                                  WHERE b.postulante_id = po.postulante_id AND b.convocatoria_id = po.convocatoria_id
                                    AND b.eliminado_en IS NULL)
                ORDER BY f.posicion ASC, f.id ASC LIMIT 1
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Candidato(rs.getLong("usuario_id"), rs.getObject("codigo_postulante", UUID.class),
                        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"),
                        rs.getString("carrera"), rs.getInt("posicion"), rs.getBigDecimal("total")))
                .stream().findFirst();
    }
}
