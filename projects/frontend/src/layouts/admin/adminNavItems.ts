import {
  LayoutDashboardIcon,
  UsersIcon,
  UtensilsIcon,
  ClipboardListIcon,
} from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },
  { label: 'Usuarios', href: '/usuarios', icon: UsersIcon },
  { label: 'Menús', href: '/menus', icon: UtensilsIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
]
