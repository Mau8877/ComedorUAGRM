import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorConvocatorias {
  mensaje: string
  /** Campo del formulario al que pertenece el error, si aplica (A2). */
  campo?: 'fechaCierre' | 'etiqueta' | 'fechaFin'
  /** Error de conexión: la pantalla ofrece reintentar. */
  red?: boolean
  /** La convocatoria o la gestión ya no existe, o cambió de estado: hay que refrescar la lista (A6 a A9). */
  refrescar?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/**
 * Traduce un error de la API a lo que ve el Administrador (CU13). Las reglas las aplica el backend; acá solo se
 * muestran. Nunca incluye datos sensibles.
 */
export function mapearErrorConvocatorias(error: unknown): ErrorConvocatorias {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_CV_04':
        return { mensaje: error.message, campo: 'fechaCierre' }
      case 'ERR_CV_12':
        return { mensaje: error.message, campo: 'etiqueta' }
      case 'ERR_CV_13':
        return { mensaje: error.message, campo: 'fechaFin' }
      case 'ERR_SYS_01':
      case 'ERR_CV_05':
      case 'ERR_CV_06':
      case 'ERR_CV_07':
        return { mensaje: error.message }
      case 'ERR_CV_01':
      case 'ERR_CV_02':
      case 'ERR_CV_03':
      case 'ERR_CV_08':
      case 'ERR_CV_09':
      case 'ERR_CV_10':
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
