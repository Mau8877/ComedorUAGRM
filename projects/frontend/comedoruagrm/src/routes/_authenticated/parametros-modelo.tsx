import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { ParametrosModeloScreen } from '@/features/preseleccion'

// * Solo el Administrador DUBSS (RF-21); el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/parametros-modelo')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: ParametrosModeloPage,
})

function ParametrosModeloPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <ParametrosModeloScreen titulo="Parámetros del modelo" activeHref="/parametros-modelo" usuarioActual={usuario} />
  )
}
