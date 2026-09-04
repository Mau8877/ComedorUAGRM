import { useEffect } from 'react'
import { createRootRoute, Link, Outlet } from '@tanstack/react-router'
import { UtensilsCrossedIcon } from 'lucide-react'

import { buttonVariants } from '@/components/ui/button'
import { useUiStore } from '@/store'

export const Route = createRootRoute({
  component: RootLayout,
  // * Fallback global: cualquier URL que no matchee ninguna ruta del árbol
  // * cae acá (TanStack Router burbujea hasta el ancestro más cercano con
  // * `notFoundComponent`, y la raíz es el último). No hace falta
  // * declararlo de nuevo en `createRouter` (`defaultNotFoundComponent`)
  // * -- ya cubre toda la app desde acá.
  notFoundComponent: NotFoundPage,
})

// * Aplicar la clase `dark` acá (la raíz, envuelve TODAS las rutas vía
// * <Outlet/> incluida esta misma NotFoundPage) y no adentro de AppShell
// * -- AppShell solo se monta en pantallas con layout de rol, así que una
// * ruta que cae fuera de eso (esta página de 404, /login) se quedaba
// * siempre en claro sin importar el tema guardado en useUiStore.
function RootLayout() {
  const theme = useUiStore((s) => s.theme)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return <Outlet />
}

function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-4 text-center text-foreground">
      <span className="flex size-14 items-center justify-center rounded-2xl bg-accent">
        <UtensilsCrossedIcon className="size-7 text-accent-foreground" />
      </span>
      <div>
        <h1 className="font-heading text-2xl font-semibold">Página no encontrada</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          La ruta a la que intentaste entrar no existe en ComedorUAGRM.
        </p>
      </div>
      <Link to="/" className={buttonVariants({ variant: 'default' })}>
        Volver al inicio
      </Link>
    </div>
  )
}
