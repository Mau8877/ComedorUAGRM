import type { ReactNode } from 'react'

import type { LayoutUser, NotificationsController } from '@/components/layout'

export interface AdminLayoutProps {
  title: string
  user: LayoutUser
  /** Ver AppHeaderProps. */
  notifications?: NotificationsController
  /** Ver AppHeaderProps. */
  onCerrarSesion?: () => void
  /** Ver AppHeaderProps. */
  onPerfil?: () => void
  /** Ver AppHeaderProps. */
  onCambiarContrasena?: () => void
  /** Solo para pantallas de prueba: fuerza qué ítem se ve activo. */
  activeHref?: string
  children: ReactNode
}
