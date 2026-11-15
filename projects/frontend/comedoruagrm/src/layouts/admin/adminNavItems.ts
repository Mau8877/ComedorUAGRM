import {
  AwardIcon,
  CalendarDaysIcon,
  CalendarRangeIcon,
  FileTextIcon,
  HistoryIcon,
  LayoutDashboardIcon,
  ListOrderedIcon,
  MegaphoneIcon,
  ShieldCheckIcon,
  TrophyIcon,
  UserCogIcon,
  UsersIcon,
  UsersRoundIcon,
  UtensilsIcon,
  ClipboardListIcon,
} from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },
  { label: 'Convocatorias', href: '/convocatorias', icon: CalendarRangeIcon },
  { label: 'Gestiones académicas', href: '/gestiones-academicas', icon: CalendarDaysIcon },
  { label: 'Lista priorizada', href: '/lista-priorizada', icon: ListOrderedIcon },
  { label: 'Finalistas', href: '/finalistas', icon: MegaphoneIcon },
  { label: 'Auditoría y adjudicación', href: '/adjudicacion', icon: AwardIcon },
  { label: 'Publicar resultados', href: '/publicar-resultados', icon: TrophyIcon },
  { label: 'Actas', href: '/actas', icon: FileTextIcon },
  { label: 'Padrón de becados', href: '/padron-becados', icon: UsersRoundIcon },
  { label: 'Bitácora', href: '/bitacora', icon: HistoryIcon },
  { label: 'Administradores DUBSS', href: '/usuarios/administradores', icon: ShieldCheckIcon },
  { label: 'Directivos', href: '/usuarios/directivos', icon: UserCogIcon },
  { label: 'Personal del Comedor', href: '/usuarios/personal-comedor', icon: UsersIcon },
  { label: 'Menús', href: '/menus', icon: UtensilsIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
]
