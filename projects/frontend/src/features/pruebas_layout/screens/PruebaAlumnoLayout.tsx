import { UsuarioLayout } from '@/layouts/UsuarioLayout'

import { mockAlumnoUser } from '../mocks'

export function PruebaAlumnoLayout() {
  return (
    <UsuarioLayout title="Mi Menú" user={mockAlumnoUser}>
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
          Esta pantalla existe solo para verificar visualmente el header, el
          sidebar (con sus ítems de Alumno) y el footer del layout
          compartido. Probá colapsar el sidebar y reducir el ancho de la
          ventana para ver el modo mobile.
        </p>
      </div>
    </UsuarioLayout>
  )
}
