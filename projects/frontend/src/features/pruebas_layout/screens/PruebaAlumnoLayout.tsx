import { UsuarioLayout } from '@/layouts/UsuarioLayout'
import { Button } from '@/components/ui/button'
import {
  toastSuccess,
  toastError,
  toastWarning,
  toastInfo,
} from '@/components/ui/toast'

import { mockAlumnoUser } from '../mocks'

export function PruebaAlumnoLayout() {
  return (
    <UsuarioLayout
      title="Mi Menú"
      user={mockAlumnoUser}
      activeHref="/mi-menu"
    >
      <div className="mb-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Notificaciones (toast)
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Cuatro tipos disponibles, ya diseñados y listos para llamar desde
          cualquier feature: <code>toastSuccess()</code>,{' '}
          <code>toastError()</code>, <code>toastWarning()</code> y{' '}
          <code>toastInfo()</code>.
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
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
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
            className="bg-accent text-accent-foreground hover:bg-accent/90"
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
            className="bg-header text-header-foreground hover:bg-header/90"
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

      <div className="grid gap-4 sm:grid-cols-2">
        {['Plato del día', 'Próximo pedido'].map((titulo) => (
          <div
            key={titulo}
            className="rounded-2xl border border-border bg-card p-4"
          >
            <p className="text-sm text-muted-foreground">{titulo}</p>
            <p className="mt-1 font-heading text-lg font-medium text-foreground">
              Milanesa con puré
            </p>
          </div>
        ))}
      </div>

      <div className="mt-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Contenido de prueba
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Esta pantalla existe solo para verificar visualmente el header y el
          sidebar (con sus ítems de Alumno) del layout compartido. Probá
          colapsar el sidebar y reducir el ancho de la ventana para ver el
          modo mobile.
        </p>
      </div>
    </UsuarioLayout>
  )
}
