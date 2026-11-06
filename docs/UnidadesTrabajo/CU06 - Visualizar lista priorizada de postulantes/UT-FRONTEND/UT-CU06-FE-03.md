# UT-CU06-FE-03 — Tabla de la lista: paginación, búsqueda y filtro por carrera

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-03` |
| **Nombre** | Tabla de la lista: paginación, búsqueda y filtro por carrera |
| **Caso de uso** | CU06-FE |
| **Depende de** | `UT-CU06-FE-01`, `UT-CU06-FE-02` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) completo (`meta.grow`, `cardTitle`, server-side, verificación contra el backend) y [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md).
- Como referencia: `F/features/usuarios/screens/UsuariosPersonalScreen.tsx` (uso real de `useDataTable`, toolbar y paginación) y `F/utils/debounce.ts`.

## Solicitud de negocio

El administrador recorre la lista de mejor a peor puntaje, busca a una persona o acota por carrera, y siempre ve la posición real.

## Objetivo

La pantalla muestra la lista paginada de la convocatoria elegida, con búsqueda y filtro por carrera.

## Alcance

- `F/features/priorizacion/components/columnasLista.tsx`: `ColumnDef<PostulacionPriorizada>[]` con **posición** (`posicion`), **postulante** (nombres y apellidos; `meta.grow` y `cardTitle`), **CI**, **carrera** (`carrera.nombre`), **puntaje** (un decimal) y **fecha de cálculo** (fecha local). `enableSorting` apagado en todas.
- En `ListaPriorizadaScreen`: `useListaPriorizada` + `useDataTable` (`manualPagination`, 20 filas por defecto y el selector 10/20/50 del paquete), `DataTable` para ancho normal y `DataCards` para ancho chico (misma definición de columnas), `DataTablePagination`, y `getErrorMessage(error)` para el mensaje de error.
- `DataTableToolbar` con la **búsqueda** (placeholder «Buscar por nombre o CI», con `debounce`) y el filtro **Carrera** (`Select` con las carreras de `useCarreras` de `shared/` y la opción «Todas»). Cambiar la búsqueda, la carrera, la convocatoria o las filas por página vuelve a la página 1.
- Estados: cargando (`isLoading`), error de red con «Reintentar» (A8), sin resultados con «Limpiar búsqueda y filtros» (A2); la lista vacía de la convocatoria ya la resuelve UT-02.
- Volver a consultar (cambiar de página, filtrar o recargar) trae los puntajes y posiciones vigentes del servidor (A11); no se guarda la lista en ningún store.
- Tests: la tabla muestra las columnas y la posición que manda el backend (incluida una posición mayor a la página al filtrar); cambiar de página pide la página correcta; la búsqueda con retardo manda un solo pedido; el filtro de carrera manda `filter[carreraId]`; cambiar el filtro vuelve a la página 1; no se manda `sort`; «Sin resultados» y el error de red; la vista de tarjetas muestra lo mismo.

Archivos esperados (crear): `columnasLista.tsx` y sus tests. (Tocar): `ListaPriorizadaScreen.tsx`.

## Fuera de alcance (no tocar)

- El detalle (UT-04) y el PDF (UT-05).
- Ordenar por otras columnas o cambiar el orden de la lista.
- Calcular la posición en el cliente.
- Seleccionar finalistas (CU-05).

## Resultado esperado

El administrador ve la lista ordenada, pagina, busca y filtra, y cada postulante conserva su posición real.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el backend y los datos de prueba (`UT-CU06-BE-03`), la lista de la convocatoria grande se muestra paginada y al buscar un nombre conserva su posición.
