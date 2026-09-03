import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'
import type { DataTablePageMeta } from './types'

export interface DataTablePaginationProps {
  meta: DataTablePageMeta | undefined
  page: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  pageSizeOptions?: number[]
  /** Deshabilita los controles mientras la feature refetchea. */
  isLoading?: boolean
  className?: string
}

const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

/**
 * Controles de paginación basados 100% en la `PageMeta` que devuelve el
 * backend (ver .claude/rules/backend/RESPONSES_BACKEND.md) -- agnóstico de
 * si se usa debajo de `DataTable` o de `DataCards`.
 */
export function DataTablePagination({
  meta,
  page,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  isLoading = false,
  className,
}: DataTablePaginationProps) {
  const pageSize = meta?.pageSize ?? pageSizeOptions[0]
  const canPreviousPage = page > 1 && !isLoading
  const canNextPage = !!meta && page < meta.totalPages && !isLoading
  // Con totalItems en 0, totalPages viene en 0 (Math.ceil(0 / pageSize)) --
  // se muestra como 1 para no mostrar "Página 1 de 0".
  const totalPagesDisplay = meta ? Math.max(1, meta.totalPages) : undefined

  return (
    <div className={cn('flex flex-wrap items-center justify-between gap-4', className)}>
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>Filas por página</span>
        <Select value={pageSize} onValueChange={(value) => onPageSizeChange(Number(value))}>
          <SelectTrigger size="sm" aria-label="Filas por página">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {pageSizeOptions.map((option) => (
              <SelectItem key={option} value={option}>
                {option}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex items-center gap-3 text-sm text-muted-foreground">
        {meta && <span>{meta.totalItems} resultados</span>}
        <span>Página {page} de {totalPagesDisplay ?? '—'}</span>
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            size="icon-sm"
            aria-label="Página anterior"
            disabled={!canPreviousPage}
            onClick={() => onPageChange(page - 1)}
          >
            <ChevronLeftIcon />
          </Button>
          <Button
            variant="outline"
            size="icon-sm"
            aria-label="Página siguiente"
            disabled={!canNextPage}
            onClick={() => onPageChange(page + 1)}
          >
            <ChevronRightIcon />
          </Button>
        </div>
      </div>
    </div>
  )
}
