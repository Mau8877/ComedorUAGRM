# UT-CU11-FE-02 — Pantalla, ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-02` |
| **Nombre** | Pantalla, ruta y menú |
| **Caso de uso** | CU11-FE |
| **Depende de** | UT-CU11-FE-01 |

## Antes de implementar

[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Mockup: img 111. Molde: `PlanillasScreen`.

## Solicitud de negocio

El Administrador elige una planilla, ve sus registros digitales, registra el cotejo y consulta los anteriores.

## Objetivo

La pantalla «Cotejo de asistencia» funciona y es accesible desde el menú.

## Alcance

- `F/features/cotejos/screens/CotejoScreen.tsx`: selectores de convocatoria y planilla; resumen (becados en la planilla, registros digitales, becados con asistencia) y aviso A1; tabla paginada y ordenable de registros digitales; formulario «Registrar cotejo» (total, coincidencias, discrepancias y resultado deducidos, observaciones) con validación inline; tabla del historial; errores con «Reintentar».
- `F/routes/_authenticated/cotejo-asistencia.tsx` con `exigirRol('ADMINISTRADOR_DUBSS')`.
- `F/layouts/admin/adminNavItems.ts`: entrada «Cotejo de asistencia» tras «Planillas», más su test.
- Barrels `index.ts` de la feature.

Archivos esperados (crear/tocar): la pantalla, la ruta, `adminNavItems.ts` y `adminNavItems.test.ts`, y los `index.ts`.

## Fuera de alcance (no tocar)

Marcar firma por becado y día; la lectura automática de la planilla; `PlanillasScreen`.

## Resultado esperado

El Administrador puede cotejar una planilla de punta a punta.

## Validación

1. `pnpm lint` y `tsc -b` sin hallazgos.
2. Con el backend levantado: elegir una planilla, ver los registros, registrar un cotejo con discrepancias y ver que aparece en el historial (prueba manual, ver el registro de implementación).
