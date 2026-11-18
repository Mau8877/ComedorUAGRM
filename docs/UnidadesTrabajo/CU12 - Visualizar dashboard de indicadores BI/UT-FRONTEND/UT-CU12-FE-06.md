# UT-CU12-FE-06 — Pantalla: análisis BI (embudo, facultades, convocatorias, puntajes, cobertura, asistencia, tendencia)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-06` |
| **Nombre** | Pantalla: análisis BI (embudo, facultades, convocatorias, puntajes, cobertura, asistencia, tendencia) |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-FE-03`, `UT-CU12-FE-05` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (solo tokens de tema; los anchos de barra dinámicos son estilo en línea) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Solicitud de negocio

El Directivo debe ver el análisis completo en la misma pantalla del dashboard.

## Objetivo

Los siete bloques del análisis dentro de `DashboardScreen`, con filtro cruzado.

## Alcance

- Componentes en `F/features/dashboard/components/`: `EmbudoProceso` (barras con conversión por etapa), `RankingFacultades` (barras postulaciones/adjudicadas; elegir una facultad muestra sus carreras, filtro cruzado solo de pantalla), `ComparacionConvocatorias` (tabla con variación; elegir una convocatoria filtra el dashboard), `HistogramaPuntajes`, `CoberturaCupos`, `AsistenciaPorDiaSemana` (con las becas en riesgo) y `TendenciaGestiones`; helpers de `formato.ts` (`formatearVariacion`, `formatearPorcentajeONulo`, `nombreDia`).
- `DashboardScreen`: `useBi(aplicada)`; sección «Análisis BI» solo si hay datos; si falla solo el análisis, aviso con «Reintentar» sin perder los indicadores; la tarjeta de asistencia muestra el valor real.

## Fuera de alcance (no tocar)

- El botón de Excel (`UT-CU12-FE-07`).
- Proyecciones a futuro.

## Resultado esperado

La pantalla muestra el análisis BI y permite filtrar por convocatoria desde la comparación.

## Validación

1. `AnalisisBi.test.tsx` y `formato.test.ts`: embudo, cobertura, becas en riesgo, facultad → carreras, convocatoria → filtro, sin datos, fallo del análisis.
2. `pnpm lint`, `pnpm exec tsc -b` y `pnpm exec vitest run src/features/dashboard` en verde.
