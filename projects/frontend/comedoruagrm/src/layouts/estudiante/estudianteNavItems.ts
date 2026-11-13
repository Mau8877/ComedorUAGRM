import { UtensilsIcon, ClipboardListIcon, TrophyIcon, UserIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const estudianteNavItems: NavItem[] = [
  { label: 'Mi Menú', href: '/mi-menu', icon: UtensilsIcon },
  { label: 'Mis Pedidos', href: '/mis-pedidos', icon: ClipboardListIcon },
  { label: 'Resultados', href: '/resultados', icon: TrophyIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]
