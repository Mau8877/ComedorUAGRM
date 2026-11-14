import { AdminLayout } from '@/layouts'

import { mockAdminUser, mockUsuariosTabla } from '../mocks'

export function PruebaBreadcrumbsUsuarios() {
  const usuarios = mockUsuariosTabla.slice(0, 6)

  return (
    <AdminLayout title="Usuarios (demo)" user={mockAdminUser}>
      <div className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">Usuarios</h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Lista mínima -- clickeá un nombre para ver el breadcrumb con los 3 niveles.
        </p>
        <ul className="mt-4 divide-y divide-border">
          {usuarios.map((usuario) => (
            <li key={usuario.id}>
              <a
                href={`/pruebas-layout/breadcrumbs/usuarios/${usuario.id}`}
                className="flex items-center justify-between py-2.5 text-sm text-foreground transition-colors hover:text-primary"
              >
                <span>{usuario.nombre}</span>
                <span className="text-xs text-muted-foreground">{usuario.rol}</span>
              </a>
            </li>
          ))}
        </ul>
      </div>
    </AdminLayout>
  )
}
