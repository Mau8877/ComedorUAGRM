import { AppShell } from '@/components/layout'
import { useNotificaciones } from '@/features/notificaciones'

import { comedorNavItems } from './comedorNavItems'
import { ContenidoEnShell } from '../shell/ShellPersistente'
import { useShellPersistente } from '../shell/shellContext'
import type { ComedorLayoutProps } from './comedorTypes'

function ComedorMarco({
  title,
  user,
  notifications,
  onCerrarSesion,
  onPerfil,
  onCambiarContrasena,
  activeHref,
  icono,
  migas,
  children,
}: ComedorLayoutProps) {
  // La campanita usa la bandeja real salvo que quien arma el layout pase su propio controlador (pantallas de prueba).
  const bandeja = useNotificaciones({ enabled: !notifications })
  return (
    <AppShell
      appName="ComedorU Personal"
      title={title}
      navItems={comedorNavItems}
      activeHref={activeHref}
      icono={icono}
      migas={migas}
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
export function ComedorLayout(props: ComedorLayoutProps) {
  const shell = useShellPersistente()
  if (shell) {
    return (
      <ContenidoEnShell shell={shell} title={props.title}
        activeHref={props.activeHref}
        icono={props.icono}
        migas={props.migas}
      >
        {props.children}
      </ContenidoEnShell>
    )
  }
  return <ComedorMarco {...props} />
}
