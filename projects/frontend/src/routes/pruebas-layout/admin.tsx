import { createFileRoute } from '@tanstack/react-router'

import { PruebaAdminLayout } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/admin')({
  component: PruebaAdminLayout,
  staticData: { breadcrumb: 'Dashboard' },
})
