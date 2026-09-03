import {
  DownloadIcon,
  FilterIcon,
  PencilIcon,
  PlusIcon,
  SearchIcon,
  Trash2Icon,
  TrendingDownIcon,
  TrendingUpIcon,
} from 'lucide-react'

import { AdminLayout } from '@/layouts/AdminLayout'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  toastSuccess,
  toastError,
  toastWarning,
  toastInfo,
} from '@/components/ui/toast'

import { MiniBarChart, ProgressBar, StatusBadge } from '../components'
import {
  mockAdminUser,
  mockMenuSemana,
  mockPedidosPorCategoria,
  mockUsuariosRecientes,
} from '../mocks'
import type { EstadoDisponibilidad } from '../types'

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

export function PruebaAdminLayout() {
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
                'El menú de la semana ya está visible para los alumnos.'
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
                'Los alumnos ya pueden postular para el próximo semestre.'
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
