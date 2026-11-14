package com.comedoruagrm.backend.features.resultados.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaPublicadaResponse;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;

import lombok.RequiredArgsConstructor;

/**
 * Consultas propias de la publicacion de resultados (CU22): decisiones, conteos, destinatarios del aviso y la
 * lista publica de adjudicados.
 *
 * * Se usa JDBC porque cruza convocatoria, finalista, postulacion, adjudicacion, usuario y carrera sin leer el
 * * repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * * Las listas (decisiones y adjudicados) resuelven todo en dos consultas (la pagina y el total), sin importar
 * * cuantos finalistas haya: nada se consulta dentro de un bucle.
 * * Solo lee, salvo `marcarConvocatoriaPublicada`: la convocatoria es de otro modulo y su cambio de estado a
 * * «publicada» se hace aqui, con el bloqueo de fila que el `service` ya tomo.
 * ! La lista publica nunca selecciona CI, correo ni datos socioeconomicos.
 */
@Repository
@RequiredArgsConstructor
public class ResultadosConsultaRepository {

    private static final String ERR_SYS_05 = "ERR_SYS_05";

    private static final Map<String, String> ORDEN_DECISIONES = Map.of(
            "puntajeFinal", "a.puntaje_final",
            "apellidos", "u.apellidos",
            "nombres", "u.nombres");

    private static final Map<String, String> ORDEN_ADJUDICADOS = Map.of(
            "nota", "a.puntaje_final",
            "apellidos", "u.apellidos",
            "nombres", "u.nombres");

    private final NamedParameterJdbcTemplate jdbc;

    /** Lo que el `service` necesita saber de la convocatoria. */
    public record DatosConvocatoria(long id, UUID codigo, String nombre, String estado) {
    }

    /** Cuando se publico y el nombre para mostrar de quien lo hizo. */
    public record DatosPublicacion(Instant publicadoEn, String publicadoPor) {
    }

    /** Conteos de los finalistas de una convocatoria; las decisiones se cuentan solo por adjudicacion. */
    public record Conteos(long adjudicados, long noAdjudicados, long calificadas, long totalFinalistas) {
    }

    /** Un finalista con decision de adjudicacion, a quien se le avisa su resultado. */
    public record Destinatario(long usuarioId, String nombres, String correo, long postulacionId,
            UUID postulacionCodigo, String resultado) {
    }

    /** Filtros de las decisiones del Administrador: `resultado` (`ADJUDICADA`/`NO_ADJUDICADA`) y `search`. */
    public record FiltroDecisiones(String resultado, String search) {
    }

    /** El resultado y la nota del finalista con decision de un usuario. */
    public record ResultadoPropio(String resultado, java.math.BigDecimal nota) {
    }

    /** Una pagina de filas y el total de filas que cumplen el filtro. */
    public record Pagina<T>(List<T> filas, long total) {
    }

