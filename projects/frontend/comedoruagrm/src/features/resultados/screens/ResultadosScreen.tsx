import { useMemo, useState } from 'react'
import type { SortingState } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { mapearErrorResultados, useAdjudicadosPublicos, useConvocatoriasPublicadas, useMiResultado } from '../api'
import { columnasAdjudicados, formatearFechaHora, formatearPuntaje, ResultadoBadge } from '../components'

const PAGE_SIZE_INICIAL = 20
const PAGE_SIZE_CONVOCATORIAS = 50
const RETARDO_BUSQUEDA_MS = 300

export interface ResultadosScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante). */
  usuarioActual: Usuario
}

function Alerta({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
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

/**
 * «Resultados» (CU22): lo que el postulante ve una vez publicados los resultados: su propio resultado y la lista
 * oficial de adjudicados (nombre y apellido, nota, carrera y facultad). Nunca se muestran los no adjudicados, el
 * CI, el correo ni el motivo de una decisión: lo que viene lo fija el backend.
 */
export function ResultadosScreen({ titulo, activeHref, usuarioActual }: ResultadosScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)

  const publicadas = useConvocatoriasPublicadas({ page: 1, pageSize: PAGE_SIZE_CONVOCATORIAS })
  const disponibles = publicadas.data?.data ?? []
  // La más reciente (la primera) viene preseleccionada.
  const convocatoriaId = elegida && disponibles.some((c) => c.codigo === elegida) ? elegida : disponibles[0]?.codigo
  const actual = disponibles.find((c) => c.codigo === convocatoriaId)

  const mio = useMiResultado(convocatoriaId)
  const adjudicados = useAdjudicadosPublicos(convocatoriaId, {
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
    data: adjudicados.data?.data ?? [],
    columns: columnasAdjudicados,
    meta: adjudicados.data?.meta,
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
    getRowId: (fila) => `${fila.nombres}|${fila.apellidos}|${fila.carrera}|${fila.nota ?? ''}`,
  })

  const errorPublicadas = publicadas.error ? mapearErrorResultados(publicadas.error) : null
  const errorMio = mio.error ? mapearErrorResultados(mio.error) : null
  const errorLista = adjudicados.error ? mapearErrorResultados(adjudicados.error) : null

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {errorPublicadas && (
        <div className="mb-4">
          <Alerta mensaje={errorPublicadas.mensaje} onReintentar={() => void publicadas.refetch()} />
        </div>
      )}
      {publicadas.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando resultados…</p>}
      {publicadas.isSuccess && disponibles.length === 0 && (
        <p className="py-8 text-center text-sm text-muted-foreground">Aún no hay resultados publicados</p>
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
                  setPage(1)
                  setBusqueda('')
                  setBusquedaAplicada('')
                  setSorting([])
                  setElegida(valor)
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
            {actual && (
              <p className="text-sm text-muted-foreground">Publicados el {formatearFechaHora(actual.publicadoEn)}</p>
            )}
          </div>

          <section aria-label="Mi resultado">
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-medium text-muted-foreground">Mi resultado</CardTitle>
              </CardHeader>
              <CardContent>
                {mio.isLoading && <p className="text-sm text-muted-foreground">Cargando tu resultado…</p>}
                {errorMio && <Alerta mensaje={errorMio.mensaje} onReintentar={() => void mio.refetch()} />}
                {mio.data?.resultado ? (
                  <div className="flex flex-wrap items-center gap-3">
                    <ResultadoBadge resultado={mio.data.resultado} />
                    {mio.data.resultado === 'ADJUDICADA' && (
                      <span className="text-sm">Nota: {formatearPuntaje(mio.data.nota)}</span>
                    )}
                  </div>
                ) : (
                  mio.data && <p className="text-sm">No participaste en esta etapa</p>
                )}
              </CardContent>
            </Card>
          </section>

          <div className="rounded-2xl border border-border bg-card p-4">
            <h2 className="mb-3 font-heading text-lg">Adjudicados</h2>
            {errorLista && <Alerta mensaje={errorLista.mensaje} onReintentar={() => void adjudicados.refetch()} />}

            {!errorLista && (
              <>
                <DataTableToolbar
                  searchValue={busqueda}
                  onSearchChange={(valor) => {
                    setBusqueda(valor)
                    aplicarBusqueda(valor)
                  }}
                  searchPlaceholder="Buscar por nombre, carrera o facultad..."
                />

                <div className="mt-4 hidden md:block">
                  <DataTable
                    table={table}
                    isLoading={adjudicados.isLoading}
                    // El error se muestra arriba con su alerta y «Reintentar»; acá solo queda la lista.
                    isError={false}
                    emptyMessage="No hay adjudicados para mostrar."
                  />
                </div>
                <div className="mt-4 md:hidden">
                  <DataCards
                    table={table}
                    isLoading={adjudicados.isLoading}
                    // El error se muestra arriba con su alerta y «Reintentar»; acá solo queda la lista.
                    isError={false}
                    emptyMessage="No hay adjudicados para mostrar."
                  />
                </div>

                <DataTablePagination
                  className="mt-4"
                  meta={adjudicados.data?.meta}
                  page={page}
                  isLoading={adjudicados.isFetching}
                  onPageChange={setPage}
                  onPageSizeChange={(nuevo) => {
                    setPage(1)
                    setPageSize(nuevo)
                  }}
                />
              </>
            )}
          </div>
        </div>
      )}
    </EstudianteLayout>
  )
}
