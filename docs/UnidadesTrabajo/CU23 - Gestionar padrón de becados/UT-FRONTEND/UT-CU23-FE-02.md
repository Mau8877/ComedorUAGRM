# UT-CU23-FE-02 — Pantalla del padrón, alta, ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-FE-02` |
| **Nombre** | Pantalla del padrón, alta, ruta y menú |
| **Caso de uso** | CU23-FRONTEND |
| **Depende de** | UT-CU23-FE-01 |

## Antes de implementar

[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md),
[`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y
[`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

El Administrador entra a «Padrón de becados», elige la convocatoria y ve a sus becados.

## Objetivo

La ruta `/padron-becados` con el selector de convocatoria, el resumen, el botón «Dar de alta» y la tabla del padrón.

## Alcance

- `components/columnasPadron.tsx` (una definición para `DataTable` y `DataCards`): Becado, CI, Carrera, Estado (con
  `EstadoBecaBadge`), Inicio. `enableSorting` solo en lo que el backend acepta.
- `screens/PadronScreen.tsx`: selector de convocatorias publicadas, resumen (vigentes, suspendidas, bajas, pendientes),
  buscador (nombre o CI), filtro de estado, «Dar de alta» con confirmación, paginación y errores con «Reintentar».
- `routes/_authenticated/padron-becados.tsx` con `exigirRol('ADMINISTRADOR_DUBSS')` y el ítem de menú.

Archivos esperados (crear/tocar): `columnasPadron.tsx`, `EstadoBecaBadge.tsx`, `PadronScreen.tsx`, `padron-becados.tsx`
(ruta), `adminNavItems.ts`.

## Fuera de alcance (no tocar)

- Los diálogos de baja, suspensión y reemplazo (FE-03).
- Los componentes de `components/ui/tanstack-table`.

## Resultado esperado

El Administrador ve el padrón de la convocatoria elegida y puede dar de alta a los adjudicados pendientes.

## Validación

1. `pnpm exec vite build`, `pnpm exec tsc -b` y `pnpm exec eslint src/features/becas` sin errores.
2. Manual (backend en `dev`): elegir una convocatoria publicada, dar de alta y ver los becados como «Vigente».
