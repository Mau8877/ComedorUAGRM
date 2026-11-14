import { createFileRoute, Outlet } from '@tanstack/react-router'

// * Layout puramente estructural (sin UI propia, solo <Outlet/>) -- existe
// * para que TanStack Router matchee este nivel de la URL junto con sus
// * rutas hijas, así Breadcrumbs.tsx tiene un cruce "Demo breadcrumbs" que
// * mostrar en la cadena. El shell real (AdminLayout) lo sigue renderizando
// * cada pantalla hija por su cuenta -- mismo patrón que el resto de
// * pruebas_layout, no se introduce composición por Outlet para el shell.
export const Route = createFileRoute('/pruebas-layout/breadcrumbs')({
  staticData: { breadcrumb: 'Demo breadcrumbs' },
  component: () => <Outlet />,
})
