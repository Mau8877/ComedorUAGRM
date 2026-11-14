import { createFileRoute } from '@tanstack/react-router'

import { ActasScreen } from '@/features/actas'
import { exigirRol } from '@/features/auth'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/actas')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: ActasPage,
})

function ActasPage() {
  const { usuario } = Route.useRouteContext()
  return <ActasScreen titulo="Actas de adjudicación" activeHref="/actas" usuarioActual={usuario} />
}
