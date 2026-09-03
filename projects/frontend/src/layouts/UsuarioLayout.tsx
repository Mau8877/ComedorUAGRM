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
  children: ReactNode
}

export function UsuarioLayout({ title, user, children }: UsuarioLayoutProps) {
  return (
    <AppShell
      appName="ComedorU"
      title={title}
      navItems={usuarioNavItems}
      user={user}
    >
      {children}
    </AppShell>
  )
}