    /** Vacio si la convocatoria no existe o fue eliminada. */
    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_convocatoria, nombre, CAST(estado AS text) AS estado
                FROM convocatoria
                WHERE codigo_convocatoria = CAST(:codigo AS uuid) AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getString("estado")))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la convocatoria hasta el fin de la transaccion: dos publicaciones simultaneas se
     * atienden una despues de la otra y la segunda ve la publicacion ya hecha.
     */
    public void bloquearConvocatoria(long id) {
        jdbc.queryForList("SELECT id FROM convocatoria WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", id), Long.class);
    }

    /** Pasa la convocatoria a «publicada». La unica escritura de esta clase: ver la nota de arriba. */
    public void marcarConvocatoriaPublicada(long id) {
        jdbc.update("UPDATE convocatoria SET estado = CAST('PUBLICADA' AS estado_convocatoria), "
                + "actualizado_en = now() WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    /** La publicacion de la convocatoria, o vacio si todavia no se publico. */
    public Optional<DatosPublicacion> publicacion(long convocatoriaId) {
        return jdbc.query("""
                SELECT pr.publicado_en, u.nombres || ' ' || u.apellidos AS publicado_por
                FROM publicacion_resultado pr
                JOIN usuario u ON u.id = pr.publicado_por
                WHERE pr.convocatoria_id = :convocatoria AND pr.eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new DatosPublicacion(instante(rs.getObject("publicado_en", OffsetDateTime.class)),
                        rs.getString("publicado_por")))
                .stream().findFirst();
    }

    /** Adjudicados, no adjudicados (solo por decision), finalistas en «calificada» y total, en una consulta. */
    public Conteos contarPorEstado(long convocatoriaId) {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE a.resultado = 'ADJUDICADA') AS adjudicados,
                       count(*) FILTER (WHERE a.resultado = 'NO_ADJUDICADA') AS no_adjudicados,
                       count(*) FILTER (WHERE a.id IS NULL AND po.estado = 'calificada') AS calificadas,
                       count(*) AS total
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                LEFT JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Conteos(rs.getLong("adjudicados"), rs.getLong("no_adjudicados"),
                        rs.getLong("calificadas"), rs.getLong("total")));
    }

    /**
     * Una fila por finalista con adjudicacion, en una sola consulta. Los archivados, rechazados y devueltos no
     * tienen adjudicacion y por eso no salen: ya fueron avisados en su momento.
     */
    public List<Destinatario> destinatariosDelAviso(long convocatoriaId) {
        return jdbc.query("""
                SELECT u.id AS usuario_id, u.nombres, u.correo, po.id AS postulacion_id, po.codigo_postulacion,
                       CAST(a.resultado AS text) AS resultado
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                ORDER BY f.id
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Destinatario(rs.getLong("usuario_id"), rs.getString("nombres"),
                        rs.getString("correo"), rs.getLong("postulacion_id"),
                        rs.getObject("codigo_postulacion", UUID.class), rs.getString("resultado")));
    }

    /** Una pagina de decisiones con `filter[resultado]`, `search` y `sort`; un `sort` o filtro invalido es `ERR_SYS_05`. */
    public Pagina<DecisionResultadoResponse> decisiones(long convocatoriaId, FiltroDecisiones filtro,
            Pageable pageable) {
        String orden = ordenar(pageable, ORDEN_DECISIONES, "a.puntaje_final DESC NULLS LAST");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("convocatoria", convocatoriaId);
        StringBuilder where = new StringBuilder();
        String resultado = filtro == null ? null : filtro.resultado();
        if (resultado != null && !resultado.isBlank()) {
            String valor = resultado.trim().toUpperCase();
            if (!valor.equals("ADJUDICADA") && !valor.equals("NO_ADJUDICADA")) {
                throw new ValidationException(ERR_SYS_05,
                        "El filtro «resultado» solo admite ADJUDICADA o NO_ADJUDICADA");
            }
            where.append(" AND CAST(a.resultado AS text) = :resultado");
            p.addValue("resultado", valor, Types.VARCHAR);
        }
        String busqueda = buscar(filtro == null ? null : filtro.search());
        if (busqueda != null) {
            where.append(" AND (u.nombres ILIKE :busqueda OR u.apellidos ILIKE :busqueda "
                    + "OR (u.nombres || ' ' || u.apellidos) ILIKE :busqueda)");
            p.addValue("busqueda", busqueda, Types.VARCHAR);
        }
        String desde = """
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria""" + where;
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<DecisionResultadoResponse> filas = jdbc.query("""
                SELECT f.codigo_finalista, u.nombres, u.apellidos, CAST(a.resultado AS text) AS resultado,
                       a.puntaje_final, a.fecha_adjudicacion
                """ + desde + " ORDER BY " + orden
                + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> new DecisionResultadoResponse(rs.getObject("codigo_finalista", UUID.class),
                        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("resultado"),
                        rs.getBigDecimal("puntaje_final"),
                        instante(rs.getObject("fecha_adjudicacion", OffsetDateTime.class))));
        return new Pagina<>(filas, total == null ? 0 : total);
    }

    /**
     * La lista publica: solo adjudicados, con nombre y apellido, nota (el puntaje final), carrera y facultad.
     * `search` busca en nombres, apellidos, carrera y facultad.
     */
    public Pagina<AdjudicadoPublicoResponse> adjudicadosPublicos(long convocatoriaId, String search,
            Pageable pageable) {
        String orden = ordenar(pageable, ORDEN_ADJUDICADOS, "a.puntaje_final DESC NULLS LAST");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("convocatoria", convocatoriaId);
        String busqueda = buscar(search);
        String filtro = "";
        if (busqueda != null) {
            filtro = " AND (u.nombres ILIKE :busqueda OR u.apellidos ILIKE :busqueda "
                    + "OR (u.nombres || ' ' || u.apellidos) ILIKE :busqueda "
                    + "OR c.nombre ILIKE :busqueda OR c.facultad ILIKE :busqueda)";
            p.addValue("busqueda", busqueda, Types.VARCHAR);
        }
        String desde = """
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                     AND a.resultado = 'ADJUDICADA'
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = u.id
                JOIN carrera c ON c.id = pt.carrera_id
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria""" + filtro;
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<AdjudicadoPublicoResponse> filas = jdbc.query(
                "SELECT u.nombres, u.apellidos, a.puntaje_final AS nota, c.nombre AS carrera, c.facultad "
                        + desde + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> new AdjudicadoPublicoResponse(rs.getString("nombres"), rs.getString("apellidos"),
                        rs.getBigDecimal("nota"), rs.getString("carrera"), rs.getString("facultad")));
        return new Pagina<>(filas, total == null ? 0 : total);
    }

    /** El resultado y la nota del finalista con decision de ese usuario en la convocatoria, o vacio. */
    public Optional<ResultadoPropio> miResultado(long convocatoriaId, UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT CAST(a.resultado AS text) AS resultado, a.puntaje_final
                FROM finalista f
                JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
                JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                  AND u.codigo_usuario = CAST(:usuario AS uuid)
                """, new MapSqlParameterSource().addValue("convocatoria", convocatoriaId)
                        .addValue("usuario", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new ResultadoPropio(rs.getString("resultado"), rs.getBigDecimal("puntaje_final")))
                .stream().findFirst();
    }

    /** Las convocatorias con resultados publicados, la publicada mas recientemente primero. */
    public Pagina<ConvocatoriaPublicadaResponse> convocatoriasPublicadas(Pageable pageable) {
        Long total = jdbc.getJdbcTemplate().queryForObject("""
                SELECT count(*) FROM publicacion_resultado pr
                JOIN convocatoria cv ON cv.id = pr.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE pr.eliminado_en IS NULL""", Long.class);
        List<ConvocatoriaPublicadaResponse> filas = jdbc.query("""
                SELECT cv.codigo_convocatoria, cv.nombre, pr.publicado_en
                FROM publicacion_resultado pr
                JOIN convocatoria cv ON cv.id = pr.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE pr.eliminado_en IS NULL
                ORDER BY pr.publicado_en DESC, pr.id DESC
                LIMIT :limite OFFSET :desplazamiento
                """, new MapSqlParameterSource().addValue("limite", pageable.getPageSize())
                        .addValue("desplazamiento", pageable.getOffset()),
                (rs, n) -> new ConvocatoriaPublicadaResponse(rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), instante(rs.getObject("publicado_en", OffsetDateTime.class))));
        return new Pagina<>(filas, total == null ? 0 : total);
    }

    /**
     * El `ORDER BY` seguro: solo campos de la lista blanca (nunca texto del cliente dentro del SQL) y un
     * desempate por `a.id`. Un campo no soportado, o mas de uno, es `ERR_SYS_05`.
     */
    private static String ordenar(Pageable pageable, Map<String, String> permitidos, String porDefecto) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return porDefecto + ", a.id";
        }
        List<Sort.Order> ordenes = sort.toList();
        if (ordenes.size() > 1) {
            throw new ValidationException(ERR_SYS_05, "Solo se puede ordenar por un campo a la vez");
        }
        Sort.Order orden = ordenes.get(0);
        String columna = permitidos.get(orden.getProperty());
        if (columna == null) {
            throw new ValidationException(ERR_SYS_05, "No se puede ordenar por «" + orden.getProperty()
                    + "». Campos válidos: " + String.join(", ", permitidos.keySet().stream().sorted().toList()));
        }
        String sentido = orden.isAscending() ? "ASC" : "DESC";
        return columna + " " + sentido + " NULLS LAST, a.id";
    }

    /** El texto de `search` listo para `ILIKE` (con `%`, sin comodines propios), o nulo si esta vacio. */
    private static String buscar(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String limpio = search.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
