# UT-CU06-FE-02 — Ruta, menú y pantalla con selector de convocatoria y resumen

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-02` |
| **Nombre** | Ruta, menú y pantalla con selector de convocatoria y resumen |
| **Caso de uso** | CU06-FE |
| **Depende de** | `UT-CU06-FE-01` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `F/routes/_authenticated/usuarios/administradores.tsx`, `F/features/usuarios/screens/UsuariosPersonalScreen.tsx` y `F/layouts/admin/`.

## Solicitud de negocio

El administrador entra a «Lista priorizada» desde el menú, elige una convocatoria y ve cuántas postulaciones hay calificadas y cuántas pendientes.

## Objetivo

La ruta `/lista-priorizada` existe, aparece en el menú del administrador y muestra el selector de convocatoria y el resumen, con sus casos vacíos y de error.

## Alcance

- `F/routes/_authenticated/lista-priorizada.tsx`: ruta privada de `Administrador DUBSS`; cualquier otro rol redirige a `/panel` (A7).
- `F/layouts/admin/adminNavItems.ts`: ítem «Lista priorizada» (`href: '/lista-priorizada'`, ícono de `lucide-react`).
- `F/features/priorizacion/screens/ListaPriorizadaScreen.tsx` dentro de `AdminLayout` (mismas props que `UsuariosPersonalScreen`: título, `activeHref`, `usuarioActual`), con:
  - `Select` de convocatoria (nombre + gestión + estado), la más reciente preseleccionada; si no hay ninguna: «Todavía no hay convocatorias registradas» (A9).
  - El resumen: «N postulaciones calificadas · M pendientes de precalificación».
  - Sin postulaciones calificadas: «No hay postulaciones calificadas en esta convocatoria» y la cantidad de pendientes, sin tabla (A1, A4).
  - Convocatoria inexistente (`ERR_PRI_01`): aviso y vuelta al selector (A10).
  - Carga y error de red con «Reintentar» (A8); un `401` lo resuelve el flujo de sesión existente (A6).
  - El espacio donde irán la tabla (UT-03) y el botón de exportar (UT-05).
- `F/features/priorizacion/index.ts` exporta `ListaPriorizadaScreen`.
- Tests (con el cliente simulado, como en `features/usuarios/tests/`): con convocatorias se preselecciona la más reciente y se muestra el resumen; sin convocatorias se muestra A9; sin calificadas se muestra A1; `ERR_PRI_01` vuelve al selector; error de red muestra el reintento; un usuario que no es administrador es redirigido a `/panel`; el ítem del menú aparece para el administrador.

Archivos esperados (crear): `routes/_authenticated/lista-priorizada.tsx`, `ListaPriorizadaScreen.tsx`, el barrel y los tests. (Tocar): `adminNavItems.ts`.

## Fuera de alcance (no tocar)

- La tabla, la búsqueda y el filtro (UT-03), el detalle (UT-04) y el PDF (UT-05).
- El layout o las pantallas de otros roles.

## Resultado esperado

El administrador llega a la pantalla desde el menú, elige convocatoria y ve el resumen, y los casos sin datos o con error se explican solos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
