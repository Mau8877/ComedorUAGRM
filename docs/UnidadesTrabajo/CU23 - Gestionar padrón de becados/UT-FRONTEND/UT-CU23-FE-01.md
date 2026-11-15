# UT-CU23-FE-01 — Tipos, API y hooks del padrón

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-FE-01` |
| **Nombre** | Tipos, API y hooks del padrón |
| **Caso de uso** | CU23-FRONTEND |
| **Depende de** | UT-CU23-BE-05 |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (`queryKey`, invalidación).

## Solicitud de negocio

La pantalla necesita datos y operaciones del padrón sin armar peticiones a mano.

## Objetivo

`features/becas/` con tipos, funciones de API, claves de caché y los hooks de consulta y de mutación.

## Alcance

- `types/index.ts`: `Beca`, `ResumenPadron`, `CandidatoReemplazo`, `ParamsPadron`.
- `api/keys.ts` (`['becas', 'padron', ...]`, `['becas', 'resumen', codigo]`), `api/becasApi.ts` y `api/hooks.ts`:
  `useResumenPadron`, `usePadron`, `useDarDeAlta`, `useDarDeBaja`, `useSuspender`, `useCandidatoReemplazo`,
  `useReemplazar`. Cada mutación invalida el padrón y el resumen de la convocatoria.
- `api/mapearErrorBecas.ts`: traduce `ERR_BEC_*` a mensajes en español.
- Barrel `index.ts`.

Archivos esperados (crear/tocar): los de `src/features/becas/{types,api}` y `index.ts`.

## Fuera de alcance (no tocar)

- `apiClient` y la configuración de TanStack Query.

## Resultado esperado

Un componente obtiene y modifica el padrón con hooks tipados.

## Validación

1. `pnpm exec tsc -b` sin errores.
2. `pnpm exec vitest run src/features/becas` en verde (FE-04).
