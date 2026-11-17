import { useState } from 'react'

import {
  DataCards,
  DataTable,
  DataTablePagination,
  getErrorMessage,
  useDataTable,
} from '@/components/ui/tanstack-table'

import { useHistorialVersiones } from '../api'
import { columnasHistorial } from './columnasHistorial'

const PAGE_SIZE_INICIAL = 10

/** «Historial de versiones» (CU26): todas las versiones guardadas, la más reciente primero, con su fecha y estado. */
export function HistorialVersiones() {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const historial = useHistorialVersiones({ page, pageSize })

  const { table } = useDataTable({
    data: historial.data?.data ?? [],
    columns: columnasHistorial,
    meta: historial.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nuevo) => {
      setPage(1)
      setPageSize(nuevo)
    },
    getRowId: (fila) => fila.codigo,
  })

  const errorMessage = historial.isError ? getErrorMessage(historial.error) : undefined
  const sinVersiones = 'Todavía no se guardó ninguna versión'

  return (
    <section aria-label="Historial de versiones" className="rounded-2xl border border-border bg-card p-4">
      <h2 className="font-heading text-lg">Historial de versiones</h2>
      <div className="mt-3 hidden md:block">
        <DataTable
          table={table}
          isLoading={historial.isLoading}
          isError={historial.isError && !historial.data}
          errorMessage={errorMessage}
          emptyMessage={sinVersiones}
        />
      </div>
      <div className="mt-3 md:hidden">
        <DataCards
          table={table}
          isLoading={historial.isLoading}
          isError={historial.isError && !historial.data}
          errorMessage={errorMessage}
          emptyMessage={sinVersiones}
        />
      </div>
      <DataTablePagination
        className="mt-3"
        meta={historial.data?.meta}
        page={page}
        isLoading={historial.isFetching}
        onPageChange={setPage}
        onPageSizeChange={(nuevo) => {
          setPage(1)
          setPageSize(nuevo)
        }}
      />
    </section>
  )
}
