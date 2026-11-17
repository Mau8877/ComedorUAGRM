import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { PrecalificacionScreen } from '@/features/preseleccion'

// * Solo el Administrador DUBSS; el backend es quien niega de verdad con 403 (ERR_AUTH_07). Se llega desde el
// * detalle de una postulación en la lista priorizada, por eso ese ítem del menú queda activo.
export const Route = createFileRoute('/_authenticated/precalificacion/$codigo')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: PrecalificacionPage,
})

function PrecalificacionPage() {
  const { usuario } = Route.useRouteContext()
  const { codigo } = Route.useParams()
  return (
    <PrecalificacionScreen
      titulo="Precalificación con IA"
      activeHref="/lista-priorizada"
      usuarioActual={usuario}
      codigo={codigo}
    />
  )
}
