import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { PadronScreen } from '@/features/becas'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/padron-becados')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: PadronPage,
})

function PadronPage() {
  const { usuario } = Route.useRouteContext()
  return <PadronScreen titulo="Padrón de becados" activeHref="/padron-becados" usuarioActual={usuario} />
}
