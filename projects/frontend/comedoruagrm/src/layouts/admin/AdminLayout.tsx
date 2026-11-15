import { AppShell } from '@/components/layout'
import { useNotificaciones } from '@/features/notificaciones'

import { adminNavItems } from './adminNavItems'
import { ContenidoEnShell } from '../shell/ShellPersistente'
import { useShellPersistente } from '../shell/shellContext'
import type { AdminLayoutProps } from './adminTypes'

function AdminMarco({
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

// * Dentro del shell persistente (rutas privadas) solo se aporta el contenido; fuera de él (pantallas de prueba)
// * se dibuja el marco completo.
export function AdminLayout(props: AdminLayoutProps) {
  const shell = useShellPersistente()
  if (shell) {
    return (
      <ContenidoEnShell shell={shell} title={props.title} activeHref={props.activeHref}>
        {props.children}
      </ContenidoEnShell>
    )
  }
  return <AdminMarco {...props} />
}
