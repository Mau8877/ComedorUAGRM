import { useMemo, useState } from 'react'
import type { Row, SortingState } from '@tanstack/react-table'
import { ArrowLeftRightIcon, BanIcon, PauseCircleIcon, RefreshCwIcon, UserPlusIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  RowActionButton,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useConvocatorias } from '@/features/convocatorias'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { mapearErrorBecas, usePadron, useResumenPadron } from '../api'
import { AltaPadronDialog, columnasPadron, etiquetaEstadoBeca, MotivoBecaDialog, ReemplazoDialog } from '../components'
import type { AccionMotivo } from '../components'
import type { Beca, EstadoBeca } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300
const SIN_BECADOS = 'Sin resultados'
// El selector muestra todas las convocatorias con resultados publicados de una vez.
const CONVOCATORIAS_PUBLICADAS = { page: 1, pageSize: 50, estado: 'PUBLICADA' as const, sort: '-fechaApertura' }
const ESTADOS: EstadoBeca[] = ['VIGENTE', 'SUSPENDIDA', 'BAJA']

export interface PadronScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Padrón de becados» (CU23): el Administrador elige una convocatoria con resultados publicados, da de alta a los
 * adjudicados pendientes y administra el padrón: busca por nombre o CI, filtra por estado, ordena y da de baja,
 * suspende o reemplaza el cupo de una beca. La baja y la suspensión piden el motivo; el reemplazo muestra a quién se
 * propone antes de confirmar. Las acciones de cada fila dependen del estado de la beca.
 */
