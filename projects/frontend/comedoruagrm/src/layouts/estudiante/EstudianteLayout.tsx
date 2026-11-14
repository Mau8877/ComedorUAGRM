import { useNavigate } from '@tanstack/react-router'

import { AppShell } from '@/components/layout'
import type { NotificationItem } from '@/components/layout'
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
  const navigate = useNavigate()
  // Un aviso que apunta a una postulación (CU04) abre su detalle; sin código se comporta como siempre.
  const bandejaConDestino = {
    ...bandeja,
    onSelect: (item: NotificationItem) => {
      if (item.postulacionCodigo) {
        void navigate({ to: '/mis-postulaciones/$codigo', params: { codigo: item.postulacionCodigo } })
      }
    },
  }
  return (
    <AppShell
      appName="ComedorU"
      title={title}
      navItems={estudianteNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications ?? bandejaConDestino}
      onCerrarSesion={onCerrarSesion}
      onPerfil={onPerfil}
      onCambiarContrasena={onCambiarContrasena}
    >
      {children}
    </AppShell>
  )
}
