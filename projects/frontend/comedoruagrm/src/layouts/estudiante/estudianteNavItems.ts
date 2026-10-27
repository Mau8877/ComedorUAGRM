import { UtensilsIcon, ClipboardListIcon, UserIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const estudianteNavItems: NavItem[] = [
  { label: 'Mi Menú', href: '/mi-menu', icon: UtensilsIcon },
  { label: 'Mis Pedidos', href: '/mis-pedidos', icon: ClipboardListIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]
