import { createFileRoute, Outlet } from '@tanstack/react-router'

// Mismo patrón que el `route.tsx` padre (breadcrumbs/route.tsx): capa
// puramente estructural, solo aporta el cruce "Usuarios" a la cadena.
export const Route = createFileRoute('/pruebas-layout/breadcrumbs/usuarios')({
  staticData: { breadcrumb: 'Usuarios' },
  component: () => <Outlet />,
})
