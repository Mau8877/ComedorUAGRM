package com.comedoruagrm.backend.features.mibeca.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.mibeca.dto.AsistenciaBecadoResponse;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas del becado sobre su propia beca (CU24): la beca con su convocatoria, gestion y ultimo movimiento, y su
 * historial de asistencia.
 *
 * * Se usa JDBC porque cruza usuario, beca, convocatoria, gestion, adjudicacion, movimiento y asistencia sin leer
 * * el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`). Solo lee.
 * * La beca se busca siempre por el codigo del usuario del token: no hay forma de pedir la de otra persona.
 */
@Repository
@RequiredArgsConstructor
public class MiBecaConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** La beca con lo que muestra la pantalla; `motivo` y `fechaAdjudicacion` pueden ser nulos. */
    public record Beca(long id, UUID codigo, String estado, String convocatoria, String gestion,
            Instant fechaAdjudicacion, LocalDate fechaInicio, LocalDate fechaFin, String motivo,
            long totalAsistencias) {
    }

    /**
     * La beca del usuario. Si tiene mas de una (una por convocatoria) se prefiere la vigente y, si no hay, la mas
     * reciente. Vacio si no tiene ninguna.
     */
    public Optional<Beca> buscarBeca(UUID usuarioCodigo) {
        return jdbc.query("""
                SELECT b.id, b.codigo_beca, CAST(b.estado AS text) AS estado, cv.nombre AS convocatoria,
                       g.etiqueta AS gestion, ad.fecha_adjudicacion, b.fecha_inicio, b.fecha_fin,
                       (SELECT m.motivo FROM movimiento_beca m
                         WHERE m.beca_id = b.id AND m.eliminado_en IS NULL
                           AND CAST(m.tipo AS text) IN ('BAJA', 'SUSPENSION')
                         ORDER BY m.id DESC LIMIT 1) AS motivo,
                       (SELECT count(*) FROM asistencia a
                         WHERE a.beca_id = b.id AND a.eliminado_en IS NULL) AS total
                FROM usuario u
                JOIN beca b ON b.postulante_id = u.id AND b.eliminado_en IS NULL
                JOIN convocatoria cv ON cv.id = b.convocatoria_id
                JOIN gestion_academica g ON g.id = cv.gestion_id
                LEFT JOIN adjudicacion ad ON ad.id = b.adjudicacion_id AND ad.eliminado_en IS NULL
                WHERE u.codigo_usuario = CAST(:usuario AS uuid) AND u.eliminado_en IS NULL
                ORDER BY (CAST(b.estado AS text) = 'VIGENTE') DESC, b.id DESC
                LIMIT 1
                """, new MapSqlParameterSource().addValue("usuario", usuarioCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new Beca(rs.getLong("id"), rs.getObject("codigo_beca", UUID.class),
                        rs.getString("estado"), rs.getString("convocatoria"), rs.getString("gestion"),
                        instante(rs.getObject("fecha_adjudicacion", OffsetDateTime.class)),
                        rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                        rs.getString("motivo"), rs.getLong("total")))
                .stream().findFirst();
    }

    /** Cuantas asistencias de la beca caen en el rango (extremos incluidos; un extremo nulo no limita). */
    public long contarAsistencias(long becaId, LocalDate desde, LocalDate hasta) {
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM asistencia a
                WHERE a.beca_id = :beca AND a.eliminado_en IS NULL
                  AND (CAST(:desde AS date) IS NULL OR a.fecha >= CAST(:desde AS date))
                  AND (CAST(:hasta AS date) IS NULL OR a.fecha <= CAST(:hasta AS date))
                """, parametros(becaId, desde, hasta), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Una pagina de asistencias de la beca. `orden` es un fragmento fijo (`ASC` o `DESC`) que elige el `service`,
     * nunca texto del cliente.
     */
    public List<AsistenciaBecadoResponse> listarAsistencias(long becaId, LocalDate desde, LocalDate hasta,
            boolean descendente, int limite, long desplazamiento) {
        String direccion = descendente ? "DESC" : "ASC";
        return jdbc.query("""
                SELECT a.fecha, a.hora_registro FROM asistencia a
                WHERE a.beca_id = :beca AND a.eliminado_en IS NULL
                  AND (CAST(:desde AS date) IS NULL OR a.fecha >= CAST(:desde AS date))
                  AND (CAST(:hasta AS date) IS NULL OR a.fecha <= CAST(:hasta AS date))
                ORDER BY a.fecha %s
                LIMIT :limite OFFSET :desplazamiento
                """.formatted(direccion),
                parametros(becaId, desde, hasta).addValue("limite", limite).addValue("desplazamiento",
                        desplazamiento),
                (rs, n) -> new AsistenciaBecadoResponse(rs.getObject("fecha", LocalDate.class),
                        instante(rs.getObject("hora_registro", OffsetDateTime.class))));
    }

    private static MapSqlParameterSource parametros(long becaId, LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource().addValue("beca", becaId)
                .addValue("desde", desde == null ? null : desde.toString(), Types.VARCHAR)
                .addValue("hasta", hasta == null ? null : hasta.toString(), Types.VARCHAR);
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
