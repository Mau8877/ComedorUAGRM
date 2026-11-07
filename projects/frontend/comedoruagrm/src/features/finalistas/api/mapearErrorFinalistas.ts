import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorFinalistas {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una (A9). */
  convocatoriaInexistente?: boolean
  /** Error de conexión: la pantalla ofrece reintentar (A14). */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/** Traduce un error de la API de finalistas, comunicados y avisos a lo que ve el Administrador (CU21). */
export function mapearErrorFinalistas(error: unknown): ErrorFinalistas {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_FIN_01':
        return { mensaje: 'El finalista ya no existe. Actualiza la lista.' }
      case 'ERR_FIN_02':
        return { mensaje: 'La convocatoria ya no existe. Elige otra.', convocatoriaInexistente: true }
      case 'ERR_FIN_03':
        return { mensaje: 'La fecha de recepción no puede ser futura.' }
      case 'ERR_FIN_04':
        return { mensaje: 'El plazo de entrega debe ser posterior a hoy.' }
      case 'ERR_COM_01':
        return { mensaje: 'No hay destinatarios para ese comunicado. No se publicó nada.' }
      case 'ERR_COM_02':
        return { mensaje: 'Para avisar a los finalistas hay que elegir una convocatoria.' }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      case 'ERR_FIN_05':
      case 'ERR_SYS_01':
      case 'ERR_SYS_05':
        return { mensaje: error.message }
      default:
        return { mensaje: GENERICO }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  return { mensaje: GENERICO }
}
