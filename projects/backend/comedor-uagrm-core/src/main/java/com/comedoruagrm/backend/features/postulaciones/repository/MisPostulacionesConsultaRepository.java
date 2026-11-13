package com.comedoruagrm.backend.features.postulaciones.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas propias de «Mis postulaciones» (CU04): el listado y el detalle de las postulaciones enviadas de un
 * postulante.
 *
 * * Se usa JDBC porque cruza postulacion, convocatoria, gestion, publicacion, historial, finalista, adjudicacion
 * * y notificacion sin leer el repository de otro modulo. Todo respeta el borrado logico (`eliminado_en IS NULL`).
 * * Solo lee: ninguna consulta escribe ni marca avisos como leidos.
 * * Cada metodo resuelve su lectura en un numero fijo de consultas (la pagina y el total en el listado): nada se
 * * consulta dentro de un bucle.
 * ! Nunca selecciona CI, correo, datos socioeconomicos ni quien hizo un cambio del historial.
 */
@Repository
@RequiredArgsConstructor
public class MisPostulacionesConsultaRepository {

    /** Las postulaciones enviadas de un postulante (`:usuario`): todo menos los borradores. */
    private static final String ENVIADAS_DESDE = """
            FROM postulacion po
            JOIN convocatoria cv ON cv.id = po.convocatoria_id AND cv.eliminado_en IS NULL
            JOIN gestion_academica g ON g.id = cv.gestion_id
            WHERE po.postulante_id = :usuario AND po.eliminado_en IS NULL
              AND po.estado <> CAST('borrador' AS estado_postulacion)
            """;

