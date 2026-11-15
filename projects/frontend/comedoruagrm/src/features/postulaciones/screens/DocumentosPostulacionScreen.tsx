import { useNavigate } from '@tanstack/react-router'
import { ArrowLeftIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'

import { DocumentosPostulacionPanel } from '../components'

export interface DocumentosPostulacionScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante). */
  usuarioActual: Usuario
  /** El código público de la postulación, de la ruta. */
  codigo: string
}

/**
 * Los documentos de una postulación (CU16): lo que exige la convocatoria, lo que ya se adjuntó y lo que falta, con
 * acciones para adjuntar, reemplazar, eliminar y abrir cada archivo mientras la postulación se pueda modificar.
 */
export function DocumentosPostulacionScreen({
  titulo,
  activeHref,
  usuarioActual,
  codigo,
}: DocumentosPostulacionScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const navigate = useNavigate()

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-4">
        <div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void navigate({ to: '/mis-postulaciones/$codigo', params: { codigo } })}
          >
            <ArrowLeftIcon data-icon="inline-start" />
            Volver a la postulación
          </Button>
        </div>
        <DocumentosPostulacionPanel codigo={codigo} />
      </div>
    </EstudianteLayout>
  )
}
