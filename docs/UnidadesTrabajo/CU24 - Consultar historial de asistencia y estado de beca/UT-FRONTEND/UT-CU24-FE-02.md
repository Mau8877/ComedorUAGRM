# UT-CU24-FE-02 — Pantalla «Mi beca», ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-FE-02` |
| **Nombre** | Pantalla «Mi beca», ruta y menú |
| **Caso de uso** | CU24-FE |
| **Depende de** | UT-CU24-FE-01 |

## Antes de implementar

[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (solo tokens del tema).

## Solicitud de negocio

El becado quiere ver en la web el estado de su beca y su historial de asistencia.

## Objetivo

Existe la pantalla «Mi beca», accesible desde el menú del Postulante.

## Alcance

- `components/`: `formatos.ts` (fechas en La Paz), `EstadoBecaBadge` (un tono del tema por estado), `columnasAsistencias` (una definición para `DataTable` y `DataCards`; solo `fecha` ordenable).
- `screens/MiBecaScreen.tsx`: tarjeta de estado (convocatoria, gestión, adjudicación, inicio, total, motivo si no está vigente), historial con rango de fechas, paginación y estados de carga, vacío, error con «Reintentar» y «sin beca» con «Ir a Mis postulaciones».
- `routes/_authenticated/mi-beca.tsx` (`exigirRol('POSTULANTE')`) y el ítem «Mi beca» en `layouts/estudiante/estudianteNavItems.ts` (+ su test).

Archivos esperados (crear/tocar): `features/mibeca/components/`, `screens/`, la ruta y `estudianteNavItems(.test).ts`.

## Fuera de alcance (no tocar)

El backend, el resto de rutas y layouts, el calendario mensual del mockup y cualquier acción de edición.

## Resultado esperado

Un postulante becado abre «Mi beca» y ve su estado y su historial; uno sin beca ve el aviso con el enlace a «Mis postulaciones».

## Validación

1. `pnpm exec vite build` (regenera `routeTree.gen.ts`), `pnpm exec tsc -b` y `pnpm lint` sin errores.
2. `MiBecaScreen.test.tsx` y `estudianteNavItems.test.ts` (ver FE-03).
