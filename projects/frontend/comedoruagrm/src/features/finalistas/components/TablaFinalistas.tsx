import { useEffect, useMemo, useState } from 'react'
import { ClipboardCheckIcon, RefreshCwIcon, TriangleAlertIcon } from 'lucide-react'
import type { Row } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  getErrorMessage,
  RowActionButton,
  useDataTable,
} from '@/components/ui/tanstack-table'
import { debounce } from '@/utils/debounce'

import { mapearErrorFinalistas, useFinalistas } from '../api'
import type { Finalista } from '../types'
import { crearColumnasFinalistas } from './columnasFinalistas'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300

interface TablaFinalistasProps {
  convocatoriaId: string
  /** Códigos de los finalistas marcados para notificar (de cualquier página). */
  seleccionados: ReadonlySet<string>
  onSeleccionChange: (seleccionados: Set<string>) => void
  /** Finalista elegido para registrar su entrega (se resalta la fila). */
  activoCodigo?: string
  onElegirEntrega: (finalista: Finalista) => void
  /** El backend dijo que la convocatoria ya no existe (A9): la pantalla vuelve al selector. */
  onConvocatoriaInexistente?: () => void
}

/**
 * Tabla de finalistas de una convocatoria (CU21, pasos 3 y 8): posición, finalista, puntaje y el estado de
 * su aviso y de su entrega, con búsqueda por nombre o CI, selección múltiple y paginación en el servidor.
 * Solo lectura: nada de lo que se hace acá cambia el estado de una postulación.
 */
export function TablaFinalistas({
  convocatoriaId,
  seleccionados,
  onSeleccionChange,
  activoCodigo,
  onElegirEntrega,
  onConvocatoriaInexistente,
}: TablaFinalistasProps) {
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)

  const lista = useFinalistas({ convocatoriaId, search: busquedaAplicada || undefined, page, pageSize })
  const filas = useMemo(() => lista.data?.data ?? [], [lista.data])

  // A9: una convocatoria eliminada responde 404 (ERR_FIN_02); se avisa a la pantalla para que vuelva al selector.
  const convocatoriaInexistente = lista.isError && mapearErrorFinalistas(lista.error).convocatoriaInexistente
  useEffect(() => {
    if (convocatoriaInexistente) onConvocatoriaInexistente?.()
  }, [convocatoriaInexistente, onConvocatoriaInexistente])

  // La búsqueda escribe al instante en el input, pero consulta al backend recién cuando se deja de teclear.
  const aplicarBusqueda = useMemo(
    () =>
      debounce((valor: string) => {
        setPage(1)
        setBusquedaAplicada(valor.trim())
      }, RETARDO_BUSQUEDA_MS),
    [],
  )

  const columnas = useMemo(
    () =>
      crearColumnasFinalistas({
        seleccionados,
        codigosPagina: filas.map((f) => f.codigo),
        onToggle: (codigo, marcado) => {
          const nuevo = new Set(seleccionados)
          if (marcado) nuevo.add(codigo)
          else nuevo.delete(codigo)
          onSeleccionChange(nuevo)
        },
        onTogglePagina: (marcado) => {
          const nuevo = new Set(seleccionados)
          for (const f of filas) {
            if (marcado) nuevo.add(f.codigo)
            else nuevo.delete(f.codigo)
          }
          onSeleccionChange(nuevo)
        },
      }),
    [seleccionados, filas, onSeleccionChange],
  )

  const { table } = useDataTable({
    data: filas,
    columns: columnas,
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

  const fallidosVisibles = filas.filter((f) => f.estadoNotificacion === 'FALLIDA')
  const sinFinalistas = lista.isSuccess && lista.data.meta.totalItems === 0 && !busquedaAplicada
  const sinDatos = lista.isError && !lista.data

  const renderAcciones = (fila: Row<Finalista>) => (
    <RowActionButton
      icon={<ClipboardCheckIcon />}
      label={activoCodigo === fila.original.codigo ? 'Entrega en edición' : 'Registrar entrega'}
      onClick={() => onElegirEntrega(fila.original)}
    />
  )

  if (sinFinalistas) {
    return (
      <p className="py-8 text-center text-sm text-muted-foreground">
        Todavía no hay finalistas en esta convocatoria.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <DataTableToolbar
        searchValue={busqueda}
        onSearchChange={(valor) => {
          setBusqueda(valor)
          aplicarBusqueda(valor)
        }}
        searchPlaceholder="Buscar por nombre o CI..."
        actions={
          <>
            <Button
              variant="outline"
              size="sm"
              disabled={fallidosVisibles.length === 0}
              onClick={() => onSeleccionChange(new Set(fallidosVisibles.map((f) => f.codigo)))}
            >
              <TriangleAlertIcon data-icon="inline-start" />
              Seleccionar fallidos
            </Button>
            <Button variant="outline" size="sm" onClick={() => void lista.refetch()} disabled={lista.isFetching}>
              <RefreshCwIcon data-icon="inline-start" />
              Refrescar
            </Button>
          </>
        }
      />

      <div className="hidden md:block">
        <DataTable
          table={table}
          isLoading={lista.isLoading}
          isError={sinDatos}
          errorMessage={getErrorMessage(lista.error)}
          emptyMessage="Sin resultados para esa búsqueda."
          renderActions={renderAcciones}
        />
      </div>
      <div className="md:hidden">
        <DataCards
          table={table}
          isLoading={lista.isLoading}
          isError={sinDatos}
          errorMessage={getErrorMessage(lista.error)}
          emptyMessage="Sin resultados para esa búsqueda."
          renderActions={renderAcciones}
        />
      </div>

      {busquedaAplicada && lista.isSuccess && filas.length === 0 && (
        <div className="flex justify-center">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setBusqueda('')
              setBusquedaAplicada('')
              setPage(1)
            }}
          >
            Limpiar búsqueda
          </Button>
        </div>
      )}

      <DataTablePagination
        meta={lista.data?.meta}
        page={page}
        isLoading={lista.isFetching}
        onPageChange={setPage}
        onPageSizeChange={(nuevo) => {
          setPage(1)
          setPageSize(nuevo)
        }}
      />
    </div>
  )
}
