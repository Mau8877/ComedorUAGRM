# UT-CU12-FE-07 — Descarga del análisis en Excel

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-07` |
| **Nombre** | Descarga del análisis en Excel |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-FE-05`, `UT-CU12-FE-06` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (mutaciones); mismo patrón que `UT-CU12-FE-04`.

## Solicitud de negocio

El Directivo se lleva el análisis para trabajarlo en una hoja de cálculo.

## Objetivo

Un botón descarga el Excel de la selección aplicada.

## Alcance

- `F/features/dashboard/components/DescargarExcelButton.tsx`, junto al botón de PDF: usa `useDescargarBiExcel` con la selección aplicada, se bloquea mientras genera, se deshabilita sin datos y avisa con toasts (`ERR_DAS_02`, `ERR_DAS_01`, error de red).

## Fuera de alcance (no tocar)

- Otros formatos.

## Resultado esperado

El Excel se descarga con el nombre que manda el servidor.

## Validación

1. `AnalisisBi.test.tsx`: pide `/dashboard/bi/excel` con `responseType: blob`, descarga el archivo y avisa.
2. `pnpm lint` limpio.
