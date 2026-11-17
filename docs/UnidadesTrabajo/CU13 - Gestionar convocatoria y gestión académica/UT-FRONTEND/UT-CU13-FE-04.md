# UT-CU13-FE-04 — Pantalla de convocatorias: lista, búsqueda y filtros, y su ruta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-04` |
| **Nombre** | Pantalla de convocatorias: lista, búsqueda y filtros, y su ruta |
| **Caso de uso** | CU13-FE |
| **Depende de** | `UT-CU13-FE-02`, `UT-CU13-FE-03` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (todo el documento: no se arma una tabla a mano)
- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- `F/features/actas/screens/ActasScreen.tsx`, `F/features/actas/components/columnasActas.tsx` y `F/routes/_authenticated/actas.tsx` (patrón de pantalla de administración con tabla, toolbar y ruta con guard)

## Solicitud de negocio

El Administrador entra a «Convocatorias» y ve de un vistazo cada convocatoria con su gestión, fechas, cupos y estado, puede buscar por nombre y filtrar por estado o gestión (flujo principal, paso 2).

## Objetivo

Existe la ruta `/convocatorias` con la lista paginada, con búsqueda, filtros y orden, y las acciones por fila (aún sin comportamiento).

## Alcance

- `F/features/convocatorias/screens/ConvocatoriasScreen.tsx`: recibe `titulo`, `activeHref` y `usuarioActual` como `ActasScreen`, dentro de `AdminLayout`. Estado local para `page`, `pageSize` (20), `search` (con `debounce` de `utils/`), `estado`, `gestion` y `sorting`. Usa `useConvocatorias`, `useDataTable` (server-side, `enableSorting` solo en las columnas ordenables del backend), `DataTable`/`DataCards`, `DataTableToolbar` (búsqueda por nombre, `Select` de estado, `Select` de gestión alimentado por `useGestiones` con `pageSize` grande, botón «Nueva convocatoria» y botón de refrescar), `DataTablePagination` y `getErrorMessage` para el error, con reintento en error de red. Mensaje de lista vacía: «Todavía no hay convocatorias».
- `F/features/convocatorias/components/columnasConvocatorias.tsx`: `ColumnDef<Convocatoria>[]` con `nombre` (`meta.grow: true`, `cardTitle`), gestión, fecha de apertura, fecha de cierre (formato `dd/mm/aaaa`), cupos, puntaje mínimo, cantidad de finalistas y estado. Los opcionales vacíos se muestran «—».
- `F/features/convocatorias/components/EstadoConvocatoriaBadge.tsx`: etiqueta con el color del sistema de diseño por estado (`BORRADOR` neutro, `ABIERTA` `success`, `CERRADA` `muted`, `PUBLICADA` `info`), solo con tokens de tema (sin hex ni valores arbitrarios).
- `F/features/convocatorias/components/formatos.ts`: formato `dd/mm/aaaa` a partir de `yyyy-MM-dd` **sin** pasar por `Date` (para no correr el día por zona horaria).
- `renderActions` con `RowActionButton` (ícono + tooltip): «Ver detalle», «Editar», «Abrir», «Cerrar» y «Eliminar» según el estado (`BORRADOR` → Ver, Editar, Abrir, Eliminar; `ABIERTA` → Ver, Editar, Cerrar; `CERRADA` y `PUBLICADA` → Ver). En esta UT los botones llaman a callbacks que las UT 05 y 06 conectan; hasta entonces no hacen nada.
- `F/routes/_authenticated/convocatorias.tsx`: `createFileRoute('/_authenticated/convocatorias')` con `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')` y el componente que monta `ConvocatoriasScreen` con `activeHref="/convocatorias"` (mismo esquema que `actas.tsx`). `routeTree.gen.ts` se regenera solo y no se commitea.
- Barrels `index.ts` de `components/` y `screens/`, y se exporta `ConvocatoriasScreen` desde el barrel de la feature (las rutas importan de `@/features/convocatorias`).
- Tests (`F/features/convocatorias/tests/`, con cliente simulado): renderiza las filas con el formato de fecha correcto y «—» en los opcionales; la búsqueda y los filtros llegan al hook como `search`, `estado` y `gestion`; las acciones de cada fila dependen del estado; estado vacío; error con reintento; un test de la ruta que verifique el guard (como `rutaActas.test.ts`).

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

- El formulario de crear y editar (`UT-CU13-FE-05`) y abrir, cerrar y eliminar (`UT-CU13-FE-06`).
- La pantalla de gestiones (`UT-CU13-FE-07`).
- Modificar el paquete `components/ui/tanstack-table` o los tokens de diseño ya fijados.
- Mostrar el detalle completo con los documentos: «Ver detalle» abre el formulario en modo lectura, que arma la UT 05.

## Resultado esperado

El Administrador entra a `/convocatorias`, ve y filtra la lista, y los demás roles no pueden abrir la ruta.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el stack levantado y una sesión de Administrador, `/convocatorias` lista las convocatorias reales; sin esa sesión redirige al login o al módulo del rol.
