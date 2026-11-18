# UT-CU12-BE-10 — Migración: vistas materializadas del análisis BI

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-10` |
| **Nombre** | Migración: vistas materializadas del análisis BI |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-01`, `UT-CU12-BE-02` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones con timestamp, `DiagramaConceptual.mermaid` al día) y la migración de `mv_indicadores_convocatoria` como referencia.

## Solicitud de negocio

El Directivo quiere ver más que cuatro totales: facultades, puntajes, cobertura y asistencia. Esos cálculos no pueden hacerse al consultar.

## Objetivo

Cuatro vistas materializadas nuevas, refrescadas por el mismo lote nocturno.

## Alcance

- `R/db/migration/V20261123100000__crear_vistas_bi_dashboard.sql`:
  - `mv_bi_asistencia_dia`: por beca y día de servicio (asistió o no), para la tasa por día de la semana y las becas en riesgo.
  - `mv_bi_convocatoria`: por convocatoria, el embudo (postulaciones, precalificadas, finalistas, adjudicadas con resultados publicados, becas vigentes), cupos, cobertura (suspendidas, bajas, reemplazos), puntaje promedio y becas en riesgo (≥ 5 días de servicio y < 60 % de asistencia; lee `mv_bi_asistencia_dia`, por eso se refresca después).
  - `mv_bi_carrera_convocatoria`: por convocatoria, facultad y carrera, postulaciones y adjudicadas.
  - `mv_bi_puntaje_convocatoria`: por convocatoria y tramo de 10 puntos (0–70), cantidad de puntajes vigentes de la IA.
  - Cada una con un índice único (para `REFRESH … CONCURRENTLY`) y un índice por gestión.
- `RefrescoIndicadoresService`: `VISTAS` (lista pública, en orden de refresco) y `refrescarTodas(prefijo)`; refresca cada vista aunque una falle y es éxito solo si todas salen bien. `refrescarSiEstaVacia()` sigue mirando la vista original.
- `docs/DiagramaConceptual.mermaid`: comentario con las cuatro vistas.

Archivos esperados (crear/tocar): la migración, `RefrescoIndicadoresService`, `DiagramaConceptual.mermaid`.

## Fuera de alcance (no tocar)

- Tablas transaccionales nuevas o columnas nuevas en tablas de otros CU.
- El endpoint que lee las vistas (`UT-CU12-BE-11`).

## Resultado esperado

Las cuatro vistas existen, se pueblan con datos reales y se refrescan sin bloquear las lecturas.

## Validación

1. `VistasBiMigracionTest` (base real): las vistas existen con sus índices únicos y devuelven las cuentas esperadas para datos sembrados.
2. `RefrescoIndicadoresServiceTest`: se refrescan todas, en orden, y un fallo de una no impide refrescar las demás.
