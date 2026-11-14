import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { ListaPriorizadaScreen } from '@/features/priorizacion'

// * Solo el Administrador DUBSS (RF-04); el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/lista-priorizada')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: ListaPriorizadaPage,
})

function ListaPriorizadaPage() {
  const { usuario } = Route.useRouteContext()
  return <ListaPriorizadaScreen titulo="Lista priorizada" activeHref="/lista-priorizada" usuarioActual={usuario} />
}
