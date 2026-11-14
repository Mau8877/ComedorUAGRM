import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { MisPostulacionesScreen } from '@/features/postulaciones'

// * Solo el Postulante; el backend es quien niega de verdad con 403 (ERR_AUTH_07) a otro rol.
export const Route = createFileRoute('/_authenticated/mis-postulaciones')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: MisPostulacionesPage,
})

function MisPostulacionesPage() {
  const { usuario } = Route.useRouteContext()
  return <MisPostulacionesScreen titulo="Mis postulaciones" activeHref="/mis-postulaciones" usuarioActual={usuario} />
}
