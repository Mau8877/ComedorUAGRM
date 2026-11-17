import { useMemo, useState } from 'react'
import type { Row, SortingState } from '@tanstack/react-table'
import { PencilIcon, PlusIcon, RefreshCwIcon } from 'lucide-react'

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

import { useGestiones } from '../api'
import { columnasGestiones, GestionFormModal } from '../components'
import type { GestionAcademica } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300
const SIN_GESTIONES = 'Todavía no hay gestiones académicas'

/** El modal abierto: el alta (`gestion` ausente) o la edición de una gestión. */
type AccionGestion = { gestion?: GestionAcademica }

export interface GestionesAcademicasScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Gestiones académicas» (CU13): la lista de gestiones con búsqueda por etiqueta, orden y paginación del backend, y
 * su alta y edición. Sin una gestión no se puede crear ninguna convocatoria. No se eliminan.
 */
export function GestionesAcademicasScreen({ titulo, activeHref, usuarioActual }: GestionesAcademicasScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [accion, setAccion] = useState<AccionGestion | null>(null)

  // La búsqueda escribe al instante en el input, pero consulta al backend recién cuando se deja de teclear.
  const aplicarBusqueda = useMemo(
    () =>
      debounce((valor: string) => {
        setPage(1)
        setBusquedaAplicada(valor.trim())
      }, RETARDO_BUSQUEDA_MS),
    [],
  )

  const consulta = useGestiones({
    page,
    pageSize,
    search: busquedaAplicada || undefined,
    sort: toSortQueryParam(sorting),
  })

  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnasGestiones,
    meta: consulta.data?.meta,
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

  const renderAcciones = (fila: Row<GestionAcademica>) => (
    <RowActionButton icon={<PencilIcon />} label="Editar" onClick={() => setAccion({ gestion: fila.original })} />
  )

  const mensajeError = getErrorMessage(consulta.error)
  const sinDatos = consulta.isError && !consulta.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Gestiones académicas" className="rounded-2xl border border-border bg-card p-4">
        <DataTableToolbar
          searchValue={busqueda}
          onSearchChange={(valor) => {
            setBusqueda(valor)
            aplicarBusqueda(valor)
          }}
          searchPlaceholder="Buscar por etiqueta..."
          actions={
            <>
              <Button variant="outline" size="sm" onClick={() => void consulta.refetch()} disabled={consulta.isFetching}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
              <Button size="sm" onClick={() => setAccion({})}>
                <PlusIcon data-icon="inline-start" />
                Nueva gestión
              </Button>
            </>
          }
        />

        {sinDatos && (
          <div
            role="alert"
            className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{mensajeError}</span>
            <Button variant="outline" size="sm" onClick={() => void consulta.refetch()}>
              Reintentar
            </Button>
          </div>
        )}

        {!sinDatos && (
          <>
            <div className="mt-4 hidden md:block">
              <DataTable
                table={table}
                isLoading={consulta.isLoading}
                isError={false}
                emptyMessage={SIN_GESTIONES}
                renderActions={renderAcciones}
              />
            </div>
            <div className="mt-4 md:hidden">
              <DataCards
                table={table}
                isLoading={consulta.isLoading}
                isError={false}
                emptyMessage={SIN_GESTIONES}
                renderActions={renderAcciones}
              />
            </div>
          </>
        )}

        <DataTablePagination
          className="mt-4"
          meta={consulta.data?.meta}
          page={page}
          isLoading={consulta.isFetching}
          onPageChange={setPage}
          onPageSizeChange={(nuevo) => {
            setPage(1)
            setPageSize(nuevo)
          }}
        />
      </section>

      {accion && (
        <GestionFormModal
          open
          onOpenChange={(abierto) => {
            if (!abierto) setAccion(null)
          }}
          gestion={accion.gestion}
        />
      )}
    </AdminLayout>
  )
}
