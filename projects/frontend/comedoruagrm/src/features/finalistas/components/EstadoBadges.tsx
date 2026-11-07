import type { EstadoNotificacion } from '../types'

const ETIQUETAS_NOTIFICACION: Record<EstadoNotificacion, string> = {
  SIN_ENVIAR: 'Sin enviar',
  ENVIADA: 'Enviada',
  FALLIDA: 'Fallida',
}

const CLASES_NOTIFICACION: Record<EstadoNotificacion, string> = {
  SIN_ENVIAR: 'bg-muted text-muted-foreground',
  ENVIADA: 'bg-success/15 text-success',
  FALLIDA: 'bg-destructive/15 text-destructive',
}

/** Estado del último aviso al finalista: «Sin enviar», «Enviada» o «Fallida». */
export function EstadoNotificacionBadge({ estado }: { estado: EstadoNotificacion }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${CLASES_NOTIFICACION[estado]}`}>
      {ETIQUETAS_NOTIFICACION[estado]}
    </span>
  )
}

/** Estado de la entrega física: «Pendiente» o «Recibida». */
export function EntregaBadge({ recibida }: { recibida: boolean }) {
  return (
    <span
      className={`rounded-full px-2 py-0.5 text-xs font-medium ${recibida ? 'bg-success/15 text-success' : 'bg-warning/15 text-warning'}`}
    >
      {recibida ? 'Recibida' : 'Pendiente'}
    </span>
  )
}
