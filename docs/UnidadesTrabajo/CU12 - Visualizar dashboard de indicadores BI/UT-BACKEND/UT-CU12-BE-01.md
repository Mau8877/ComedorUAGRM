# UT-CU12-BE-01 — Migración: vista materializada `mv_indicadores_convocatoria`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-01` |
| **Nombre** | Migración: vista materializada `mv_indicadores_convocatoria` |
| **Caso de uso** | CU12-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md): nombre `V{yyyyMMddHHmmss}__descripcion.sql`, borrado lógico (`eliminado_en IS NULL`), diseño lógico antes que la migración.
- Como referencia: `V20261111100000__crear_publicacion_resultado.sql` (CU22).

## Solicitud de negocio

El Directivo necesita ver indicadores consolidados sin que la consulta cargue las tablas con las que opera el sistema.

## Objetivo

La base de datos tiene la vista precalculada con una fila por convocatoria y mes, lista para refrescarse por lote.

## Alcance

- Actualizar **antes** el diseño lógico (`diseno_base_de_datos.md`, sección BI) con las columnas reales de la vista y su regla de refresco.
- `R/db/migration/V{timestamp}__crear_vista_indicadores_convocatoria.sql`: `CREATE MATERIALIZED VIEW mv_indicadores_convocatoria` con una fila por `(convocatoria_id, mes)` y las columnas:
  - `convocatoria_id`, `gestion_id`, `mes` (`date`, primer día del mes en `America/La_Paz`);
  - `postulaciones_recibidas`: postulaciones con `fecha_envio` no nula y `eliminado_en IS NULL`, en el mes de `fecha_envio`;
  - `finalistas`: filas de `finalista` no eliminadas, en el mes de `creado_en`;
  - `adjudicadas`: adjudicaciones `ADJUDICADA` no eliminadas **solo de convocatorias con fila vigente en `publicacion_resultado`**, en el mes de `fecha_adjudicacion`;
  - `calculado_en` = `now()` del momento del refresco.
- Se arma con tres subconsultas agrupadas (`UNION ALL` + `GROUP BY convocatoria, mes`) para recorrer cada tabla una sola vez.
- Índice único `(convocatoria_id, mes)` (necesario para `REFRESH ... CONCURRENTLY`) e índice por `gestion_id`.
- Creada `WITH DATA`: queda poblada con lo que haya al migrar.
- Un comentario SQL: `-- TODO CU-10: agregar la asistencia (asistencias y promedio por mes) cuando exista el registro de asistencia.`
- Tests de migración con Testcontainers (se saltan sin Docker): con filas de prueba, un borrador no cuenta; una adjudicación de una convocatoria **sin** publicación no cuenta y una **con** publicación sí; los meses caen en el mes correcto; la vista acepta `REFRESH ... CONCURRENTLY`; el refresco cambia `calculado_en`.

Archivos esperados (crear/tocar): la migración, su test y el diseño lógico.

## Fuera de alcance (no tocar)

- Tablas o columnas de asistencia, becas o padrón (CU-10, CU-23).
- Tocar tablas de otros módulos.
- Código Java y la tarea de refresco (UT-02).

## Resultado esperado

Flyway aplica la migración y la vista devuelve las cifras correctas por convocatoria y mes.

## Validación

1. `./mvnw test` pasa (Flyway aplica sobre PostgreSQL real).
2. `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_convocatoria` funciona.
3. El diseño lógico describe la vista igual que la migración.
