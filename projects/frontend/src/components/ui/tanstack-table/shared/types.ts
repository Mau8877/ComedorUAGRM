import type { ColumnDef, RowData, SortingState, Table } from '@tanstack/react-table'

// Extiende `ColumnMeta` de TanStack Table con la metadata que este paquete
// usa para derivar la vista de Cards a partir de las mismas columnas que
// usa la tabla -- una sola fuente de verdad, sin repetir el mapeo campo→UI
// por feature. Vive acá (no en cada feature) porque TypeScript aplica una
// module augmentation de forma global apenas el módulo se importa una vez
// en el árbol, y `components/ui/` es lo que todo el árbol termina
// importando.
declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- TData/TValue son parte de la firma que el declaration merging con la interfaz original de la librería debe respetar (mismo número de type params), aunque este proyecto no los use en el cuerpo.
  interface ColumnMeta<TData extends RowData, TValue> {
    /** Marca esta columna como el título principal de la card derivada. */
    cardTitle?: boolean
    /** Label mostrado en la card antes del valor (default: `header` de la columna, si es string). */
    cardLabel?: string
    /** Oculta esta columna en DataCards (sigue visible en DataTable). */
    hideInCard?: boolean
    /** Orden explícito del campo dentro de la card (default 0, estable por índice de columna). */
    cardOrder?: number
    /** Alineación de la celda, solo usada por DataTable. */
    align?: 'left' | 'center' | 'right'
  }
}

// Espejo de `PageMeta` del backend (ver .claude/rules/backend/RESPONSES_BACKEND.md).
// Se define acá en vez de importar el tipo `meta` de `src/store/apiClient.ts`
// para que este paquete de `components/ui/` no dependa de `store/` -- sigue
// siendo un sistema de diseño transversal, reusable sin acoplarse al cliente
// HTTP concreto de esta app.
export interface DataTablePageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface UseDataTableOptions<TData> {
  data: TData[]
  columns: ColumnDef<TData, unknown>[]
  /** Metadata de paginación del backend. `undefined` mientras no llegó la primera respuesta. */
  meta: DataTablePageMeta | undefined
  /** Página actual, 1-based (misma convención que el query param `page` del backend). */
  page: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  /**
   * El backend soporta orden por un único campo vía el query param `sort`
   * (`sort=campo` ascendente, `sort=-campo` descendente -- ver
   * .claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort). Sigue
   * deshabilitado por default: es opt-in por feature, porque no toda
   * columna es ordenable en cada endpoint (cada uno documenta qué campos
   * acepta) y habilitarlo a ciegas dejaría headers clicables sin efecto si
   * la feature no traduce el estado a ese query param. Cuando se habilita,
   * `useDataTable` fuerza `enableMultiSort: false` -- el backend no soporta
   * multi-columna, así que un click en otra columna reemplaza el orden
   * anterior en vez de acumularlo. Usar `toSortQueryParam` (exportado por
   * este paquete) para convertir el `SortingState` resultante al formato
   * exacto que espera el backend.
   */
  enableSorting?: boolean
  sorting?: SortingState
  onSortingChange?: (sorting: SortingState) => void
  getRowId?: (row: TData, index: number) => string
}

export interface UseDataTableReturn<TData> {
  table: Table<TData>
  pageCount: number
  canPreviousPage: boolean
  canNextPage: boolean
}

// Estado async compartido por DataTable y DataCards, para no acoplar
// ninguno de los dos a `UseQueryResult` de TanStack Query.
export interface DataStateProps {
  isLoading: boolean
  isError: boolean
  errorMessage?: string
}
