package com.comedoruagrm.backend.features.cotejos.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
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
import com.comedoruagrm.backend.features.cotejos.dto.CotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.RegistroDigitalResponse;
import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas del cotejo de asistencia (CU11): la planilla, los registros digitales de su periodo y los cotejos ya
 * registrados.
 *
 * * Se usa JDBC porque cruza planilla, convocatoria, beca, usuario, carrera y asistencia sin leer el repository de
 * * otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee.
 * * Los registros digitales de una planilla son las asistencias del periodo de las becas de su convocatoria que ya
 * * existian cuando se genero (`beca.creado_en <= planilla.generada_en`): la planilla no guarda la lista de becados.
 * ! El CI no se selecciona en ninguna lectura de este repositorio.
 */
@Repository
@RequiredArgsConstructor
public class CotejosConsultaRepository {

    private static final String ERR_SYS_05 = "ERR_SYS_05";

    private static final Map<String, String> ORDEN_REGISTROS = Map.of(
            "fecha", "a.fecha",
            "apellidos", "u.apellidos");

    private static final Map<String, String> ORDEN_COTEJOS = Map.of(
            "creadoEn", "c.creado_en",
            "fechaCotejo", "c.fecha_cotejo");

    // Los registros digitales de una planilla: se comparte entre el conteo, el resumen y la lista.
    private static final String REGISTROS_DE_LA_PLANILLA = """
            FROM planilla_asistencia pl
            JOIN beca b ON b.convocatoria_id = pl.convocatoria_id AND b.eliminado_en IS NULL
                       AND b.creado_en <= pl.generada_en
            JOIN asistencia a ON a.beca_id = b.id AND a.eliminado_en IS NULL
                       AND a.fecha BETWEEN pl.fecha_desde AND pl.fecha_hasta
            JOIN usuario u ON u.id = b.postulante_id
            JOIN postulante pt ON pt.usuario_id = u.id
            JOIN carrera c ON c.id = pt.carrera_id
            WHERE pl.codigo_planilla_asistencia = CAST(:planilla AS uuid) AND pl.eliminado_en IS NULL""";

    private final NamedParameterJdbcTemplate jdbc;

    /** La planilla tal como la necesita el cotejo. */
    public record DatosPlanilla(long id, UUID codigo, UUID convocatoria, String convocatoriaNombre,
            LocalDate fechaDesde, LocalDate fechaHasta, int totalBecados, Instant generadaEn) {
    }

    /** Cuantos registros digitales hay y de cuantos becados distintos. */
    public record ResumenDigital(long registros, long becados) {
    }

    /** Vacio si la planilla no existe, fue eliminada o es de una convocatoria eliminada. */
    public Optional<DatosPlanilla> planilla(UUID codigo) {
        return jdbc.query("""
                SELECT pl.id, pl.codigo_planilla_asistencia, cv.codigo_convocatoria, cv.nombre, pl.fecha_desde,
                       pl.fecha_hasta, pl.total_becados, pl.generada_en
                FROM planilla_asistencia pl
                JOIN convocatoria cv ON cv.id = pl.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE pl.codigo_planilla_asistencia = CAST(:codigo AS uuid) AND pl.eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosPlanilla(rs.getLong("id"), rs.getObject("codigo_planilla_asistencia", UUID.class),
                        rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre"),
                        rs.getObject("fecha_desde", LocalDate.class), rs.getObject("fecha_hasta", LocalDate.class),
                        rs.getInt("total_becados"), instante(rs.getObject("generada_en", OffsetDateTime.class))))
                .stream().findFirst();
    }

    public ResumenDigital resumenDigital(UUID planilla) {
        return jdbc.queryForObject("SELECT count(*) AS registros, count(DISTINCT b.id) AS becados "
                + REGISTROS_DE_LA_PLANILLA, parametroPlanilla(planilla),
                (rs, n) -> new ResumenDigital(rs.getLong("registros"), rs.getLong("becados")));
    }

    /** Una pagina de registros digitales; `sort` es `fecha` o `apellidos` (por defecto por fecha y apellidos). */
    public Page<RegistroDigitalResponse> registros(UUID planilla, Pageable pageable) {
        String orden = ordenar(pageable, ORDEN_REGISTROS, "a.fecha, u.apellidos, u.nombres, a.id", "a.id");
        MapSqlParameterSource p = parametroPlanilla(planilla);
        Long total = jdbc.queryForObject("SELECT count(*) " + REGISTROS_DE_LA_PLANILLA, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<RegistroDigitalResponse> filas = jdbc.query("""
                SELECT u.nombres, u.apellidos, c.nombre AS carrera, a.fecha, a.hora_registro
                """ + REGISTROS_DE_LA_PLANILLA + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> new RegistroDigitalResponse(rs.getString("nombres"), rs.getString("apellidos"),
                        rs.getString("carrera"), rs.getObject("fecha", LocalDate.class),
                        instante(rs.getObject("hora_registro", OffsetDateTime.class))));
        return new PageImpl<>(filas, pageable, total == null ? 0 : total);
    }

    /** Una pagina de cotejos de la planilla; `sort` es `creadoEn` o `fechaCotejo` (por defecto el mas reciente). */
    public Page<CotejoResponse> cotejos(UUID planilla, Pageable pageable) {
        String orden = ordenar(pageable, ORDEN_COTEJOS, "c.creado_en DESC, c.id DESC", "c.id");
        MapSqlParameterSource p = parametroPlanilla(planilla);
        String desde = """
                FROM cotejo_asistencia c
                JOIN planilla_asistencia pl ON pl.id = c.planilla_id AND pl.eliminado_en IS NULL
                JOIN usuario u ON u.id = c.realizado_por
                WHERE c.eliminado_en IS NULL AND pl.codigo_planilla_asistencia = CAST(:planilla AS uuid)""";
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<CotejoResponse> filas = jdbc.query("""
                SELECT c.codigo_cotejo_asistencia, pl.codigo_planilla_asistencia, c.fecha_cotejo, c.total_registros,
                       c.coincidencias, c.discrepancias, c.resultado, c.observaciones, c.creado_en,
                       u.nombres || ' ' || u.apellidos AS realizado_por
                """ + desde + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> new CotejoResponse(rs.getObject("codigo_cotejo_asistencia", UUID.class),
                        rs.getObject("codigo_planilla_asistencia", UUID.class),
                        rs.getObject("fecha_cotejo", LocalDate.class), rs.getInt("total_registros"),
                        rs.getInt("coincidencias"), rs.getInt("discrepancias"),
                        ResultadoCotejo.valueOf(rs.getString("resultado")), rs.getString("observaciones"),
                        rs.getString("realizado_por"), instante(rs.getObject("creado_en", OffsetDateTime.class))));
        return new PageImpl<>(filas, pageable, total == null ? 0 : total);
    }

    private static MapSqlParameterSource parametroPlanilla(UUID planilla) {
        return new MapSqlParameterSource().addValue("planilla", planilla.toString(), Types.VARCHAR);
    }

    /**
     * El `ORDER BY` seguro: solo campos de la lista blanca (nunca texto del cliente dentro del SQL) y un desempate.
     * Sin `sort` se usa `porDefecto`. Un campo no soportado, o mas de uno, es `ERR_SYS_05`.
     */
    private static String ordenar(Pageable pageable, Map<String, String> permitidos, String porDefecto,
            String desempate) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return porDefecto;
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
        return columna + (orden.isAscending() ? " ASC" : " DESC") + ", " + desempate;
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
