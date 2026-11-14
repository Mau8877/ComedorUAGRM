import { ListOrderedIcon } from 'lucide-react'

import { ModalDetailLayout } from '@/components/ui/modal-layout'

import type { PostulacionPriorizada } from '../types'
import { formatearFecha, formatearPuntaje } from './columnasLista'

interface DetallePostulacionDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  postulacion: PostulacionPriorizada
}

// Estados de la postulación tal como los guarda el backend, en palabras para el Administrador.
const ETIQUETAS_ESTADO: Record<string, string> = { calificada: 'Calificada' }

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="text-sm font-medium text-foreground">{valor}</dd>
    </div>
  )
}

/**
 * Detalle de solo lectura de una postulación de la lista priorizada (CU06, paso 8). Muestra los
 * datos que ya vienen en la fila: no pide nada más al backend. El paso a la auditoría lo agrega el
 * CU-05 cuando exista.
 */
export function DetallePostulacionDialog({ open, onOpenChange, postulacion }: DetallePostulacionDialogProps) {
  return (
    <ModalDetailLayout
      open={open}
      onOpenChange={onOpenChange}
      icon={ListOrderedIcon}
      title={`${postulacion.nombres} ${postulacion.apellidos}`}
      subtitle={`Posición ${postulacion.posicion} de la lista priorizada`}
    >
      <dl className="grid gap-4 sm:grid-cols-2">
        <Dato etiqueta="Posición" valor={String(postulacion.posicion)} />
        <Dato etiqueta="Puntaje de vulnerabilidad" valor={formatearPuntaje(postulacion.puntaje)} />
        <Dato etiqueta="Nombres" valor={postulacion.nombres} />
        <Dato etiqueta="Apellidos" valor={postulacion.apellidos} />
        <Dato etiqueta="CI" valor={postulacion.ci} />
        <Dato etiqueta="Carrera" valor={postulacion.carrera.nombre} />
        <Dato etiqueta="Puntaje calculado el" valor={formatearFecha(postulacion.fechaCalculo)} />
        <Dato etiqueta="Estado" valor={ETIQUETAS_ESTADO[postulacion.estado] ?? postulacion.estado} />
      </dl>
    </ModalDetailLayout>
  )
}
