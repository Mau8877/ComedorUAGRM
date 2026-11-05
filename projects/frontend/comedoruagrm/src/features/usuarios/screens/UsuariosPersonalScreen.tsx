import { useMemo, useState } from 'react'
import { ArrowRightLeftIcon, PencilIcon, PlusIcon, RefreshCwIcon, UserCheckIcon, UserXIcon } from 'lucide-react'
import type { ColumnDef, Row, SortingState } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import { toastInfo } from '@/components/ui/toast'
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
import { AdminLayout } from '@/layouts'
import { debounce } from '@/utils/debounce'

import { useUsuarios } from '../api'
import {
  CambiarEstadoUsuarioDialog,
  CambiarRolModal,
  CrearUsuarioModal,
  EditarUsuarioModal,
  EstadoCuentaBadge,
} from '../components'
import type { RolPersonal } from '../roles'
import type { UsuarioCreado, UsuarioPersonal } from '../types'

const PAGE_SIZE_INICIAL = 20
const RETARDO_BUSQUEDA_MS = 300

const OPCIONES_ESTADO = [
  { label: 'Activas', value: 'true' },
  { label: 'Desactivadas', value: 'false' },
]

// * Una sola definición de columnas alimenta DataTable y DataCards. Solo se
// * habilita `enableSorting` en lo que el backend acepta en `sort`:
// * apellidos, ci y correo (el rol y el estado son fijos o de filtro).
const columnas: ColumnDef<UsuarioPersonal, unknown>[] = [
  {
    id: 'apellidos',
    header: 'Nombre',
    accessorFn: (fila) => `${fila.apellidos} ${fila.nombres}`,
    enableSorting: true,
    meta: { cardTitle: true, grow: true },
  },
  {
    accessorKey: 'ci',
    header: 'CI',
    enableSorting: true,
    meta: { cardOrder: 0, width: 'w-32' },
  },
  {
    accessorKey: 'correo',
    header: 'Correo',
    enableSorting: true,
    meta: { cardOrder: 1, width: 'w-56' },
  },
  {
    accessorKey: 'rol',
    header: 'Rol',
    enableSorting: false,
    cell: (ctx) => etiquetaRol(ctx.getValue<string>()),
    meta: { cardOrder: 2, width: 'w-44' },
  },
  {
    accessorKey: 'activo',
    header: 'Estado',
    enableSorting: false,
    cell: (ctx) => <EstadoCuentaBadge activo={ctx.getValue<boolean>()} />,
    meta: { cardOrder: 3, width: 'w-36' },
  },
]

/** La acción en curso sobre una fila: abre el modal que corresponde. */
type AccionFila =
  | { tipo: 'editar'; usuario: UsuarioPersonal }
  | { tipo: 'cambiar-rol'; usuario: UsuarioPersonal }
  | { tipo: 'desactivar'; usuario: UsuarioPersonal }
  | { tipo: 'reactivar'; usuario: UsuarioPersonal }

export interface UsuariosPersonalScreenProps {
  /** Rol de la vista: viaja como `filter[rol]`. */
  rol: RolPersonal
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador que gestiona). */
  usuarioActual: Usuario
}

