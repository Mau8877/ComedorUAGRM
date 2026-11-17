package com.comedoruagrm.backend.features.preseleccion.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas del calculo del puntaje (CU03). JDBC porque lee `postulacion`, `respuesta_socioeconomica`,
 * `postulante` y `carrera` sin tocar los repositories de otros modulos. Respeta el borrado logico.
 */
@Repository
@RequiredArgsConstructor
public class PrecalificacionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Las variables del modelo de una postulacion `pendiente` con sus respuestas completas. */
    public record DatosPuntaje(UUID postulacion, BigDecimal ingresosFamiliares, int dependientes, String procedencia,
            String carrera, String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico,
            String dependenciaEconomica, String ocupacionSosten, Boolean tieneHijos, String cantidadHijos,
            Boolean discapacidad) {

        /** Solo las variables originales (sin los datos del formulario oficial). */
        public DatosPuntaje(UUID postulacion, BigDecimal ingresosFamiliares, int dependientes, String procedencia,
                String carrera, String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico) {
            this(postulacion, ingresosFamiliares, dependientes, procedencia, carrera, patrimonio,
                    estadoVulnerabilidad, promedioAcademico, null, null, null, null, null);
        }
    }

    /** Vacio si la postulacion ya no esta `pendiente`, no existe o no tiene todas sus respuestas. */
    public Optional<DatosPuntaje> datosPendiente(long postulacionId) {
        return datos(postulacionId, "pendiente");
    }

    /** Igual, para recalcular una postulacion `calificada` (CU26). */
    public Optional<DatosPuntaje> datosCalificada(long postulacionId) {
        return datos(postulacionId, "calificada");
    }

    private Optional<DatosPuntaje> datos(long postulacionId, String estado) {
        return jdbc.query("""
                SELECT po.codigo_postulacion, r.ingresos_familiares, r.dependientes, r.procedencia, c.nombre AS carrera,
                       r.patrimonio, r.estado_vulnerabilidad, r.promedio_academico,
                       r.datos_extra ->> 'dependenciaEconomica' AS dependencia_economica,
                       r.datos_extra ->> 'ocupacionSosten' AS ocupacion_sosten,
                       CAST(r.datos_extra ->> 'tieneHijos' AS boolean) AS tiene_hijos,
                       r.datos_extra ->> 'cantidadHijos' AS cantidad_hijos,
                       CAST(r.datos_extra ->> 'discapacidad' AS boolean) AS discapacidad
                FROM postulacion po
                JOIN respuesta_socioeconomica r ON r.postulacion_id = po.id AND r.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = po.postulante_id
                JOIN carrera c ON c.id = pt.carrera_id
                WHERE po.id = :id AND po.eliminado_en IS NULL AND po.estado = CAST(:estado AS estado_postulacion)
                  AND r.ingresos_familiares IS NOT NULL AND r.dependientes IS NOT NULL
                  AND r.procedencia IS NOT NULL AND r.patrimonio IS NOT NULL
                  AND r.estado_vulnerabilidad IS NOT NULL AND r.promedio_academico IS NOT NULL
                """, new MapSqlParameterSource().addValue("id", postulacionId).addValue("estado", estado),
                (rs, n) -> new DatosPuntaje(rs.getObject("codigo_postulacion", UUID.class),
                        rs.getBigDecimal("ingresos_familiares"), rs.getInt("dependientes"),
                        rs.getString("procedencia"), rs.getString("carrera"), rs.getString("patrimonio"),
                        rs.getString("estado_vulnerabilidad"), rs.getBigDecimal("promedio_academico"),
                        rs.getString("dependencia_economica"), rs.getString("ocupacion_sosten"),
                        rs.getObject("tiene_hijos", Boolean.class), rs.getString("cantidad_hijos"),
                        rs.getObject("discapacidad", Boolean.class)))
                .stream().findFirst();
    }

    /** Bloquea la fila de la postulacion hasta el fin de la transaccion. */
    public void bloquearPostulacion(long postulacionId) {
        jdbc.queryForList("SELECT id FROM postulacion WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", postulacionId), Long.class);
    }

    /** Si la postulacion sigue `pendiente` (se vuelve a mirar despues del bloqueo). */
    public boolean sigueEnPendiente(long postulacionId) {
        return sigueEn(postulacionId, "pendiente");
    }

    /** Si la postulacion sigue `calificada` (CU26, despues del bloqueo). */
    public boolean sigueCalificada(long postulacionId) {
        return sigueEn(postulacionId, "calificada");
    }

    private boolean sigueEn(long postulacionId, String estado) {
        return !jdbc.queryForList("""
                SELECT id FROM postulacion
                WHERE id = :id AND eliminado_en IS NULL AND estado = CAST(:estado AS estado_postulacion)
                """, new MapSqlParameterSource().addValue("id", postulacionId).addValue("estado", estado),
                Long.class).isEmpty();
    }

    /** Si la postulacion ya tiene un puntaje vigente de esa version del modelo (CU26: no se recalcula dos veces). */
    public boolean tieneVigenteDelModelo(long postulacionId, long modeloId) {
        return !jdbc.queryForList("""
                SELECT id FROM puntaje_postulacion
                WHERE postulacion_id = :id AND modelo_id = :modelo AND vigente AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("id", postulacionId).addValue("modelo", modeloId),
                Long.class).isEmpty();
    }

    /** Pasa a historial el puntaje vigente (si hay) para que entre el nuevo: solo uno vigente por postulacion. */
    public int retirarVigentes(long postulacionId) {
        return jdbc.update("""
                UPDATE puntaje_postulacion SET vigente = false, actualizado_en = now()
                WHERE postulacion_id = :id AND vigente AND eliminado_en IS NULL
                """, new MapSqlParameterSource("id", postulacionId));
    }

    /** Los ids de las `pendiente` con respuestas que no estan marcadas para revision, las mas antiguas primero. */
    public List<Long> pendientes(int limite) {
        return jdbc.queryForList("""
                SELECT po.id
                FROM postulacion po
                JOIN respuesta_socioeconomica r ON r.postulacion_id = po.id AND r.eliminado_en IS NULL
                WHERE po.eliminado_en IS NULL AND po.estado = 'pendiente'
                  AND po.precalificacion_revision_en IS NULL
                ORDER BY po.fecha_envio NULLS LAST, po.id
                LIMIT :limite
                """, new MapSqlParameterSource("limite", limite), Long.class);
    }

    /**
     * Cuenta un rechazo de la IA y, al llegar a `maximo`, marca la postulacion para revision (CU03 A1): el
     * reintento automatico ya no la toma. Devuelve si quedo marcada.
     */
    public boolean registrarFallo(long postulacionId, int maximo) {
        return jdbc.queryForList("""
                UPDATE postulacion
                SET intentos_precalificacion = intentos_precalificacion + 1,
                    precalificacion_revision_en = CASE WHEN intentos_precalificacion + 1 >= :maximo
                        THEN COALESCE(precalificacion_revision_en, now()) ELSE precalificacion_revision_en END
                WHERE id = :id AND estado = 'pendiente'
                RETURNING precalificacion_revision_en IS NOT NULL
                """, new MapSqlParameterSource().addValue("id", postulacionId).addValue("maximo", maximo),
                Boolean.class).stream().anyMatch(Boolean.TRUE::equals);
    }

    /** Lo que se muestra de una postulacion y su puntaje vigente (si lo tiene). `detalle` es el JSON guardado. */
    public record CabeceraPrecalificacion(long id, UUID codigo, String nombres, String apellidos, String ci,
            String convocatoria, String estado, int intentos, Instant revisionDesde, BigDecimal puntaje,
            String modelo, Instant fechaCalculo, String detalle) {
    }

    /** Un cambio de estado del historial de la postulacion. */
    public record CambioEstado(String estadoNuevo, String motivo, Instant fecha) {
    }

    /** Vacio si la postulacion no existe o esta eliminada. */
    public Optional<CabeceraPrecalificacion> cabecera(UUID codigo) {
        return jdbc.query("""
                SELECT po.id, po.codigo_postulacion, u.nombres, u.apellidos, u.ci, cv.nombre AS convocatoria,
                       po.estado::text AS estado, po.intentos_precalificacion, po.precalificacion_revision_en,
                       pu.puntaje_total, m.version, pu.fecha_calculo, CAST(pu.detalle AS text) AS detalle
                FROM postulacion po
                JOIN convocatoria cv ON cv.id = po.convocatoria_id
                JOIN usuario u ON u.id = po.postulante_id
                LEFT JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                     AND pu.eliminado_en IS NULL
                LEFT JOIN modelo_preseleccion m ON m.id = pu.modelo_id
                WHERE po.codigo_postulacion = :codigo AND po.eliminado_en IS NULL
                """, new MapSqlParameterSource("codigo", codigo),
                (rs, n) -> new CabeceraPrecalificacion(rs.getLong("id"),
                        rs.getObject("codigo_postulacion", UUID.class), rs.getString("nombres"),
                        rs.getString("apellidos"), rs.getString("ci"), rs.getString("convocatoria"),
                        rs.getString("estado"), rs.getInt("intentos_precalificacion"),
                        instante(rs, "precalificacion_revision_en"), rs.getBigDecimal("puntaje_total"),
                        rs.getString("version"), instante(rs, "fecha_calculo"), rs.getString("detalle")))
                .stream().findFirst();
    }

    /**
     * El lugar de una postulacion `calificada` en la lista priorizada de su convocatoria (el mismo orden del CU06);
     * vacio si no esta calificada.
     */
    public Optional<Long> posicion(long postulacionId) {
        return jdbc.query("""
                WITH lista AS (
                    SELECT po.id,
                           ROW_NUMBER() OVER (ORDER BY pu.puntaje_total DESC, po.fecha_envio ASC NULLS LAST,
                                              po.id ASC) AS posicion
                    FROM postulacion po
                    JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente
                         AND pu.eliminado_en IS NULL
                    WHERE po.estado = 'calificada' AND po.eliminado_en IS NULL
                      AND po.convocatoria_id = (SELECT convocatoria_id FROM postulacion WHERE id = :id)
                )
                SELECT posicion FROM lista WHERE id = :id
                """, new MapSqlParameterSource("id", postulacionId), (rs, n) -> rs.getLong("posicion"))
                .stream().findFirst();
    }

    /** Los cambios de estado de la postulacion, del mas antiguo al mas reciente. */
    public List<CambioEstado> historial(long postulacionId) {
        return jdbc.query("""
                SELECT estado_nuevo::text AS estado_nuevo, motivo, creado_en
                FROM postulacion_historial
                WHERE postulacion_id = :id AND eliminado_en IS NULL
                ORDER BY creado_en, id
                """, new MapSqlParameterSource("id", postulacionId),
                (rs, n) -> new CambioEstado(rs.getString("estado_nuevo"), rs.getString("motivo"),
                        instante(rs, "creado_en")));
    }

    private static Instant instante(java.sql.ResultSet rs, String columna) throws java.sql.SQLException {
        java.sql.Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toInstant();
    }
}
