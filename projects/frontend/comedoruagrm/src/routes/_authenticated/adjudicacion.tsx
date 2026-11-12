import { createFileRoute } from '@tanstack/react-router'

import { AdjudicacionScreen } from '@/features/adjudicacion'
import { exigirRol } from '@/features/auth'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/adjudicacion')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: AdjudicacionPage,
})

function AdjudicacionPage() {
  const { usuario } = Route.useRouteContext()
  return <AdjudicacionScreen titulo="Auditoría y adjudicación" activeHref="/adjudicacion" usuarioActual={usuario} />
}
