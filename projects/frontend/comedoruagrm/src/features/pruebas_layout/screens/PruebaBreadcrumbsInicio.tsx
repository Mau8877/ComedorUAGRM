import { AdminLayout } from '@/layouts'

import { mockAdminUser } from '../mocks'

// Pantalla exclusiva para verificar visualmente Breadcrumbs.tsx -- necesita
// rutas anidadas de verdad (una ruta con hijas) para que haya más de un
// cruce clickeable, algo que hoy no existe en ninguna ruta real del
// proyecto (adminNavItems.ts apunta a hrefs como /usuarios que todavía no
// tienen archivo de ruta). No es el patrón a copiar para una feature real.
export function PruebaBreadcrumbsInicio() {
  return (
    <AdminLayout title="Demo de breadcrumbs" user={mockAdminUser}>
      <div className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">Cómo probarlo</h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Esta ruta y sus hijas (<code>/usuarios</code>, <code>/usuarios/$usuarioId</code>) son
          rutas anidadas de verdad -- entrá a "Usuarios" y después a cualquier nombre para ver la
          cadena completa (Demo breadcrumbs → Usuarios → Detalle) con los dos primeros cruces
          como links clickeables en el header.
        </p>
        <a
          href="/pruebas-layout/breadcrumbs/usuarios"
          className="mt-4 inline-block text-sm font-medium text-primary hover:underline"
        >
          Ir a Usuarios →
        </a>
      </div>
    </AdminLayout>
  )
}
