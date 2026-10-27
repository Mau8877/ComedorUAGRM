import { createFileRoute } from '@tanstack/react-router'

import { PruebaBreadcrumbsUsuarios } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/breadcrumbs/usuarios/')({
  component: PruebaBreadcrumbsUsuarios,
})
