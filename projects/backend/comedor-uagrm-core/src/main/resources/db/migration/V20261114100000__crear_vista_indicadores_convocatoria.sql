-- =====================================================================
-- Indicadores del dashboard BI (CU12)
-- Vista materializada: mv_indicadores_convocatoria (sin tablas nuevas)
-- Fuente: diseno logico de la base de datos, seccion BI (docs/DiagramaConceptual.mermaid)
--
-- Una fila por (convocatoria, mes). Se refresca por lote cada noche (REFRESH ... CONCURRENTLY, por eso el
-- indice unico) y la consulta del Directivo lee solo esta vista: nunca las tablas transaccionales.
--   postulaciones_recibidas: postulaciones con fecha_envio (los borradores nunca la tienen), en el mes del envio.
--   finalistas:              filas de finalista, en el mes de su creacion.
--   adjudicadas:             adjudicaciones ADJUDICADA, SOLO de convocatorias con resultados publicados
--                            (fila vigente en publicacion_resultado), en el mes de fecha_adjudicacion.
--   calculado_en:            momento del refresco; la «ultima actualizacion» es su maximo.
-- Los meses se calculan en America/La_Paz; `mes` es el primer dia del mes.
--
-- TODO CU-10: agregar la asistencia (asistencias y promedio por mes) cuando exista el registro de asistencia.
-- =====================================================================

CREATE MATERIALIZED VIEW mv_indicadores_convocatoria AS
SELECT d.convocatoria_id,
       c.gestion_id,
       d.mes,
       CAST(sum(d.postulaciones_recibidas) AS integer) AS postulaciones_recibidas,
       CAST(sum(d.finalistas) AS integer)              AS finalistas,
       CAST(sum(d.adjudicadas) AS integer)             AS adjudicadas,
       now()                                           AS calculado_en
FROM (
    SELECT po.convocatoria_id,
           CAST(date_trunc('month', po.fecha_envio AT TIME ZONE 'America/La_Paz') AS date) AS mes,
           count(*)           AS postulaciones_recibidas,
           CAST(0 AS bigint)  AS finalistas,
           CAST(0 AS bigint)  AS adjudicadas
    FROM postulacion po
    WHERE po.fecha_envio IS NOT NULL AND po.eliminado_en IS NULL
    GROUP BY po.convocatoria_id, 2

    UNION ALL

    SELECT po.convocatoria_id,
           CAST(date_trunc('month', f.creado_en AT TIME ZONE 'America/La_Paz') AS date) AS mes,
           CAST(0 AS bigint),
           count(*),
           CAST(0 AS bigint)
    FROM finalista f
    JOIN postulacion po ON po.id = f.postulacion_id AND po.eliminado_en IS NULL
    WHERE f.eliminado_en IS NULL
    GROUP BY po.convocatoria_id, 2

    UNION ALL

    SELECT po.convocatoria_id,
           CAST(date_trunc('month', a.fecha_adjudicacion AT TIME ZONE 'America/La_Paz') AS date) AS mes,
           CAST(0 AS bigint),
           CAST(0 AS bigint),
           count(*)
    FROM adjudicacion a
    JOIN postulacion po ON po.id = a.postulacion_id AND po.eliminado_en IS NULL
    WHERE a.resultado = 'ADJUDICADA' AND a.eliminado_en IS NULL
      AND EXISTS (SELECT 1 FROM publicacion_resultado pr
                  WHERE pr.convocatoria_id = po.convocatoria_id AND pr.eliminado_en IS NULL)
    GROUP BY po.convocatoria_id, 2
) d
JOIN convocatoria c ON c.id = d.convocatoria_id AND c.eliminado_en IS NULL
GROUP BY d.convocatoria_id, c.gestion_id, d.mes
WITH DATA;

-- Necesario para REFRESH MATERIALIZED VIEW CONCURRENTLY, y la consulta por convocatoria y mes.
CREATE UNIQUE INDEX uq_mv_indicadores_convocatoria_mes ON mv_indicadores_convocatoria (convocatoria_id, mes);
-- La consulta del dashboard filtra por gestion.
CREATE INDEX ix_mv_indicadores_convocatoria_gestion ON mv_indicadores_convocatoria (gestion_id);
