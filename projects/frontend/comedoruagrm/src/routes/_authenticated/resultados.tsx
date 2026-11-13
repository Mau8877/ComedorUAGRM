import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { ResultadosScreen } from '@/features/resultados'

// * Solo el Postulante; el backend deja consultar a cualquier sesión válida, pero la pantalla es la de su módulo.
export const Route = createFileRoute('/_authenticated/resultados')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: ResultadosPage,
})

function ResultadosPage() {
  const { usuario } = Route.useRouteContext()
  return <ResultadosScreen titulo="Resultados" activeHref="/resultados" usuarioActual={usuario} />
}
