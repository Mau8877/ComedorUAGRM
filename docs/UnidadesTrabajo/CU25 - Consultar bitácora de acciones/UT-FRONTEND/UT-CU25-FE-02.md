# UT-CU25-FE-02 — Pantalla, ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-FE-02` |
| **Nombre** | Pantalla, ruta y menú |
| **Caso de uso** | CU25-FRONTEND |
| **Depende de** | UT-CU25-FE-01 |

## Antes de implementar

[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md),
[`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y
[`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

El Administrador DUBSS entra a «Bitácora» desde el menú y filtra las acciones.

## Objetivo

La ruta `/bitacora` con la tabla de solo lectura y sus filtros, y el ítem de menú del Administrador.

## Alcance

- `components/columnasBitacora.tsx` (una definición para `DataTable` y `DataCards`).
- `screens/BitacoraScreen.tsx`: búsqueda por usuario (debounce), selector de acción (con `/bitacora/acciones`),
  fechas «Desde»/«Hasta», refrescar, error con «Reintentar» y paginación.
- `routes/_authenticated/bitacora.tsx` con `exigirRol('ADMINISTRADOR_DUBSS')` y el ítem «Bitácora» en
  `adminNavItems.ts`.

Archivos esperados (crear/tocar): `columnasBitacora.tsx`, `BitacoraScreen.tsx`, `bitacora.tsx` (ruta),
`adminNavItems.ts`.

## Fuera de alcance (no tocar)

- Los componentes de `components/ui/tanstack-table` (se usan tal cual).
- Cualquier botón de editar o eliminar.

## Resultado esperado

El Administrador ve la bitácora de la más reciente a la más antigua y la filtra.

## Validación

1. `pnpm exec vite build`, `pnpm exec tsc -b` y `pnpm exec eslint src/features/bitacora` sin errores.
2. Manual (backend en `dev`): entrar a `/bitacora` como `admin@dubss.com`, filtrar por acción y por fechas, y ver
   «Sin resultados» con un filtro que no coincide.
3. Con «Desde» posterior a «Hasta», aparece el aviso y no se manda la consulta.
