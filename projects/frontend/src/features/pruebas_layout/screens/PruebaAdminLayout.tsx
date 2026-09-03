import { AdminLayout } from '@/layouts/AdminLayout'
import { Button } from '@/components/ui/button'
import { toastSuccess, toastError } from '@/components/ui/toast'

import { mockAdminUser } from '../mocks'

export function PruebaAdminLayout() {
  return (
    <AdminLayout title="Dashboard" user={mockAdminUser} activeHref="/panel">
      <div className="mb-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Notificaciones (toast)
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Dos tipos disponibles, ya diseñados y listos para llamar desde
          cualquier feature: <code>toastSuccess()</code> y{' '}
          <code>toastError()</code>.
        </p>
        <div className="mt-3 flex gap-3">
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
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            onClick={() =>
              toastError(
                'No se pudo publicar el menú',
                'Faltan platos por asignar en alguno de los días.'
              )
            }
          >
            Probar error
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {['Usuarios activos', 'Pedidos hoy', 'Menús publicados'].map(
          (titulo, i) => (
            <div
              key={titulo}
              className="rounded-2xl border border-border bg-card p-4"
            >
              <p className="text-sm text-muted-foreground">{titulo}</p>
              <p className="mt-1 font-heading text-2xl font-medium text-foreground">
                {[128, 342, 12][i]}
              </p>
            </div>
          )
        )}
      </div>

      <div className="mt-6 rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Contenido de prueba
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Esta pantalla existe solo para verificar visualmente el header y el
          sidebar (con sus ítems de Admin) del layout compartido. Probá
          colapsar el sidebar y reducir el ancho de la ventana para ver el
          modo mobile.
        </p>
      </div>
    </AdminLayout>
  )
}
