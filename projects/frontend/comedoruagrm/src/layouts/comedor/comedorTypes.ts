import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'

import type { LayoutUser, MigaPagina, NotificationsController } from '@/components/layout'

export interface ComedorLayoutProps {
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
  /** Ícono del encabezado (por defecto el del ítem activo). */
  icono?: LucideIcon
  /** Migas del encabezado (por defecto solo la página actual). */
  migas?: MigaPagina[]
  children: ReactNode
}
