# UT-CU04-FE-02 — Pantalla «Mis postulaciones»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-02` |
| **Nombre** | Pantalla «Mis postulaciones» |
| **Caso de uso** | CU04-FE |
| **Depende de** | `UT-CU04-FE-01` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (todo listado paginado usa ese paquete; verificación contra el backend), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (tokens, sin valores arbitrarios), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).
- Como referencia: `F/features/resultados/screens/ResultadosScreen.tsx` y `columnasAdjudicados.tsx`.

## Solicitud de negocio

El postulante quiere ver sus postulaciones y su estado de un vistazo.

## Objetivo

Existe la pantalla con la lista paginada, el filtro por gestión, el estado vacío y los errores.

## Alcance

- `F/features/postulaciones/screens/MisPostulacionesScreen.tsx`: layout del postulante, `useMisPostulaciones`, `page`/`pageSize`/`sorting`/filtro en `useState`; `DataTable` en pantallas anchas y `DataCards` en móvil con una sola definición de columnas.
- `F/features/postulaciones/components/columnasPostulaciones.tsx`: columnas `convocatoria` (`meta.grow`, `cardTitle`), `gestionAcademica`, `fechaEnvio` (`enableSorting`) y `estado` (con `EstadoPostulacionBadge`); acción de fila «Ver detalle» con `RowActionButton` que navega al detalle.
- `F/features/postulaciones/components/EstadoPostulacionBadge.tsx`: un `Badge` por estado con tokens del tema.
- Toolbar con `DataTableToolbarFilter` por gestión académica (`filter[gestionAcademica]`); al cambiar el filtro, la página vuelve a 1.
- Estado vacío (A1): mensaje informativo; el botón «Postular» solo si existe la pantalla del CU-02.
- Error (A10): `getErrorMessage` y botón «Reintentar».
- Tests de componente: lista con filas y estado, vacío, filtro que repite la consulta con el parámetro, orden por fecha, error con reintento.

Archivos esperados (crear): pantalla, columnas, badge, barrel de `components/` y `screens/` y tests.

## Fuera de alcance (no tocar)

- La pantalla de detalle (UT-03) y las rutas/menú (UT-04).
- Cualquier cálculo del estado visible en el cliente.
- Estilos con valores arbitrarios o colores sueltos.

## Resultado esperado

El postulante ve su lista, la filtra y puede ir al detalle de cada postulación.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Con datos de prueba, la lista muestra las postulaciones de la más reciente a la más antigua y el filtro las reduce.
