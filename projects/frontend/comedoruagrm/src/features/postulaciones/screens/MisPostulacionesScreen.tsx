import { useState } from 'react'
import type { Row, SortingState } from '@tanstack/react-table'
import { useNavigate } from '@tanstack/react-router'
import { EyeIcon, RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  RowActionButton,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import type { DataTableToolbarFilter } from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'

import { mapearErrorPostulaciones, useMisPostulaciones } from '../api'
import { columnasPostulaciones, RUTA_POSTULAR } from '../components'
import type { MiPostulacionResumen } from '../types'

const PAGE_SIZE_INICIAL = 20
const SIN_POSTULACIONES = 'Todavía no enviaste ninguna postulación'
const SIN_RESULTADOS = 'No hay postulaciones en esa gestión'

export interface MisPostulacionesScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante). */
  usuarioActual: Usuario
}

/**
 * «Mis postulaciones» (CU04): la lista paginada de las postulaciones enviadas del postulante, de la más reciente a
 * la más antigua, con su gestión, su fecha de envío y su estado visible. El estado se muestra tal cual lo manda el
 * backend. El filtro por gestión ofrece las gestiones de las postulaciones que ya se cargaron (no hay un endpoint
 * de gestiones para el postulante).
 */
export function MisPostulacionesScreen({ titulo, activeHref, usuarioActual }: MisPostulacionesScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const navigate = useNavigate()

  const [gestion, setGestion] = useState<string | undefined>(undefined)
  const [sorting, setSorting] = useState<SortingState>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  // Las gestiones vistas hasta ahora (código -> etiqueta): al filtrar, la lista solo trae una, pero las demás
  // siguen disponibles para volver a elegirlas.
  const [gestiones, setGestiones] = useState<Map<string, string>>(new Map())

  const lista = useMisPostulaciones({
    page,
    pageSize,
    gestionAcademica: gestion,
    sort: toSortQueryParam(sorting),
  })

  // Se anotan las gestiones nuevas al renderizar (patrón de React para ajustar el estado a una consulta).
  const nuevas = (lista.data?.data ?? []).filter((p) => !gestiones.has(p.gestionAcademica.codigo))
  if (nuevas.length > 0) {
    setGestiones((previas) => {
      const siguientes = new Map(previas)
      for (const p of nuevas) siguientes.set(p.gestionAcademica.codigo, p.gestionAcademica.nombre)
      return siguientes
    })
  }

  const { table } = useDataTable({
    data: lista.data?.data ?? [],
    columns: columnasPostulaciones,
    meta: lista.data?.meta,
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

  const filtros: DataTableToolbarFilter[] = [
    {
      id: 'gestionAcademica',
      label: 'Gestión',
      value: gestion,
      onValueChange: (valor) => {
        setPage(1)
        setGestion(valor)
      },
      options: [...gestiones.entries()].map(([value, label]) => ({ value, label })),
    },
  ]

  const verDetalle = (postulacion: MiPostulacionResumen) =>
    void navigate({ to: '/mis-postulaciones/$codigo', params: { codigo: postulacion.codigo } })

  const renderAcciones = (fila: Row<MiPostulacionResumen>) => (
    <RowActionButton icon={<EyeIcon />} label="Ver detalle" onClick={() => verDetalle(fila.original)} />
  )

  const error = lista.error ? mapearErrorPostulaciones(lista.error) : null
  const sinDatos = lista.isError && !lista.data
  const sinNinguna = lista.isSuccess && lista.data.meta.totalItems === 0 && !gestion

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {error && sinDatos && (
        <div
          role="alert"
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{error.mensaje}</span>
          <Button variant="outline" size="sm" onClick={() => void lista.refetch()}>
            Reintentar
          </Button>
        </div>
      )}

      {lista.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando postulaciones…</p>}

      {sinNinguna && (
        <div className="flex flex-col items-center gap-3 py-8 text-center">
          <p className="text-sm text-muted-foreground">{SIN_POSTULACIONES}</p>
          {RUTA_POSTULAR && (
            <a href={RUTA_POSTULAR} className="text-sm font-medium text-primary underline-offset-4 hover:underline">
              Postular
            </a>
          )}
        </div>
      )}

      {!sinDatos && !sinNinguna && !lista.isLoading && (
        <section aria-label="Mis postulaciones" className="rounded-2xl border border-border bg-card p-4">
          <DataTableToolbar
            filters={filtros}
            actions={
              <Button variant="outline" size="sm" onClick={() => void lista.refetch()} disabled={lista.isFetching}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
            }
          />

          <div className="mt-4 hidden md:block">
            <DataTable
              table={table}
              isLoading={lista.isLoading}
              isError={false}
              emptyMessage={SIN_RESULTADOS}
              renderActions={renderAcciones}
            />
          </div>
          <div className="mt-4 md:hidden">
            <DataCards
              table={table}
              isLoading={lista.isLoading}
              isError={false}
              emptyMessage={SIN_RESULTADOS}
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
        </section>
      )}
    </EstudianteLayout>
  )
}
