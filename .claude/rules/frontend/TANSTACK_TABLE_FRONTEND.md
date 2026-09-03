---
globs: projects/frontend/**/*
---

# Tabla de datos (tanstack-table) — Frontend

Todo listado paginado del panel (usuarios, pedidos, menús, etc.) usa el
paquete `src/components/ui/tanstack-table/` — **nunca se arma una tabla
o un grid de cards ad-hoc para un listado nuevo**. Si el paquete no cubre
un caso real, se extiende el paquete (nueva prop, nuevo componente
dentro de sus carpetas), no se resuelve por fuera con un `<table>` suelto
en la feature.

## Las 5 piezas y cuándo usar cada una

| Pieza | Qué es | Se usa siempre que |
| --- | --- | --- |
| `shared/useDataTable` | El "cerebro" — wrapper server-side sobre `useReactTable` | Hay un listado paginado, sin excepción |
| `shared/DataTablePagination` | Controles de paginación (números + ellipsis) | Se muestra `DataTable` y/o `DataCards` |
| `shared/toSortQueryParam`, `getErrorMessage`, `getPaginationRange` | Utilidades del cerebro | Según haga falta (sorting, manejo de errores) |
| `table/DataTable` | Vista tabla | Layout desktop / pantallas anchas |
| `card/DataCards` | Vista cards, misma fuente de columnas | Layout mobile / donde una tabla no entra |
| `toolbar/DataTableToolbar` | Búsqueda + filtros por `Select` + acciones | El listado tiene búsqueda, filtros o botones (crear, refrescar, etc.) |
| `actions/RowActionButton` | Ícono + tooltip para una fila | Dentro de `renderActions` de `DataTable`/`DataCards` |

Una sola definición de columnas (`ColumnDef<TData>[]`, con `meta.cardTitle`/
`cardLabel`/`hideInCard`/`cardOrder` donde corresponda) alimenta **tanto**
`DataTable` como `DataCards` — no se duplica el mapeo campo→UI entre las
dos vistas. Cuál de las dos se muestra (por breakpoint, por toggle, etc.)
es una decisión de la feature (`hidden md:block` / `md:hidden`, o lo que
haga falta) — el paquete no las combina por sí solo.

## Reglas de uso obligatorias

- **`DataTable` siempre necesita una columna con `meta.grow: true`** —
  la de contenido más largo/variable (típicamente la que tiene el
  nombre/título, igual que `cardTitle`). `DataTable` usa
  `table-layout: fixed` con anchos explícitos por columna (`w-12` en
  `#`, `w-56` en la columna `grow`, `w-28` en Acciones, el resto se
  reparte en partes iguales el espacio que sobra) — así el espacio libre
  se reparte proporcionalmente entre todas las columnas en vez de que una
  sola se lo quede entero (lo que pasaba con `table-layout: auto`, el
  default de HTML: ni siquiera `max-width` en una celda ayuda ahí, la
  mayoría de los navegadores lo ignora para el cálculo de ancho de
  columna). El resto de las columnas no necesita nada — se reparten el
  espacio libre en partes iguales.
- **100% server-side**: `useDataTable` nunca pagina/ordena/filtra en el
  cliente. `page`/`pageSize`/`sorting` viven en el `useState` de la
  feature y se mandan al backend a través del hook de `api/` de esa
  feature (TanStack Query) — ver
  [TANSTACK_QUERY_FRONTEND.md](TANSTACK_QUERY_FRONTEND.md) para la
  convención de `queryKey`. No se usa este paquete para "tablas" de datos
  que ya están 100% en el cliente (esas no necesitan `manualPagination`
  ni este paquete — un `<table>` simple alcanza).
- **`enableSorting` es opt-in por columna y por endpoint** — nunca se
  habilita a ciegas. Antes de poner `enableSorting: true` en una columna,
  confirmar que el endpoint real acepta ese campo en `sort` (ver
  verificación contra el backend, abajo).
- **`pageSizeOptions` no se inventa por feature** — el default del
  paquete (10/20/50, inicial 20) ya es el set acordado con el backend.
  Solo se pasa un `pageSizeOptions` custom para un caso puntual y
  documentado (ej. una demo), nunca como default de una feature real.
- **Errores**: el `errorMessage` que se le pasa a `DataTable`/`DataCards`
  sale de `getErrorMessage(query.error)`, no de un string hardcodeado —
  así se hereda el mensaje de negocio en español que ya arma el backend
  (ver [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)) en vez de
  inventar un texto que puede quedar desactualizado.
