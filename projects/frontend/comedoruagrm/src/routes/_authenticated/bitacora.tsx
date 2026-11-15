import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { BitacoraScreen } from '@/features/bitacora'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/bitacora')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: BitacoraPage,
})

function BitacoraPage() {
  const { usuario } = Route.useRouteContext()
  return <BitacoraScreen titulo="Bitácora" activeHref="/bitacora" usuarioActual={usuario} />
}
