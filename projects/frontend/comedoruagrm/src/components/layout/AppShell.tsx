import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'

import { AppHeader } from './AppHeader'
import { AppSidebar } from './AppSidebar'
import { EncabezadoPagina } from './EncabezadoPagina'
import type { MigaPagina } from './EncabezadoPagina'
import { migasPorDefecto } from './migasPorDefecto'
import type { LayoutUser, NavItem, NotificationsController } from './types'

interface AppShellProps {
  appName: string
  title: string
  navItems: NavItem[]
  activeHref?: string
  /** Ícono del encabezado; por defecto el del ítem activo del sidebar. */
  icono?: LucideIcon
  /** Migas del encabezado; por defecto se derivan del ítem activo y de la ruta (ver `migasPorDefecto`). */
  migas?: MigaPagina[]
  user: LayoutUser
  /** Ver AppHeaderProps. */
  notifications?: NotificationsController
  /** Ver AppHeaderProps. */
  onCerrarSesion?: () => void
  /** Ver AppHeaderProps. */
  onPerfil?: () => void
  /** Ver AppHeaderProps. */
  onCambiarContrasena?: () => void
  children: ReactNode
}

// * `window.location` y no un hook del router: el shell se vuelve a dibujar en cada navegación (la página nueva
// * registra su título) y así no depende de estar dentro de un RouterProvider.
function rutaActual() {
  return typeof window === 'undefined' ? '' : window.location.pathname
}

// * Esqueleto compartido por todos los layouts de rol (Admin, Estudiante, y los
// * que se agreguen a futuro) -- no conoce roles, solo recibe navItems/user
// * por props. Cada layout de rol (src/layouts/) es el único que decide QUÉ
// * le pasa acá, nunca se bifurca por rol dentro de este componente.
export function AppShell({
  appName,
  title,
  navItems,
  activeHref,
  icono,
  migas,
  user,
  notifications,
  onCerrarSesion,
  onPerfil,
  onCambiarContrasena,
  children,
}: AppShellProps) {
  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      <AppHeader
        appName={appName}
        user={user}
        notifications={notifications}
        onCerrarSesion={onCerrarSesion}
        onPerfil={onPerfil}
        onCambiarContrasena={onCambiarContrasena}
      />

      {/* * p-4 acá es lo que crea el espacio real entre el sidebar y el
          header -- el sidebar es una tarjeta flotante (bordes redondeados
          propios), no una columna pegada de borde a borde. */}
      <div className="flex min-h-0 flex-1 gap-4 overflow-hidden p-4">
        <AppSidebar items={navItems} activeHref={activeHref} />
        <main className="min-w-0 flex-1 overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-sm">
          {title && (
            <EncabezadoPagina
              icono={icono ?? navItems.find((item) => item.href === activeHref)?.icon}
              titulo={title}
              migas={migas ?? migasPorDefecto({ title, navItems, activeHref, pathname: rutaActual() })}
            />
          )}
          <div className="mt-6">{children}</div>
        </main>
      </div>
    </div>
  )
}
