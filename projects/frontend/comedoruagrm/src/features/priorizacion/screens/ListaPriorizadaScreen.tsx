import { useMemo, useState } from 'react'
import { EyeIcon, RefreshCwIcon } from 'lucide-react'
import type { Row } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  getErrorMessage,
  RowActionButton,
  useDataTable,
} from '@/components/ui/tanstack-table'
import type { DataTableToolbarFilter } from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'
import { useCarreras } from '@/shared/api'
import { debounce } from '@/utils/debounce'

import {
  mapearErrorPriorizacion,
  useConvocatoriasPriorizables,
  useListaPriorizada,
  useResumenLista,
} from '../api'
import { columnasLista, DetallePostulacionDialog, ExportarListaButton } from '../components'
import type { PostulacionPriorizada } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300

export interface ListaPriorizadaScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * Lista priorizada de postulantes (CU06): elegir una convocatoria, ver el resumen, recorrer la lista
 * ordenada por puntaje (con búsqueda y filtro por carrera), ver el detalle de una postulación y
 * exportarla a PDF. Solo lectura: nada de lo que se hace acá cambia el estado de una postulación.
 */
export function ListaPriorizadaScreen({ titulo, activeHref, usuarioActual }: ListaPriorizadaScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [carreraId, setCarreraId] = useState<string | undefined>(undefined)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [detalle, setDetalle] = useState<PostulacionPriorizada | null>(null)

  const convocatorias = useConvocatoriasPriorizables()
  const carreras = useCarreras()

  // La más reciente (la primera) viene preseleccionada; si la elegida ya no está en la lista, también.
  const disponibles = convocatorias.data ?? []
  const convocatoriaId =
    elegida && disponibles.some((c) => c.codigo === elegida) ? elegida : disponibles[0]?.codigo

  const resumen = useResumenLista(convocatoriaId)
  const sinCalificadas = resumen.data?.totalCalificadas === 0

  const filtros = useMemo(
    () => (convocatoriaId ? { convocatoriaId, carreraId, search: busquedaAplicada || undefined } : undefined),
    [convocatoriaId, carreraId, busquedaAplicada],
  )
  const lista = useListaPriorizada(
    filtros ? { ...filtros, page, pageSize } : undefined,
    resumen.isSuccess && !sinCalificadas,
  )

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
    data: lista.data?.data ?? [],
    columns: columnasLista,
    meta: lista.data?.meta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nuevo) => {
      setPage(1)
      setPageSize(nuevo)
    },
    getRowId: (fila) => fila.codigo,
  })

  const filtrosToolbar: DataTableToolbarFilter[] = [
    {
      id: 'carreraId',
      label: 'Carrera',
      value: carreraId,
      onValueChange: (valor) => {
        setPage(1)
        setCarreraId(valor)
      },
      options: (carreras.data ?? []).map((c) => ({ label: c.nombre, value: c.codigo })),
    },
  ]

  const renderAcciones = (fila: Row<PostulacionPriorizada>) => (
    <RowActionButton icon={<EyeIcon />} label="Ver" onClick={() => setDetalle(fila.original)} />
  )

  const refrescar = () => {
    void convocatorias.refetch()
    void resumen.refetch()
    void lista.refetch()
  }

  // El primer error que haya, en el orden en que se piden los datos.
  const error = convocatorias.error ?? resumen.error ?? lista.error
  const mapeado = error ? mapearErrorPriorizacion(error) : null
  const mensajeError = getErrorMessage(lista.error)
  const sinDatos = lista.isError && !lista.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="rounded-2xl border border-border bg-card p-4">
        {mapeado && (
          <div
            role="alert"
            className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{mapeado.mensaje}</span>
            {mapeado.convocatoriaInexistente ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setElegida(undefined)
                  void convocatorias.refetch()
                }}
              >
                Elegir otra convocatoria
              </Button>
            ) : (
              <Button variant="outline" size="sm" onClick={refrescar}>
                Reintentar
              </Button>
            )}
          </div>
        )}

        {convocatorias.isSuccess && disponibles.length === 0 && (
          <p className="py-8 text-center text-sm text-muted-foreground">Todavía no hay convocatorias registradas.</p>
        )}

        {convocatorias.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando convocatorias…</p>}

        {convocatoriaId && (
          <>
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
                <Select
                  value={convocatoriaId}
                  onValueChange={(valor) => {
                    if (!valor) return
                    setPage(1)
                    setElegida(valor)
                  }}
                >
                  <SelectTrigger size="sm" aria-label="Convocatoria" className="min-w-72">
                    <SelectValue>
                      {(valor: string | null) => {
                        const actual = disponibles.find((c) => c.codigo === valor)
                        return actual ? `${actual.nombre} (${actual.gestion})` : ''
                      }}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {disponibles.map((c) => (
                      <SelectItem key={c.codigo} value={c.codigo}>
                        {c.nombre} ({c.gestion})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {resumen.data && (
                <p className="text-sm text-muted-foreground" aria-live="polite">
                  {resumen.data.totalCalificadas} postulaciones calificadas · {resumen.data.totalPendientes} pendientes
                  de precalificación
                </p>
              )}
            </div>

            {sinCalificadas ? (
              <p className="py-8 text-center text-sm text-muted-foreground">
                No hay postulaciones calificadas en esta convocatoria.
              </p>
            ) : (
              <>
                <DataTableToolbar
                  className="mt-4"
                  searchValue={busqueda}
                  onSearchChange={(valor) => {
                    setBusqueda(valor)
                    aplicarBusqueda(valor)
                  }}
                  searchPlaceholder="Buscar por nombre o CI..."
                  filters={filtrosToolbar}
                  actions={
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={refrescar}
                        disabled={lista.isFetching || resumen.isFetching}
                      >
                        <RefreshCwIcon data-icon="inline-start" />
                        Refrescar
                      </Button>
                      {filtros && (
                        <ExportarListaButton
                          filtros={filtros}
                          deshabilitado={!lista.data || lista.data.meta.totalItems === 0}
                        />
                      )}
                    </>
                  }
                />

                <div className="mt-4 hidden md:block">
                  <DataTable
                    table={table}
                    isLoading={lista.isLoading}
                    isError={sinDatos}
                    errorMessage={mensajeError}
                    emptyMessage="Sin resultados para esa búsqueda."
                    renderActions={renderAcciones}
                  />
                </div>
                <div className="mt-4 md:hidden">
                  <DataCards
                    table={table}
                    isLoading={lista.isLoading}
                    isError={sinDatos}
                    errorMessage={mensajeError}
                    emptyMessage="Sin resultados para esa búsqueda."
                    renderActions={renderAcciones}
                  />
                </div>

                <DataTablePagination
                  className="mt-4"
                  meta={lista.data?.meta}
                  page={page}
                  isLoading={lista.isFetching}
                  onPageChange={setPage}
                  onPageSizeChange={(nuevo) => {
                    setPage(1)
                    setPageSize(nuevo)
                  }}
                />
              </>
            )}
          </>
        )}
      </div>

      {/* Se monta solo mientras está abierto: al cerrar se descarta. */}
      {detalle && (
        <DetallePostulacionDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setDetalle(null)
          }}
          postulacion={detalle}
        />
      )}
    </AdminLayout>
  )
}
