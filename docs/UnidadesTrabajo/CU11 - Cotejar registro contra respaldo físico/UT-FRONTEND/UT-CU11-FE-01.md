# UT-CU11-FE-01 — Tipos, API, hooks, errores y esquema del formulario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-01` |
| **Nombre** | Tipos, API, hooks, errores y esquema del formulario |
| **Caso de uso** | CU11-FE |
| **Depende de** | — (la API de UT-CU11-BE-04 para probar de punta a punta) |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (`queryKey` por módulo, `apiClient` único) y [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md). Molde: `features/planillas/api/` y `schemas/periodoSchema.ts`.

## Solicitud de negocio

La pantalla necesita hablar con los endpoints del cotejo y validar el formulario antes de enviarlo.

## Objetivo

Existen los tipos, las funciones de API, los hooks, el traductor de errores y el esquema del formulario.

## Alcance

- `F/features/cotejos/types/index.ts`: `DatosCotejo`, `RegistroDigital`, `Cotejo`, `RegistrarCotejoRequest`, `ListadoParams`.
- `api/keys.ts` (`cotejosKeys`), `api/cotejosApi.ts` (cuatro funciones sobre `apiClient`), `api/hooks.ts` (`useDatosCotejo`, `useRegistrosDigitales`, `useCotejos`, `useRegistrarCotejo` que invalida el historial) y `api/mapearErrorCotejos.ts` (`ERR_COT_01..04`, permisos, red y servidor).
- `schemas/cotejoSchema.ts`: `cotejoSchema` (total y coincidencias enteros ≥ 0, coincidencias ≤ total, observaciones obligatorias con discrepancias, máx. 2000), `calcularCotejo` y `crearSolicitudCotejo`.
- `components/formatos.ts` y `components/columnas.tsx` (columnas de las dos tablas).

Archivos esperados (crear/tocar): los listados arriba.

## Fuera de alcance (no tocar)

La pantalla (UT-FE-02) y los hooks de `planillas` y `convocatorias`.

## Resultado esperado

Con estos archivos se pueden consultar y registrar cotejos, y el formulario nunca produce una solicitud inconsistente.

## Validación

1. `pnpm exec vitest run src/features/cotejos` (esquema, API y errores) en verde y `tsc -b` sin errores.
