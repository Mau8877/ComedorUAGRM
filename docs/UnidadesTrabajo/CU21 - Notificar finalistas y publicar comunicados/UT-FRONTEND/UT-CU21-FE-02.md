# UT-CU21-FE-02 — Tabla de finalistas con estados y selección

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-02` |
| **Nombre** | Tabla de finalistas con estados y selección |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-FE-01` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) completo y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (tokens `success`, `warning`, `destructive`).
- Como referencia: `F/features/priorizacion/components/columnasLista.tsx` y `ListaPriorizadaScreen.tsx`.

## Solicitud de negocio

El administrador ve a los finalistas, qué aviso tiene cada uno y si ya entregó, y elige a quiénes avisar.

## Objetivo

Existe la tabla de finalistas con sus estados, búsqueda y selección múltiple.

## Alcance

- `F/features/finalistas/components/columnasFinalistas.tsx`: **Pos.**, **Finalista** (`meta.grow`, `cardTitle`), **Puntaje**, **Notificación** («Sin enviar» / «Enviada» / «Fallida», con color de token) y **Doc. física** («Pendiente» / «Recibida»); `enableSorting` apagado. Columna de selección con casilla por fila y para la página.
- `TablaFinalistas`: `useFinalistas` + `useDataTable` (server-side, 10/20/50), `DataTable` y `DataCards`, `DataTableToolbar` con búsqueda «Buscar por nombre o CI» (con `debounce`) y `DataTablePagination`. Fila activa para el panel de entrega (UT-04). La selección persiste al cambiar de página. Su barra tiene «Seleccionar fallidos» (marca solo a los de estado «Fallida», A2) y «Refrescar».
- Estados: cargando, error con `getErrorMessage` y «Reintentar», sin resultados con «Limpiar búsqueda», y la convocatoria sin finalistas → «Todavía no hay finalistas en esta convocatoria» sin tabla (A10).
- Tests: columnas y estados que manda el backend, selección múltiple, búsqueda con un solo pedido, no se manda `sort`, estado vacío y de error.

Archivos esperados (crear): `columnasFinalistas.tsx`, `TablaFinalistas.tsx` y sus tests.

## Fuera de alcance (no tocar)

- El diálogo de envío (UT-03) y el panel de entrega (UT-04).
- Calcular posiciones o estados en el cliente.
- Seleccionar finalistas de verdad (CU-05).

## Resultado esperado

El administrador ve los finalistas con sus estados y puede seleccionarlos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
