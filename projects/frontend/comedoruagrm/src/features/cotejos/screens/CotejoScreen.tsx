import { useRef, useState } from 'react'
import { useForm, useStore } from '@tanstack/react-form'
import type { SortingState } from '@tanstack/react-table'
import { RefreshCwIcon, SaveIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { toastSuccess } from '@/components/ui/toast'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useConvocatorias } from '@/features/convocatorias'
import { useAccionesDeCuenta } from '@/features/perfil'
import { usePlanillas } from '@/features/planillas'
import { AdminLayout } from '@/layouts'

import { mapearErrorCotejos, useCotejos, useDatosCotejo, useRegistrarCotejo, useRegistrosDigitales } from '../api'
import type { ErrorCotejos } from '../api'
import { columnasCotejos, columnasRegistros, etiquetaResultado, formatearPeriodo } from '../components'
import { calcularCotejo, cotejoSchema, crearSolicitudCotejo, MAX_OBSERVACIONES } from '../schemas'
import { errorVisible } from '@/utils/errorCampo'
import { validacionFormulario } from '@/utils/validacionFormulario'

const PAGE_SIZE_INICIAL = 20
const SIN_REGISTROS = 'No hay registros digitales de asistencia en el periodo de esta planilla'
const SIN_COTEJOS = 'Esta planilla todavía no fue cotejada'
// El selector muestra todas las convocatorias con resultados publicados de una vez: solo ellas tienen planillas.
const CONVOCATORIAS_PUBLICADAS = { page: 1, pageSize: 50, estado: 'PUBLICADA' as const, sort: '-fechaApertura' }
// Las planillas de una convocatoria, la más reciente primero, de una vez para el selector.
const PLANILLAS_DEL_SELECTOR = { page: 1, pageSize: 50, sort: '-generadaEn' }

export interface CotejoScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

function Alerta({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div
      role="alert"
      className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
    >
      <span>{mensaje}</span>
      {onReintentar && (
        <Button variant="outline" size="sm" onClick={onReintentar}>
          Reintentar
        </Button>
      )}
    </div>
  )
}

function Dato({ valor, etiqueta }: { valor: number; etiqueta: string }) {
  return (
    <div className="rounded-xl border border-border bg-background p-3">
      <p className="font-heading text-2xl">{valor}</p>
      <p className="text-xs text-muted-foreground">{etiqueta}</p>
    </div>
  )
}

/**
 * «Cotejar asistencia contra respaldo físico» (CU11): el Administrador elige una planilla ya generada (CU09), ve
 * cuántos registros digitales hay en su periodo y la lista de esos registros, los compara a mano con la planilla firmada
 * y registra lo que constató: total de registros, coincidencias, discrepancias, resultado y observaciones. Abajo están
 * los cotejos ya registrados de la planilla.
 *
 * * No hay lectura automática de la planilla firmada (todavía): el total y las coincidencias los declara el
 * * Administrador; las discrepancias y el resultado se deducen de ellos (`calcularCotejo`) y con discrepancias las
 * * observaciones son obligatorias (A2). El servidor vuelve a validar y su error se muestra tal cual.
 * * Se pueden registrar varios cotejos de la misma planilla (A3): el formulario se limpia al guardar y el historial se
 * * refresca. Una planilla sin registros digitales no impide cotejar: se informa (A1).
 */
