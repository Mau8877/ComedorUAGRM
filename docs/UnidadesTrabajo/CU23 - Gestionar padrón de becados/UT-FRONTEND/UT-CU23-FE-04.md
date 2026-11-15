# UT-CU23-FE-04 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-FE-04` |
| **Nombre** | Pruebas |
| **Caso de uso** | CU23-FRONTEND |
| **Depende de** | UT-CU23-FE-01, FE-02, FE-03 |

## Antes de implementar

[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md) (los tests de una feature viven en su
carpeta `tests/`).

## Solicitud de negocio

Que el contrato con el backend y las reglas de la pantalla no se rompan sin avisar.

## Objetivo

Pruebas de la API, los esquemas, los mensajes de error y la pantalla.

## Alcance

- `becasApi.test.ts`: rutas, métodos y parámetros (`filter[estado]`, `sort`, `search`), claves de caché.
- `motivoSchema.test.ts` y `mapearErrorBecas.test.ts`.
- `PadronScreen.test.tsx` (con `apiClient` simulado): selector, resumen, tabla y «Sin resultados»; «Dar de alta» con
  confirmación y oculto con 0 pendientes; acciones habilitadas según el estado; baja sin motivo no se envía; reemplazo
  con y sin candidato.

Archivos esperados (crear/tocar): los de `src/features/becas/tests/`.

## Fuera de alcance (no tocar)

- Pruebas de otras features.

## Resultado esperado

El comportamiento de la pantalla queda cubierto.

## Validación

1. `pnpm exec vitest run src/features/becas` en verde.
2. `pnpm exec eslint src/features/becas` y `pnpm exec tsc -b` sin errores.
