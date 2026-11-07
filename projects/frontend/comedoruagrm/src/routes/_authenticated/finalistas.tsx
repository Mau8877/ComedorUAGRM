import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { FinalistasScreen } from '@/features/finalistas'

// * Solo el Administrador DUBSS (RF-16); el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/finalistas')({
  beforeLoad: exigirRol('ADMINISTRADOR_DUBSS'),
  component: FinalistasPage,
})

function FinalistasPage() {
  const { usuario } = Route.useRouteContext()
  const navigate = useNavigate()
  return (
    <FinalistasScreen
      titulo="Finalistas: notificación y documentación física"
      activeHref="/finalistas"
      usuarioActual={usuario}
      onAuditar={(finalista) => void navigate({ to: '/finalistas/$codigo', params: { codigo: finalista.codigo } })}
    />
  )
}
