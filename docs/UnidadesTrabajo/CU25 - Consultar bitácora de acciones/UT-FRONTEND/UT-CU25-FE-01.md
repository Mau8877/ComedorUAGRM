# UT-CU25-FE-01 — Tipos, API y hooks de la bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-FE-01` |
| **Nombre** | Tipos, API y hooks de la bitácora |
| **Caso de uso** | CU25-FRONTEND |
| **Depende de** | UT-CU25-BE-02 |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`).

## Solicitud de negocio

La pantalla necesita datos de la bitácora sin armar peticiones a mano.

## Objetivo

`features/bitacora/` con tipos, funciones de API, claves de caché y los hooks `useBitacora` y `useAccionesBitacora`.

## Alcance

- `types/index.ts`, `api/keys.ts`, `api/bitacoraApi.ts` (`filter[accion]`, `filter[desde]`, `filter[hasta]`),
  `api/hooks.ts` (`keepPreviousData`), `api/index.ts` y el barrel `index.ts`.

Archivos esperados (crear/tocar): los de `src/features/bitacora/{types,api}` y `index.ts`.

## Fuera de alcance (no tocar)

- `apiClient` y la configuración de TanStack Query.

## Resultado esperado

Un componente obtiene la página de bitácora con `useBitacora(params)`.

## Validación

1. `pnpm exec tsc -b` sin errores.
2. `pnpm exec vitest run src/features/bitacora` en verde (UT-CU25-FE-03).
