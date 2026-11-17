import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { CotejoScreen } from '@/features/cotejos'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/cotejo-asistencia')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: CotejoPage,
})

function CotejoPage() {
  const { usuario } = Route.useRouteContext()
  return <CotejoScreen titulo="Cotejo de asistencia" activeHref="/cotejo-asistencia" usuarioActual={usuario} />
}
