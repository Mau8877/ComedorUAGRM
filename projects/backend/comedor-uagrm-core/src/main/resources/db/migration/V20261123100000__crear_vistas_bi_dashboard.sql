-- =====================================================================
-- Vistas del dashboard BI (CU12, version analitica)
-- Vistas materializadas nuevas (sin tablas transaccionales): mv_bi_asistencia_dia, mv_bi_convocatoria,
-- mv_bi_carrera_convocatoria y mv_bi_puntaje_convocatoria. Se suman a mv_indicadores_convocatoria.
-- Fuente: diseno logico de la base de datos, seccion BI (docs/DiagramaConceptual.mermaid)
--
-- Modelo estrella pequeno: las convocatorias (y su gestion) son la dimension comun; cada vista es una tabla de
-- hechos con su propio grano. Se refrescan por lote cada noche con REFRESH ... CONCURRENTLY (por eso cada una
-- tiene su indice unico) y el dashboard del Directivo lee SOLO estas vistas, nunca las tablas transaccionales.
--
--   mv_bi_asistencia_dia:          grano (convocatoria, fecha). asistieron = asistencias registradas ese dia;
--                                  esperados = becas vigentes ese dia segun fecha_inicio y fecha_fin de la beca
--                                  (nunca menos que asistieron, para que la tasa no pase de 100 %).
--   mv_bi_convocatoria:            grano (convocatoria). Embudo (recibidas, precalificadas, finalistas,
--                                  adjudicadas), padron de becas por estado, reemplazos, puntaje promedio y
--                                  becas en riesgo de asistencia. Las adjudicadas cuentan SOLO con resultados
--                                  publicados, igual que mv_indicadores_convocatoria.
--   mv_bi_carrera_convocatoria:    grano (convocatoria, carrera) con su facultad: postulaciones y adjudicadas.
--   mv_bi_puntaje_convocatoria:    grano (convocatoria, tramo de 10 puntos, 0 a 6) con el puntaje vigente.
--
-- Criterio adoptado (a validar con la DUBSS): tasa de asistencia = asistieron / esperados; una beca esta «en
-- riesgo» cuando, con al menos 5 dias de servicio desde su inicio, asistio menos del 60 % de ellos.
-- =====================================================================

CREATE MATERIALIZED VIEW mv_bi_asistencia_dia AS
WITH dias AS (
    SELECT b.convocatoria_id, a.fecha, count(*) AS asistieron
    FROM asistencia a
    JOIN beca b ON b.id = a.beca_id AND b.eliminado_en IS NULL
    WHERE a.eliminado_en IS NULL
    GROUP BY b.convocatoria_id, a.fecha
)
SELECT d.convocatoria_id,
       c.gestion_id,
       d.fecha,
       CAST(d.asistieron AS integer) AS asistieron,
       CAST(greatest(d.asistieron, (
           SELECT count(*) FROM beca b
           WHERE b.convocatoria_id = d.convocatoria_id AND b.eliminado_en IS NULL
             AND b.fecha_inicio <= d.fecha AND (b.fecha_fin IS NULL OR b.fecha_fin >= d.fecha)
       )) AS integer) AS esperados,
       now() AS calculado_en
FROM dias d
JOIN convocatoria c ON c.id = d.convocatoria_id AND c.eliminado_en IS NULL
WITH DATA;

CREATE UNIQUE INDEX uq_mv_bi_asistencia_dia ON mv_bi_asistencia_dia (convocatoria_id, fecha);
CREATE INDEX ix_mv_bi_asistencia_dia_gestion ON mv_bi_asistencia_dia (gestion_id);

CREATE MATERIALIZED VIEW mv_bi_convocatoria AS
SELECT c.id AS convocatoria_id,
       c.gestion_id,
       c.nombre,
       c.fecha_apertura,
       c.cupos,
       CAST(coalesce(p.recibidas, 0) AS integer)       AS recibidas,
       CAST(coalesce(p.precalificadas, 0) AS integer)  AS precalificadas,
       p.puntaje_promedio,
       CAST(coalesce(f.finalistas, 0) AS integer)      AS finalistas,
       CAST(coalesce(a.adjudicadas, 0) AS integer)     AS adjudicadas,
       CAST(coalesce(b.vigentes, 0) AS integer)        AS becas_vigentes,
       CAST(coalesce(b.suspendidas, 0) AS integer)     AS becas_suspendidas,
       CAST(coalesce(b.bajas, 0) AS integer)           AS becas_baja,
       CAST(coalesce(r.reemplazos, 0) AS integer)      AS reemplazos,
       CAST(coalesce(rg.en_riesgo, 0) AS integer)      AS becas_en_riesgo,
       now()                                           AS calculado_en
