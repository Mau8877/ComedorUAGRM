import { useParams } from '@tanstack/react-router'

import { AdminLayout } from '@/layouts'

import { mockAdminUser, mockUsuariosTabla } from '../mocks'

// * `useParams({ from: '...' })` con el id de ruta como string -- no se
// * importa el `Route` de `routes/pruebas-layout/breadcrumbs/usuarios/
// * $usuarioId.tsx` para leer el param, porque esa ruta ya importa esta
// * pantalla (sería un import circular). El id de ruta se valida igual
// * contra el árbol tipado generado (routeTree.gen.ts).
export function PruebaBreadcrumbsUsuarioDetalle() {
  const { usuarioId } = useParams({ from: '/pruebas-layout/breadcrumbs/usuarios/$usuarioId' })
  const usuario = mockUsuariosTabla.find((item) => item.id === usuarioId)

  return (
    <AdminLayout title={usuario?.nombre ?? 'Usuario no encontrado'} user={mockAdminUser}>
      <div className="rounded-2xl border border-border bg-card p-4">
        {usuario ? (
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <dt className="text-muted-foreground">Nombre</dt>
            <dd className="text-foreground">{usuario.nombre}</dd>
            <dt className="text-muted-foreground">Rol</dt>
            <dd className="text-foreground">{usuario.rol}</dd>
            <dt className="text-muted-foreground">Correo</dt>
            <dd className="text-foreground">{usuario.correo}</dd>
          </dl>
        ) : (
          <p className="text-sm text-muted-foreground">No se encontró ese usuario.</p>
        )}
        <a
          href="/pruebas-layout/breadcrumbs/usuarios"
          className="mt-4 inline-block text-sm font-medium text-primary hover:underline"
        >
          ← Volver a Usuarios
        </a>
      </div>
    </AdminLayout>
  )
}
