# UT-CU12-FE-05 — API del análisis BI: tipos, keys y hooks

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-05` |
| **Nombre** | API del análisis BI: tipos, keys y hooks |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-FE-01`, `UT-CU12-BE-12` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md): `queryKey` por fábrica y hooks en `api/`.

## Solicitud de negocio

La pantalla necesita pedir el análisis BI y su Excel.

## Objetivo

Tipos, keys y hooks del análisis BI.

## Alcance

- `F/features/dashboard/types/index.ts`: `Bi` y sus partes (`Embudo`, `FacultadBi`, `CarreraBi`, `ConvocatoriaComparada`, `TramoPuntaje`, `Cobertura`, `AsistenciaBi`, `AsistenciaDia`, `GestionTendencia`), igual al `BiResponse` del backend.
- `F/features/dashboard/api/keys.ts`: `dashboardKeys.bi(seleccion)`.
- `F/features/dashboard/api/dashboardApi.ts`: `obtenerBiApi` y `descargarBiExcelApi` (nombre `.xlsx` del `Content-Disposition`).
- `F/features/dashboard/api/useDashboard.ts`: `useBi` (conserva los datos anteriores mientras llegan los nuevos) y `useDescargarBiExcel` (mutación, sin caché).

## Fuera de alcance (no tocar)

- Componentes y pantalla (`UT-CU12-FE-06`).

## Resultado esperado

Los hooks del análisis existen y usan el `apiClient` compartido.

## Validación

1. `dashboardApi.test.ts` y `useDashboard.test.tsx`: la consulta lleva los filtros y la descarga conserva el nombre del servidor.
2. `pnpm exec tsc -b` sin errores.
