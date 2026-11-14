import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { ExpedienteScreen } from '@/features/expedientes'

// * El «_» tras `finalistas` saca esta ruta del anidado de `/finalistas` (que es la lista): son pantallas
// * hermanas. Solo el Administrador DUBSS (RF-16); el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/finalistas_/$codigo')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: ExpedientePage,
})

function ExpedientePage() {
  const { usuario } = Route.useRouteContext()
  const { codigo } = Route.useParams()
  const navigate = useNavigate()
  return (
    <ExpedienteScreen
      titulo="Auditar expediente"
      activeHref="/finalistas"
      usuarioActual={usuario}
      codigo={codigo}
      onFinalistaInexistente={() => void navigate({ to: '/finalistas', replace: true })}
    />
  )
}
