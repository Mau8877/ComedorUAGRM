package com.comedoruagrm.backend.features.planillas.repository;

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
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas de la planilla de asistencia (CU09): la convocatoria con su gestion, los becados vigentes para el PDF y
 * las planillas generadas.
 *
 * * Se usa JDBC porque cruza convocatoria, gestion, beca, usuario y carrera sin leer el repository de otro
 * * modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee.
 * * El padron vigente es `beca.estado = 'VIGENTE'` (ver PERSISTENCIA_BD_BACKEND.md y CU23).
 * ! El CI solo sale de `becadosVigentes` (lo lleva el PDF); ninguna otra lectura selecciona CI ni correo.
 */
@Repository
@RequiredArgsConstructor
public class PlanillasConsultaRepository {

    private static final String ERR_SYS_05 = "ERR_SYS_05";

    private static final Map<String, String> ORDEN_PLANILLAS = Map.of(
            "generadaEn", "pl.generada_en",
            "fechaDesde", "pl.fecha_desde");

    private final NamedParameterJdbcTemplate jdbc;

    /** Lo que el `service` necesita saber de la convocatoria y de su gestion academica. */
    public record DatosConvocatoria(long id, UUID codigo, String nombre, String gestion, LocalDate gestionDesde,
            LocalDate gestionHasta) {
    }

    /** Un becado vigente tal como sale en el PDF. */
    public record Becado(String nombres, String apellidos, String ci, String carrera) {
    }

    /** Vacio si la convocatoria no existe o fue eliminada. */
    public Optional<DatosConvocatoria> convocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT cv.id, cv.codigo_convocatoria, cv.nombre, g.etiqueta, g.fecha_inicio, g.fecha_fin
                FROM convocatoria cv
                JOIN gestion_academica g ON g.id = cv.gestion_id
                WHERE cv.codigo_convocatoria = CAST(:codigo AS uuid) AND cv.eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getString("etiqueta"),
                        rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class)))
                .stream().findFirst();
    }

    /** Cuantas becas vigentes tiene la convocatoria. */
    public long contarVigentes(long convocatoriaId) {
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM beca
                WHERE convocatoria_id = :convocatoria AND estado = 'VIGENTE' AND eliminado_en IS NULL
                """, new MapSqlParameterSource("convocatoria", convocatoriaId), Long.class);
        return total == null ? 0 : total;
    }

    /** Los becados vigentes de la convocatoria en una sola consulta, por apellidos y nombres. */
    public List<Becado> becadosVigentes(long convocatoriaId) {
        return jdbc.query("""
                SELECT u.nombres, u.apellidos, u.ci, c.nombre AS carrera
                FROM beca b
                JOIN usuario u ON u.id = b.postulante_id AND u.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = u.id
                JOIN carrera c ON c.id = pt.carrera_id
                WHERE b.convocatoria_id = :convocatoria AND b.estado = 'VIGENTE' AND b.eliminado_en IS NULL
                ORDER BY u.apellidos, u.nombres, b.id
                """, new MapSqlParameterSource("convocatoria", convocatoriaId),
                (rs, n) -> new Becado(rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"),
                        rs.getString("carrera")));
    }

    /**
     * Una pagina de planillas de la convocatoria; `sort` es `generadaEn` o `fechaDesde` (por defecto la mas
     * reciente primero) y un campo no soportado es `ERR_SYS_05`.
     */
    public Page<PlanillaResponse> planillas(UUID convocatoriaCodigo, Pageable pageable) {
        String orden = ordenar(pageable);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("convocatoria",
                convocatoriaCodigo.toString(), Types.VARCHAR);
        String desde = """
                FROM planilla_asistencia pl
                JOIN convocatoria cv ON cv.id = pl.convocatoria_id AND cv.eliminado_en IS NULL
                JOIN usuario u ON u.id = pl.generada_por
                WHERE pl.eliminado_en IS NULL AND cv.codigo_convocatoria = CAST(:convocatoria AS uuid)""";
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        List<PlanillaResponse> filas = jdbc.query("""
                SELECT pl.codigo_planilla_asistencia, cv.codigo_convocatoria, pl.fecha_desde, pl.fecha_hasta,
                       pl.total_becados, pl.generada_en, u.nombres || ' ' || u.apellidos AS generada_por
                """ + desde + " ORDER BY " + orden + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> new PlanillaResponse(rs.getObject("codigo_planilla_asistencia", UUID.class),
                        rs.getObject("codigo_convocatoria", UUID.class), rs.getObject("fecha_desde", LocalDate.class),
                        rs.getObject("fecha_hasta", LocalDate.class), rs.getInt("total_becados"),
                        instante(rs.getObject("generada_en", OffsetDateTime.class)), rs.getString("generada_por")));
        return new PageImpl<>(filas, pageable, total == null ? 0 : total);
    }

    /**
     * El `ORDER BY` seguro: solo campos de la lista blanca (nunca texto del cliente dentro del SQL) y un
     * desempate por `pl.id`. Un campo no soportado, o mas de uno, es `ERR_SYS_05`.
     */
    private static String ordenar(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return "pl.generada_en DESC, pl.id DESC";
        }
        List<Sort.Order> ordenes = sort.toList();
        if (ordenes.size() > 1) {
            throw new ValidationException(ERR_SYS_05, "Solo se puede ordenar por un campo a la vez");
        }
        Sort.Order orden = ordenes.get(0);
        String columna = ORDEN_PLANILLAS.get(orden.getProperty());
        if (columna == null) {
            throw new ValidationException(ERR_SYS_05, "No se puede ordenar por «" + orden.getProperty()
                    + "». Campos válidos: " + String.join(", ",
                            ORDEN_PLANILLAS.keySet().stream().sorted().toList()));
        }
        return columna + (orden.isAscending() ? " ASC" : " DESC") + ", pl.id";
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
