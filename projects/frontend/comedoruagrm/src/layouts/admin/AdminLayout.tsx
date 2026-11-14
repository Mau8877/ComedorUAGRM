import { AppShell } from '@/components/layout'
import { useNotificaciones } from '@/features/notificaciones'

import { adminNavItems } from './adminNavItems'
import type { AdminLayoutProps } from './adminTypes'

export function AdminLayout({
  title,
  user,
  notifications,
  onCerrarSesion,
  onPerfil,
  onCambiarContrasena,
  activeHref,
  children,
}: AdminLayoutProps) {
  // La campanita usa la bandeja real salvo que quien arma el layout pase su propio controlador (pantallas de prueba).
  const bandeja = useNotificaciones({ enabled: !notifications })
  return (
    <AppShell
      appName="ComedorU Admin"
      title={title}
      navItems={adminNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications ?? bandeja}
      onCerrarSesion={onCerrarSesion}
      onPerfil={onPerfil}
      onCambiarContrasena={onCambiarContrasena}
    >
      {children}
    </AppShell>
  )
}