FROM convocatoria c
LEFT JOIN (
    SELECT po.convocatoria_id,
           count(*) AS recibidas,
           count(*) FILTER (WHERE pu.id IS NOT NULL) AS precalificadas,
           round(avg(pu.puntaje_total), 2) AS puntaje_promedio
    FROM postulacion po
    LEFT JOIN puntaje_postulacion pu
           ON pu.postulacion_id = po.id AND pu.vigente AND pu.eliminado_en IS NULL
    WHERE po.fecha_envio IS NOT NULL AND po.eliminado_en IS NULL
    GROUP BY po.convocatoria_id
) p ON p.convocatoria_id = c.id
LEFT JOIN (
    SELECT po.convocatoria_id, count(*) AS finalistas
    FROM finalista fi
    JOIN postulacion po ON po.id = fi.postulacion_id AND po.eliminado_en IS NULL
    WHERE fi.eliminado_en IS NULL
    GROUP BY po.convocatoria_id
) f ON f.convocatoria_id = c.id
LEFT JOIN (
    SELECT po.convocatoria_id, count(*) AS adjudicadas
    FROM adjudicacion ad
    JOIN postulacion po ON po.id = ad.postulacion_id AND po.eliminado_en IS NULL
    WHERE ad.resultado = 'ADJUDICADA' AND ad.eliminado_en IS NULL
      AND EXISTS (SELECT 1 FROM publicacion_resultado pr
                  WHERE pr.convocatoria_id = po.convocatoria_id AND pr.eliminado_en IS NULL)
    GROUP BY po.convocatoria_id
) a ON a.convocatoria_id = c.id
LEFT JOIN (
    SELECT convocatoria_id,
           count(*) FILTER (WHERE estado = 'VIGENTE')    AS vigentes,
           count(*) FILTER (WHERE estado = 'SUSPENDIDA') AS suspendidas,
           count(*) FILTER (WHERE estado = 'BAJA')       AS bajas
    FROM beca
    WHERE eliminado_en IS NULL
    GROUP BY convocatoria_id
) b ON b.convocatoria_id = c.id
LEFT JOIN (
    SELECT be.convocatoria_id, count(*) AS reemplazos
    FROM movimiento_beca m
    JOIN beca be ON be.id = m.beca_id AND be.eliminado_en IS NULL
    WHERE m.tipo = 'REEMPLAZO' AND m.eliminado_en IS NULL
    GROUP BY be.convocatoria_id
) r ON r.convocatoria_id = c.id
LEFT JOIN (
    SELECT t.convocatoria_id, count(*) AS en_riesgo
    FROM (
        SELECT be.convocatoria_id, be.id,
               (SELECT count(*) FROM mv_bi_asistencia_dia d
                WHERE d.convocatoria_id = be.convocatoria_id AND d.fecha >= be.fecha_inicio
                  AND (be.fecha_fin IS NULL OR d.fecha <= be.fecha_fin)) AS dias_servicio,
               (SELECT count(*) FROM asistencia a
                WHERE a.beca_id = be.id AND a.eliminado_en IS NULL) AS asistencias
        FROM beca be
        WHERE be.estado = 'VIGENTE' AND be.eliminado_en IS NULL
    ) t
    WHERE t.dias_servicio >= 5 AND t.asistencias < 0.6 * t.dias_servicio
    GROUP BY t.convocatoria_id
) rg ON rg.convocatoria_id = c.id
WHERE c.eliminado_en IS NULL
WITH DATA;

CREATE UNIQUE INDEX uq_mv_bi_convocatoria ON mv_bi_convocatoria (convocatoria_id);
CREATE INDEX ix_mv_bi_convocatoria_gestion ON mv_bi_convocatoria (gestion_id);

CREATE MATERIALIZED VIEW mv_bi_carrera_convocatoria AS
SELECT po.convocatoria_id,
       c.gestion_id,
       ca.id AS carrera_id,
       ca.facultad,
       ca.nombre AS carrera,
       CAST(count(*) AS integer) AS postulaciones,
       CAST(count(*) FILTER (WHERE ad.id IS NOT NULL AND EXISTS (
           SELECT 1 FROM publicacion_resultado pr
           WHERE pr.convocatoria_id = po.convocatoria_id AND pr.eliminado_en IS NULL)) AS integer) AS adjudicadas,
       now() AS calculado_en
FROM postulacion po
JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
JOIN postulante pt ON pt.usuario_id = po.postulante_id AND pt.eliminado_en IS NULL
JOIN carrera ca ON ca.id = pt.carrera_id
LEFT JOIN adjudicacion ad
       ON ad.postulacion_id = po.id AND ad.resultado = 'ADJUDICADA' AND ad.eliminado_en IS NULL
WHERE po.fecha_envio IS NOT NULL AND po.eliminado_en IS NULL
GROUP BY po.convocatoria_id, c.gestion_id, ca.id, ca.facultad, ca.nombre
WITH DATA;

CREATE UNIQUE INDEX uq_mv_bi_carrera_convocatoria ON mv_bi_carrera_convocatoria (convocatoria_id, carrera_id);
CREATE INDEX ix_mv_bi_carrera_convocatoria_gestion ON mv_bi_carrera_convocatoria (gestion_id);

CREATE MATERIALIZED VIEW mv_bi_puntaje_convocatoria AS
SELECT po.convocatoria_id,
       c.gestion_id,
       CAST(least(floor(pu.puntaje_total / 10), 6) AS integer) AS tramo,
       CAST(count(*) AS integer) AS cantidad,
       now() AS calculado_en
FROM postulacion po
JOIN convocatoria c ON c.id = po.convocatoria_id AND c.eliminado_en IS NULL
JOIN puntaje_postulacion pu ON pu.postulacion_id = po.id AND pu.vigente AND pu.eliminado_en IS NULL
WHERE po.fecha_envio IS NOT NULL AND po.eliminado_en IS NULL
GROUP BY po.convocatoria_id, c.gestion_id, 3
WITH DATA;

CREATE UNIQUE INDEX uq_mv_bi_puntaje_convocatoria ON mv_bi_puntaje_convocatoria (convocatoria_id, tramo);
CREATE INDEX ix_mv_bi_puntaje_convocatoria_gestion ON mv_bi_puntaje_convocatoria (gestion_id);