- **Acciones de fila** van con `RowActionButton` (ícono + tooltip) dentro
  de `renderActions`, no con botones de texto ("Ver", "Editar") sueltos —
  mantiene el patrón ya establecido (columna "Acciones" a la derecha,
  tooltip explicando cada ícono).
- **No se tocan los tokens de diseño ya fijados** del paquete (header en
  `bg-accent`/`text-accent-foreground`, fila cebra `bg-muted/40`, página
  activa en `bg-accent`) — son decisiones de marca ya resueltas, no un
  punto de partida a reinventar por feature.

## Verificación obligatoria contra el backend

**Antes de armar las columnas, filtros o el `sort` de un listado nuevo,
se verifica el contrato real del endpoint** — no se asume por el nombre
del Model ni se copia el shape de otro listado "porque seguro es
igual":

1. **Nombres de campo**: los `accessorKey` de las columnas usan el nombre
   exacto que expone el `Response` DTO del backend (camelCase), nunca el
   nombre de columna de la base de datos ni una suposición. Si el
   endpoint todavía no existe, se coordina el shape antes de escribir las
   columnas — no se inventa un DTO de mentira que después no calza.
2. **`search`**: solo se muestra el buscador del toolbar si el endpoint
   documenta sobre qué campos busca (ver
   [ENDPOINTS_BACKEND.md](../backend/ENDPOINTS_BACKEND.md)) — un buscador
   que no hace nada del lado del servidor es peor que no tenerlo.
3. **`filter[campo]`**: cada filtro del toolbar (`DataTableToolbarFilter`)
   corresponde a un campo que el `service` del endpoint realmente acepta
   como `filter[...]` — no se arma un `Select` de filtro "por si sirve".
4. **`sort`**: `enableSorting` en una columna solo si el controller de ese
   endpoint documenta ese campo como ordenable (ver
   [el formato de `sort`](../backend/ENDPOINTS_BACKEND.md#formato-de-sort)
   — single-field, `campo`/`-campo`). Un campo no soportado devuelve
   `400` del lado del backend; habilitarlo en el frontend sin esa
   confirmación es forzar un error evitable.
5. **`meta`**: `page`/`pageSize`/`totalItems`/`totalPages` siempre
   presentes en un listado — si un endpoint nuevo no los devuelve así,
   es el endpoint el que está mal (ver
   [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)), no se
   adapta el frontend a un contrato roto.

**Si el backend cambia** (nuevo campo, un filtro que se agrega o se
saca, el formato de `sort` cambia), el frontend se actualiza en el mismo
cambio que toca esa feature — no queda una tabla mostrando/filtrando por
un campo que el backend ya no expone. Ante cualquier duda de si el
frontend sigue reflejando el contrato real, se relee
[ENDPOINTS_BACKEND.md](../backend/ENDPOINTS_BACKEND.md) y
[RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md) antes de asumir
que sigue igual — esas dos reglas son la fuente de verdad del contrato,
esta regla no la duplica.

## Ejemplo de uso (feature real, no el mock de `pruebas_layout`)

```tsx
// src/features/usuarios/screens/ListaUsuarios.tsx
const [page, setPage] = useState(1)
const [pageSize, setPageSize] = useState(20) // mismo default que el backend
const [search, setSearch] = useState('')
const [rolFilter, setRolFilter] = useState<string>()
const [sorting, setSorting] = useState<SortingState>([])

const { data, isLoading, isError, error } = useUsuariosQuery({
  page,
  pageSize,
  search,
  filter: { rol: rolFilter },
  sort: toSortQueryParam(sorting),
})

const { table } = useDataTable({
  data: data?.data ?? [],
  columns: usuariosColumns,
  meta: data?.meta,
  page,
  pageSize,
  onPageChange: setPage,
  onPageSizeChange: (n) => { setPage(1); setPageSize(n) },
  enableSorting: true, // solo si /usuarios documenta sort=nombre|correo
  sorting,
  onSortingChange: (s) => { setPage(1); setSorting(s) },
})

// <DataTableToolbar searchValue={search} onSearchChange={...} filters={[...]} actions={...} />
// <DataTable table={table} isLoading={isLoading} isError={isError} errorMessage={getErrorMessage(error)}
//   renderActions={(row) => <RowActionButton icon={<PencilIcon />} label="Editar" ... />} />
// <DataTablePagination meta={data?.meta} page={page} onPageChange={setPage} onPageSizeChange={...} />
```

El demo de `features/pruebas_layout/` usa datos mock client-side
simulando lo anterior únicamente para verificación visual del sistema de
diseño — **no es el patrón a copiar para una feature real**, que siempre
pasa por un hook de `api/` con TanStack Query contra el backend.
