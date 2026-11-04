# UT-CU14-FE-04 — Pantalla de listado por rol y sus rutas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-04` |
| **Nombre** | Pantalla de listado por rol y sus rutas |
| **Caso de uso** | CU14-FE |
| **Depende de** | `UT-CU14-FE-02`, `UT-CU14-FE-03` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (las 5 piezas, `meta.grow`, verificación contra el backend)
- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El Administrador necesita ver las cuentas de cada rol, buscarlas y filtrarlas por estado para decidir qué hacer con cada una (flujo A).

## Objetivo

Las tres rutas `/usuarios/...` muestran la lista paginada de cuentas de un rol, dentro del layout del Administrador.

## Alcance

- `F/features/usuarios/screens/UsuariosPersonalScreen.tsx`: recibe el `rol` (de `ROLES_PERSONAL`) y lo pasa como `filter[rol]`. Usa `useUsuarios`, `useDataTable`, `DataTable` (vista ancha), `DataCards` (vista angosta), `DataTableToolbar` y `DataTablePagination`. Estado en `useState`: `page`, `pageSize` (20; opciones 10/20/50), `search` (con `debounce` de `src/utils`), filtro de estado (Todas / Activas / Desactivadas → `filter[activo]`) y `sorting`; cambiar la búsqueda, el filtro o el orden vuelve a la página 1. Muestra estado de carga, lista vacía y error (`getErrorMessage`) con la opción de reintentar.
- Columnas (una sola definición para tabla y cards): «Nombre» (apellidos y nombres juntos, `meta.grow: true`, `cardTitle`, ordenable por `apellidos`), «CI» (ordenable), «Correo» (ordenable), «Rol» (etiqueta de `etiquetaRol`) y «Estado» (`StatusBadge`: Activa / Desactivada, con tokens de tema). Solo se habilita `enableSorting` en los campos que el backend acepta en `sort` (`apellidos`, `ci`, `correo`).
- Dentro de `AdminLayout`, con el título y el `activeHref` de la vista. Deja los puntos de enganche para el botón «Nueva cuenta» (toolbar) y las acciones por fila, que conectan las UT 05 a 07; esta UT **no** los implementa.
- Rutas (`F/routes/_authenticated/usuarios/`): `route.tsx` (con `beforeLoad: () => exigirRol('ADMINISTRADOR_DUBSS')` y un `Outlet`, así las hijas heredan el guard), `index.tsx` (redirige a `/usuarios/administradores`), `administradores.tsx`, `directivos.tsx` y `personal-comedor.tsx` (cada una monta la pantalla con su rol).
- Tests (mockeando `apiClient`): la pantalla pide `filter[rol]` del rol de la ruta; búsqueda y filtro de estado mandan `search` y `filter[activo]` y vuelven a la página 1; cambiar de página y de tamaño; ordenar manda `sort`; estados de carga, vacío y error; el guard (un usuario no Administrador no llega a la pantalla).

Archivos esperados (crear): la pantalla, las cinco rutas, los barrels de la feature y sus tests. `routeTree.gen.ts` se regenera solo y no se commitea.

## Fuera de alcance (no tocar)

- El alta, la edición, el cambio de rol y la desactivación: las UT 05 a 07 completan los botones y los modales.
- Un store de Zustand y una tabla armada a mano.
- Una vista de Postulantes.

## Resultado esperado

Desde el sidebar el Administrador abre cada vista y ve la lista de cuentas de ese rol, con búsqueda, filtro, orden y paginación.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con el backend levantado, la lista de cada rol muestra solo cuentas de ese rol.
