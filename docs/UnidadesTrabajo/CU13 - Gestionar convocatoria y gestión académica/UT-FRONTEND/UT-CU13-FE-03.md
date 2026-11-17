# UT-CU13-FE-03 — Entradas del sidebar del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-03` |
| **Nombre** | Entradas del sidebar del Administrador |
| **Caso de uso** | CU13-FE |
| **Depende de** | — |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol)
- `F/layouts/admin/adminNavItems.ts` y `adminNavItems.test.ts`

## Solicitud de negocio

El Administrador tiene que encontrar «Convocatorias» y «Gestiones académicas» en el menú lateral (flujo principal, paso 1).

## Objetivo

El sidebar del Administrador DUBSS tiene las dos entradas nuevas con su ruta.

## Alcance

- `F/layouts/admin/adminNavItems.ts`: agrega `{ label: 'Convocatorias', href: '/convocatorias', icon: ... }` y `{ label: 'Gestiones académicas', href: '/gestiones-academicas', icon: ... }` (íconos de `lucide-react`, por ejemplo `CalendarRangeIcon` y `CalendarDaysIcon`). Van antes de «Lista priorizada»: una convocatoria es el primer paso del proceso. Se conservan todas las entradas existentes.
- `F/layouts/admin/adminNavItems.test.ts`: dos pruebas nuevas (`it('tiene la entrada «Convocatorias» que lleva a su pantalla (CU13)')` y la de gestiones) en el estilo de las existentes.

Archivos esperados (crear/tocar): `adminNavItems.ts`, `adminNavItems.test.ts`.

## Fuera de alcance (no tocar)

- Las rutas y pantallas (UT 04 y 07).
- Entradas para otros roles.
- Reordenar o renombrar las entradas ya existentes.

## Resultado esperado

El Administrador ve las dos entradas en el menú.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
