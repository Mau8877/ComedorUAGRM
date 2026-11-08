import type { EstadoNotificacion, ResultadoAuditoriaFinalista } from '../types'

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

/** La postulación fue devuelta para corrección (CU19): no se audita hasta que el postulante la reenvíe. */
export function DevueltaBadge() {
  return <span className="rounded-full bg-warning/15 px-2 py-0.5 text-xs font-medium text-warning">Devuelta</span>
}

const ETIQUETAS_AUDITORIA: Record<ResultadoAuditoriaFinalista, string> = {
  SIN_AUDITAR: 'Sin auditar',
  VALIDADO: 'Validado',
  OBSERVADO: 'Observado',
  NO_VALIDO: 'No válido',
}

const CLASES_AUDITORIA: Record<ResultadoAuditoriaFinalista, string> = {
  SIN_AUDITAR: 'bg-muted text-muted-foreground',
  VALIDADO: 'bg-success/15 text-success',
  OBSERVADO: 'bg-warning/15 text-warning',
  NO_VALIDO: 'bg-destructive/15 text-destructive',
}

/** Resultado de la última auditoría del expediente (CU07): «Sin auditar», «Validado», «Observado» o «No válido». */
export function AuditoriaBadge({ resultado }: { resultado: ResultadoAuditoriaFinalista }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${CLASES_AUDITORIA[resultado]}`}>
      {ETIQUETAS_AUDITORIA[resultado]}
    </span>
  )
}
