# UT-CU13-FE-07 — Gestiones académicas: lista, alta y edición, y su ruta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-07` |
| **Nombre** | Gestiones académicas: lista, alta y edición, y su ruta |
| **Caso de uso** | CU13-FE |
| **Depende de** | `UT-CU13-FE-01`, `UT-CU13-FE-02`, `UT-CU13-FE-03` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md)
- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- `F/features/convocatorias/screens/ConvocatoriasScreen.tsx` (de la `UT-CU13-FE-04`, para mantener el mismo patrón) y `F/routes/_authenticated/actas.tsx`

## Solicitud de negocio

Sin una gestión académica («2026-II») no se puede crear ninguna convocatoria. El Administrador tiene que poder crearlas y corregirlas él mismo.

## Objetivo

Existe la ruta `/gestiones-academicas` con la lista de gestiones y su alta y edición.

## Alcance

- `F/features/convocatorias/screens/GestionesAcademicasScreen.tsx`: mismo patrón que `ConvocatoriasScreen` (recibe `titulo`, `activeHref` y `usuarioActual`, dentro de `AdminLayout`): `useGestiones`, `useDataTable` server-side con `sort` por `etiqueta`, `fechaInicio` y `fechaFin`, `DataTable`/`DataCards`, `DataTableToolbar` (búsqueda por etiqueta y botón «Nueva gestión»), `DataTablePagination` y `RowActionButton` «Editar». Lista vacía: «Todavía no hay gestiones académicas».
- `F/features/convocatorias/components/columnasGestiones.tsx`: columnas `etiqueta` (`meta.grow: true`, `cardTitle`), fecha de inicio y fecha de fin (`dd/mm/aaaa` con `formatos.ts`).
- `F/features/convocatorias/components/GestionFormModal.tsx`: modal con TanStack Form y `gestionSchema` para crear o editar (`useCrearGestion` / `useEditarGestion`, `PUT`). Errores inline solo en campos tocados; `ERR_CV_12` («ya existe una gestión con esa etiqueta») bajo el campo etiqueta, `ERR_CV_13` bajo la fecha de fin, `ERR_CV_02` («ya no existe») refresca la lista, error de red con reintentar. Éxito: cierra, toast y refresca.
- `F/routes/_authenticated/gestiones-academicas.tsx`: ruta con `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')` y `activeHref="/gestiones-academicas"`.
- Barrels `index.ts` y se exporta `GestionesAcademicasScreen` desde el barrel de la feature.
- Tests: lista y formato de fechas; crear llama al hook con el cuerpo correcto; fin igual al inicio se rechaza en el formulario; `ERR_CV_12` marca la etiqueta; editar precarga los valores; la ruta aplica el guard.

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

- Eliminar gestiones académicas.
- Mostrar cuántas convocatorias tiene cada gestión.
- El formulario de convocatorias (`UT-CU13-FE-05`), que solo consulta las gestiones para elegir una.
- Modificar el paquete `components/ui/tanstack-table`.

## Resultado esperado

El Administrador mantiene las gestiones académicas desde su propia pantalla y las ve disponibles al crear convocatorias.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el stack levantado: crear la gestión «2026-II» la muestra en la lista y en el selector de gestión del formulario de convocatoria; repetir la etiqueta muestra el error bajo ese campo.
