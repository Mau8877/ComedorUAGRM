package com.comedoruagrm.backend.features.postulaciones.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas y escrituras puntuales del envio de la postulacion (CU02).
 *
 * * Se usa JDBC porque cruza usuario, postulante, carrera, convocatoria y gestion sin leer el repository de
 * * otro modulo, y porque `respuesta_socioeconomica` (PK = FK a la postulacion) se guarda con un solo
 * * `INSERT ... ON CONFLICT`. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * * El cambio de estado de la postulacion NO se hace aca: lo hace `Postulacion.enviar()` desde el service.
 */
@Repository
@RequiredArgsConstructor
public class EnvioPostulacionConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** El perfil de un postulante activo: lo que el formulario precarga y lo que necesita la IA. */
    public record Perfil(long postulanteId, String nombres, String apellidos, String ci, String carrera) {
    }

    /** La convocatoria que esta recibiendo postulaciones. */
    public record ConvocatoriaAbierta(long id, UUID codigo, String nombre, String gestion, Instant fechaCierre) {
    }

    /** La proxima convocatoria ya abierta pero que todavia no llego a su fecha de apertura. */
    public record ProximaConvocatoria(String nombre, Instant fechaApertura) {
    }

    /** Una postulacion del postulante en una convocatoria. */
    public record PostulacionActual(long id, UUID codigo, String estado, int numeroEnvios, Instant fechaEnvio) {
    }

    /** Una postulacion con su convocatoria, para enviarla. */
    public record CabeceraEnvio(long postulacionId, UUID postulacionCodigo, String estado, int numeroEnvios,
            Instant fechaEnvio, long convocatoriaId, UUID convocatoriaCodigo, String convocatoriaNombre,
            String gestion) {
    }

    /** Las respuestas del formulario (todas pueden ser nulas en un borrador). */
    public record Respuesta(BigDecimal ingresosFamiliares, Integer dependientes, String procedencia,
            String patrimonio, String estadoVulnerabilidad, BigDecimal promedioAcademico) {
    }

    /** Vacio si la cuenta no existe, esta eliminada o no es de un postulante. */
    public Optional<Perfil> perfil(UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT u.id, u.nombres, u.apellidos, u.ci, c.nombre AS carrera
                FROM usuario u
                JOIN postulante p ON p.usuario_id = u.id AND p.eliminado_en IS NULL
                JOIN carrera c ON c.id = p.carrera_id
                WHERE u.eliminado_en IS NULL AND u.codigo_usuario = CAST(:codigo AS uuid)
                """, new MapSqlParameterSource().addValue("codigo", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new Perfil(rs.getLong("id"), rs.getString("nombres"), rs.getString("apellidos"),
                        rs.getString("ci"), rs.getString("carrera")))
                .stream().findFirst();
    }

    /** La convocatoria `ABIERTA` dentro de sus fechas; si hubiera mas de una (gestiones distintas), la que cierra antes. */
    public Optional<ConvocatoriaAbierta> convocatoriaAbierta(Instant ahora) {
        return jdbc.query("""
                SELECT c.id, c.codigo_convocatoria, c.nombre, g.etiqueta, c.fecha_cierre
                FROM convocatoria c
                JOIN gestion_academica g ON g.id = c.gestion_id AND g.eliminado_en IS NULL
                WHERE c.eliminado_en IS NULL AND c.estado = 'ABIERTA'
                  AND c.fecha_apertura <= :ahora AND c.fecha_cierre >= :ahora
                ORDER BY c.fecha_cierre, c.id
                LIMIT 1
                """, new MapSqlParameterSource().addValue("ahora", java.sql.Timestamp.from(ahora)),
                (rs, n) -> new ConvocatoriaAbierta(rs.getLong("id"), rs.getObject("codigo_convocatoria", UUID.class),
                        rs.getString("nombre"), rs.getString("etiqueta"), rs.getTimestamp("fecha_cierre").toInstant()))
                .stream().findFirst();
    }

    public Optional<ProximaConvocatoria> proximaConvocatoria(Instant ahora) {
        return jdbc.query("""
                SELECT nombre, fecha_apertura
                FROM convocatoria
                WHERE eliminado_en IS NULL AND estado = 'ABIERTA' AND fecha_apertura > :ahora
                ORDER BY fecha_apertura, id
                LIMIT 1
                """, new MapSqlParameterSource().addValue("ahora", java.sql.Timestamp.from(ahora)),
                (rs, n) -> new ProximaConvocatoria(rs.getString("nombre"),
                        rs.getTimestamp("fecha_apertura").toInstant()))
                .stream().findFirst();
    }

    /** La postulacion del postulante en esa convocatoria (hay una sola: indice unico parcial). */
    public Optional<PostulacionActual> postulacionDe(long postulanteId, long convocatoriaId) {
        return jdbc.query("""
                SELECT id, codigo_postulacion, CAST(estado AS text) AS estado, numero_envios, fecha_envio
                FROM postulacion
                WHERE eliminado_en IS NULL AND postulante_id = :postulante AND convocatoria_id = :convocatoria
                """, new MapSqlParameterSource().addValue("postulante", postulanteId)
                        .addValue("convocatoria", convocatoriaId),
                (rs, n) -> actual(rs)).stream().findFirst();
    }

    /** Crea la fila en `borrador` (CU17 la completa y el envio la pasa a `pendiente`). */
    public PostulacionActual crearBorrador(long postulanteId, long convocatoriaId) {
        return jdbc.query("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, numero_envios)
                VALUES (:convocatoria, :postulante, CAST('borrador' AS estado_postulacion), 0)
                RETURNING id, codigo_postulacion, CAST(estado AS text) AS estado, numero_envios, fecha_envio
                """, new MapSqlParameterSource().addValue("postulante", postulanteId)
                        .addValue("convocatoria", convocatoriaId),
                (rs, n) -> actual(rs)).get(0);
    }

    /** La postulacion del usuario del token; vacio si no existe, esta eliminada o es de otro. */
    public Optional<CabeceraEnvio> cabecera(UUID postulacionCodigo, UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT po.id, po.codigo_postulacion, CAST(po.estado AS text) AS estado, po.numero_envios,
                       po.fecha_envio, c.id AS convocatoria_id, c.codigo_convocatoria, c.nombre, g.etiqueta
                FROM postulacion po
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
                JOIN gestion_academica g ON g.id = c.gestion_id
                WHERE po.eliminado_en IS NULL
                  AND po.codigo_postulacion = CAST(:postulacion AS uuid)
                  AND u.codigo_usuario = CAST(:usuario AS uuid)
                """, new MapSqlParameterSource().addValue("postulacion", postulacionCodigo.toString(), Types.VARCHAR)
                        .addValue("usuario", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> {
                    var envio = rs.getTimestamp("fecha_envio");
                    return new CabeceraEnvio(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class),
                            rs.getString("estado"), rs.getInt("numero_envios"),
                            envio == null ? null : envio.toInstant(), rs.getLong("convocatoria_id"),
                            rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre"),
                            rs.getString("etiqueta"));
                }).stream().findFirst();
    }

    /** Bloquea la fila de la postulacion hasta el fin de la transaccion: dos envios simultaneos se atienden en fila. */
    public void bloquearPostulacion(long postulacionId) {
        jdbc.queryForList("SELECT id FROM postulacion WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", postulacionId), Long.class);
    }

    /** El estado actual (despues del bloqueo, para ver lo que dejo la peticion que llego primero). */
    public Optional<String> estadoActual(long postulacionId) {
        return jdbc.queryForList("SELECT CAST(estado AS text) FROM postulacion WHERE id = :id",
                new MapSqlParameterSource("id", postulacionId), String.class).stream().findFirst();
    }

    public Optional<Respuesta> respuesta(long postulacionId) {
        return jdbc.query("""
                SELECT ingresos_familiares, dependientes, procedencia, patrimonio, estado_vulnerabilidad,
                       promedio_academico
                FROM respuesta_socioeconomica
                WHERE eliminado_en IS NULL AND postulacion_id = :postulacion
                """, new MapSqlParameterSource("postulacion", postulacionId),
                (rs, n) -> new Respuesta(rs.getBigDecimal("ingresos_familiares"),
                        rs.getObject("dependientes", Integer.class), rs.getString("procedencia"),
                        rs.getString("patrimonio"), rs.getString("estado_vulnerabilidad"),
                        rs.getBigDecimal("promedio_academico")))
                .stream().findFirst();
    }

    /** Crea o reemplaza las respuestas de la postulacion (una por postulacion). */
    public void guardarRespuesta(long postulacionId, Respuesta r) {
        jdbc.update("""
                INSERT INTO respuesta_socioeconomica (postulacion_id, ingresos_familiares, dependientes, procedencia,
                                                     patrimonio, estado_vulnerabilidad, promedio_academico)
                VALUES (:postulacion, :ingresos, :dependientes, :procedencia, :patrimonio, :vulnerabilidad,
                        :promedio)
                ON CONFLICT (postulacion_id) DO UPDATE SET
                    ingresos_familiares = EXCLUDED.ingresos_familiares,
                    dependientes = EXCLUDED.dependientes,
                    procedencia = EXCLUDED.procedencia,
                    patrimonio = EXCLUDED.patrimonio,
                    estado_vulnerabilidad = EXCLUDED.estado_vulnerabilidad,
                    promedio_academico = EXCLUDED.promedio_academico,
                    actualizado_en = now(),
                    eliminado_en = NULL
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId)
                        .addValue("ingresos", r.ingresosFamiliares()).addValue("dependientes", r.dependientes())
                        .addValue("procedencia", r.procedencia()).addValue("patrimonio", r.patrimonio())
                        .addValue("vulnerabilidad", r.estadoVulnerabilidad())
                        .addValue("promedio", r.promedioAcademico()));
    }

    /**
     * El JSON de `datos_extra` (los datos del formulario oficial que no tienen columna); vacio si la fila no existe
     * o todavia no lo tiene. Quien lo convierte a objeto es el service.
     */
    public Optional<String> complementarios(long postulacionId) {
        return jdbc.queryForList("""
                SELECT CAST(datos_extra AS text) FROM respuesta_socioeconomica
                WHERE eliminado_en IS NULL AND postulacion_id = :postulacion
                """, new MapSqlParameterSource("postulacion", postulacionId), String.class)
                .stream().filter(java.util.Objects::nonNull).findFirst();
    }

    /**
     * Guarda el JSON de `datos_extra` (o lo vacia si es nulo). Se llama despues de {@link #guardarRespuesta}, que
     * crea la fila y no toca esta columna.
     */
    public void guardarComplementarios(long postulacionId, String json) {
        jdbc.update("""
                UPDATE respuesta_socioeconomica
                SET datos_extra = CAST(:json AS jsonb), actualizado_en = now()
                WHERE eliminado_en IS NULL AND postulacion_id = :postulacion
                """, new MapSqlParameterSource().addValue("postulacion", postulacionId)
                        .addValue("json", json, Types.VARCHAR));
    }

    /** Fecha y hora del ultimo guardado de las respuestas (CU17); vacio si todavia no se guardo ninguna. */
    public Optional<Instant> guardadoEn(long postulacionId) {
        return jdbc.query("""
                SELECT actualizado_en FROM respuesta_socioeconomica
                WHERE eliminado_en IS NULL AND postulacion_id = :postulacion
                """, new MapSqlParameterSource("postulacion", postulacionId),
                (rs, n) -> rs.getTimestamp("actualizado_en").toInstant()).stream().findFirst();
    }

    /** El borrador de la convocatoria abierta del usuario, para «Mi borrador» (CU17). */
    public record BorradorAbierto(long postulacionId, UUID codigo, long convocatoriaId, String convocatoria,
            String gestion, Instant fechaCierre) {
    }

    /** Vacio si no tiene un borrador, o si su convocatoria ya cerro (queda bloqueado). */
    public Optional<BorradorAbierto> borradorAbierto(UUID usuarioCodigo, Instant ahora) {
        return jdbc.query("""
                SELECT po.id, po.codigo_postulacion, c.id AS convocatoria_id, c.nombre, g.etiqueta, c.fecha_cierre
                FROM postulacion po
                JOIN usuario u ON u.id = po.postulante_id AND u.eliminado_en IS NULL
                JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
                JOIN gestion_academica g ON g.id = c.gestion_id
                WHERE po.eliminado_en IS NULL AND po.estado = CAST('borrador' AS estado_postulacion)
                  AND u.codigo_usuario = CAST(:usuario AS uuid)
                  AND c.estado = 'ABIERTA' AND c.fecha_apertura <= :ahora AND c.fecha_cierre >= :ahora
                ORDER BY po.id DESC
                LIMIT 1
                """, new MapSqlParameterSource().addValue("usuario", usuarioCodigo.toString(), Types.VARCHAR)
                        .addValue("ahora", java.sql.Timestamp.from(ahora)),
                (rs, n) -> new BorradorAbierto(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class),
                        rs.getLong("convocatoria_id"), rs.getString("nombre"), rs.getString("etiqueta"),
                        rs.getTimestamp("fecha_cierre").toInstant()))
                .stream().findFirst();
    }

    private static PostulacionActual actual(java.sql.ResultSet rs) throws java.sql.SQLException {
        var envio = rs.getTimestamp("fecha_envio");
        return new PostulacionActual(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class),
                rs.getString("estado"), rs.getInt("numero_envios"), envio == null ? null : envio.toInstant());
    }
}
