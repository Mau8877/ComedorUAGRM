import type { ReactNode } from 'react'
import {
  LayoutDashboardIcon,
  UsersIcon,
  UtensilsIcon,
  ClipboardListIcon,
} from 'lucide-react'

import { AppShell } from '@/components/layout'
import type { LayoutUser, NavItem } from '@/components/layout'

const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },
  { label: 'Usuarios', href: '/usuarios', icon: UsersIcon },
  { label: 'Menús', href: '/menus', icon: UtensilsIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
]

interface AdminLayoutProps {
  title: string
  user: LayoutUser
  children: ReactNode
}

export function AdminLayout({ title, user, children }: AdminLayoutProps) {
  return (
    <AppShell
      appName="ComedorU Admin"
      title={title}
      navItems={adminNavItems}
      user={user}
    >
      {children}
    </AppShell>
  )
}
