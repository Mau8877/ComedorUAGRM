import { ApiError } from '@/store'
import { mapearErrorFinalistas, type ErrorFinalistas } from '@/features/finalistas'

export interface ErrorExpedientes extends ErrorFinalistas {
  /** El finalista ya no existe: hay que volver a la lista (A11). */
  finalistaInexistente?: boolean
  /** El expediente quedó desactualizado (ERR_ARC_01, ERR_ADJ_01 o ERR_ADJ_04): conviene volver a pedirlo. */
  refrescar?: boolean
}

/** Traduce un error de la API del expediente a lo que ve el Administrador (CU07). */
export function mapearErrorExpedientes(error: unknown): ErrorExpedientes {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_EXP_01':
        return { mensaje: 'Primero registra la entrega de la documentación física.' }
      case 'ERR_EXP_02':
        return { mensaje: 'La postulación ya no está calificada.' }
      case 'ERR_EXP_03':
        return { mensaje: 'Las observaciones son obligatorias si el expediente es observado o no válido.' }
      case 'ERR_EXP_04':
        return { mensaje: 'El puntaje académico debe estar entre 0 y 30, con hasta dos decimales.' }
      case 'ERR_EXP_05':
        return { mensaje: 'El documento ya no está disponible en este expediente.' }
      case 'ERR_EXP_06':
        return { mensaje: 'Esa auditoría ya se registró con otros datos. Revisa el historial.' }
      case 'ERR_EXP_07':
        return { mensaje: 'No se pudo identificar el envío. Inténtalo de nuevo.' }
      case 'ERR_DEV_01':
        return { mensaje: 'La postulación ya no está calificada: otra persona pudo devolverla antes.' }
      case 'ERR_DEV_02':
        return { mensaje: 'Primero registra la auditoría del expediente.' }
      case 'ERR_ARC_01':
        return {
          mensaje: 'La postulación ya no está calificada o los resultados ya se publicaron; se actualizó el expediente.',
          refrescar: true,
        }
      case 'ERR_ADJ_01':
        return {
          mensaje: 'La postulación ya no está calificada o los resultados ya se publicaron; se actualizó el expediente.',
          refrescar: true,
        }
      case 'ERR_ADJ_02':
        return {
          mensaje: error.message.includes('no fue auditado')
            ? 'Primero hay que auditar el expediente.'
            : 'El expediente debe estar auditado y validado para adjudicar. Puede auditarlo de nuevo o usar “No adjudicar”.',
        }
      case 'ERR_ADJ_03':
        return { mensaje: 'El puntaje total no alcanza el puntaje mínimo de la convocatoria.' }
      case 'ERR_ADJ_04':
        return { mensaje: 'Ya no quedan cupos disponibles. Solo se admite “No adjudicar”.', refrescar: true }
      case 'ERR_ADJ_05':
        return { mensaje: 'La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.' }
      case 'ERR_FIN_01':
        return { mensaje: 'El finalista ya no existe.', finalistaInexistente: true }
      default:
        break
    }
  }
  return mapearErrorFinalistas(error)
}
