import { useState } from 'react'
import {
  DownloadIcon,
  EyeIcon,
  FilterIcon,
  PencilIcon,
  PlusIcon,
  RefreshCwIcon,
  SearchIcon,
  Trash2Icon,
  TrendingDownIcon,
  TrendingUpIcon,
} from 'lucide-react'
import type { ColumnDef, SortingState } from '@tanstack/react-table'

import { AdminLayout } from '@/layouts'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  toastSuccess,
  toastError,
  toastWarning,
  toastInfo,
} from '@/components/ui/toast'
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

import { MiniBarChart, ProgressBar, StatusBadge } from '../components'
import {
  mockAdminUser,
  mockMenuSemana,
  mockPedidosPorCategoria,
  mockUsuariosRecientes,
  mockUsuariosTabla,
} from '../mocks'
import type { EstadoDisponibilidad, UsuarioTabla } from '../types'

const estadoBadge: Record<
  EstadoDisponibilidad,
  { label: string; variant: 'success' | 'warning' | 'destructive' }
> = {
  disponible: { label: 'Disponible', variant: 'success' },
  'pocas-porciones': { label: 'Pocas porciones', variant: 'warning' },
  agotado: { label: 'Agotado', variant: 'destructive' },
}

const stats = [
  { titulo: 'Usuarios activos', valor: 128, tendencia: '+8%', positiva: true },
  { titulo: 'Pedidos hoy', valor: 342, tendencia: '+12%', positiva: true },
  { titulo: 'Menús publicados', valor: 12, tendencia: '-2%', positiva: false },
]

// Columnas del demo de components/ui/tanstack-table -- una sola definición
// alimenta tanto DataTable como DataCards (ver meta.cardTitle/cardLabel/
// hideInCard/cardOrder).
const usuariosTablaColumns: ColumnDef<UsuarioTabla, unknown>[] = [
  {
    accessorKey: 'nombre',
    header: 'Nombre',
    meta: { cardTitle: true },
  },
  {
    accessorKey: 'correo',
    header: 'Correo',
    meta: { cardLabel: 'Correo', cardOrder: 1 },
  },
  {
    accessorKey: 'rol',
    header: 'Rol',
    enableSorting: false,
    cell: (ctx) => (
      <Badge variant={ctx.getValue<UsuarioTabla['rol']>() === 'Administrador' ? 'default' : 'secondary'}>
        {ctx.getValue<string>()}
      </Badge>
    ),
    meta: { cardOrder: 0 },
  },
]

const usuariosRolFilterOptions = [
  { label: 'Administrador', value: 'Administrador' },
  { label: 'Estudiante', value: 'Estudiante' },
]

const DEMO_PAGE_SIZE = 8

