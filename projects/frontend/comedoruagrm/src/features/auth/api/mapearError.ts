import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorMapeado {
  mensaje: string
  /** Campo del formulario en conflicto (CI, correo, registro universitario, carrera), si aplica. */
  campo?: 'ci' | 'correo' | 'registroUniversitario' | 'carreraCodigo'
  /** Error de conexión: el formulario ofrece reintentar. */
  red?: boolean
}

const CAMPO_POR_CODIGO: Record<string, ErrorMapeado['campo']> = {
  ERR_US_01: 'ci',
  ERR_US_02: 'correo',
  ERR_US_03: 'registroUniversitario',
  ERR_US_04: 'carreraCodigo',
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/**
 * Traduce un error de la API a lo que ve el usuario (CU01: A1, A3, A4, A6, A9).
 * Las reglas de contraseña, el bloqueo y el mensaje único de credenciales los
 * aplica el backend; acá solo se muestran. Nunca incluye datos sensibles.
 */
export function mapearError(error: unknown): ErrorMapeado {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_AUTH_04':
        return { mensaje: 'Correo o contraseña incorrectos.' }
      case 'ERR_AUTH_05': // el backend ya dice cuántos minutos faltan
        return { mensaje: error.message }
      case 'ERR_AUTH_03':
        return { mensaje: 'Tu cuenta está desactivada. Comunícate con la DUBSS.' }
      case 'ERR_US_01':
      case 'ERR_US_02':
      case 'ERR_US_03':
      case 'ERR_US_04':
        return { mensaje: error.message, campo: CAMPO_POR_CODIGO[error.code] }
      case 'ERR_SYS_01':
        return { mensaje: error.message }
      case 'ERR_SYS_02':
        return { mensaje: 'Demasiados intentos en poco tiempo. Espera un momento e inténtalo de nuevo.' }
      default:
        return { mensaje: GENERICO }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  return { mensaje: GENERICO }
}
