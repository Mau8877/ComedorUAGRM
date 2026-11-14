import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { PublicarResultadosScreen } from '@/features/resultados'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/publicar-resultados')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: PublicarResultadosPage,
})

function PublicarResultadosPage() {
  const { usuario } = Route.useRouteContext()
  return (
    <PublicarResultadosScreen titulo="Publicar resultados" activeHref="/publicar-resultados" usuarioActual={usuario} />
  )
}
