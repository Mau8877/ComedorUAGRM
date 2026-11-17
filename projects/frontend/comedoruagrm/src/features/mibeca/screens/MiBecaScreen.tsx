import { useState } from 'react'
import type { SortingState } from '@tanstack/react-table'
import { useNavigate } from '@tanstack/react-router'
import { RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  getErrorMessage,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'
import { ApiError } from '@/store'

import { useAsistenciasBecado, useMiBeca } from '../api'
import { columnasAsistencias, EstadoBecaBadge, formatearDia, formatearFechaInstante } from '../components'

const PAGE_SIZE_INICIAL = 20
const ERR_SIN_BECA = 'ERR_MBE_01'
const SIN_ASISTENCIAS = 'Todavía no hay asistencias registradas'
const SIN_ASISTENCIAS_EN_RANGO = 'No hay asistencias en ese rango de fechas'

export interface MiBecaScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante becado). */
  usuarioActual: Usuario
}

/**
 * «Mi beca» (CU24): el estado de la beca del becado (con el motivo si está suspendida o dada de baja) y su historial
 * de asistencia, paginado, de la fecha más reciente a la más antigua y filtrable por rango de fechas. Es de solo
 * lectura y siempre muestra la beca de la sesión. Sin beca (ERR_MBE_01) avisa y lleva a «Mis postulaciones» (CU04).
 */
export function MiBecaScreen({ titulo, activeHref, usuarioActual }: MiBecaScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const beca = useMiBeca()
  const navigate = useNavigate()

  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)

  const rangoInvalido = Boolean(desde && hasta && desde > hasta)
  const sinBeca = beca.error instanceof ApiError && beca.error.code === ERR_SIN_BECA

  const asistencias = useAsistenciasBecado(
    {
      page,
      pageSize,
      // Un rango invertido no se manda: el backend lo rechazaría con ERR_MBE_02.
      desde: rangoInvalido ? undefined : desde || undefined,
      hasta: rangoInvalido ? undefined : hasta || undefined,
      sort: toSortQueryParam(sorting),
    },
    Boolean(beca.data),
  )

  const { table } = useDataTable({
    data: asistencias.data?.data ?? [],
    columns: columnasAsistencias,
    meta: asistencias.data?.meta,
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
    getRowId: (fila) => fila.fecha,
  })

  const hayRango = Boolean(desde || hasta)
  const mensajeVacio = hayRango ? SIN_ASISTENCIAS_EN_RANGO : SIN_ASISTENCIAS
  const errorAsistencias = asistencias.isError && !asistencias.data

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {beca.isLoading && <p className="text-sm text-muted-foreground">Cargando tu beca...</p>}

      {sinBeca && (
        <section aria-label="Sin beca" className="rounded-2xl border border-border bg-card p-4">
          <h2 className="font-heading text-lg">No cuentas con una beca</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            No figuras en el padrón de becados. Revisa el estado de tu postulación.
          </p>
          <Button className="mt-3" onClick={() => void navigate({ to: '/mis-postulaciones' })}>
            Ir a Mis postulaciones
          </Button>
        </section>
      )}

      {beca.isError && !sinBeca && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{getErrorMessage(beca.error)}</span>
          <Button variant="outline" size="sm" onClick={() => void beca.refetch()}>
            Reintentar
          </Button>
        </div>
      )}

      {beca.data && (
        <>
          <section aria-label="Estado de la beca" className="rounded-2xl border border-border bg-card p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="font-heading text-lg">Estado de la beca</h2>
                <p className="text-sm text-muted-foreground">
                  {beca.data.convocatoria} · Gestión {beca.data.gestion}
                </p>
              </div>
              <EstadoBecaBadge estado={beca.data.estado} />
            </div>

            <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-muted-foreground">Fecha de adjudicación</dt>
                <dd>{beca.data.fechaAdjudicacion ? formatearFechaInstante(beca.data.fechaAdjudicacion) : '—'}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Inicio de la beca</dt>
                <dd>{formatearDia(beca.data.fechaInicio)}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Asistencias registradas</dt>
                <dd>{beca.data.totalAsistencias}</dd>
              </div>
            </dl>

            {beca.data.estado !== 'VIGENTE' && beca.data.motivo && (
              <p className="mt-3 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm">
                <span className="font-medium">Motivo: </span>
                {beca.data.motivo}
              </p>
            )}

            <p className="mt-3 rounded-md border border-info/30 bg-info/10 p-3 text-sm">
              El registro lo realiza el personal del comedor. Si detectas un error, comunícate con la DUBSS.
            </p>
          </section>

          <section aria-label="Historial de asistencia" className="mt-4 rounded-2xl border border-border bg-card p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-heading text-lg">Historial de asistencia</h2>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void asistencias.refetch()}
                disabled={asistencias.isFetching}
              >
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
            </div>

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

            {errorAsistencias && (
              <div
                role="alert"
                className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
              >
                <span>{getErrorMessage(asistencias.error)}</span>
                <Button variant="outline" size="sm" onClick={() => void asistencias.refetch()}>
                  Reintentar
                </Button>
              </div>
            )}

            {!errorAsistencias && (
              <>
                <div className="mt-4 hidden md:block">
                  <DataTable
                    table={table}
                    isLoading={asistencias.isLoading}
                    isError={false}
                    emptyMessage={mensajeVacio}
                  />
                </div>
                <div className="mt-4 md:hidden">
                  <DataCards
                    table={table}
                    isLoading={asistencias.isLoading}
                    isError={false}
                    emptyMessage={mensajeVacio}
                  />
                </div>
              </>
            )}

            <DataTablePagination
              className="mt-4"
              meta={asistencias.data?.meta}
              page={page}
              isLoading={asistencias.isFetching}
              onPageChange={setPage}
              onPageSizeChange={(nuevo) => {
                setPage(1)
                setPageSize(nuevo)
              }}
            />
          </section>
        </>
      )}
    </EstudianteLayout>
  )
}
