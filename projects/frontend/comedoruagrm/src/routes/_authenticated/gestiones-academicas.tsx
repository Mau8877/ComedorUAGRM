import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { GestionesAcademicasScreen } from '@/features/convocatorias'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/gestiones-academicas')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: GestionesAcademicasPage,
})

function GestionesAcademicasPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <GestionesAcademicasScreen titulo="Gestiones académicas" activeHref="/gestiones-academicas" usuarioActual={usuario} />
  )
}
