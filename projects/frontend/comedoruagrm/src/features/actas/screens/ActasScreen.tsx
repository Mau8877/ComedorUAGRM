import { useMemo, useState } from 'react'
import type { Row, SortingState } from '@tanstack/react-table'
import { DownloadIcon, RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  getErrorMessage,
  RowActionButton,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { useActas } from '../api'
import { columnasActas, GenerarActaPanel, useDescargarActa } from '../components'
import type { Acta } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300
const SIN_ACTAS = 'Todavía no hay actas generadas'

export interface ActasScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Actas» (CU08): la tabla de actas de adjudicación generadas, con búsqueda por número o convocatoria, orden por
 * número o fecha, paginación del backend y la descarga del PDF de cada una. La dirección de descarga se pide en
 * cada clic (vence en minutos) y nunca se muestra ni se guarda. Encima de la tabla va el panel para generar actas.
 */
export function ActasScreen({ titulo, activeHref, usuarioActual }: ActasScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const descarga = useDescargarActa()

  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)

  const actas = useActas({
    page,
    pageSize,
    search: busquedaAplicada || undefined,
    sort: toSortQueryParam(sorting),
  })

  // La búsqueda escribe al instante en el input, pero consulta al backend recién cuando se deja de teclear.
  const aplicarBusqueda = useMemo(
    () =>
      debounce((valor: string) => {
        setPage(1)
        setBusquedaAplicada(valor.trim())
      }, RETARDO_BUSQUEDA_MS),
    [],
  )

  const { table } = useDataTable({
    data: actas.data?.data ?? [],
    columns: columnasActas,
    meta: actas.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nuevo) => {
      setPage(1)
      setPageSize(nuevo)
    },
    enableSorting: true,
    sorting,
    onSortingChange: (nuevo) => {
      setPage(1)
      setSorting(nuevo)
    },
    getRowId: (fila) => fila.codigo,
  })

  const renderAcciones = (fila: Row<Acta>) => (
    <RowActionButton
      icon={<DownloadIcon />}
      label="Descargar"
      disabled={descarga.descargando === fila.original.convocatoria.codigo}
      onClick={() => descarga.descargar(fila.original.convocatoria.codigo)}
    />
  )

  const mensajeError = getErrorMessage(actas.error)
  const sinDatos = actas.isError && !actas.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-4">
        {descarga.error && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning"
          >
            <span>{descarga.error}</span>
            <Button variant="outline" size="sm" onClick={descarga.limpiarError}>
              Cerrar
            </Button>
          </div>
        )}

        <GenerarActaPanel />

        <section aria-label="Actas generadas" className="rounded-2xl border border-border bg-card p-4">
          <h2 className="mb-3 font-heading text-lg">Actas generadas</h2>
          <DataTableToolbar
            searchValue={busqueda}
            onSearchChange={(valor) => {
              setBusqueda(valor)
              aplicarBusqueda(valor)
            }}
            searchPlaceholder="Buscar por número o convocatoria..."
            actions={
              <Button variant="outline" size="sm" onClick={() => void actas.refetch()} disabled={actas.isFetching}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
            }
          />

          {sinDatos && (
            <div
              role="alert"
              className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              <span>{mensajeError}</span>
              <Button variant="outline" size="sm" onClick={() => void actas.refetch()}>
                Reintentar
              </Button>
            </div>
          )}

          {!sinDatos && (
            <>
              <div className="mt-4 hidden md:block">
                <DataTable
                  table={table}
                  isLoading={actas.isLoading}
                  isError={false}
                  emptyMessage={SIN_ACTAS}
                  renderActions={renderAcciones}
                />
              </div>
              <div className="mt-4 md:hidden">
                <DataCards
                  table={table}
                  isLoading={actas.isLoading}
                  isError={false}
                  emptyMessage={SIN_ACTAS}
                  renderActions={renderAcciones}
                />
              </div>
            </>
          )}

          <DataTablePagination
            className="mt-4"
            meta={actas.data?.meta}
            page={page}
            isLoading={actas.isFetching}
            onPageChange={setPage}
            onPageSizeChange={(nuevo) => {
              setPage(1)
              setPageSize(nuevo)
            }}
          />
        </section>
      </div>
    </AdminLayout>
  )
}
