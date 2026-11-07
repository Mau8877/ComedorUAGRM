import type { ResultadoAuditoria } from '../types'

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

/** Instante ISO → `dd/mm/aaaa hh:mm` en la zona del navegador. */
export function formatearFechaHora(instante: string): string {
  return FORMATO_FECHA_HORA.format(new Date(instante))
}

export function formatearPuntos(valor: number | null): string {
  return valor === null ? '—' : Number(valor).toFixed(2)
}

export const ETIQUETA_RESULTADO_AUDITORIA: Record<ResultadoAuditoria, string> = {
  VALIDADO: 'Validado',
  OBSERVADO: 'Observado',
  NO_VALIDO: 'No válido',
}

export const CLASE_RESULTADO_AUDITORIA: Record<ResultadoAuditoria, string> = {
  VALIDADO: 'bg-success/15 text-success',
  OBSERVADO: 'bg-warning/15 text-warning',
  NO_VALIDO: 'bg-destructive/15 text-destructive',
}
