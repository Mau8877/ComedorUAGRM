import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { DashboardScreen } from '@/features/dashboard'

// * Solo el Directivo; el backend es quien niega de verdad con 403 (ERR_AUTH_07).
export const Route = createFileRoute('/_authenticated/dashboard')({
  beforeLoad: exigirRol('DIRECTIVO'),
  component: DashboardPage,
})

function DashboardPage() {
  const { usuario } = Route.useRouteContext()
  return <DashboardScreen titulo="Dashboard de indicadores" activeHref="/dashboard" usuarioActual={usuario} />
}
