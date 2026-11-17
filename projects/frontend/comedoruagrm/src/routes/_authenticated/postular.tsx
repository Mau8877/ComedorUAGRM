import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { PostularScreen } from '@/features/postulaciones'

// * Nueva postulación y «Corregir y reenviar» (CU-02): las dos entran acá, el backend decide si abre un borrador nuevo
// * o la postulación devuelta. Solo el Postulante.
export const Route = createFileRoute('/_authenticated/postular')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: PostularPage,
})

function PostularPage() {
  const { usuario } = Route.useRouteContext()
  return <PostularScreen titulo="Nueva postulación" activeHref="/mis-postulaciones" usuarioActual={usuario} />
}
