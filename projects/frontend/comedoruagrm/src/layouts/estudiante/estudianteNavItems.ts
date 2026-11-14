import { UtensilsIcon, ClipboardListIcon, ClipboardCheckIcon, TrophyIcon, UserIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const estudianteNavItems: NavItem[] = [
  { label: 'Mi Menú', href: '/mi-menu', icon: UtensilsIcon },
  { label: 'Mis Pedidos', href: '/mis-pedidos', icon: ClipboardListIcon },
  { label: 'Mis postulaciones', href: '/mis-postulaciones', icon: ClipboardCheckIcon },
  { label: 'Resultados', href: '/resultados', icon: TrophyIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]
