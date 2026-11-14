import type { Expediente } from '../types'

/** Por qué no se puede auditar todavía (ERR_EXP_01 / ERR_EXP_02), o `null` si se puede. */
export function motivoBloqueoAuditoria(expediente: Expediente): string | null {
  if (expediente.estadoPostulacion !== 'calificada') return 'La postulación ya no está calificada.'
  if (expediente.finalista.entrega.estado !== 'RECIBIDA') {
    return 'Primero registra la entrega de la documentación física.'
  }
  return null
}
