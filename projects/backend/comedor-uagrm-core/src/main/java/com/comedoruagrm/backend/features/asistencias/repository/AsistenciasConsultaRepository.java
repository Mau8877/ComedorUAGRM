package com.comedoruagrm.backend.features.asistencias.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas del registro de asistencia (CU10): el becado que corresponde a un CI y la cabecera de una beca.
 *
 * * Se usa JDBC porque cruza usuario, beca, carrera, convocatoria y asistencia sin leer el repository de otro
 * * modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee.
 * ! El CI es dato personal: se usa como parametro de busqueda y nunca se escribe en un log ni en la bitacora.
 */
@Repository
@RequiredArgsConstructor
public class AsistenciasConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Un becado tal como lo ve el Personal del Comedor: la beca, sus datos y, si ya se registro, la hora de la
     * asistencia de la fecha consultada.
     */
    public record Becado(long becaId, UUID becaCodigo, String nombres, String apellidos, String ci, String carrera,
            String convocatoria, String estado, Instant horaRegistro) {
    }

    /** Lo minimo de una beca para registrar su asistencia. */
    public record CabeceraBeca(long id, UUID codigo, String estado) {
    }

    /**
     * El becado con ese CI (sin distinguir mayusculas). Si tiene mas de una beca (una por convocatoria) se
     * prefiere la vigente y, si no hay, la mas reciente. Vacio si el CI no tiene ninguna beca.
     */
    public Optional<Becado> buscarPorCi(String ci, LocalDate fecha) {
        return jdbc.query("""
                SELECT b.id, b.codigo_beca, u.nombres, u.apellidos, u.ci, c.nombre AS carrera,
                       cv.nombre AS convocatoria, b.estado::text AS estado, a.hora_registro
                FROM usuario u
                JOIN beca b ON b.postulante_id = u.id AND b.eliminado_en IS NULL
                JOIN postulante pt ON pt.usuario_id = u.id
                JOIN carrera c ON c.id = pt.carrera_id
                JOIN convocatoria cv ON cv.id = b.convocatoria_id
                LEFT JOIN asistencia a ON a.beca_id = b.id AND a.fecha = :fecha AND a.eliminado_en IS NULL
                WHERE upper(u.ci) = upper(:ci) AND u.eliminado_en IS NULL
                ORDER BY (b.estado = 'VIGENTE') DESC, b.id DESC
                LIMIT 1
                """, new MapSqlParameterSource().addValue("ci", ci, Types.VARCHAR).addValue("fecha", fecha),
                (rs, n) -> new Becado(rs.getLong("id"), rs.getObject("codigo_beca", UUID.class),
                        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("ci"),
                        rs.getString("carrera"), rs.getString("convocatoria"), rs.getString("estado"),
                        instante(rs.getObject("hora_registro", OffsetDateTime.class))))
                .stream().findFirst();
    }

    /** La beca con ese codigo publico; vacio si no existe o fue eliminada. */
    public Optional<CabeceraBeca> cabeceraBeca(UUID codigo) {
        return jdbc.query("""
                SELECT id, codigo_beca, estado::text AS estado
                FROM beca
                WHERE codigo_beca = CAST(:codigo AS uuid) AND eliminado_en IS NULL
                """, new MapSqlParameterSource().addValue("codigo", codigo.toString(), Types.VARCHAR),
                (rs, n) -> new CabeceraBeca(rs.getLong("id"), rs.getObject("codigo_beca", UUID.class),
                        rs.getString("estado")))
                .stream().findFirst();
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
