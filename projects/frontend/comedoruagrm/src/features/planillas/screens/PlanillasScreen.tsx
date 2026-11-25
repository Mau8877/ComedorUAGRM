import { useEffect, useRef, useState } from 'react'
import { useForm, useStore } from '@tanstack/react-form'
import type { Row, SortingState } from '@tanstack/react-table'
import { DownloadIcon, EyeIcon, FileTextIcon, RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  RowActionButton,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { toastSuccess } from '@/components/ui/toast'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useConvocatorias } from '@/features/convocatorias'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'

import { mapearErrorPlanillas, usePlanillas, useGenerarPlanilla, useResumenPlanilla, useVistaPreviaPlanilla } from '../api'
import type { ErrorPlanillas } from '../api'
import { columnasPlanillas, formatearPeriodo, useDescargarPlanilla } from '../components'
import { crearPeriodoSchema } from '../schemas'
import type { Planilla } from '../types'
import { errorVisible } from '@/utils/errorCampo'
import { validacionFormulario } from '@/utils/validacionFormulario'

const PAGE_SIZE_INICIAL = 20
const SIN_PLANILLAS = 'Todavía no hay planillas generadas para esta convocatoria'
// El selector muestra todas las convocatorias con resultados publicados de una vez: solo ellas tienen padrón.
const CONVOCATORIAS_PUBLICADAS = { page: 1, pageSize: 50, estado: 'PUBLICADA' as const, sort: '-fechaApertura' }

type Accion = 'generar' | 'vista-previa'

/** El PDF borrador que se está mostrando: su dirección de objeto local y a qué convocatoria y periodo pertenece. */
interface VistaPrevia {
  url: string
  clave: string
}

export interface PlanillasScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Planilla de asistencia» (CU09): el Administrador elige una convocatoria con resultados publicados, ingresa el
 * periodo y genera la planilla imprimible (firma y fotocopia de carnet) con el padrón vigente. Antes de generar ve
 * cuántos becados entrarían y puede mirar la vista previa; abajo están las planillas ya generadas, con descarga.
 *
 * * El periodo se valida en el cliente con las mismas reglas del backend (`crearPeriodoSchema`); el servidor vuelve a
 * * validar y su error se muestra tal cual. Sin becas vigentes no se puede generar ni ver la vista previa (A2).
 * * El PDF borrador es estado local de esta pantalla: el `Blob` se muestra en un `<iframe>` con una dirección de objeto
 * * que se libera (`URL.revokeObjectURL`) al pedir otra vista previa, al cambiar de convocatoria o periodo y al
 * * desmontar. Nunca pasa por la caché ni por un store.
 */
