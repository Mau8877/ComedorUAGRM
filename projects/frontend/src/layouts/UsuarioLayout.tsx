import type { ReactNode } from 'react'
import { UtensilsIcon, ClipboardListIcon, UserIcon } from 'lucide-react'

import { AppShell } from '@/components/layout'
import type { LayoutUser, NavItem } from '@/components/layout'

const usuarioNavItems: NavItem[] = [
  { label: 'Mi Menú', href: '/mi-menu', icon: UtensilsIcon },
  { label: 'Mis Pedidos', href: '/mis-pedidos', icon: ClipboardListIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]

interface UsuarioLayoutProps {
  title: string
  user: LayoutUser
  /** Solo para pantallas de prueba: fuerza qué ítem se ve activo. */
  activeHref?: string
  children: ReactNode
}

export function UsuarioLayout({
  title,
  user,
  activeHref,
  children,
}: UsuarioLayoutProps) {
  return (
    <AppShell
      appName="ComedorU"
      title={title}
      navItems={usuarioNavItems}
      activeHref={activeHref}
      user={user}
    >
      {children}
    </AppShell>
  )
}
