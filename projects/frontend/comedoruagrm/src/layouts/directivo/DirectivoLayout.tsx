import { AppShell } from '@/components/layout'
import { useNotificaciones } from '@/features/notificaciones'

import { directivoNavItems } from './directivoNavItems'
import type { DirectivoLayoutProps } from './directivoTypes'

export function DirectivoLayout({
  title,
  user,
  notifications,
  onCerrarSesion,
  onPerfil,
  onCambiarContrasena,
  activeHref,
  children,
}: DirectivoLayoutProps) {
  // La campanita usa la bandeja real salvo que quien arma el layout pase su propio controlador (pantallas de prueba).
  const bandeja = useNotificaciones({ enabled: !notifications })
  return (
    <AppShell
      appName="ComedorU Directivo"
      title={title}
      navItems={directivoNavItems}
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
