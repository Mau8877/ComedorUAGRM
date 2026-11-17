import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { MiBecaScreen } from '@/features/mibeca'

// * Solo el Postulante; el backend es quien niega de verdad con 403 (ERR_AUTH_07) a otro rol.
export const Route = createFileRoute('/_authenticated/mi-beca')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: MiBecaPage,
})

function MiBecaPage() {
  const { usuario } = Route.useRouteContext()
  return <MiBecaScreen titulo="Mi beca" activeHref="/mi-beca" usuarioActual={usuario} />
}
