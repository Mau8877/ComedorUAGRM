import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { DocumentosPostulacionScreen } from '@/features/postulaciones'

// * Hermana de `mis-postulaciones_.$codigo.tsx` (el detalle): sale del anidado de la lista. Solo el Postulante; el
// * backend responde 404 si la postulación no es suya.
export const Route = createFileRoute('/_authenticated/mis-postulaciones_/$codigo/documentos')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: DocumentosPostulacionPage,
})

function DocumentosPostulacionPage() {
  const { usuario } = Route.useRouteContext()
  const { codigo } = Route.useParams()
  return (
    <DocumentosPostulacionScreen
      titulo="Documentos de la postulación"
      activeHref="/mis-postulaciones"
      usuarioActual={usuario}
      codigo={codigo}
    />
  )
}
