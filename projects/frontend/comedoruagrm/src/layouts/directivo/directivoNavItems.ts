import { LayoutDashboardIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

// * Un solo ítem: la opción «Reportes» del mockup no se incluye (decisión del CU12).
export const directivoNavItems: NavItem[] = [{ label: 'Dashboard', href: '/dashboard', icon: LayoutDashboardIcon }]
