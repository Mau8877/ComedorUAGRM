package com.comedoruagrm.backend.features.postulaciones.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaPriorizableResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura de la lista priorizada (CU06).
 *
 * * La lista no es una tabla: es una consulta con `ROW_NUMBER()` sobre TODAS las postulaciones
 * * calificadas de la convocatoria (por puntaje vigente descendente; desempate por fecha de envio
 * * y luego por id). La busqueda y el filtro por carrera se aplican DESPUES de numerar, asi cada
 * * postulante conserva su posicion real (A2). Se usa JDBC en vez de una entidad porque cruza cinco
 * * tablas de cuatro modulos y el tipado de la ventana es mas predecible que con una proyeccion JPA.
 * * Todas las consultas toleran el borrado logico (`eliminado_en IS NULL`).
 */
@Repository
@RequiredArgsConstructor
public class ListaPriorizadaRepository {

    private static final String BASE = """
            WITH base AS (
                SELECT po.codigo_postulacion AS codigo,
                       ROW_NUMBER() OVER (ORDER BY pu.puntaje_total DESC, po.fecha_envio ASC NULLS LAST,
                                          po.id ASC) AS posicion,
                       u.nombres, u.apellidos, u.ci,
                       ca.codigo_carrera AS carrera_codigo, ca.nombre AS carrera_nombre,
                       pu.puntaje_total AS puntaje, pu.fecha_calculo, po.estado::text AS estado
                FROM convocatoria cv
                JOIN postulacion po ON po.convocatoria_id = cv.id
                     AND po.estado = 'calificada' AND po.eliminado_en IS NULL
                JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id
                     AND pu.vigente AND pu.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = po.postulante_id AND pt.eliminado_en IS NULL
                JOIN usuario u ON u.id = pt.usuario_id AND u.eliminado_en IS NULL
                JOIN carrera ca ON ca.id = pt.carrera_id
                WHERE cv.codigo_convocatoria = CAST(:convocatoria AS uuid) AND cv.eliminado_en IS NULL
            )
            """;

    private static final String FILTROS = """
            WHERE (CAST(:carrera AS uuid) IS NULL OR carrera_codigo = CAST(:carrera AS uuid))
              AND (CAST(:busqueda AS text) IS NULL
                   OR lower(nombres) LIKE :busqueda ESCAPE '\\'
                   OR lower(apellidos) LIKE :busqueda ESCAPE '\\'
                   OR lower(nombres || ' ' || apellidos) LIKE :busqueda ESCAPE '\\'
                   OR ci LIKE :busqueda ESCAPE '\\')
            """;

    private static final RowMapper<PostulacionPriorizadaResponse> FILA = ListaPriorizadaRepository::aFila;

    private final NamedParameterJdbcTemplate jdbc;

    /** Datos minimos de una convocatoria para el encabezado de la exportacion. */
    public record DatosConvocatoria(long id, String nombre, String gestion) {
    }

    /** Convocatorias no eliminadas (cualquier estado), la de apertura mas reciente primero. */
    public List<ConvocatoriaPriorizableResponse> listarConvocatorias() {
        return jdbc.query("""
                SELECT cv.codigo_convocatoria, cv.nombre, g.etiqueta, cv.estado::text AS estado,
                       cv.fecha_apertura, cv.fecha_cierre
                FROM convocatoria cv
                JOIN gestion_academica g ON g.id = cv.gestion_id AND g.eliminado_en IS NULL
                WHERE cv.eliminado_en IS NULL
                ORDER BY cv.fecha_apertura DESC, cv.id DESC
                """, (rs, n) -> new ConvocatoriaPriorizableResponse(
                        rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"),
                        rs.getString("etiqueta"),
                        rs.getString("estado"),
                        instante(rs, "fecha_apertura"),
                        instante(rs, "fecha_cierre")));
    }

    /** La convocatoria con ese codigo publico; vacio si no existe o esta eliminada. */
    public Optional<DatosConvocatoria> buscarConvocatoria(UUID codigo) {
        return jdbc.query("""
                SELECT cv.id, cv.nombre, g.etiqueta
                FROM convocatoria cv
                JOIN gestion_academica g ON g.id = cv.gestion_id
                WHERE cv.codigo_convocatoria = CAST(:convocatoria AS uuid) AND cv.eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("convocatoria", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new DatosConvocatoria(rs.getLong("id"), rs.getString("nombre"),
                        rs.getString("etiqueta")))
                .stream().findFirst();
    }

    /** Nombre de la carrera con ese codigo publico (para el encabezado del PDF). */
    public Optional<String> buscarNombreCarrera(UUID codigo) {
        return jdbc.query("SELECT nombre FROM carrera WHERE codigo_carrera = CAST(:carrera AS uuid)"
                + " AND eliminado_en IS NULL",
                new MapSqlParameterSource().addValue("carrera", codigo.toString(), Types.VARCHAR),
                (rs, n) -> rs.getString("nombre")).stream().findFirst();
    }

    /** Postulaciones de la convocatoria que siguen en estado `pendiente` de precalificacion. */
    public long contarPendientes(UUID convocatoria) {
        Long total = jdbc.queryForObject("""
                SELECT count(*)
                FROM postulacion po
                JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
                WHERE cv.codigo_convocatoria = CAST(:convocatoria AS uuid)
                  AND po.estado = 'pendiente' AND po.eliminado_en IS NULL
                """, parametros(convocatoria, null, null), Long.class);
        return total == null ? 0 : total;
    }

    /** Cuantas filas tiene la lista con esos filtros (`carrera` y `busqueda` pueden ser null). */
    public long contar(UUID convocatoria, UUID carrera, String busqueda) {
        Long total = jdbc.queryForObject(BASE + "SELECT count(*) FROM base " + FILTROS,
                parametros(convocatoria, carrera, busqueda), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Un tramo de la lista, ordenado por posicion. `busqueda` ya llega en minusculas y con los
     * comodines (`%texto%`) o null.
     */
    public List<PostulacionPriorizadaResponse> buscar(UUID convocatoria, UUID carrera, String busqueda,
            int limite, long desplazamiento) {
        MapSqlParameterSource parametros = parametros(convocatoria, carrera, busqueda)
                .addValue("limite", limite)
                .addValue("desplazamiento", desplazamiento);
        return jdbc.query(BASE + "SELECT * FROM base " + FILTROS
                + "ORDER BY posicion LIMIT :limite OFFSET :desplazamiento", parametros, FILA);
    }

    private static MapSqlParameterSource parametros(UUID convocatoria, UUID carrera, String busqueda) {
        return new MapSqlParameterSource()
                .addValue("convocatoria", convocatoria.toString(), Types.VARCHAR)
                .addValue("carrera", carrera == null ? null : carrera.toString(), Types.VARCHAR)
                .addValue("busqueda", busqueda, Types.VARCHAR);
    }

    private static PostulacionPriorizadaResponse aFila(ResultSet rs, int fila) throws SQLException {
        return new PostulacionPriorizadaResponse(
                rs.getObject("codigo", UUID.class),
                rs.getLong("posicion"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("ci"),
                new CarreraPriorizadaResponse(rs.getObject("carrera_codigo", UUID.class),
                        rs.getString("carrera_nombre")),
                rs.getBigDecimal("puntaje"),
                instante(rs, "fecha_calculo"),
                rs.getString("estado"));
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }
}
