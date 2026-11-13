import { useMemo, useState } from 'react'
import type { SortingState } from '@tanstack/react-table'
import { RefreshCwIcon, TrophyIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  getErrorMessage,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import type { DataTableToolbarFilter } from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { useConvocatoriasPriorizables } from '@/features/priorizacion'
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { mapearErrorResultados, useDecisionesResultado, useResumenPublicacion } from '../api'
import { columnasDecisiones, formatearFechaHora, motivoDeBloqueo, PublicarResultadosDialog } from '../components'
import type { ResultadoDecision } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300

const OPCIONES_RESULTADO = [
  { label: 'Adjudicada', value: 'ADJUDICADA' },
  { label: 'No adjudicada', value: 'NO_ADJUDICADA' },
]

export interface PublicarResultadosScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

function Tarjeta({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">{titulo}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="font-heading text-2xl">{texto}</p>
      </CardContent>
    </Card>
  )
}

/**
 * «Publicar resultados» (CU22): el Administrador elige una convocatoria, revisa cuántos adjudicados y no
 * adjudicados hay y quién recibirá aviso, recorre las decisiones y publica. El botón queda deshabilitado con su
 * motivo visible mientras no se pueda publicar, y desaparece una vez publicados los resultados.
 */
export function PublicarResultadosScreen({ titulo, activeHref, usuarioActual }: PublicarResultadosScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [resultado, setResultado] = useState<ResultadoDecision | undefined>(undefined)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  // Convocatorias que el backend dijo que ya no existen: se quitan del selector hasta que la lista se actualice.
  const [inexistentes, setInexistentes] = useState<Set<string>>(new Set())
  const [aviso, setAviso] = useState<string | null>(null)
  const [publicando, setPublicando] = useState(false)

  const convocatorias = useConvocatoriasPriorizables()
  const disponibles = (convocatorias.data ?? []).filter((c) => !inexistentes.has(c.codigo))
  const convocatoriaId = elegida && disponibles.some((c) => c.codigo === elegida) ? elegida : disponibles[0]?.codigo

  const resumenConsulta = useResumenPublicacion(convocatoriaId)
  const resumen = resumenConsulta.data
  const decisiones = useDecisionesResultado(convocatoriaId, {
    page,
    pageSize,
    resultado,
    search: busquedaAplicada || undefined,
    sort: toSortQueryParam(sorting),
  })

  const errorConvocatorias = convocatorias.error ? mapearErrorResultados(convocatorias.error) : null
  const errorResumen = resumenConsulta.error ? mapearErrorResultados(resumenConsulta.error) : null

  // El `404` de la convocatoria: se la quita del selector y se avisa (una sola vez por convocatoria). Es el patrón
  // de React para ajustar el estado al renderizar a partir de una consulta.
  if (errorResumen?.convocatoriaInexistente && convocatoriaId && !inexistentes.has(convocatoriaId)) {
    setInexistentes((previas) => new Set(previas).add(convocatoriaId))
    setElegida(undefined)
    setAviso('La convocatoria ya no existe. Elige otra.')
  }

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
    data: decisiones.data?.data ?? [],
    columns: columnasDecisiones,
    meta: decisiones.data?.meta,
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
    getRowId: (fila) => fila.finalista,
  })

  const filtrosToolbar: DataTableToolbarFilter[] = [
    {
      id: 'resultado',
      label: 'Resultado',
      value: resultado,
      onValueChange: (valor) => {
        setPage(1)
        setResultado(valor as ResultadoDecision | undefined)
      },
      options: OPCIONES_RESULTADO,
    },
  ]

  const convocatoriaInexistente = () => {
    if (!convocatoriaId) return
    setInexistentes((previas) => new Set(previas).add(convocatoriaId))
    setElegida(undefined)
    setAviso('La convocatoria ya no existe. Elige otra.')
    void convocatorias.refetch()
  }

  const refrescar = () => {
    void convocatorias.refetch()
    void resumenConsulta.refetch()
    void decisiones.refetch()
  }

  const bloqueo = resumen ? motivoDeBloqueo(resumen) : 'Cargando el resumen…'
  const mensajeError = getErrorMessage(decisiones.error)
  const sinDatos = decisiones.isError && !decisiones.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {errorConvocatorias && (
        <div
          role="alert"
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{errorConvocatorias.mensaje}</span>
          <Button variant="outline" size="sm" onClick={() => void convocatorias.refetch()}>
            Reintentar
          </Button>
        </div>
      )}

      {aviso && (
        <div role="alert" className="mb-4 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
          {aviso}
        </div>
      )}

      {convocatorias.isLoading && (
        <p className="py-8 text-center text-sm text-muted-foreground">Cargando convocatorias…</p>
      )}
      {convocatorias.isSuccess && disponibles.length === 0 && (
        <p className="py-8 text-center text-sm text-muted-foreground">Todavía no hay convocatorias registradas.</p>
      )}

      {convocatoriaId && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
              <Select
                value={convocatoriaId}
                onValueChange={(valor) => {
                  if (!valor) return
                  setAviso(null)
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

            <div className="flex flex-wrap items-center gap-2">
              {resumen?.yaPublicada ? (
                <p role="status" className="text-sm font-medium text-success">
                  Resultados publicados el {resumen.publicadoEn ? formatearFechaHora(resumen.publicadoEn) : '—'}
                  {resumen.publicadoPor ? ` por ${resumen.publicadoPor}` : ''}
                </p>
              ) : (
                <Button size="sm" disabled={bloqueo !== null} onClick={() => setPublicando(true)}>
                  <TrophyIcon data-icon="inline-start" />
                  Publicar resultados
                </Button>
              )}
            </div>
          </div>

          {resumen && !resumen.yaPublicada && bloqueo && (
            <p role="note" className="text-right text-xs text-muted-foreground">
              {bloqueo}
            </p>
          )}

          {resumenConsulta.isLoading && (
            <p className="py-8 text-center text-sm text-muted-foreground">Cargando resumen…</p>
          )}

          {errorResumen && !errorResumen.convocatoriaInexistente && (
            <div
              role="alert"
              className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              <span>{errorResumen.mensaje}</span>
              <Button variant="outline" size="sm" onClick={() => void resumenConsulta.refetch()}>
                Reintentar
              </Button>
            </div>
          )}

          {resumen && (
            <section aria-label="Resumen de la publicación" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <Tarjeta titulo="Adjudicados" texto={String(resumen.adjudicados)} />
              <Tarjeta titulo="No adjudicados" texto={String(resumen.noAdjudicados)} />
              <Tarjeta titulo="Decisiones pendientes" texto={String(resumen.calificadasPendientes)} />
              <Tarjeta titulo="Finalistas a avisar" texto={String(resumen.aAvisar)} />
            </section>
          )}

          <div className="rounded-2xl border border-border bg-card p-4">
            <DataTableToolbar
              searchValue={busqueda}
              onSearchChange={(valor) => {
                setBusqueda(valor)
                aplicarBusqueda(valor)
              }}
              searchPlaceholder="Buscar por nombre o apellido..."
              filters={filtrosToolbar}
              actions={
                <Button variant="outline" size="sm" onClick={refrescar} disabled={decisiones.isFetching}>
                  <RefreshCwIcon data-icon="inline-start" />
                  Refrescar
                </Button>
              }
            />

            <div className="mt-4 hidden md:block">
              <DataTable
                table={table}
                isLoading={decisiones.isLoading}
                isError={sinDatos}
                errorMessage={mensajeError}
                emptyMessage="Todavía no hay decisiones para mostrar."
              />
            </div>
            <div className="mt-4 md:hidden">
              <DataCards
                table={table}
                isLoading={decisiones.isLoading}
                isError={sinDatos}
                errorMessage={mensajeError}
                emptyMessage="Todavía no hay decisiones para mostrar."
              />
            </div>

            <DataTablePagination
              className="mt-4"
              meta={decisiones.data?.meta}
              page={page}
              isLoading={decisiones.isFetching}
              onPageChange={setPage}
              onPageSizeChange={(nuevo) => {
                setPage(1)
                setPageSize(nuevo)
              }}
            />
          </div>
        </div>
      )}

      {/* Se monta solo mientras está abierta: al cerrar se descarta. */}
      {publicando && resumen && (
        <PublicarResultadosDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setPublicando(false)
          }}
          resumen={resumen}
          onDesactualizado={() => void resumenConsulta.refetch()}
          onConvocatoriaInexistente={convocatoriaInexistente}
        />
      )}
    </AdminLayout>
  )
}
