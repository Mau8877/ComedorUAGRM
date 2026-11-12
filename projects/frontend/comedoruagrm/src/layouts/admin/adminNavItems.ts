import {
  AwardIcon,
  LayoutDashboardIcon,
  ListOrderedIcon,
  MegaphoneIcon,
  ShieldCheckIcon,
  UserCogIcon,
  UsersIcon,
  UtensilsIcon,
  ClipboardListIcon,
} from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },
  { label: 'Lista priorizada', href: '/lista-priorizada', icon: ListOrderedIcon },
  { label: 'Finalistas', href: '/finalistas', icon: MegaphoneIcon },
  { label: 'Auditoría y adjudicación', href: '/adjudicacion', icon: AwardIcon },
  { label: 'Administradores DUBSS', href: '/usuarios/administradores', icon: ShieldCheckIcon },
  { label: 'Directivos', href: '/usuarios/directivos', icon: UserCogIcon },
  { label: 'Personal del Comedor', href: '/usuarios/personal-comedor', icon: UsersIcon },
  { label: 'Menús', href: '/menus', icon: UtensilsIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
]