export function UsuariosPersonalScreen({ rol, titulo, activeHref, usuarioActual }: UsuariosPersonalScreenProps) {
  const cerrarSesion = useCerrarSesion()

  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE_INICIAL)
  const [busqueda, setBusqueda] = useState('')
  const [busquedaAplicada, setBusquedaAplicada] = useState('')
  const [estado, setEstado] = useState<string | undefined>(undefined)
  const [sorting, setSorting] = useState<SortingState>([])
  const [creando, setCreando] = useState(false)
  const [accion, setAccion] = useState<AccionFila | null>(null)

  // La lista ya se refresca sola (la mutación invalida los listados); si la cuenta es de otro rol, no aparecerá en esta vista.
  const alCrearCuenta = (creada: UsuarioCreado) => {
    if (creada.rol !== rol) {
      toastInfo('Cuenta creada', `${creada.nombres} ${creada.apellidos} tiene el rol ${etiquetaRol(creada.rol)}; la verás en su vista.`)
    }
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

  const consulta = useUsuarios({
    page,
    pageSize,
    search: busquedaAplicada || undefined,
    rol,
    activo: estado === undefined ? undefined : estado === 'true',
    sort: toSortQueryParam(sorting),
  })

  const { table } = useDataTable({
    data: consulta.data?.data ?? [],
    columns: columnas,
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
      id: 'activo',
      label: 'Estado',
      value: estado,
      onValueChange: (valor) => {
        setPage(1)
        setEstado(valor)
      },
      options: OPCIONES_ESTADO,
    },
  ]

  // En la fila de la propia cuenta no se ofrece «Cambiar rol» ni «Desactivar»: es solo una ayuda de
  // interfaz, el backend las niega igual (403 ERR_US_08).
  const renderAcciones = (fila: Row<UsuarioPersonal>) => {
    const cuenta = fila.original
    const esPropia = cuenta.codigo === usuarioActual.codigo
    return (
      <>
        <RowActionButton icon={<PencilIcon />} label="Editar" onClick={() => setAccion({ tipo: 'editar', usuario: cuenta })} />
        {!esPropia && (
          <RowActionButton
            icon={<ArrowRightLeftIcon />}
            label="Cambiar rol"
            onClick={() => setAccion({ tipo: 'cambiar-rol', usuario: cuenta })}
          />
        )}
        {cuenta.activo
          ? !esPropia && (
              <RowActionButton
                icon={<UserXIcon />}
                label="Desactivar"
                variant="destructive"
                onClick={() => setAccion({ tipo: 'desactivar', usuario: cuenta })}
              />
            )
          : (
              <RowActionButton
                icon={<UserCheckIcon />}
                label="Reactivar"
                onClick={() => setAccion({ tipo: 'reactivar', usuario: cuenta })}
              />
            )}
      </>
    )
  }

  const cerrarAccion = (abierto: boolean) => {
    if (!abierto) setAccion(null)
  }

  const mensajeError = getErrorMessage(consulta.error)
  // Con datos previos en pantalla (página que falla al refrescar) no se tapa la lista: solo se avisa.
  const sinDatos = consulta.isError && !consulta.data

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
    >
      <div className="rounded-2xl border border-border bg-card p-4">
        <DataTableToolbar
          searchValue={busqueda}
          onSearchChange={(valor) => {
            setBusqueda(valor)
            aplicarBusqueda(valor)
          }}
          searchPlaceholder="Buscar por nombre o CI..."
          filters={filtros}
          actions={
            <>
              <Button variant="outline" size="sm" onClick={() => void consulta.refetch()} disabled={consulta.isFetching}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
              <Button size="sm" onClick={() => setCreando(true)}>
                <PlusIcon data-icon="inline-start" />
                Nueva cuenta
              </Button>
            </>
          }
        />

        {consulta.isError && (
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

        <div className="mt-4 hidden md:block">
          <DataTable
            table={table}
            isLoading={consulta.isLoading}
            isError={sinDatos}
            errorMessage={mensajeError}
            emptyMessage="No hay cuentas que coincidan."
            showRowNumber
            renderActions={renderAcciones}
          />
        </div>
        <div className="mt-4 md:hidden">
          <DataCards
            table={table}
            isLoading={consulta.isLoading}
            isError={sinDatos}
            errorMessage={mensajeError}
            emptyMessage="No hay cuentas que coincidan."
            showRowNumber
            renderActions={renderAcciones}
          />
        </div>

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
      </div>

      {/* Se monta solo mientras está abierto: al cerrar se descarta su estado (incluida la contraseña temporal). */}
      {creando && (
        <CrearUsuarioModal open onOpenChange={setCreando} rolInicial={rol} onCreada={alCrearCuenta} />
      )}
      {accion?.tipo === 'editar' && <EditarUsuarioModal open onOpenChange={cerrarAccion} usuario={accion.usuario} />}
      {accion?.tipo === 'cambiar-rol' && <CambiarRolModal open onOpenChange={cerrarAccion} usuario={accion.usuario} />}
      {(accion?.tipo === 'desactivar' || accion?.tipo === 'reactivar') && (
        <CambiarEstadoUsuarioDialog open onOpenChange={cerrarAccion} usuario={accion.usuario} accion={accion.tipo} />
      )}
    </AdminLayout>
  )
}
