import { UtensilsIcon, ClipboardListIcon, ClipboardCheckIcon, TrophyIcon, UserIcon, WalletCardsIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const estudianteNavItems: NavItem[] = [
  { label: 'Mi Menú', href: '/mi-menu', icon: UtensilsIcon },
  { label: 'Mis Pedidos', href: '/mis-pedidos', icon: ClipboardListIcon },
  { label: 'Mis postulaciones', href: '/mis-postulaciones', icon: ClipboardCheckIcon },
  { label: 'Resultados', href: '/resultados', icon: TrophyIcon },
  { label: 'Mi beca', href: '/mi-beca', icon: WalletCardsIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]