export function CotejoScreen({ titulo, activeHref, usuarioActual }: CotejoScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const convocatorias = useConvocatorias(CONVOCATORIAS_PUBLICADAS)
  const registrar = useRegistrarCotejo()

  const [convocatoriaElegida, setConvocatoriaElegida] = useState<string | undefined>(undefined)
  const [planillaElegida, setPlanillaElegida] = useState<string | undefined>(undefined)
  const [sortingRegistros, setSortingRegistros] = useState<SortingState>([])
  const [pageRegistros, setPageRegistros] = useState(1)
  const [pageSizeRegistros, setPageSizeRegistros] = useState(PAGE_SIZE_INICIAL)
  const [sortingCotejos, setSortingCotejos] = useState<SortingState>([])
  const [pageCotejos, setPageCotejos] = useState(1)
  const [pageSizeCotejos, setPageSizeCotejos] = useState(PAGE_SIZE_INICIAL)
  const [error, setError] = useState<ErrorCotejos | null>(null)
  // Evita un segundo envío antes de que `isPending` llegue a pintarse.
  const enviando = useRef(false)

  const disponibles = convocatorias.data?.data ?? []
  const convocatoria = disponibles.find((c) => c.codigo === convocatoriaElegida) ?? disponibles[0]
  const planillas = usePlanillas(convocatoria?.codigo, PLANILLAS_DEL_SELECTOR)
  const delSelector = planillas.data?.data ?? []
  const planilla = delSelector.find((p) => p.codigo === planillaElegida) ?? delSelector[0]
  const codigo = planilla?.codigo

  const datos = useDatosCotejo(codigo)
  const registros = useRegistrosDigitales(codigo, {
    page: pageRegistros,
    pageSize: pageSizeRegistros,
    sort: toSortQueryParam(sortingRegistros),
  })
  const cotejos = useCotejos(codigo, {
    page: pageCotejos,
    pageSize: pageSizeCotejos,
    sort: toSortQueryParam(sortingCotejos),
  })

  const form = useForm({
    defaultValues: { total: '', coincidencias: '', observaciones: '' },
    ...validacionFormulario(cotejoSchema),
    onSubmit: ({ value }) => {
      if (!codigo || enviando.current) return
      enviando.current = true
      setError(null)
      const solicitud = crearSolicitudCotejo(value)
      registrar.mutate(
        { planilla: codigo, cotejo: solicitud },
        {
          onSuccess: (cotejo) => {
            toastSuccess('Cotejo registrado', etiquetaResultado(cotejo.resultado))
            form.reset()
            setPageCotejos(1)
          },
          onError: (e) => {
            const mapeado = mapearErrorCotejos(e)
            if (mapeado.planillaInexistente) {
              setPlanillaElegida(undefined)
              void planillas.refetch()
            }
            setError(mapeado)
          },
          onSettled: () => {
            enviando.current = false
          },
        },
      )
    },
  })

  const valores = useStore(form.store, (estado) => estado.values)
  const calculado = calcularCotejo(valores)

  const { table: tablaRegistros } = useDataTable({
    data: registros.data?.data ?? [],
    columns: columnasRegistros,
    meta: registros.data?.meta,
    page: pageRegistros,
    pageSize: pageSizeRegistros,
    onPageChange: setPageRegistros,
    onPageSizeChange: (nuevo) => {
      setPageRegistros(1)
      setPageSizeRegistros(nuevo)
    },
    enableSorting: true,
    sorting: sortingRegistros,
    onSortingChange: (nuevo) => {
      setPageRegistros(1)
      setSortingRegistros(nuevo)
    },
  })

  const { table: tablaCotejos } = useDataTable({
    data: cotejos.data?.data ?? [],
    columns: columnasCotejos,
    meta: cotejos.data?.meta,
    page: pageCotejos,
    pageSize: pageSizeCotejos,
    onPageChange: setPageCotejos,
    onPageSizeChange: (nuevo) => {
      setPageCotejos(1)
      setPageSizeCotejos(nuevo)
    },
    enableSorting: true,
    sorting: sortingCotejos,
    onSortingChange: (nuevo) => {
      setPageCotejos(1)
      setSortingCotejos(nuevo)
    },
    getRowId: (fila) => fila.codigo,
  })

  const errorConvocatorias = convocatorias.isError && !convocatorias.data ? mapearErrorCotejos(convocatorias.error) : null
  const errorPlanillas = planillas.isError && !planillas.data ? mapearErrorCotejos(planillas.error) : null
  const errorDatos = datos.isError && !datos.data ? mapearErrorCotejos(datos.error) : null
  const errorRegistros = registros.isError && !registros.data ? mapearErrorCotejos(registros.error) : null
  const errorCotejos = cotejos.isError && !cotejos.data ? mapearErrorCotejos(cotejos.error) : null
  const sinPublicadas = convocatorias.isSuccess && disponibles.length === 0
  const sinPlanillas = Boolean(convocatoria) && planillas.isSuccess && delSelector.length === 0
  const arriba = errorConvocatorias ?? errorPlanillas ?? errorDatos

  const reintentar = () => {
    void convocatorias.refetch()
    void planillas.refetch()
    void datos.refetch()
    void registros.refetch()
    void cotejos.refetch()
  }

  const limpiarAlCambiar = () => {
    setError(null)
    setPageRegistros(1)
    setPageCotejos(1)
    form.reset()
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Planilla a cotejar" className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-lg">Cotejar asistencia contra respaldo físico</h2>
        <p className="mb-3 text-sm text-muted-foreground">
          Compara los registros digitales con la planilla firmada y registra el resultado
        </p>

        {convocatorias.isLoading && <p className="text-sm text-muted-foreground">Cargando convocatorias…</p>}
        {sinPublicadas && (
          <p role="status" className="text-sm text-muted-foreground">
            Todavía no hay convocatorias con resultados publicados
          </p>
        )}
        {sinPlanillas && (
          <p role="status" className="text-sm text-muted-foreground">
            Esta convocatoria todavía no tiene planillas generadas. Genera una en «Planillas».
          </p>
        )}
        {arriba && <Alerta mensaje={arriba.mensaje} onReintentar={reintentar} />}

        {convocatoria && (
          <div className="flex flex-wrap items-start gap-3">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
              <Select
                value={convocatoria.codigo}
                onValueChange={(valor) => {
                  if (!valor) return
                  setConvocatoriaElegida(valor)
                  setPlanillaElegida(undefined)
                  limpiarAlCambiar()
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

            {planilla && (
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Planilla</span>
                <Select
                  value={planilla.codigo}
                  onValueChange={(valor) => {
                    if (!valor) return
                    setPlanillaElegida(valor)
                    limpiarAlCambiar()
                  }}
                >
                  <SelectTrigger size="sm" aria-label="Planilla" className="min-w-72">
                    <SelectValue>
                      {(valor: string | null) => {
                        const p = delSelector.find((x) => x.codigo === valor)
                        return p ? `${formatearPeriodo(p.fechaDesde, p.fechaHasta)} · ${p.totalBecados} becados` : ''
                      }}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {delSelector.map((p) => (
                      <SelectItem key={p.codigo} value={p.codigo}>
                        {formatearPeriodo(p.fechaDesde, p.fechaHasta)} · {p.totalBecados} becados
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>
        )}

        {datos.data && (
          <>
            <p className="mt-3 text-sm text-muted-foreground">
              Planilla del periodo {formatearPeriodo(datos.data.fechaDesde, datos.data.fechaHasta)}
            </p>
            <div className="mt-2 grid gap-3 sm:grid-cols-3" role="group" aria-label="Resumen de la planilla">
              <Dato valor={datos.data.totalBecados} etiqueta="Becados en la planilla" />
              <Dato valor={datos.data.registrosDigitales} etiqueta="Registros digitales en el periodo" />
              <Dato valor={datos.data.becadosConAsistencia} etiqueta="Becados con asistencia digital" />
            </div>
            {datos.data.registrosDigitales === 0 && (
              <p
                role="status"
                className="mt-3 rounded-md border border-border bg-muted p-3 text-sm text-muted-foreground"
              >
                No hay registros digitales de asistencia en el periodo de esta planilla. Igual puedes registrar el
                cotejo.
              </p>
            )}
          </>
        )}
      </section>

      {codigo && (
        <section aria-label="Registros digitales" className="mt-4 rounded-2xl border border-border bg-card p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-heading text-lg">Registros digitales del periodo</h2>
            <Button variant="outline" size="sm" onClick={() => void registros.refetch()} disabled={registros.isFetching}>
              <RefreshCwIcon data-icon="inline-start" />
              Refrescar
            </Button>
          </div>
          {errorRegistros ? (
            <Alerta mensaje={errorRegistros.mensaje} onReintentar={() => void registros.refetch()} />
          ) : (
            <>
              <div className="hidden md:block">
                <DataTable table={tablaRegistros} isLoading={registros.isLoading} isError={false} emptyMessage={SIN_REGISTROS} />
              </div>
              <div className="md:hidden">
                <DataCards table={tablaRegistros} isLoading={registros.isLoading} isError={false} emptyMessage={SIN_REGISTROS} />
              </div>
            </>
          )}
          <DataTablePagination
            className="mt-4"
            meta={registros.data?.meta}
            page={pageRegistros}
            isLoading={registros.isFetching}
            onPageChange={setPageRegistros}
            onPageSizeChange={(nuevo) => {
              setPageRegistros(1)
              setPageSizeRegistros(nuevo)
            }}
          />
        </section>
      )}

      {codigo && (
        <section aria-label="Registrar cotejo" className="mt-4 rounded-2xl border border-border bg-card p-4">
          <h2 className="font-heading text-lg">Registrar cotejo</h2>
          <p className="mb-3 text-sm text-muted-foreground">
            Declara lo que constataste al comparar con la planilla firmada
          </p>
          <form
            noValidate
            aria-label="Cotejo"
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              void form.handleSubmit()
            }}
          >
            <div className="flex flex-wrap items-start gap-3">
              <form.Field name="total">
                {(field) => (
                  <div className="flex flex-col gap-1">
                    <label htmlFor="cotejo-total" className="text-xs font-medium text-muted-foreground">
                      Total de registros
                    </label>
                    <Input
                      id="cotejo-total"
                      type="number"
                      inputMode="numeric"
                      min={0}
                      value={field.state.value}
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

              <form.Field name="coincidencias">
                {(field) => (
                  <div className="flex flex-col gap-1">
                    <label htmlFor="cotejo-coincidencias" className="text-xs font-medium text-muted-foreground">
                      Coincidencias
                    </label>
                    <Input
                      id="cotejo-coincidencias"
                      type="number"
                      inputMode="numeric"
                      min={0}
                      value={field.state.value}
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

              <div className="flex flex-col gap-1" aria-live="polite">
                <span className="text-xs font-medium text-muted-foreground">Discrepancias</span>
                <span className="flex h-9 items-center text-sm" data-testid="discrepancias">
                  {calculado ? calculado.discrepancias : '—'}
                </span>
              </div>

              <div className="flex flex-col gap-1" aria-live="polite">
                <span className="text-xs font-medium text-muted-foreground">Resultado</span>
                <span className="flex h-9 items-center text-sm" data-testid="resultado">
                  {calculado ? etiquetaResultado(calculado.resultado) : '—'}
                </span>
              </div>
            </div>

            <form.Field name="observaciones">
              {(field) => (
                <div className="mt-3 flex flex-col gap-1">
                  <label htmlFor="cotejo-observaciones" className="text-xs font-medium text-muted-foreground">
                    Observaciones{calculado && calculado.discrepancias > 0 ? ' (obligatorias)' : ''}
                  </label>
                  <Textarea
                    id="cotejo-observaciones"
                    rows={3}
                    maxLength={MAX_OBSERVACIONES}
                    placeholder="Observaciones…"
                    value={field.state.value}
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

            {error && (
              <Alerta mensaje={error.mensaje} onReintentar={error.red ? () => void form.handleSubmit() : undefined} />
            )}

            <div className="mt-3">
              <Button type="submit" size="sm" disabled={registrar.isPending}>
                <SaveIcon data-icon="inline-start" />
                {registrar.isPending ? 'Guardando…' : 'Guardar cotejo'}
              </Button>
            </div>
          </form>
        </section>
      )}

      {codigo && (
        <section aria-label="Cotejos registrados" className="mt-4 rounded-2xl border border-border bg-card p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-heading text-lg">Cotejos registrados</h2>
            <Button variant="outline" size="sm" onClick={() => void cotejos.refetch()} disabled={cotejos.isFetching}>
              <RefreshCwIcon data-icon="inline-start" />
              Refrescar
            </Button>
          </div>
          {errorCotejos ? (
            <Alerta mensaje={errorCotejos.mensaje} onReintentar={() => void cotejos.refetch()} />
          ) : (
            <>
              <div className="hidden md:block">
                <DataTable table={tablaCotejos} isLoading={cotejos.isLoading} isError={false} emptyMessage={SIN_COTEJOS} />
              </div>
              <div className="md:hidden">
                <DataCards table={tablaCotejos} isLoading={cotejos.isLoading} isError={false} emptyMessage={SIN_COTEJOS} />
              </div>
            </>
          )}
          <DataTablePagination
            className="mt-4"
            meta={cotejos.data?.meta}
            page={pageCotejos}
            isLoading={cotejos.isFetching}
            onPageChange={setPageCotejos}
            onPageSizeChange={(nuevo) => {
              setPageCotejos(1)
              setPageSizeCotejos(nuevo)
            }}
          />
        </section>
      )}
    </AdminLayout>
  )
}
