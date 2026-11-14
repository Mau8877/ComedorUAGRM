import { createFileRoute } from '@tanstack/react-router'

import { exigirRol } from '@/features/auth'
import { DetallePostulacionScreen } from '@/features/postulaciones'

// * El «_» tras `mis-postulaciones` saca esta ruta del anidado de `/mis-postulaciones` (que es la lista): son
// * pantallas hermanas, igual que `finalistas_.$codigo.tsx`. Solo el Postulante; el backend responde 404 si la
// * postulación no es suya.
export const Route = createFileRoute('/_authenticated/mis-postulaciones_/$codigo')({
  beforeLoad: exigirRol('POSTULANTE'),
  component: DetallePostulacionPage,
})

function DetallePostulacionPage() {
  const { usuario } = Route.useRouteContext()
  const { codigo } = Route.useParams()
  return (
    <DetallePostulacionScreen
      titulo="Detalle de postulación"
      activeHref="/mis-postulaciones"
      usuarioActual={usuario}
      codigo={codigo}
    />
  )
}
