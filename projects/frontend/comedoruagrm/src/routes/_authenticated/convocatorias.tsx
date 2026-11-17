import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { ConvocatoriasScreen } from '@/features/convocatorias'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/convocatorias')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: ConvocatoriasPage,
})

function ConvocatoriasPage() {
  const { usuario } = Route.useRouteContext()
  return <ConvocatoriasScreen titulo="Convocatorias" activeHref="/convocatorias" usuarioActual={usuario} />
}
