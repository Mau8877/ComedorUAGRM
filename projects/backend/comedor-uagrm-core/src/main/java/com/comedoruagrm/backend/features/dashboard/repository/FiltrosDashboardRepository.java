package com.comedoruagrm.backend.features.dashboard.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Catalogos del dashboard (CU12): las gestiones academicas y las convocatorias que se pueden consultar.
 *
 * * Son dos consultas fijas, sin importar cuantas gestiones haya: el `service` agrupa las convocatorias por
 * * gestion en memoria. Respeta el borrado logico.
 * * Las convocatorias en BORRADOR no se ofrecen: todavia no tienen datos.
 */
@Repository
@RequiredArgsConstructor
public class FiltrosDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Una gestion academica. */
    public record GestionFila(long id, UUID codigo, String etiqueta, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    /** Una convocatoria con el id de su gestion. */
    public record ConvocatoriaFila(long gestionId, UUID codigo, String nombre) {
    }

    /** Las gestiones no eliminadas, de la mas reciente a la mas antigua. */
    public List<GestionFila> gestiones() {
        return jdbc.query("""
                SELECT id, codigo_gestion_academica, etiqueta, fecha_inicio, fecha_fin
                FROM gestion_academica
                WHERE eliminado_en IS NULL
                ORDER BY fecha_inicio DESC, id DESC
                """, (rs, n) -> new GestionFila(rs.getLong("id"), rs.getObject("codigo_gestion_academica", UUID.class),
                rs.getString("etiqueta"), rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class)));
    }

    /** Las convocatorias no eliminadas y que no son borrador, de la de apertura mas reciente a la mas antigua. */
    public List<ConvocatoriaFila> convocatorias() {
        return jdbc.query("""
                SELECT c.gestion_id, c.codigo_convocatoria, c.nombre
                FROM convocatoria c
                JOIN gestion_academica g ON g.id = c.gestion_id AND g.eliminado_en IS NULL
                WHERE c.eliminado_en IS NULL AND c.estado <> 'BORRADOR'
                ORDER BY c.fecha_apertura DESC, c.id DESC
                """, (rs, n) -> new ConvocatoriaFila(rs.getLong("gestion_id"),
                rs.getObject("codigo_convocatoria", UUID.class), rs.getString("nombre")));
    }
}