export function PruebaAdminLayout() {
  // Simula lo que en una feature real haría un hook de api/ con TanStack
  // Query contra el backend (page/pageSize -> meta) -- acá se resuelve
  // client-side sobre el mock solo para el propósito de esta pantalla de
  // prueba visual.
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(DEMO_PAGE_SIZE)
  const [sorting, setSorting] = useState<SortingState>([])
  const [search, setSearch] = useState('')
  const [rolFilter, setRolFilter] = useState<string | undefined>(undefined)
  const sortParam = toSortQueryParam(sorting)

  // search/filter[rol] también los resolvería el backend (ver
  // .claude/rules/backend/ENDPOINTS_BACKEND.md) -- acá se simulan
  // client-side sobre el mock solo para esta pantalla de prueba.
  const datosFiltrados = mockUsuariosTabla.filter((usuario) => {
    const coincideBusqueda =
      search.trim() === '' ||
      usuario.nombre.toLowerCase().includes(search.toLowerCase()) ||
      usuario.correo.toLowerCase().includes(search.toLowerCase())
    const coincideRol = !rolFilter || usuario.rol === rolFilter
    return coincideBusqueda && coincideRol
  })

  // El orden también lo resolvería el backend (`sort=campo`/`sort=-campo`,
  // ver .claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort).
  const datosOrdenados = [...datosFiltrados].sort((a, b) => {
    const [first] = sorting
    if (!first) return 0
    const campo = first.id as keyof UsuarioTabla
    const comparacion = String(a[campo]).localeCompare(String(b[campo]))
    return first.desc ? -comparacion : comparacion
  })

  const totalItems = datosOrdenados.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const paginaData = datosOrdenados.slice((page - 1) * pageSize, page * pageSize)
  const usuariosTablaMeta = { page, pageSize, totalItems, totalPages }

  const usuariosTablaFilters: DataTableToolbarFilter[] = [
    {
      id: 'rol',
      label: 'Rol',
      value: rolFilter,
      onValueChange: (value) => {
        setPage(1)
        setRolFilter(value)
      },
      options: usuariosRolFilterOptions,
    },
  ]

  const { table: usuariosTable } = useDataTable({
    data: paginaData,
    columns: usuariosTablaColumns,
    meta: usuariosTablaMeta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nextPageSize) => {
      setPage(1)
      setPageSize(nextPageSize)
    },
    enableSorting: true,
    sorting,
    onSortingChange: (nextSorting) => {
      setPage(1)
      setSorting(nextSorting)
    },
  })

  return (
    <AdminLayout title="Dashboard" user={mockAdminUser} activeHref="/panel">
      {/* Notificaciones (toast) */}
      <div className="mb-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Notificaciones (toast)
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Cuatro tipos disponibles, ya diseñados y listos para llamar desde
          cualquier feature: <code>toastSuccess()</code>,{' '}
          <code>toastError()</code>, <code>toastWarning()</code> y{' '}
          <code>toastInfo()</code>. El color de cada botón acá abajo coincide
          con el toast real que dispara.
        </p>
        <div className="mt-3 flex flex-wrap gap-3">
          <Button
            className="bg-success text-success-foreground hover:bg-success/90"
            onClick={() =>
              toastSuccess(
                'Menú publicado',
                'El menú de la semana ya está visible para los estudiantes.'
              )
            }
          >
            Probar éxito
          </Button>
          <Button
            className="bg-accent text-accent-foreground hover:bg-accent/90"
            onClick={() =>
              toastError(
                'No se pudo publicar el menú',
                'Faltan platos por asignar en alguno de los días.'
              )
            }
          >
            Probar error
          </Button>
          <Button
            className="bg-warning text-warning-foreground hover:bg-warning/90"
            onClick={() =>
              toastWarning(
                'Pocas porciones disponibles',
                'Quedan menos de 10 porciones de Pique a lo macho para hoy.'
              )
            }
          >
            Probar advertencia
          </Button>
          <Button
            className="bg-info text-info-foreground hover:bg-info/90"
            onClick={() =>
              toastInfo(
                'Nuevo periodo de becas',
                'Los estudiantes ya pueden postular para el próximo semestre.'
              )
            }
          >
            Probar info
          </Button>
        </div>
      </div>

      {/* Barra de acciones */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          <Button>
            <PlusIcon data-icon="inline-start" />
            Nuevo menú
          </Button>
          <Button variant="secondary">
            <DownloadIcon data-icon="inline-start" />
            Exportar reporte
          </Button>
          <Button variant="outline">
            <FilterIcon data-icon="inline-start" />
            Filtrar
          </Button>
          <Button
            variant="ghost"
            className="text-destructive hover:bg-destructive/10"
          >
            <Trash2Icon data-icon="inline-start" />
            Eliminar seleccionados
          </Button>
        </div>
        <div className="relative">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="search"
            placeholder="Buscar usuario o menú..."
            className="h-8 w-56 rounded-2xl border border-input bg-background pr-3 pl-8 text-sm text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 focus-visible:outline-none"
          />
        </div>
      </div>

      {/* Stats con tendencia */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((stat) => (
          <div
            key={stat.titulo}
            className="rounded-2xl border border-border bg-card p-4"
          >
            <p className="text-sm text-muted-foreground">{stat.titulo}</p>
            <p className="mt-1 font-heading text-2xl font-medium text-foreground">
              {stat.valor}
            </p>
            <p
              className={
                stat.positiva
                  ? 'mt-1 flex items-center gap-1 text-xs font-medium text-success'
                  : 'mt-1 flex items-center gap-1 text-xs font-medium text-destructive'
              }
            >
              {stat.positiva ? (
                <TrendingUpIcon className="size-3.5" />
              ) : (
                <TrendingDownIcon className="size-3.5" />
              )}
              {stat.tendencia} vs. semana pasada
            </p>
          </div>
        ))}
      </div>

      {/* Menú de la semana + Pedidos por categoría */}
      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="font-heading text-sm font-medium text-foreground">
              Menú de la semana
            </h2>
            <StatusBadge variant="primary" dot={false}>
              5 días cargados
            </StatusBadge>
          </div>
          <div className="mt-3 divide-y divide-border">
            {mockMenuSemana.map((item) => (
              <div
                key={item.dia}
                className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {item.dia} · {item.plato}
                  </p>
                  <div className="mt-1.5 flex items-center gap-2">
                    <ProgressBar
                      className="w-28"
                      value={item.porciones}
                      max={item.porcionesTotal}
                      variant={
                        item.estado === 'agotado'
                          ? 'destructive'
                          : item.estado === 'pocas-porciones'
                            ? 'warning'
                            : 'success'
                      }
                    />
                    <span className="text-xs text-muted-foreground">
                      {item.porciones}/{item.porcionesTotal} porciones
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge variant={estadoBadge[item.estado].variant}>
                    {estadoBadge[item.estado].label}
                  </StatusBadge>
                  <Button variant="ghost" size="icon-sm">
                    <PencilIcon />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-4">
          <h2 className="font-heading text-sm font-medium text-foreground">
            Pedidos por categoría
          </h2>
          <p className="mt-1 text-xs text-muted-foreground">Esta semana</p>
          <div className="mt-4">
            <MiniBarChart data={mockPedidosPorCategoria} />
          </div>
        </div>
      </div>

      {/* Usuarios recientes */}
      <div className="mt-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Usuarios recientes
        </h2>
        <div className="mt-3 divide-y divide-border">
          {mockUsuariosRecientes.map((usuario) => (
            <div
              key={usuario.correo}
              className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
            >
              <div className="flex items-center gap-3">
                <Avatar>
                  <AvatarFallback
                    className={
                      usuario.rol === 'Administrador'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-secondary text-secondary-foreground'
                    }
                  >
                    {usuario.nombre
                      .split(' ')
                      .map((n) => n[0])
                      .slice(0, 2)
                      .join('')}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {usuario.nombre}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {usuario.correo}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <StatusBadge
                  variant={usuario.rol === 'Administrador' ? 'primary' : 'muted'}
                  dot={false}
                >
                  {usuario.rol}
                </StatusBadge>
                <span className="h-5 w-px bg-border" aria-hidden="true" />
                <Button variant="ghost" size="sm">
                  Ver
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="text-destructive hover:bg-destructive/10"
                >
                  <Trash2Icon />
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* components/ui/tanstack-table -- misma columnas para DataTable y DataCards */}
      <div className="mt-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Tabla de usuarios (DataTable + DataCards)
        </h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Mismas columnas para ambas vistas. DataTable visible desde
          `md`, DataCards debajo -- reducí el ancho de la ventana para ver
          el cambio. Click en "Nombre" o "Correo" para ordenar.
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          Query param que se mandaría al backend:{' '}
          <code>
            {[
              search.trim() && `search=${search.trim()}`,
              rolFilter && `filter[rol]=${rolFilter}`,
              sortParam && `sort=${sortParam}`,
            ]
              .filter(Boolean)
              .join('&') || '(sin filtros/orden aplicados)'}
          </code>
        </p>

        <DataTableToolbar
          className="mt-4"
          searchValue={search}
          onSearchChange={(value) => {
            setPage(1)
            setSearch(value)
          }}
          searchPlaceholder="Buscar por nombre o correo..."
          filters={usuariosTablaFilters}
          actions={
            <>
              <Button variant="outline" size="sm" onClick={() => { setPage(1); setSearch(''); setRolFilter(undefined) }}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
              <Button size="sm">
                <PlusIcon data-icon="inline-start" />
                Nuevo usuario
              </Button>
            </>
          }
        />

        <div className="mt-4 hidden md:block">
          <DataTable
            table={usuariosTable}
            isLoading={false}
            isError={false}
            showRowNumber
            renderActions={(row) => (
              <>
                <RowActionButton icon={<EyeIcon />} label="Ver detalles" onClick={() => {}} />
                <RowActionButton icon={<PencilIcon />} label="Editar" onClick={() => {}} />
                <RowActionButton
                  icon={<Trash2Icon />}
                  label={`Eliminar a ${row.original.nombre}`}
                  variant="destructive"
                  onClick={() => {}}
                />
              </>
            )}
          />
        </div>
        <div className="mt-4 md:hidden">
          <DataCards
            table={usuariosTable}
            isLoading={false}
            isError={false}
            showRowNumber
            renderActions={() => (
              <>
                <RowActionButton icon={<EyeIcon />} label="Ver detalles" onClick={() => {}} />
                <RowActionButton icon={<PencilIcon />} label="Editar" onClick={() => {}} />
                <RowActionButton icon={<Trash2Icon />} label="Eliminar" variant="destructive" onClick={() => {}} />
              </>
            )}
          />
        </div>

        <DataTablePagination
          className="mt-4"
          meta={usuariosTablaMeta}
          page={page}
          onPageChange={setPage}
          onPageSizeChange={(nextPageSize) => {
            setPage(1)
            setPageSize(nextPageSize)
          }}
          pageSizeOptions={[2, 4, 8, 12]}
        />
      </div>

      <div className="mt-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Contenido de prueba
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Esta pantalla existe para verificar visualmente el header, el
          sidebar y el sistema de colores completo (marca, estados, gráficos)
          contra contenido real. Probá colapsar el sidebar, reducir el ancho
          de la ventana y cambiar entre tema claro/oscuro.
        </p>
      </div>
    </AdminLayout>
  )
}