    /** Las columnas de una fila de postulacion enviada (para `FilaListado`). */
    private static final String COLUMNAS = """
            SELECT po.id, po.codigo_postulacion, cv.codigo_convocatoria, cv.nombre AS convocatoria,
                   g.codigo_gestion_academica, g.etiqueta, po.fecha_envio, CAST(po.estado AS text) AS estado,
                   po.numero_envios,
                   EXISTS (SELECT 1 FROM publicacion_resultado pr
                           WHERE pr.convocatoria_id = cv.id AND pr.eliminado_en IS NULL) AS publicada,
                   EXISTS (SELECT 1 FROM adjudicacion a
                           WHERE a.postulacion_id = po.id AND a.eliminado_en IS NULL) AS decision_final
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Una postulacion enviada del listado. `estado` es el real (el service calcula el visible) y
     * `resultadosPublicados` dice si la convocatoria ya tiene su fila de `publicacion_resultado` y
     * `decisionFinal` si la postulacion tiene su fila de `adjudicacion` (CU05). Sin esa fila, un «no adjudicada»
     * es un rechazo del CU20, que se ve de inmediato.
     */
    public record FilaListado(UUID codigo, UUID convocatoriaCodigo, String convocatoriaNombre,
            UUID gestionCodigo, String gestionNombre, Instant fechaEnvio, String estado, short numeroEnvios,
            boolean resultadosPublicados, boolean decisionFinal) {
    }

    /** La cabecera del detalle: la fila del listado y el id interno para las demas lecturas. */
    public record CabeceraDetalle(long id, FilaListado fila) {
    }

    /** Un cambio de estado del historial. Nunca lleva quien lo hizo. */
    public record FilaHistorial(String estadoNuevo, Instant fecha, String motivo) {
    }

    /** El bloque de finalista: el plazo (puede ser nulo) y si la documentacion fisica ya fue recibida. */
    public record DatosFinalista(LocalDate plazoEntrega, boolean documentacionRecibida) {
    }

    /** Una pagina de filas y el total de filas que cumplen el filtro. */
    public record Pagina<T>(List<T> filas, long total) {
    }

    /**
     * Una pagina de las postulaciones enviadas del usuario, de la mas reciente a la mas antigua (o al reves si
     * `ascendente`), con el filtro opcional por la gestion academica (codigo publico). Estado, gestion y
     * publicacion salen en una sola consulta.
     */
    public Pagina<FilaListado> listar(long usuarioId, UUID gestionAcademica, boolean ascendente, Pageable pageable) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("usuario", usuarioId);
        String filtro = "";
        if (gestionAcademica != null) {
            filtro = " AND g.codigo_gestion_academica = CAST(:gestion AS uuid)";
            p.addValue("gestion", gestionAcademica.toString(), Types.VARCHAR);
        }
        Long total = jdbc.queryForObject("SELECT count(*) " + ENVIADAS_DESDE + filtro, p, Long.class);
        p.addValue("limite", pageable.getPageSize()).addValue("desplazamiento", pageable.getOffset());
        String sentido = ascendente ? "ASC" : "DESC";
        List<FilaListado> filas = jdbc.query(COLUMNAS + ENVIADAS_DESDE + filtro + " ORDER BY po.fecha_envio "
                + sentido + " NULLS LAST, po.id " + sentido + " LIMIT :limite OFFSET :desplazamiento", p,
                (rs, n) -> aFila(rs));
        return new Pagina<>(filas, total == null ? 0 : total);
    }

    /**
     * La cabecera de una postulacion enviada del usuario, o vacio si no existe, es de otra persona, esta
     * eliminada o todavia es un borrador (los tres casos son indistinguibles para quien consulta).
     */
    public Optional<CabeceraDetalle> cabecera(long usuarioId, UUID postulacionCodigo) {
        return jdbc.query(COLUMNAS + ENVIADAS_DESDE + " AND po.codigo_postulacion = CAST(:codigo AS uuid)",
                new MapSqlParameterSource().addValue("usuario", usuarioId)
                        .addValue("codigo", postulacionCodigo.toString(), Types.VARCHAR),
                (rs, n) -> new CabeceraDetalle(rs.getLong("id"), aFila(rs)))
                .stream().findFirst();
    }

    /** El historial de la postulacion en orden (el mas antiguo primero), sin quien hizo cada cambio. */
    public List<FilaHistorial> historial(long postulacionId) {
        return jdbc.query("""
                SELECT CAST(estado_nuevo AS text) AS estado_nuevo, creado_en, motivo
                FROM postulacion_historial
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                ORDER BY creado_en, id
                """, new MapSqlParameterSource("postulacion", postulacionId),
                (rs, n) -> new FilaHistorial(rs.getString("estado_nuevo"),
                        instante(rs.getObject("creado_en", OffsetDateTime.class)), rs.getString("motivo")));
    }

    /** El bloque de finalista, o vacio si la postulacion no es de un finalista. */
    public Optional<DatosFinalista> finalista(long postulacionId) {
        return jdbc.query("""
                SELECT plazo_entrega, documentacion_recibida_en IS NOT NULL AS recibida
                FROM finalista
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                """, new MapSqlParameterSource("postulacion", postulacionId),
                (rs, n) -> new DatosFinalista(rs.getObject("plazo_entrega", LocalDate.class),
                        rs.getBoolean("recibida")))
                .stream().findFirst();
    }

    /** `ADJUDICADA` o `NO_ADJUDICADA` si la postulacion tiene su decision final (CU05); vacio si no. */
    public Optional<String> decisionFinal(long postulacionId) {
        return jdbc.query("""
                SELECT CAST(resultado AS text) AS resultado FROM adjudicacion
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                """, new MapSqlParameterSource("postulacion", postulacionId), (rs, n) -> rs.getString("resultado"))
                .stream().findFirst();
    }

    /**
     * El mensaje del ultimo aviso de finalista (CU21) ligado a la postulacion, o vacio si no hay. El aviso no
     * tiene una columna de evento: se reconoce por su titulo, que viene de la plantilla `FINALISTA_NOTIFICADO`.
     */
    public Optional<String> mensajeDelAvisoDeFinalista(long postulacionId) {
        return jdbc.query("""
                SELECT mensaje FROM notificacion
                WHERE postulacion_id = :postulacion AND eliminado_en IS NULL
                  AND titulo = 'Entrega de documentación física - Beca Comedor'
                ORDER BY creado_en DESC, id DESC
                LIMIT 1
                """, new MapSqlParameterSource("postulacion", postulacionId), (rs, n) -> rs.getString("mensaje"))
                .stream().findFirst();
    }

    private static FilaListado aFila(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FilaListado(rs.getObject("codigo_postulacion", UUID.class),
                rs.getObject("codigo_convocatoria", UUID.class), rs.getString("convocatoria"),
                rs.getObject("codigo_gestion_academica", UUID.class), rs.getString("etiqueta"),
                instante(rs.getObject("fecha_envio", OffsetDateTime.class)), rs.getString("estado"),
                rs.getShort("numero_envios"), rs.getBoolean("publicada"), rs.getBoolean("decision_final"));
    }

    private static Instant instante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
