import { createFileRoute } from '@tanstack/react-router'

import { PruebaBreadcrumbsUsuarioDetalle } from '@/features/pruebas_layout'

export const Route = createFileRoute('/pruebas-layout/breadcrumbs/usuarios/$usuarioId')({
  staticData: { breadcrumb: 'Detalle' },
  component: PruebaBreadcrumbsUsuarioDetalle,
})
