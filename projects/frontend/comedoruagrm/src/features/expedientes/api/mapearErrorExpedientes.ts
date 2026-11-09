import { ApiError } from '@/store'
import { mapearErrorFinalistas, type ErrorFinalistas } from '@/features/finalistas'

export interface ErrorExpedientes extends ErrorFinalistas {
  /** El finalista ya no existe: hay que volver a la lista (A11). */
  finalistaInexistente?: boolean
  /** El expediente quedó desactualizado (CU20, ERR_ARC_01): conviene volver a pedirlo. */
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
      case 'ERR_FIN_01':
        return { mensaje: 'El finalista ya no existe.', finalistaInexistente: true }
      default:
        break
    }
  }
  return mapearErrorFinalistas(error)
}
