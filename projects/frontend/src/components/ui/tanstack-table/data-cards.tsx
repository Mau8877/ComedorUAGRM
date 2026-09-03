import { flexRender } from '@tanstack/react-table'
import type { Cell, Row, Table as TanstackTable } from '@tanstack/react-table'
import type { ReactNode } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import type { DataStateProps } from './types'

export interface DataCardsProps<TData> extends DataStateProps {
  table: TanstackTable<TData>
  /**
   * Override total del contenido de una card. Si se define, se ignora toda
   * la derivación automática por metadata de columna para esa fila --
   * usar solo cuando una feature necesita un layout que no se puede
   * describir con `meta.cardTitle`/`cardLabel`/`hideInCard`/`cardOrder`.
   */
  renderCard?: (row: Row<TData>) => ReactNode
  /** className del grid (default: 1/2/3 columnas por breakpoint). */
  className?: string
  skeletonCount?: number
  emptyMessage?: string
}

function resolveLabel<TData>(cell: Cell<TData, unknown>): string {
  const meta = cell.column.columnDef.meta
  if (meta?.cardLabel) return meta.cardLabel

  const header = cell.column.columnDef.header
  if (typeof header === 'string') return header

  // El header es una render function -- no hay forma genérica de
  // convertirla a texto plano, así que se cae al id de columna. Si una
  // feature necesita un label legible acá, debe definir `meta.cardLabel`
  // explícito en esa columna.
  return cell.column.id
}

function AutoCard<TData>({ row }: { row: Row<TData> }) {
  const visibleCells = row.getVisibleCells().filter((cell) => !cell.column.columnDef.meta?.hideInCard)

  const titleCell =
    visibleCells.find((cell) => cell.column.columnDef.meta?.cardTitle) ?? visibleCells[0]

  if (import.meta.env.DEV && titleCell && !titleCell.column.columnDef.meta?.cardTitle) {
    console.warn(
      `[DataCards] Ninguna columna tiene "meta.cardTitle: true" -- usando "${titleCell.column.id}" como título por fallback.`,
    )
  }

  const fieldCells = visibleCells
    .filter((cell) => cell.id !== titleCell?.id)
    .sort((a, b) => (a.column.columnDef.meta?.cardOrder ?? 0) - (b.column.columnDef.meta?.cardOrder ?? 0))

  return (
    <Card>
      {titleCell && (
        <CardHeader>
          <CardTitle>{flexRender(titleCell.column.columnDef.cell, titleCell.getContext())}</CardTitle>
        </CardHeader>
      )}
      <CardContent className="flex flex-col gap-2">
        {fieldCells.map((cell) => (
          <div key={cell.id} className="flex items-baseline justify-between gap-3 text-sm">
            <span className="text-muted-foreground">{resolveLabel(cell)}</span>
            <span className="text-right text-foreground">
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </span>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}

export function DataCards<TData>({
  table,
  isLoading,
  isError,
  errorMessage = 'Ocurrió un error al cargar los datos',
  renderCard,
  className,
  skeletonCount = 6,
  emptyMessage = 'Sin resultados',
}: DataCardsProps<TData>) {
  const rows = table.getRowModel().rows
  const gridClassName = cn('grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3', className)

  if (isLoading) {
    return (
      <div className={gridClassName}>
        {Array.from({ length: skeletonCount }).map((_, index) => (
          <Card key={`skeleton-${index}`}>
            <CardHeader>
              <Skeleton className="h-5 w-2/3" />
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (isError) {
    return (
      <div className={gridClassName}>
        <p className="col-span-full py-8 text-center text-sm text-destructive">{errorMessage}</p>
      </div>
    )
  }

  if (rows.length === 0) {
    return (
      <div className={gridClassName}>
        <p className="col-span-full py-8 text-center text-sm text-muted-foreground">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className={gridClassName}>
      {rows.map((row) => (
        <div key={row.id}>{renderCard ? renderCard(row) : <AutoCard row={row} />}</div>
      ))}
    </div>
  )
}