export function PadronScreen({ titulo, activeHref, usuarioActual }: PadronScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const convocatorias = useConvocatorias(CONVOCATORIAS_PUBLICADAS)

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [estado, setEstado] = useState<EstadoBeca | undefined>(undefined)
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [motivo, setMotivo] = useState<{ beca: Beca; accion: AccionMotivo } | null>(null)
  const [reemplazo, setReemplazo] = useState<Beca | null>(null)
  const [altaAbierta, setAltaAbierta] = useState(false)

  const disponibles = convocatorias.data?.data ?? []
  const convocatoria = disponibles.find((c) => c.codigo === elegida) ?? disponibles[0]
  const codigo = convocatoria?.codigo

  const resumen = useResumenPadron(codigo)
  const publicados = resumen.data?.resultadosPublicados ?? false
  const padron = usePadron(
    codigo,
    { page, pageSize, search: busquedaAplicada || undefined, estado, sort: toSortQueryParam(sorting) },
    publicados,
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
    data: padron.data?.data ?? [],
    columns: columnasPadron,
    meta: padron.data?.meta,
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

  const renderAcciones = (fila: Row<Beca>) => {
    const beca = fila.original
    const vigente = beca.estado === 'VIGENTE'
    return (
      <>
        <RowActionButton
          icon={<PauseCircleIcon />}
          label="Suspender"
          disabled={!vigente}
          onClick={() => setMotivo({ beca, accion: 'suspension' })}
        />
        <RowActionButton
          icon={<BanIcon />}
          label="Dar de baja"
          variant="destructive"
          disabled={!vigente}
          onClick={() => setMotivo({ beca, accion: 'baja' })}
        />
        <RowActionButton
          icon={<ArrowLeftRightIcon />}
          label="Reemplazar"
          disabled={beca.estado !== 'BAJA'}
          onClick={() => setReemplazo(beca)}
        />
      </>
    )
  }

  const errorConvocatorias = convocatorias.isError && !convocatorias.data ? mapearErrorBecas(convocatorias.error) : null
  const errorResumen = resumen.isError && !resumen.data ? mapearErrorBecas(resumen.error) : null
  const errorPadron = padron.isError && !padron.data ? mapearErrorBecas(padron.error) : null
  const sinPublicadas = convocatorias.isSuccess && disponibles.length === 0

  const reintentar = () => {
    void convocatorias.refetch()
    void resumen.refetch()
    void padron.refetch()
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Padrón de becados" className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-lg">Padrón de becados</h2>
        <p className="mb-3 text-sm text-muted-foreground">
          {convocatoria ? `Becas de la convocatoria ${convocatoria.nombre}` : 'Elige una convocatoria con resultados publicados'}
        </p>

        {convocatorias.isLoading && <p className="text-sm text-muted-foreground">Cargando convocatorias…</p>}
        {sinPublicadas && (
          <p role="status" className="text-sm text-muted-foreground">
            Todavía no hay convocatorias con resultados publicados
          </p>
        )}
        {(errorConvocatorias || errorResumen || errorPadron) && (
          <div
            role="alert"
            className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{(errorConvocatorias ?? errorResumen ?? errorPadron)?.mensaje}</span>
            <Button variant="outline" size="sm" onClick={reintentar}>
              Reintentar
            </Button>
          </div>
        )}

        {convocatoria && (
          <>
            <div className="flex flex-wrap items-end gap-3">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
                <Select
                  value={codigo ?? ''}
                  onValueChange={(valor) => {
                    if (!valor) return
                    setElegida(valor)
                    setPage(1)
                  }}
                >
                  <SelectTrigger size="sm" aria-label="Convocatoria" className="min-w-72">
                    <SelectValue>
                      {(valor: string | null) => disponibles.find((c) => c.codigo === valor)?.nombre ?? ''}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {disponibles.map((c) => (
                      <SelectItem key={c.codigo} value={c.codigo}>
                        {c.nombre}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {resumen.data && resumen.data.pendientesDeAlta > 0 && (
                <Button size="sm" onClick={() => setAltaAbierta(true)}>
                  <UserPlusIcon data-icon="inline-start" />
                  Dar de alta ({resumen.data.pendientesDeAlta})
                </Button>
              )}
            </div>

            {resumen.data && (
              <dl aria-label="Resumen del padrón" className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm">
                <div className="flex gap-1.5">
                  <dt className="text-muted-foreground">Adjudicados:</dt>
                  <dd className="font-medium">{resumen.data.adjudicados}</dd>
                </div>
                <div className="flex gap-1.5">
                  <dt className="text-muted-foreground">Vigentes:</dt>
                  <dd className="font-medium">{resumen.data.vigentes}</dd>
                </div>
                <div className="flex gap-1.5">
                  <dt className="text-muted-foreground">Suspendidas:</dt>
                  <dd className="font-medium">{resumen.data.suspendidas}</dd>
                </div>
                <div className="flex gap-1.5">
                  <dt className="text-muted-foreground">Bajas:</dt>
                  <dd className="font-medium">{resumen.data.bajas}</dd>
                </div>
              </dl>
            )}

            {resumen.data && !resumen.data.resultadosPublicados && (
              <p role="status" className="mt-3 text-sm text-muted-foreground">
                Los resultados de esta convocatoria todavía no fueron publicados.
              </p>
            )}

            {publicados && (
              <>
                <DataTableToolbar
                  className="mt-4"
                  searchValue={busqueda}
                  onSearchChange={(valor) => {
                    setBusqueda(valor)
                    aplicarBusqueda(valor)
                  }}
                  searchPlaceholder="Buscar becado por nombre o CI..."
                  filters={[
                    {
                      id: 'estado',
                      label: 'Estado',
                      placeholder: 'Estado: todos',
                      options: ESTADOS.map((e) => ({ label: etiquetaEstadoBeca(e), value: e })),
                      value: estado,
                      onValueChange: (valor) => {
                        setPage(1)
                        setEstado(valor as EstadoBeca | undefined)
                      },
                    },
                  ]}
                  actions={
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        void padron.refetch()
                        void resumen.refetch()
                      }}
                      disabled={padron.isFetching}
                    >
                      <RefreshCwIcon data-icon="inline-start" />
                      Refrescar
                    </Button>
                  }
                />

                {!errorPadron && (
                  <>
                    <div className="mt-4 hidden md:block">
                      <DataTable
                        table={table}
                        isLoading={padron.isLoading}
                        isError={false}
                        emptyMessage={SIN_BECADOS}
                        renderActions={renderAcciones}
                      />
                    </div>
                    <div className="mt-4 md:hidden">
                      <DataCards
                        table={table}
                        isLoading={padron.isLoading}
                        isError={false}
                        emptyMessage={SIN_BECADOS}
                        renderActions={renderAcciones}
                      />
                    </div>
                  </>
                )}

                <DataTablePagination
                  className="mt-4"
                  meta={padron.data?.meta}
                  page={page}
                  isLoading={padron.isFetching}
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
      </section>

      {motivo && (
        <MotivoBecaDialog
          open
          onOpenChange={(abierto) => !abierto && setMotivo(null)}
          beca={motivo.beca}
          accion={motivo.accion}
        />
      )}
      {reemplazo && <ReemplazoDialog open onOpenChange={(abierto) => !abierto && setReemplazo(null)} beca={reemplazo} />}
      {convocatoria && resumen.data && (
        <AltaPadronDialog
          open={altaAbierta}
          onOpenChange={setAltaAbierta}
          convocatoria={{ codigo: convocatoria.codigo, nombre: convocatoria.nombre }}
          pendientes={resumen.data.pendientesDeAlta}
        />
      )}
    </AdminLayout>
  )
}
