import type { Expediente } from '../types'

/** Por qué no se puede devolver todavía (ERR_DEV_01 / ERR_DEV_02), o `null` si se puede intentar. */
export function motivoBloqueoDevolucion(expediente: Expediente): string | null {
  if (expediente.estadoPostulacion === 'devuelta') return 'La postulación ya está devuelta.'
  if (expediente.estadoPostulacion !== 'calificada') return 'La postulación ya no está calificada.'
  if (expediente.auditorias.length === 0) return 'Primero registra la auditoría del expediente.'
  return null
}
