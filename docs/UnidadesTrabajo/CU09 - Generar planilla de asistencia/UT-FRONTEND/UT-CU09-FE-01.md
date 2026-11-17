# UT-CU09-FE-01 — API: tipos, keys, hooks y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-01` |
| **Nombre** | API: tipos, keys, hooks y mensajes de error |
| **Caso de uso** | CU09-FE |
| **Depende de** | `UT-CU09-BE-06` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (factory de keys, `apiClient`, sin `queryKey` en componentes) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `F/features/actas/api/` (CU08).

## Solicitud de negocio

La pantalla necesita hablar con el backend de planillas sin armar llamadas a mano.

## Objetivo

Existen los tipos, las keys, las funciones de API y los hooks de la feature `planillas`, con mensajes de error en español.

## Alcance

- `F/features/planillas/types/`: `Planilla`, `ResumenPlanilla`, `PeriodoPlanilla`, `AccesoPlanilla`, `Pagina<T>` y `PlanillasParams` (`page`, `pageSize`, `sort`); sin la ruta del archivo.
- `F/features/planillas/api/keys.ts`: `planillasKeys` (`all`, `lists()`, `list(convocatoria, params)`, `resumen(convocatoria)`).
- `F/features/planillas/api/planillasApi.ts` con `apiClient`: `obtenerResumenPlanillaApi`, `listarPlanillasApi`, `generarPlanillaApi`, `obtenerAccesoPlanillaApi` y `obtenerVistaPreviaPlanillaApi` (PDF como `Blob`, con `desde` y `hasta`).
- Hooks: `useResumenPlanilla`, `usePlanillas` (conserva la página anterior), `useGenerarPlanilla` (invalida `['planillas']`), `useAccesoPlanilla` y `useVistaPreviaPlanilla` (mutaciones: no se guardan en caché).
- `mapearErrorPlanillas`: textos en español para `ERR_PRI_01`, `ERR_PLA_01` a `ERR_PLA_06`, `ERR_AUTH_07`, `ERR_SYS_00`, errores de red y 5xx; cualquier otro usa `getErrorMessage`.
- `F/features/planillas/index.ts` (barrel) y tests (mockeando `apiClient`): rutas y parámetros de cada función, claves de caché, cada código de error.

Archivos esperados (crear): tipos, `keys.ts`, `planillasApi.ts`, `hooks.ts`, `mapearErrorPlanillas.ts`, `index.ts` de `api/` y de la feature, y `planillasApi.test.ts` y `mapearErrorPlanillas.test.ts` en `tests/`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (FE-03) y el esquema del periodo (FE-02).
- Guardar el PDF o la dirección firmada en la caché o en un store.

## Resultado esperado

La pantalla importa hooks ya armados y recibe errores legibles.

## Validación

1. `pnpm test -- src/features/planillas` pasa.
2. `pnpm lint` y `pnpm exec tsc -b` sin errores.
