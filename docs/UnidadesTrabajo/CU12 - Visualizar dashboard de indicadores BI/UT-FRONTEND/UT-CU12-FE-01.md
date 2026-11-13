# UT-CU12-FE-01 — API: hooks, tipos, keys y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-01` |
| **Nombre** | API: hooks, tipos, keys y mensajes de error |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-BE-07` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`, `apiClient`, feature `api/`) y [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).
- Como referencia: `F/features/resultados/api/` (CU-22) y `F/features/priorizacion/api/listaPriorizadaApi.ts` (descarga de PDF).

## Solicitud de negocio

La pantalla necesita hablar con el backend con tipos y mensajes claros, sin que cada componente arme sus llamadas.

## Objetivo

La feature tiene su capa de API: tipos, claves de consulta, hooks y mapeo de errores.

## Alcance

- `F/features/dashboard/types/`: tipos que reflejan exactamente los DTOs del backend (`FiltrosDashboard`, `Indicadores`, `Totales`, `IndicadorMensual`, `asistenciaPromedio` y `asistencia` como `number | null`).
- `F/features/dashboard/api/keys.ts`: `dashboardKeys` (`all`, `filtros()`, `indicadores(params)`), mismo patrón que `usuariosKeys`.
- `F/features/dashboard/api/dashboardApi.ts` con `apiClient` (nunca una instancia nueva de axios): filtros, indicadores (con `filter[gestion]` y `filter[convocatoria]` solo si tienen valor) y PDF (respuesta `blob`, nombre de archivo del `Content-Disposition`).
- Hooks: `useFiltrosDashboard`, `useIndicadores(params)` (con `placeholderData` para no parpadear al cambiar de selección) y `useDescargarDashboard` (mutación; no guarda el PDF en caché).
- `F/features/dashboard/api/mapearErrorDashboard.ts`: `ERR_DAS_01` → «La gestión o la convocatoria elegida ya no existe»; `ERR_DAS_02` → «No hay datos para exportar»; otro error → `getErrorMessage`.
- `F/features/dashboard/index.ts` (barrel) con lo que otras partes pueden importar.
- Tests (con `apiClient` simulado): manda solo los filtros que tienen valor; cada hook usa su key; el mapeo de errores devuelve el texto correcto; la descarga devuelve el nombre del servidor.

Archivos esperados (crear): tipos, `keys.ts`, `dashboardApi.ts`, los hooks, `mapearErrorDashboard.ts`, `index.ts` y sus tests en `tests/`.

## Fuera de alcance (no tocar)

- Componentes y pantallas (UT-02 a UT-04).
- Mutaciones que invaliden caché: el dashboard es solo lectura.
- `F/store/apiClient.ts`.

## Resultado esperado

Cualquier pantalla puede consultar el dashboard con un hook, sin conocer el contrato HTTP.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
