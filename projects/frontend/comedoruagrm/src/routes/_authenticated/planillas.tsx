import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { PlanillasScreen } from '@/features/planillas'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/planillas')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: PlanillasPage,
})

function PlanillasPage() {
  const { usuario } = Route.useRouteContext()
  return <PlanillasScreen titulo="Planillas de asistencia" activeHref="/planillas" usuarioActual={usuario} />
}
