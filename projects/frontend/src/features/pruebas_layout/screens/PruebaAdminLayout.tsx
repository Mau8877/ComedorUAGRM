import { AdminLayout } from '@/layouts/AdminLayout'

import { mockAdminUser } from '../mocks'

export function PruebaAdminLayout() {
  return (
    <AdminLayout title="Dashboard" user={mockAdminUser}>
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
          Esta pantalla existe solo para verificar visualmente el header, el
          sidebar (con sus ítems de Admin) y el footer del layout compartido.
          Probá colapsar el sidebar y reducir el ancho de la ventana para ver
          el modo mobile.
        </p>
      </div>
    </AdminLayout>
  )
}
