package com.comedoruagrm.backend.features.actas.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas del acta de adjudicacion (CU08): la convocatoria, la publicacion, los adjudicados para el PDF, las
 * actas generadas y las convocatorias pendientes de acta.
 *
 * * Se usa JDBC porque cruza convocatoria, gestion, finalista, postulacion, adjudicacion, usuario y carrera sin
 * * leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * * Cada metodo resuelve su lectura en un numero fijo de consultas (la pagina y el total en las listas): nada se
 * * consulta dentro de un bucle.
 * * Solo lee, salvo `bloquearConvocatoria` (bloqueo de fila) y `bloquearNumeracion` (bloqueo de transaccion).
 * ! El CI solo sale de `adjudicadosDelActa` (lo lleva el PDF); ninguna otra lectura selecciona CI ni correo.
 */
@Repository
@RequiredArgsConstructor
public class ActasConsultaRepository {

    private static final String ERR_SYS_05 = "ERR_SYS_05";

    /** Primera mitad de la clave del bloqueo de numeracion (la segunda es el año). */
    private static final int CLAVE_BLOQUEO_NUMERACION = 80_008;

    private static final Map<String, String> ORDEN_ACTAS = Map.of(
            "numero", "ac.numero",
            "generadaEn", "ac.generada_en");

    /** Las adjudicaciones con resultado «adjudicada» de una convocatoria (`:convocatoria`). */
    private static final String ADJUDICADOS_DESDE = """
            FROM finalista f
            JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
            JOIN adjudicacion a ON a.postulacion_id = po.id AND a.eliminado_en IS NULL
                 AND a.resultado = 'ADJUDICADA'
            """;

    /** Subconsulta con la cantidad de adjudicados de la convocatoria `cv` de la consulta que la usa. */
    private static final String CUENTA_ADJUDICADOS = "(SELECT count(*) " + ADJUDICADOS_DESDE
            + " WHERE f.eliminado_en IS NULL AND po.convocatoria_id = cv.id)";

    private final NamedParameterJdbcTemplate jdbc;

    /** Lo que el `service` necesita saber de la convocatoria. */
    public record DatosConvocatoria(long id, UUID codigo, String nombre, String estado, String gestion,
            Instant fechaApertura, Instant fechaCierre, Integer cupos, BigDecimal puntajeMinimo) {
    }

    /** Un adjudicado tal como sale en el PDF del acta. */
    public record Adjudicado(String nombres, String apellidos, String ci, String carrera, BigDecimal puntajeFinal) {
    }

