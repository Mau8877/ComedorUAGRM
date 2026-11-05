import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorUsuarios {
  mensaje: string
  /** Campo del formulario en conflicto, si aplica (A1). */
  campo?: 'ci' | 'correo'
  /** Error de conexión: la pantalla ofrece reintentar (A10). */
  red?: boolean
  /** La cuenta ya no existe o ya estaba en ese estado: hay que refrescar la lista (A8, A9). */
  refrescar?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/**
 * Traduce un error de la API a lo que ve el Administrador (CU14). Las reglas las
 * aplica el backend; acá solo se muestran. Nunca incluye datos sensibles.
 */
export function mapearErrorUsuarios(error: unknown): ErrorUsuarios {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_US_01':
        return { mensaje: error.message, campo: 'ci' }
      case 'ERR_US_02':
        return { mensaje: error.message, campo: 'correo' }
      case 'ERR_SYS_01':
      case 'ERR_US_10':
      case 'ERR_US_08':
      case 'ERR_US_09':
        return { mensaje: error.message }
      case 'ERR_US_06':
      case 'ERR_US_07':
      case 'ERR_US_12':
        return { mensaje: error.message, refrescar: true }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      default:
        return { mensaje: GENERICO }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  return { mensaje: GENERICO }
}
