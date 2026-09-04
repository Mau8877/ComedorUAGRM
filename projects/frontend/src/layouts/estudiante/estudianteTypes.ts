import type { ReactNode } from 'react'

import type { LayoutUser } from '@/components/layout'

export interface EstudianteLayoutProps {
  title: string
  user: LayoutUser
  /** Solo para pantallas de prueba: fuerza qué ítem se ve activo. */
  activeHref?: string
  children: ReactNode
}
