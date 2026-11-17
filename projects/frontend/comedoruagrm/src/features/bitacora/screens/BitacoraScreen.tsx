import { useMemo, useState } from 'react'
import type { SortingState } from '@tanstack/react-table'
import { RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  getErrorMessage,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { useAccionesBitacora, useBitacora } from '../api'
import { columnasBitacora } from '../components'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300
const SIN_REGISTROS = 'Sin resultados'

export interface BitacoraScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Bitácora» (CU25): registro de acciones de solo lectura, con filtro por usuario (nombre o correo), acción y rango
 * de fechas, orden por fecha, acción o entidad y paginación del backend. No hay ninguna acción de fila: la
 * bitácora no se edita ni se elimina (RF-20).
 */
export function BitacoraScreen({ titulo, activeHref, usuarioActual }: BitacoraScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const acciones = useAccionesBitacora()

  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [accion, setAccion] = useState<string | undefined>()
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)

  const rangoInvalido = Boolean(desde && hasta && desde > hasta)

  const bitacora = useBitacora({
    page,
    pageSize,
    search: busquedaAplicada || undefined,
    accion,
    // Un rango invertido no se manda: el backend lo rechazaría con ERR_BIT_02.
    desde: rangoInvalido ? undefined : desde || undefined,
    hasta: rangoInvalido ? undefined : hasta || undefined,
    sort: toSortQueryParam(sorting),
  })

  const aplicarBusqueda = useMemo(
    () =>
      debounce((valor: string) => {
        setPage(1)
        setBusquedaAplicada(valor.trim())
      }, RETARDO_BUSQUEDA_MS),
    [],
  )

  const { table } = useDataTable({
    data: bitacora.data?.data ?? [],
    columns: columnasBitacora,
    meta: bitacora.data?.meta,
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

  const mensajeError = getErrorMessage(bitacora.error)
  const sinDatos = bitacora.isError && !bitacora.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Bitácora del sistema" className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-lg">Bitácora del sistema</h2>
        <p className="mb-3 text-sm text-muted-foreground">Registro de acciones (solo lectura)</p>

        <DataTableToolbar
          searchValue={busqueda}
          onSearchChange={(valor) => {
            setBusqueda(valor)
            aplicarBusqueda(valor)
          }}
          searchPlaceholder="Usuario (nombre o correo)..."
          filters={[
            {
              id: 'accion',
              label: 'Acción',
              placeholder: 'Acción: todas',
              options: (acciones.data ?? []).map((valor) => ({ label: valor, value: valor })),
              value: accion,
              onValueChange: (valor) => {
                setPage(1)
                setAccion(valor)
              },
            },
          ]}
          actions={
            <Button variant="outline" size="sm" onClick={() => void bitacora.refetch()} disabled={bitacora.isFetching}>
              <RefreshCwIcon data-icon="inline-start" />
              Refrescar
            </Button>
          }
        />

        <div className="mt-3 flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-sm">
            Desde
            <Input
              type="date"
              value={desde}
              onChange={(e) => {
                setPage(1)
                setDesde(e.target.value)
              }}
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Hasta
            <Input
              type="date"
              value={hasta}
              onChange={(e) => {
                setPage(1)
                setHasta(e.target.value)
              }}
            />
          </label>
          {rangoInvalido && (
            <span role="alert" className="text-xs text-destructive">
              La fecha «desde» no puede ser posterior a «hasta»
            </span>
          )}
        </div>

        {sinDatos && (
          <div
            role="alert"
            className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{mensajeError}</span>
            <Button variant="outline" size="sm" onClick={() => void bitacora.refetch()}>
              Reintentar
            </Button>
          </div>
        )}

        {!sinDatos && (
          <>
            <div className="mt-4 hidden md:block">
              <DataTable table={table} isLoading={bitacora.isLoading} isError={false} emptyMessage={SIN_REGISTROS} />
            </div>
            <div className="mt-4 md:hidden">
              <DataCards table={table} isLoading={bitacora.isLoading} isError={false} emptyMessage={SIN_REGISTROS} />
            </div>
          </>
        )}

        <DataTablePagination
          className="mt-4"
          meta={bitacora.data?.meta}
          page={page}
          isLoading={bitacora.isFetching}
          onPageChange={setPage}
          onPageSizeChange={(nuevo) => {
            setPage(1)
            setPageSize(nuevo)
          }}
        />
      </section>
    </AdminLayout>
  )
}
