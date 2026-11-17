package com.comedoruagrm.backend.features.preseleccion.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Versiones del modelo de preseleccion y sus parametros (CU26). JDBC: crear una version es archivar la vigente,
 * insertar la nueva y sus parametros, todo en la transaccion del service. Respeta el borrado logico.
 */
@Repository
@RequiredArgsConstructor
public class ModeloPreseleccionConsultaRepository {

    private static final String COLUMNAS = "id, codigo_modelo_preseleccion, version, descripcion, activo, creado_en";

    private final NamedParameterJdbcTemplate jdbc;

    /** Una version del modelo. */
    public record ModeloFila(long id, UUID codigo, String version, String descripcion, boolean activo,
            Instant creadoEn) {
    }

    /** La ponderacion de una variable. */
    public record ParametroFila(String clave, BigDecimal ponderacion) {
    }

    private static ModeloFila fila(ResultSet rs) throws SQLException {
        return new ModeloFila(rs.getLong("id"), rs.getObject("codigo_modelo_preseleccion", UUID.class),
                rs.getString("version"), rs.getString("descripcion"), rs.getBoolean("activo"),
                rs.getTimestamp("creado_en").toInstant());
    }

    /** La version vigente (la unica `activo`), si ya se guardo alguna. */
    public Optional<ModeloFila> vigente() {
        return jdbc.query("SELECT " + COLUMNAS + " FROM modelo_preseleccion WHERE activo AND eliminado_en IS NULL",
                new MapSqlParameterSource(), (rs, n) -> fila(rs)).stream().findFirst();
    }

    public List<ParametroFila> parametros(long modeloId) {
        return jdbc.query("""
                SELECT clave, ponderacion FROM parametro_modelo
                WHERE modelo_id = :modelo AND eliminado_en IS NULL ORDER BY id
                """, new MapSqlParameterSource("modelo", modeloId),
                (rs, n) -> new ParametroFila(rs.getString("clave"), rs.getBigDecimal("ponderacion")));
    }

    /** Todas las versiones guardadas (solo el texto), para calcular la siguiente. */
    public List<String> versiones() {
        return jdbc.queryForList("SELECT version FROM modelo_preseleccion WHERE eliminado_en IS NULL",
                new MapSqlParameterSource(), String.class);
    }

    /** Las versiones, la mas reciente primero. */
    public List<ModeloFila> listar(int limite, long desde) {
        return jdbc.query("SELECT " + COLUMNAS + """
                 FROM modelo_preseleccion WHERE eliminado_en IS NULL
                ORDER BY creado_en DESC, id DESC LIMIT :limite OFFSET :desde
                """, new MapSqlParameterSource().addValue("limite", limite).addValue("desde", desde),
                (rs, n) -> fila(rs));
    }

    public long contar() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM modelo_preseleccion WHERE eliminado_en IS NULL",
                new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    /** La version vigente pasa a archivada (el indice unico solo admite una activa). */
    public void archivarVigente() {
        jdbc.update("UPDATE modelo_preseleccion SET activo = false, actualizado_en = now() "
                + "WHERE activo AND eliminado_en IS NULL", new MapSqlParameterSource());
    }

    /** Crea una version `activo`. Una version repetida (dos guardados a la vez) viola el indice unico. */
    public ModeloFila crear(long creadoPor, String version, String descripcion) {
        return jdbc.query("""
                INSERT INTO modelo_preseleccion (creado_por, version, descripcion, activo)
                VALUES (:creadoPor, :version, :descripcion, true)
                RETURNING """ + " " + COLUMNAS, new MapSqlParameterSource().addValue("creadoPor", creadoPor)
                        .addValue("version", version).addValue("descripcion", descripcion),
                (rs, n) -> fila(rs)).get(0);
    }

    public void insertarParametro(long modeloId, String clave, BigDecimal ponderacion) {
        jdbc.update("""
                INSERT INTO parametro_modelo (modelo_id, clave, ponderacion) VALUES (:modelo, :clave, :ponderacion)
                """, new MapSqlParameterSource().addValue("modelo", modeloId).addValue("clave", clave)
                        .addValue("ponderacion", ponderacion));
    }
}
