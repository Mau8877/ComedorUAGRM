import {
  BadgeCheckIcon,
  BellIcon,
  FlameIcon,
  UtensilsCrossedIcon,
} from 'lucide-react'

import { UsuarioLayout } from '@/layouts/UsuarioLayout'
import { Button } from '@/components/ui/button'
import {
  toastSuccess,
  toastError,
  toastWarning,
  toastInfo,
} from '@/components/ui/toast'

import { MiniBarChart, ProgressBar, StatusBadge } from '../components'
import { mockAlumnoUser, mockConsumoSemanal, mockHistorialPedidos } from '../mocks'
import type { EstadoPedido } from '../types'

const estadoPedidoBadge: Record<
  EstadoPedido,
  { label: string; variant: 'success' | 'warning' | 'destructive' }
> = {
  entregado: { label: 'Entregado', variant: 'success' },
  pendiente: { label: 'Pendiente', variant: 'warning' },
  cancelado: { label: 'Cancelado', variant: 'destructive' },
}

const avisos = [
  {
    titulo: 'Nuevo periodo de becas',
    detalle: 'Ya podés postular para el próximo semestre.',
  },
  {
    titulo: 'Cambio de horario',
    detalle: 'El comedor cierra 30 minutos antes este viernes.',
  },
]

export function PruebaAlumnoLayout() {
  return (
    <UsuarioLayout
      title="Mi Menú"
      user={mockAlumnoUser}
      activeHref="/mi-menu"
    >
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
              toastSuccess('Pedido confirmado', 'Tu almuerzo ya está listo.')
            }
          >
            Probar éxito
          </Button>
          <Button
            className="bg-accent text-accent-foreground hover:bg-accent/90"
            onClick={() =>
              toastError(
                'No se pudo pedir',
                'Tu beca comedor no está activa hoy.'
              )
            }
          >
            Probar error
          </Button>
          <Button
            className="bg-warning text-warning-foreground hover:bg-warning/90"
            onClick={() =>
              toastWarning(
                'Poco stock del plato',
                'La Ensalada de quinoa se está por agotar.'
              )
            }
          >
            Probar advertencia
          </Button>
          <Button
            className="bg-info text-info-foreground hover:bg-info/90"
            onClick={() =>
              toastInfo(
                'Cambio de horario',
                'El comedor cierra 30 minutos antes este viernes.'
              )
            }
          >
            Probar info
          </Button>
        </div>
      </div>

      {/* Banner beca activa */}
      <div className="mb-6 flex items-center gap-3 rounded-2xl border border-success/30 bg-success/10 p-4">
        <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-success/15">
          <BadgeCheckIcon className="size-5 text-success" />
        </div>
        <div>
          <p className="text-sm font-medium text-foreground">
            Tu beca comedor está activa
          </p>
          <p className="text-xs text-muted-foreground">
            Podés pedir tu almuerzo hasta las 14:00 en el comedor central.
          </p>
        </div>
      </div>

      {/* Plato del día */}
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="overflow-hidden rounded-2xl border border-border bg-card lg:col-span-2">
          <div className="flex h-28 items-center justify-center bg-accent/10">
            <UtensilsCrossedIcon className="size-10 text-accent" />
          </div>
          <div className="p-4">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge variant="success">Disponible</StatusBadge>
              <StatusBadge variant="accent">Nuevo</StatusBadge>
              <StatusBadge variant="warning" dot={false}>
                <FlameIcon className="size-3" />
                Picante
              </StatusBadge>
            </div>
            <p className="mt-2 font-heading text-lg font-medium text-foreground">
              Milanesa con puré
            </p>
            <p className="text-sm text-muted-foreground">
              620 kcal · Incluye ensalada y refresco
            </p>

            <div className="mt-4">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>Porciones restantes</span>
                <span className="font-medium text-warning">8/40</span>
              </div>
              <ProgressBar className="mt-1.5" value={8} max={40} variant="warning" />
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              <Button>Confirmar pedido</Button>
              <Button variant="outline">Ver menú completo</Button>
              <Button
                variant="ghost"
                className="text-destructive hover:bg-destructive/10"
              >
                Cancelar pedido
              </Button>
            </div>
          </div>
        </div>

        {/* Consumo semanal */}
        <div className="rounded-2xl border border-border bg-card p-4">
          <h2 className="font-heading text-sm font-medium text-foreground">
            Consumo semanal
          </h2>
          <p className="mt-1 text-xs text-muted-foreground">Calorías por día</p>
          <div className="mt-4">
            <MiniBarChart data={mockConsumoSemanal} />
          </div>
        </div>
      </div>

      {/* Historial de pedidos + avisos */}
      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-4 lg:col-span-2">
          <h2 className="font-heading text-sm font-medium text-foreground">
            Historial de pedidos
          </h2>
          <div className="mt-3 divide-y divide-border">
            {mockHistorialPedidos.map((pedido, i) => (
              <div
                key={i}
                className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {pedido.plato}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {pedido.fecha}
                  </p>
                </div>
                <StatusBadge variant={estadoPedidoBadge[pedido.estado].variant}>
                  {estadoPedidoBadge[pedido.estado].label}
                </StatusBadge>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-4">
          <h2 className="flex items-center gap-2 font-heading text-sm font-medium text-foreground">
            <BellIcon className="size-4 text-info" />
            Avisos
          </h2>
          <div className="mt-3 space-y-3">
            {avisos.map((aviso) => (
              <div
                key={aviso.titulo}
                className="rounded-xl border border-info/30 bg-info/10 p-3"
              >
                <p className="text-sm font-medium text-foreground">
                  {aviso.titulo}
                </p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {aviso.detalle}
                </p>
              </div>
            ))}
          </div>
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
    </UsuarioLayout>
  )
}
