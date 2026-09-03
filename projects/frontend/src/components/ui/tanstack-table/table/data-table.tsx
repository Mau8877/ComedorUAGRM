import { flexRender } from '@tanstack/react-table'
import type { Row, Table as TanstackTable } from '@tanstack/react-table'
import { ArrowDownIcon, ArrowUpIcon, ChevronsUpDownIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import type { DataStateProps } from '../shared'

export interface DataTableProps<TData> extends DataStateProps {
  table: TanstackTable<TData>
  /** Filas placeholder mientras `isLoading` (default 5). */
  skeletonRowCount?: number
  emptyMessage?: string
  /** Antepone una columna "#" con el número de fila (1-based, respeta la página actual vía `table.getState().pagination`). */
  showRowNumber?: boolean
  /**
   * Agrega una columna final con lo que devuelva esta función por fila
   * (típicamente botones de editar/eliminar/ver). El paquete no sabe qué
   * acciones existen -- solo reserva y alinea la columna.
   */
  renderActions?: (row: Row<TData>) => ReactNode
  /** Header de la columna de acciones (default "Acciones"). Sin efecto si no se pasa `renderActions`. */
  actionsLabel?: string
  /** className del contenedor con borde -- el scroll horizontal lo maneja el `<Table>` de shadcn internamente, nunca el body. */
  className?: string
}

export function DataTable<TData>({
  table,
  isLoading,
  isError,
  errorMessage = 'Ocurrió un error al cargar los datos',
  skeletonRowCount = 5,
  emptyMessage = 'Sin resultados',
  showRowNumber = false,
  renderActions,
  actionsLabel = 'Acciones',
  className,
}: DataTableProps<TData>) {
  const columnCount = table.getAllLeafColumns().length + (showRowNumber ? 1 : 0) + (renderActions ? 1 : 0)
  const rows = table.getRowModel().rows
  const { pageIndex, pageSize } = table.getState().pagination

  return (
    <div className={cn('w-full overflow-hidden rounded-md border', className)}>
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id} className="bg-accent hover:bg-accent">
              {showRowNumber && (
                <TableHead className="w-px whitespace-nowrap text-accent-foreground">#</TableHead>
              )}
              {headerGroup.headers.map((header) => {
                const canSort = header.column.getCanSort()
                const sortDirection = header.column.getIsSorted()

                return (
                  <TableHead
                    key={header.id}
                    className="text-accent-foreground"
                    style={{ textAlign: header.column.columnDef.meta?.align }}
                  >
                    {header.isPlaceholder ? null : canSort ? (
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 hover:text-accent-foreground/70"
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {sortDirection === 'asc' ? (
                          <ArrowUpIcon className="size-3.5" />
                        ) : sortDirection === 'desc' ? (
                          <ArrowDownIcon className="size-3.5" />
                        ) : (
                          <ChevronsUpDownIcon className="size-3.5 text-accent-foreground/50" />
                        )}
                      </button>
                    ) : (
                      flexRender(header.column.columnDef.header, header.getContext())
                    )}
                  </TableHead>
                )
              })}
              {renderActions && (
                <TableHead className="w-px whitespace-nowrap text-right text-accent-foreground">
                  {actionsLabel}
                </TableHead>
              )}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {isLoading ? (
            Array.from({ length: skeletonRowCount }).map((_, rowIndex) => (
              <TableRow key={`skeleton-${rowIndex}`}>
                {Array.from({ length: columnCount }).map((_, cellIndex) => (
                  <TableCell key={cellIndex}>
                    <Skeleton className="h-4 w-full" />
                  </TableCell>
                ))}
              </TableRow>
            ))
          ) : isError ? (
            <TableRow>
              <TableCell colSpan={columnCount} className="h-24 text-center text-destructive">
                {errorMessage}
              </TableCell>
            </TableRow>
          ) : rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={columnCount} className="h-24 text-center text-muted-foreground">
                {emptyMessage}
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row, rowIndexInPage) => (
              <TableRow key={row.id}>
                {showRowNumber && (
                  <TableCell className="w-px whitespace-nowrap text-muted-foreground">
                    {pageIndex * pageSize + rowIndexInPage + 1}
                  </TableCell>
                )}
                {row.getVisibleCells().map((cell) => (
                  <TableCell key={cell.id} style={{ textAlign: cell.column.columnDef.meta?.align }}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </TableCell>
                ))}
                {renderActions && (
                  <TableCell className="w-px whitespace-nowrap text-right">
                    <div className="flex items-center justify-end gap-1">{renderActions(row)}</div>
                  </TableCell>
                )}
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  )
}
