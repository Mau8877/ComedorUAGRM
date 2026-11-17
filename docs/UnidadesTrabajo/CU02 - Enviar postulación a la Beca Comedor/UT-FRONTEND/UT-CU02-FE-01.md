# UT-CU02-FE-01 — API, hooks, esquema y errores del envío

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-01` |
| **Nombre** | API, hooks, esquema y errores del envío |
| **Caso de uso** | CU02-FE |
| **Depende de** | — |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) y [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)

## Solicitud de negocio

La pantalla necesita hablar con el backend y validar el formulario con las mismas reglas.

## Objetivo

Existen `iniciarPostulacionApi`, `enviarPostulacionApi`, sus hooks, el esquema Zod y el traductor de errores.

## Alcance

- `features/postulaciones/types/envio.ts`: el contrato del backend.
- `api/postulacionesApi.ts` y `api/hooks.ts`: `useIniciarPostulacion` y `useEnviarPostulacion` (invalida `postulacionesKeys.all`).
- `schemas/respuestasSocioeconomicasSchema.ts`: texto → número, con las mismas reglas del backend.
- `api/mapearErrorEnvio.ts`.
- Pruebas: `respuestasSocioeconomicasSchema.test.ts` y `envioApi.test.tsx`.

Archivos esperados (crear/tocar): los listados y los `index.ts` de `api/`, `schemas/` y `types/`.

## Fuera de alcance (no tocar)

- Las pantallas (`UT-CU02-FE-02`).
- Guardar borrador (CU17).

## Resultado esperado

Las funciones y el esquema están probados y exportados.

## Validación

1. `pnpm exec tsc -b`, `pnpm lint` y `pnpm exec vitest run src/features/postulaciones` en verde.
