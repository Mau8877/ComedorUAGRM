import { LayoutDashboardIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

// * Un solo ítem: el registro de asistencia (CU10) es de la app móvil; en la web el Personal del Comedor solo ve su
// * panel con el resumen del día.
export const comedorNavItems: NavItem[] = [{ label: 'Panel', href: '/panel', icon: LayoutDashboardIcon }]
