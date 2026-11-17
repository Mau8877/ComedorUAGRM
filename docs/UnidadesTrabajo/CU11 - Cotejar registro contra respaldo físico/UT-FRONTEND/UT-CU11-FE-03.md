# UT-CU11-FE-03 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-03` |
| **Nombre** | Pruebas del frontend del cotejo |
| **Caso de uso** | CU11-FE |
| **Depende de** | UT-CU11-FE-02 |

## Antes de implementar

[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md) (tests de una feature en su carpeta `tests/`). Molde: `features/planillas/tests/`.

## Solicitud de negocio

Garantizar el comportamiento del formulario, las tablas y los errores.

## Objetivo

Hay pruebas del esquema, la API, los errores, la ruta y la pantalla.

## Alcance

- `tests/cotejoSchema.test.ts`, `cotejosApi.test.ts`, `mapearErrorCotejos.test.ts` y `rutaCotejo.test.ts`.
- `tests/CotejoScreen.test.tsx` (con `clienteSimulado.ts` y `montar.tsx`): resumen y tablas, A1, sin convocatorias y sin planillas, resultado deducido, observaciones obligatorias, sin datos, registro con y sin discrepancias, error del servidor y error de carga.

Archivos esperados (crear/tocar): los archivos de `tests/`.

## Fuera de alcance (no tocar)

Tests de otras features.

## Resultado esperado

Los CA13 a CA15 y las ramas de error quedan cubiertas.

## Validación

1. `pnpm exec vitest run src/features/cotejos src/layouts` en verde.
