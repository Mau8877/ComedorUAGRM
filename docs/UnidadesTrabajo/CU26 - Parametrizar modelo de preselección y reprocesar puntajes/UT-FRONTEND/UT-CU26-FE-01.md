# UT-CU26-FE-01 — Tipos, API, esquema y errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-FE-01` |
| **Nombre** | Tipos, API, esquema y errores |
| **Caso de uso** | CU26-FRONTEND |
| **Depende de** | — |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) y
[`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md).

## Solicitud de negocio

Que la pantalla hable con el backend del CU26 con el mismo contrato y las mismas reglas de validación.

## Objetivo

La capa de datos de la feature `preseleccion`: tipos, funciones de API, hooks, claves de caché, esquema de Zod y traducción de errores.

## Alcance

- `types/index.ts`; `api/preseleccionApi.ts`, `api/hooks.ts` (`useModeloVigente`, `useHistorialVersiones`, `useResumenReprocesamiento`, `useLoteReprocesamiento` con `refetchInterval`, `useGuardarModelo`, `useReprocesar`), `api/keys.ts`, `api/mapearErrorPreseleccion.ts` (`ERR_MOD_01` a `ERR_MOD_07`, `ERR_CV_01`).
- `schemas/parametrosSchema.ts`: `VARIABLES_MODELO`, `parametrosSchema`, `sumaPonderaciones`, `sumaEsValida`, `valoresIniciales`, `aPayload`, `siguienteVersion`.
- Pruebas: `parametrosSchema.test.ts`.

Archivos esperados (crear/tocar): todo lo anterior bajo `src/features/preseleccion/`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (FE-02 y FE-03).
- Otras instancias de axios: se usa `apiClient`.

## Resultado esperado

Los hooks piden y escriben contra `/modelos-preseleccion`, `/convocatorias/{codigo}/resumen-reprocesamiento`, `/convocatorias/{codigo}/reprocesar` y `/lotes-reprocesamiento/{codigo}`.

## Validación

1. `pnpm exec tsc -b` y `pnpm exec eslint src/features/preseleccion` sin errores.
2. `pnpm exec vitest run src/features/preseleccion/tests/parametrosSchema.test.ts` en verde.
