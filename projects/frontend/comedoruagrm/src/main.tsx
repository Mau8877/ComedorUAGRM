import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider, createRouter } from '@tanstack/react-router'
import './index.css'
import { routeTree } from './routeTree.gen'
import { instalarManejadorDeSesion } from '@/features/auth'
import { QueryProvider } from './store'
import { CargandoPagina } from '@/components/layout'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'

// * Feedback inmediato al navegar: por defecto el router espera 1000 ms antes de mostrar el pendingComponent
// * (y la pantalla vieja queda congelada). Con 0 ms el skeleton aparece al hacer clic, y el mínimo de 250 ms
// * evita el parpadeo cuando la ruta carga casi al instante.
const router = createRouter({
  routeTree,
  defaultPendingComponent: CargandoPagina,
  defaultPendingMs: 0,
  defaultPendingMinMs: 250,
})

// * Ante un 401 el cliente HTTP intenta renovar la sesión; si no se puede, lleva
// * al login (salvo que ya se esté en una pantalla pública). Se instala acá, antes
// * de que corra cualquier guard, porque los guards ya hacen peticiones.
const RUTAS_PUBLICAS = ['/', '/login', '/registro', '/recuperar-contrasena', '/restablecer-contrasena']
instalarManejadorDeSesion({
  alPerderSesion: () => {
    if (!RUTAS_PUBLICAS.includes(router.state.location.pathname)) {
      void router.navigate({ to: '/login' })
    }
  },
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryProvider>
      <TooltipProvider>
        <RouterProvider router={router} />
        <Toaster position="top-right" />
      </TooltipProvider>
    </QueryProvider>
  </StrictMode>,
)