    /** Vacio si la convocatoria no existe o fue eliminada. */
    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT cv.id, cv.codigo_convocatoria, cv.nombre, CAST(cv.estado AS text) AS estado,
                       g.etiqueta, cv.fecha_apertura, cv.fecha_cierre, cv.cupos, cv.puntaje_minimo
                FROM convocatoria cv
                JOIN gestion_academica g ON g.id = cv.gestion_id
                WHERE cv.codigo_convocatoria = CAST(:codigo AS uuid) AND cv.eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getString("estado"), rs.getString("etiqueta"),
                        instante(rs.getObject("fecha_apertura", OffsetDateTime.class)),
                        instante(rs.getObject("fecha_cierre", OffsetDateTime.class)),
                        (Integer) rs.getObject("cupos"), rs.getBigDecimal("puntaje_minimo")))
                .stream().findFirst();
    }

    /**
     * Bloquea la fila de la convocatoria hasta el fin de la transaccion: dos generaciones simultaneas se
     * atienden una despues de la otra y la segunda ve el acta ya registrada.
     */
    public void bloquearConvocatoria(long id) {
        jdbc.queryForList("SELECT id FROM convocatoria WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", id), Long.class);
    }

    /** Cuando se publicaron los resultados, o vacio si todavia no se publicaron. */
    public Optional<Instant> publicacion(long convocatoriaId) {
        return jdbc.query("""
                SELECT publicado_en FROM publicacion_resultado
                WHERE convocatoria_id = :convocatoria AND eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> instante(rs.getObject("publicado_en", OffsetDateTime.class)))
                .stream().findFirst();
    }

    /**
     * Los adjudicados de la convocatoria en una sola consulta: de mayor a menor puntaje final (los sin puntaje
     * al final) y, a igual puntaje, por la posicion del finalista. Solo adjudicados.
     */
    public List<Adjudicado> adjudicadosDelActa(long convocatoriaId) {
        return jdbc.query("SELECT u.nombres, u.apellidos, u.ci, c.nombre AS carrera, a.puntaje_final "
                + ADJUDICADOS_DESDE + """
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = u.id
                JOIN carrera c ON c.id = pt.carrera_id
                WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria
                ORDER BY a.puntaje_final DESC NULLS LAST, f.posicion, a.id
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Adjudicado(rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"),
                        rs.getString("carrera"), rs.getBigDecimal("puntaje_final")));
    }

    public long contarAdjudicados(long convocatoriaId) {
        Long total = jdbc.queryForObject("SELECT count(*) " + ADJUDICADOS_DESDE
                + " WHERE f.eliminado_en IS NULL AND po.convocatoria_id = :convocatoria",
                new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
        return total == null ? 0 : total;
    }

    /** El acta de la convocatoria con el nombre de quien la genero, o vacio si todavia no tiene. */
    public Optional<ActaResponse> acta(long convocatoriaId) {
        return jdbc.query("""
                SELECT ac.codigo_acta_adjudicacion, ac.numero, ac.generada_en, cv.codigo_convocatoria, cv.nombre,
                       g.etiqueta, u.nombres || ' ' || u.apellidos AS generada_por,
                """ + CUENTA_ADJUDICADOS + """
                 AS adjudicados
                FROM acta_adjudicacion ac
                JOIN convocatoria cv ON cv.id = ac.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN gestion_academica g ON g.id = cv.gestion_id
                JOIN usuario u ON u.id = ac.generada_por
                WHERE ac.convocatoria_id = :convocatoria AND ac.eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId), (rs, n) -> aActa(rs))
                .stream().findFirst();
    }

    /**
     * Una pagina de actas con `search` (numero y nombre de la convocatoria) y `sort` (`numero` o `generadaEn`,
     * por defecto la mas reciente primero); un `sort` no soportado es `ERR_SYS_05`.
     */
    public Page<ActaResponse> actas(String search, Pageable pageable) {
        String orden = ordenar(pageable);
        MapSqlParameterSource p = new MapSqlParameterSource();
        String filtro = "";
        String busqueda = buscar(search);
        if (busqueda != null) {
            filtro = " AND (ac.numero ILIKE :busqueda OR cv.nombre ILIKE :busqueda)";
            p.addValue("busqueda", busqueda, Types.VARCHAR);
        }
        String desde = """
                FROM acta_adjudicacion ac
                JOIN convocatoria cv ON cv.id = ac.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN gestion_academica g ON g.id = cv.gestion_id
                JOIN usuario u ON u.id = ac.generada_por
                WHERE ac.eliminado_en IS NULL""" + filtro;
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<ActaResponse> filas = jdbc.query("""
                SELECT ac.codigo_acta_adjudicacion, ac.numero, ac.generada_en, cv.codigo_convocatoria, cv.nombre,
                       g.etiqueta, u.nombres || ' ' || u.apellidos AS generada_por,
                """ + CUENTA_ADJUDICADOS + " AS adjudicados " + desde
                + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento", p, (rs, n) -> aActa(rs));
        return new PageImpl<>(filas, pageable, total == null ? 0 : total);
    }

    /** Las convocatorias con resultados publicados y sin acta, la publicada mas recientemente primero. */
    public Page<ConvocatoriaPendienteActaResponse> convocatoriasPendientes(Pageable pageable) {
        String desde = """
                FROM publicacion_resultado pr
                JOIN convocatoria cv ON cv.id = pr.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE pr.eliminado_en IS NULL
                  AND NOT EXISTS (SELECT 1 FROM acta_adjudicacion ac
                                  WHERE ac.convocatoria_id = cv.id AND ac.eliminado_en IS NULL)""";
        Long total = jdbc.getJdbcTemplate().queryForObject("SELECT count(*) " + desde, Long.class);
        List<ConvocatoriaPendienteActaResponse> filas = jdbc.query(
                "SELECT cv.codigo_convocatoria, cv.nombre, pr.publicado_en, " + CUENTA_ADJUDICADOS
                        + " AS adjudicados " + desde
                        + " ORDER BY pr.publicado_en DESC, pr.id DESC LIMIT :limite OFFSET :desplazamiento",
                new MapSqlParameterSource().addValue("limite", pageable.getPageSize())
                        .addValue("desplazamiento", pageable.getOffset()),
                (rs, n) -> new ConvocatoriaPendienteActaResponse(rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), instante(rs.getObject("publicado_en", OffsetDateTime.class)),
                        rs.getLong("adjudicados")));
        return new PageImpl<>(filas, pageable, total == null ? 0 : total);
    }

    /**
     * Bloquea hasta el fin de la transaccion la asignacion de numeros del año: dos generaciones simultaneas de
     * convocatorias distintas no calculan el mismo correlativo.
     */
    public void bloquearNumeracion(int anio) {
        jdbc.query("SELECT pg_advisory_xact_lock(:clase, :anio)",
                new MapSqlParameterSource().addValue("clase", CLAVE_BLOQUEO_NUMERACION).addValue("anio", anio),
                rs -> {
                });
    }

    /**
     * El mayor correlativo ya usado en el año, 0 si no hay ninguno. Cuenta tambien las actas eliminadas: un
     * numero nunca se reutiliza.
     */
    public int mayorCorrelativoDelAnio(int anio) {
        Integer mayor = jdbc.queryForObject("""
                SELECT COALESCE(max(CAST(split_part(numero, '/', 1) AS integer)), 0)
                FROM acta_adjudicacion
                WHERE split_part(numero, '/', 2) = :anio
                  AND split_part(numero, '/', 1) ~ '^[0-9]+$'
                """, new MapSqlParameterSource().addValue("anio", String.valueOf(anio), Types.VARCHAR),
                Integer.class);
        return mayor == null ? 0 : mayor;
    }

    private static ActaResponse aActa(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ActaResponse(rs.getObject("codigo_acta_adjudicacion", UUID.class), rs.getString("numero"),
                new ConvocatoriaActaResponse(rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre")),
                rs.getString("etiqueta"), rs.getLong("adjudicados"),
                instante(rs.getObject("generada_en", OffsetDateTime.class)), rs.getString("generada_por"));
    }

    /**
     * El `ORDER BY` seguro: solo campos de la lista blanca (nunca texto del cliente dentro del SQL) y un
     * desempate por `ac.id`. Un campo no soportado, o mas de uno, es `ERR_SYS_05`.
     */
    private static String ordenar(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return "ac.generada_en DESC, ac.id DESC";
        }
        List<Sort.Order> ordenes = sort.toList();
        if (ordenes.size() > 1) {
            throw new ValidationException(ERR_SYS_05, "Solo se puede ordenar por un campo a la vez");
        }
        Sort.Order orden = ordenes.get(0);
        String columna = ORDEN_ACTAS.get(orden.getProperty());
        if (columna == null) {
            throw new ValidationException(ERR_SYS_05, "No se puede ordenar por «" + orden.getProperty()
                    + "». Campos válidos: " + String.join(", ", ORDEN_ACTAS.keySet().stream().sorted().toList()));
        }
        return columna + (orden.isAscending() ? " ASC" : " DESC") + ", ac.id";
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
