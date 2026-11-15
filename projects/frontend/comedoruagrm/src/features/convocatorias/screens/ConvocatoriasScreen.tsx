import { useMemo, useState } from 'react'
import type { Row, SortingState } from '@tanstack/react-table'
import { EyeIcon, LockIcon, PencilIcon, PlayIcon, PlusIcon, RefreshCwIcon, Trash2Icon } from 'lucide-react'

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
import type { DataTableToolbarFilter } from '@/components/ui/tanstack-table'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { useConvocatorias, useGestiones } from '../api'
import {
  CambiarEstadoConvocatoriaDialog,
  columnasConvocatorias,
  ConvocatoriaFormModal,
  EliminarConvocatoriaDialog,
  etiquetaEstado,
} from '../components'
import type { ModoConvocatoria } from '../components'
import type { Convocatoria, EstadoConvocatoria } from '../types'

const PAGE_SIZE_INICIAL = 20
const PAGE_SIZE_GESTIONES = 100
const RETARDO_BUSQUEDA_MS = 300
const SIN_CONVOCATORIAS = 'Todavía no hay convocatorias'

const ESTADOS: EstadoConvocatoria[] = ['BORRADOR', 'ABIERTA', 'CERRADA', 'PUBLICADA']
const OPCIONES_ESTADO = ESTADOS.map((estado) => ({ label: etiquetaEstado(estado), value: estado }))

/** La acción en curso sobre una fila (o el alta): abre el modal que corresponde. */
type AccionFila =
  | { tipo: 'formulario'; modo: ModoConvocatoria; convocatoria?: Convocatoria }
  | { tipo: 'abrir'; convocatoria: Convocatoria }
  | { tipo: 'cerrar'; convocatoria: Convocatoria }
  | { tipo: 'eliminar'; convocatoria: Convocatoria }

export interface ConvocatoriasScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Convocatorias» (CU13): la lista de convocatorias de la Beca Comedor con su gestión, fechas, cupos y estado, con
 * búsqueda por nombre, filtros por estado y gestión, orden y paginación del backend. Desde acá se crea, edita, abre,
 * cierra y elimina. Las acciones de cada fila dependen del estado (solo es ayuda de interfaz: el backend valida).
 */
export function ConvocatoriasScreen({ titulo, activeHref, usuarioActual }: ConvocatoriasScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [estado, setEstado] = useState<EstadoConvocatoria | undefined>(undefined)
  const [gestion, setGestion] = useState<string | undefined>(undefined)
  const [sorting, setSorting] = useState<SortingState>([])
  const [accion, setAccion] = useState<AccionFila | null>(null)

  // La búsqueda escribe al instante en el input, pero consulta al backend recién cuando se deja de teclear.
  const aplicarBusqueda = useMemo(
    () =>
      debounce((valor: string) => {
        setPage(1)
        setBusquedaAplicada(valor.trim())
      }, RETARDO_BUSQUEDA_MS),
    [],
  )

  const consulta = useConvocatorias({
    page,
    pageSize,
    search: busquedaAplicada || undefined,
    estado,
    gestion,
    sort: toSortQueryParam(sorting),
  })
  const gestiones = useGestiones({ page: 1, pageSize: PAGE_SIZE_GESTIONES, sort: '-fechaInicio' })

  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnasConvocatorias,
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

  const filtros: DataTableToolbarFilter[] = [
    {
      id: 'estado',
      label: 'Estado',
      value: estado,
      onValueChange: (valor) => {
        setPage(1)
        setEstado(valor as EstadoConvocatoria | undefined)
      },
      options: OPCIONES_ESTADO,
    },
    {
      id: 'gestion',
      label: 'Gestión',
      value: gestion,
      onValueChange: (valor) => {
        setPage(1)
        setGestion(valor)
      },
      options: (gestiones.data?.data ?? []).map((g) => ({ label: g.etiqueta, value: g.codigo })),
    },
  ]

  // BORRADOR: ver, editar, abrir y eliminar. ABIERTA: ver, editar y cerrar. CERRADA y PUBLICADA: solo ver.
  const renderAcciones = (fila: Row<Convocatoria>) => {
    const convocatoria = fila.original
    const editable = convocatoria.estado === 'BORRADOR' || convocatoria.estado === 'ABIERTA'
    return (
      <>
        <RowActionButton
          icon={<EyeIcon />}
          label="Ver detalle"
          onClick={() => setAccion({ tipo: 'formulario', modo: 'ver', convocatoria })}
        />
        {editable && (
          <RowActionButton
            icon={<PencilIcon />}
            label="Editar"
            onClick={() => setAccion({ tipo: 'formulario', modo: 'editar', convocatoria })}
          />
        )}
        {convocatoria.estado === 'BORRADOR' && (
          <RowActionButton icon={<PlayIcon />} label="Abrir" onClick={() => setAccion({ tipo: 'abrir', convocatoria })} />
        )}
        {convocatoria.estado === 'ABIERTA' && (
          <RowActionButton
            icon={<LockIcon />}
            label="Cerrar"
            variant="destructive"
            onClick={() => setAccion({ tipo: 'cerrar', convocatoria })}
          />
        )}
        {convocatoria.estado === 'BORRADOR' && (
          <RowActionButton
            icon={<Trash2Icon />}
            label="Eliminar"
            variant="destructive"
            onClick={() => setAccion({ tipo: 'eliminar', convocatoria })}
          />
        )}
      </>
    )
  }

  const mensajeError = getErrorMessage(consulta.error)
  const sinDatos = consulta.isError && !consulta.data
  const cerrarAccion = (abierto: boolean) => {
    if (!abierto) setAccion(null)
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <section aria-label="Convocatorias" className="rounded-2xl border border-border bg-card p-4">
        <DataTableToolbar
          searchValue={busqueda}
          onSearchChange={(valor) => {
            setBusqueda(valor)
            aplicarBusqueda(valor)
          }}
          searchPlaceholder="Buscar por nombre..."
          filters={filtros}
          actions={
            <>
              <Button variant="outline" size="sm" onClick={() => void consulta.refetch()} disabled={consulta.isFetching}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
              <Button size="sm" onClick={() => setAccion({ tipo: 'formulario', modo: 'crear' })}>
                <PlusIcon data-icon="inline-start" />
                Nueva convocatoria
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
                emptyMessage={SIN_CONVOCATORIAS}
                renderActions={renderAcciones}
              />
            </div>
            <div className="mt-4 md:hidden">
              <DataCards
                table={table}
                isLoading={consulta.isLoading}
                isError={false}
                emptyMessage={SIN_CONVOCATORIAS}
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

      {accion?.tipo === 'formulario' && (
        <ConvocatoriaFormModal
          open
          onOpenChange={cerrarAccion}
          modo={accion.modo}
          codigo={accion.convocatoria?.codigo}
        />
      )}
      {accion?.tipo === 'abrir' && (
        <CambiarEstadoConvocatoriaDialog open onOpenChange={cerrarAccion} convocatoria={accion.convocatoria} accion="abrir" />
      )}
      {accion?.tipo === 'cerrar' && (
        <CambiarEstadoConvocatoriaDialog open onOpenChange={cerrarAccion} convocatoria={accion.convocatoria} accion="cerrar" />
      )}
      {accion?.tipo === 'eliminar' && (
        <EliminarConvocatoriaDialog open onOpenChange={cerrarAccion} convocatoria={accion.convocatoria} />
      )}
    </AdminLayout>
  )
}