export function PlanillasScreen({ titulo, activeHref, usuarioActual }: PlanillasScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const convocatorias = useConvocatorias(CONVOCATORIAS_PUBLICADAS)
  const generar = useGenerarPlanilla()
  const vistaPrevia = useVistaPreviaPlanilla()
  const descarga = useDescargarPlanilla()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [vista, setVista] = useState<VistaPrevia | null>(null)
  const [error, setError] = useState<ErrorPlanillas | null>(null)
  const [generada, setGenerada] = useState<Planilla | null>(null)
  // Qué botón disparó el envío del formulario: los dos comparten la validación del periodo.
  const accion = useRef<Accion>('generar')
  // La dirección viva, para poder liberarla desde cualquier camino (cambio, nueva vista previa o desmontar).
  const urlViva = useRef<string | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const disponibles = convocatorias.data?.data ?? []
  const convocatoria = disponibles.find((c) => c.codigo === elegida) ?? disponibles[0]
  const codigo = convocatoria?.codigo

  const resumen = useResumenPlanilla(codigo)
  const planillas = usePlanillas(codigo, { page, pageSize, sort: toSortQueryParam(sorting) })
  const becados = resumen.data?.becadosVigentes
  const sinBecados = becados === 0

  const liberar = () => {
    if (urlViva.current) {
      URL.revokeObjectURL(urlViva.current)
      urlViva.current = null
    }
  }

  const form = useForm({
    defaultValues: { desde: '', hasta: '' },
    ...validacionFormulario(crearPeriodoSchema(resumen.data)),
    onSubmit: ({ value }) => {
      if (!codigo || enviando.current) return
      enviando.current = true
      setError(null)
      const periodo = { fechaDesde: value.desde, fechaHasta: value.hasta }
      const alTerminar = () => {
        enviando.current = false
      }
      const alFallar = (e: unknown) => {
        const mapeado = mapearErrorPlanillas(e)
        liberar()
        setVista(null)
        if (mapeado.refrescar) void resumen.refetch()
        if (mapeado.convocatoriaInexistente) {
          setElegida(undefined)
          void convocatorias.refetch()
        }
        setError(mapeado)
      }

      if (accion.current === 'vista-previa') {
        vistaPrevia.mutate(
          { convocatoria: codigo, periodo },
          {
            onSuccess: (pdf) => {
              liberar()
              const url = URL.createObjectURL(pdf)
              urlViva.current = url
              setVista({ url, clave: `${codigo}|${value.desde}|${value.hasta}` })
            },
            onError: alFallar,
            onSettled: alTerminar,
          },
        )
        return
      }

      setGenerada(null)
      generar.mutate(
        { convocatoria: codigo, periodo },
        {
          onSuccess: (planilla) => {
            setGenerada(planilla)
            setPage(1)
            toastSuccess('Planilla generada', formatearPeriodo(planilla.fechaDesde, planilla.fechaHasta))
          },
          onError: alFallar,
          onSettled: alTerminar,
        },
      )
    },
  })

  const periodo = useStore(form.store, (estado) => estado.values)

  // La vista previa solo vale para la convocatoria y el periodo con que se pidió: si cambian, se descarta.
  const claveActual = `${codigo}|${periodo.desde}|${periodo.hasta}`
  const vistaVigente = vista && vista.clave === claveActual ? vista : null
  if (vista && !vistaVigente) {
    setVista(null)
  }
  useEffect(() => {
    if (!vistaVigente) liberar()
  }, [vistaVigente])
  // Al desmontar se libera la dirección que siga viva.
  useEffect(() => liberar, [])

  const { table } = useDataTable({
    data: planillas.data?.data ?? [],
    columns: columnasPlanillas,
    meta: planillas.data?.meta,
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

  const renderAcciones = (fila: Row<Planilla>) => (
    <RowActionButton
      icon={<DownloadIcon />}
      label="Descargar"
      disabled={descarga.descargando === fila.original.codigo}
      onClick={() => descarga.descargar(fila.original.codigo)}
    />
  )

  const errorConvocatorias = convocatorias.isError && !convocatorias.data ? mapearErrorPlanillas(convocatorias.error) : null
  const errorResumen = resumen.isError && !resumen.data ? mapearErrorPlanillas(resumen.error) : null
  const errorPlanillas = planillas.isError && !planillas.data ? mapearErrorPlanillas(planillas.error) : null
  const sinPublicadas = convocatorias.isSuccess && disponibles.length === 0
  const ocupado = generar.isPending || vistaPrevia.isPending
  const bloqueado = ocupado || sinBecados || !resumen.data

  const reintentar = () => {
    void convocatorias.refetch()
    void resumen.refetch()
    void planillas.refetch()
  }

  const enviar = (que: Accion) => {
    accion.current = que
    void form.handleSubmit()
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Planilla de asistencia" className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-lg">Planilla de asistencia</h2>
        <p className="mb-3 text-sm text-muted-foreground">Se genera a partir del padrón vigente</p>

        {convocatorias.isLoading && <p className="text-sm text-muted-foreground">Cargando convocatorias…</p>}
        {sinPublicadas && (
          <p role="status" className="text-sm text-muted-foreground">
            Todavía no hay convocatorias con resultados publicados
          </p>
        )}
        {(errorConvocatorias || errorResumen) && (
          <div
            role="alert"
            className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{(errorConvocatorias ?? errorResumen)?.mensaje}</span>
            <Button variant="outline" size="sm" onClick={reintentar}>
              Reintentar
            </Button>
          </div>
        )}

        {convocatoria && (
          <form
            noValidate
            aria-label="Periodo de la planilla"
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              enviar('generar')
            }}
          >
            <div className="flex flex-wrap items-start gap-3">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
                <Select
                  value={codigo ?? ''}
                  onValueChange={(valor) => {
                    if (!valor) return
                    setError(null)
                    setGenerada(null)
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

              <form.Field name="desde">
                {(field) => (
                  <div className="flex flex-col gap-1">
                    <label htmlFor="planilla-desde" className="text-xs font-medium text-muted-foreground">
                      Desde
                    </label>
                    <Input
                      id="planilla-desde"
                      type="date"
                      value={field.state.value}
                      min={resumen.data?.gestionDesde}
                      max={resumen.data?.gestionHasta}
                      aria-invalid={errorVisible(field) && field.state.meta.errors.length > 0 ? true : undefined}
                      onBlur={field.handleBlur}
                      onChange={(e) => {
                        setError(null)
                        field.handleChange(e.target.value)
                      }}
                    />
                    {errorVisible(field) && field.state.meta.errors[0] && (
                      <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
                    )}
                  </div>
                )}
              </form.Field>

              <form.Field name="hasta">
                {(field) => (
                  <div className="flex flex-col gap-1">
                    <label htmlFor="planilla-hasta" className="text-xs font-medium text-muted-foreground">
                      Hasta
                    </label>
                    <Input
                      id="planilla-hasta"
                      type="date"
                      value={field.state.value}
                      min={resumen.data?.gestionDesde}
                      max={resumen.data?.gestionHasta}
                      aria-invalid={errorVisible(field) && field.state.meta.errors.length > 0 ? true : undefined}
                      onBlur={field.handleBlur}
                      onChange={(e) => {
                        setError(null)
                        field.handleChange(e.target.value)
                      }}
                    />
                    {errorVisible(field) && field.state.meta.errors[0] && (
                      <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
                    )}
                  </div>
                )}
              </form.Field>

              <div className="flex items-center gap-2 self-end">
                <Button type="button" variant="outline" size="sm" disabled={bloqueado} onClick={() => enviar('vista-previa')}>
                  <EyeIcon data-icon="inline-start" />
                  {vistaPrevia.isPending ? 'Generando vista previa…' : 'Vista previa'}
                </Button>
                <Button type="submit" size="sm" disabled={bloqueado}>
                  <FileTextIcon data-icon="inline-start" />
                  {generar.isPending ? 'Generando…' : 'Generar planilla'}
                </Button>
              </div>
            </div>
          </form>
        )}

        {resumen.data && !sinBecados && (
          <p
            role="note"
            className="mt-3 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning"
          >
            Se incluirán {resumen.data.becadosVigentes} becados VIGENTES, con espacio para firma y fotocopia de carnet.
            El periodo debe estar dentro de la gestión {resumen.data.gestion}.
          </p>
        )}
        {sinBecados && (
          <p role="status" className="mt-3 rounded-md border border-border bg-muted p-3 text-sm text-muted-foreground">
            Esta convocatoria no tiene becas vigentes: no hay a quién incluir en la planilla.
          </p>
        )}

        {error && (
          <div
            role="alert"
            className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{error.mensaje}</span>
            {error.red && (
              <Button variant="outline" size="sm" onClick={() => enviar(accion.current)} disabled={ocupado}>
                Reintentar
              </Button>
            )}
          </div>
        )}

        {generada && (
          <div
            role="status"
            className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-success/30 bg-success/10 p-3 text-sm text-success"
          >
            <span>
              Planilla generada para {formatearPeriodo(generada.fechaDesde, generada.fechaHasta)} con{' '}
              {generada.totalBecados} becados.
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={descarga.descargando === generada.codigo}
              onClick={() => descarga.descargar(generada.codigo)}
            >
              <DownloadIcon data-icon="inline-start" />
              Descargar
            </Button>
          </div>
        )}

        <div className="mt-4">
          {vistaVigente ? (
            <iframe
              title="Vista previa de la planilla"
              src={vistaVigente.url}
              className="h-[70vh] w-full rounded-md border border-border"
            />
          ) : (
            <p className="rounded-md border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Elige el periodo y pulsa «Vista previa» para ver cómo quedará la planilla (nombre, CI, carrera, fotocopia
              de carnet y una columna de firma por día).
            </p>
          )}
        </div>
      </section>

      {codigo && (
        <section aria-label="Planillas generadas" className="mt-4 rounded-2xl border border-border bg-card p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-heading text-lg">Planillas generadas</h2>
            <Button variant="outline" size="sm" onClick={() => void planillas.refetch()} disabled={planillas.isFetching}>
              <RefreshCwIcon data-icon="inline-start" />
              Refrescar
            </Button>
          </div>

          {descarga.error && (
            <div
              role="alert"
              className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              <span>{descarga.error}</span>
              <Button variant="outline" size="sm" onClick={descarga.limpiarError}>
                Cerrar
              </Button>
            </div>
          )}

          {errorPlanillas ? (
            <div
              role="alert"
              className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              <span>{errorPlanillas.mensaje}</span>
              <Button variant="outline" size="sm" onClick={reintentar}>
                Reintentar
              </Button>
            </div>
          ) : (
            <>
              <div className="hidden md:block">
                <DataTable
                  table={table}
                  isLoading={planillas.isLoading}
                  isError={false}
                  emptyMessage={SIN_PLANILLAS}
                  renderActions={renderAcciones}
                />
              </div>
              <div className="md:hidden">
                <DataCards
                  table={table}
                  isLoading={planillas.isLoading}
                  isError={false}
                  emptyMessage={SIN_PLANILLAS}
                  renderActions={renderAcciones}
                />
              </div>
            </>
          )}

          <DataTablePagination
            className="mt-4"
            meta={planillas.data?.meta}
            page={page}
            isLoading={planillas.isFetching}
            onPageChange={setPage}
            onPageSizeChange={(nuevo) => {
              setPage(1)
              setPageSize(nuevo)
            }}
          />
        </section>
      )}
    </AdminLayout>
  )
}
