import { AppShell } from '@/components/layout'
import { useNotificaciones } from '@/features/notificaciones'

import { estudianteNavItems } from './estudianteNavItems'
import type { EstudianteLayoutProps } from './estudianteTypes'

export function EstudianteLayout({
  title,
  user,
  notifications,
  onCerrarSesion,
  onPerfil,
  onCambiarContrasena,
  activeHref,
  children,
}: EstudianteLayoutProps) {
  // La campanita usa la bandeja real salvo que quien arma el layout pase su propio controlador (pantallas de prueba).
  const bandeja = useNotificaciones({ enabled: !notifications })
  return (
    <AppShell
      appName="ComedorU"
      title={title}
      navItems={estudianteNavItems}
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
