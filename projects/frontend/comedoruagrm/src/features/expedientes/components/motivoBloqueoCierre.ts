import type { Expediente } from '../types'

/**
 * Por qué no se puede archivar ni rechazar (ERR_ARC_01), o `null` si se puede intentar. Solo depende del estado:
 * a diferencia de «Devolver», no se pide auditoría previa ni entrega física.
 */
export function motivoBloqueoCierre(expediente: Expediente): string | null {
  switch (expediente.estadoPostulacion) {
    case 'calificada':
      return null
    case 'archivada':
      return 'La postulación ya está archivada.'
    case 'no_adjudicada':
      return 'La postulación ya fue marcada como no adjudicada.'
    default:
      return 'La postulación ya no está calificada.'
  }
}
