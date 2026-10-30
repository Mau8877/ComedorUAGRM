import { createFileRoute } from '@tanstack/react-router'

import { PruebaBreadcrumbsInicio } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/breadcrumbs/')({
  component: PruebaBreadcrumbsInicio,
})
