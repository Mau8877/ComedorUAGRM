import {
  AwardIcon,
  CalendarDaysIcon,
  CalendarRangeIcon,
  ClipboardCheckIcon,
  FileTextIcon,
  HistoryIcon,
  LayoutDashboardIcon,
  ListOrderedIcon,
  MegaphoneIcon,
  ScaleIcon,
  ShieldCheckIcon,
  SlidersHorizontalIcon,
  TrophyIcon,
  UserCogIcon,
  UsersIcon,
  UsersRoundIcon,
} from 'lucide-react'

import type { NavItem } from '@/components/layout'

// * Ordenado según el flujo del proceso de la beca (convocar -> preseleccionar -> adjudicar -> becados) y agrupado:
// * cada grupo es un tramo del flujo y sus ítems van seguidos (el sidebar dibuja el título cuando cambia `grupo`).
export const adminNavItems: NavItem[] = [
  { label: 'Dashboard', href: '/panel', icon: LayoutDashboardIcon },

  // 1. Convocar: primero la gestión académica, después la convocatoria que cuelga de ella.
  { label: 'Gestiones académicas', href: '/gestiones-academicas', icon: CalendarDaysIcon, grupo: 'Convocatoria' },
  { label: 'Convocatorias', href: '/convocatorias', icon: CalendarRangeIcon, grupo: 'Convocatoria' },

  // 2. Preselección con IA: se define el modelo y se revisa la lista que genera.
  { label: 'Parámetros del modelo', href: '/parametros-modelo', icon: SlidersHorizontalIcon, grupo: 'Preselección' },
  { label: 'Lista priorizada', href: '/lista-priorizada', icon: ListOrderedIcon, grupo: 'Preselección' },

  // 3. Auditoría y adjudicación.
  { label: 'Finalistas', href: '/finalistas', icon: MegaphoneIcon, grupo: 'Adjudicación' },
  { label: 'Auditoría y adjudicación', href: '/adjudicacion', icon: AwardIcon, grupo: 'Adjudicación' },
  { label: 'Publicar resultados', href: '/publicar-resultados', icon: TrophyIcon, grupo: 'Adjudicación' },
  { label: 'Actas', href: '/actas', icon: FileTextIcon, grupo: 'Adjudicación' },

  // 4. Becados y asistencia.
  { label: 'Padrón de becados', href: '/padron-becados', icon: UsersRoundIcon, grupo: 'Becados' },
  { label: 'Planillas', href: '/planillas', icon: ClipboardCheckIcon, grupo: 'Becados' },
  { label: 'Cotejo de asistencia', href: '/cotejo-asistencia', icon: ScaleIcon, grupo: 'Becados' },

  // 5. Administración del sistema.
  { label: 'Administradores DUBSS', href: '/usuarios/administradores', icon: ShieldCheckIcon, grupo: 'Administración' },
  { label: 'Directivos', href: '/usuarios/directivos', icon: UserCogIcon, grupo: 'Administración' },
  { label: 'Personal del Comedor', href: '/usuarios/personal-comedor', icon: UsersIcon, grupo: 'Administración' },
  { label: 'Bitácora', href: '/bitacora', icon: HistoryIcon, grupo: 'Administración' },
]
