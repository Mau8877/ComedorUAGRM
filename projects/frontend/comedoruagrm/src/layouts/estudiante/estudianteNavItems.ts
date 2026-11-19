import { ClipboardCheckIcon, TrophyIcon, UserIcon, WalletCardsIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

// * Agrupado según el recorrido del postulante: postula y ve el resultado, si queda becado usa su beca, y su cuenta.
export const estudianteNavItems: NavItem[] = [
  { label: 'Mis postulaciones', href: '/mis-postulaciones', icon: ClipboardCheckIcon, grupo: 'Postulación' },
  { label: 'Resultados', href: '/resultados', icon: TrophyIcon, grupo: 'Postulación' },
  { label: 'Mi beca', href: '/mi-beca', icon: WalletCardsIcon, grupo: 'Beca' },
  { label: 'Perfil', href: '/perfil', icon: UserIcon, grupo: 'Cuenta' },
]
