import { ApiError } from '@/store'

import { mapearError } from '@/features/auth'

export interface ErrorPerfil {
  mensaje: string
  /** Campo del formulario en conflicto o con el dato incorrecto, si aplica. */
  campo?: 'correo' | 'contrasenaActual'
  /** Error de conexión: el formulario ofrece reintentar. */
  red?: boolean
}

/**
 * Traduce un error de la API del perfil a lo que ve el usuario (CU15: A5, A6, A10). La contraseña
 * actual incorrecta es un `400` (`ERR_AUTH_09`) y NO cierra la sesión. Nunca incluye datos sensibles.
 */
export function mapearErrorPerfil(error: unknown): ErrorPerfil {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_AUTH_09':
        return { mensaje: 'La contraseña actual es incorrecta.', campo: 'contrasenaActual' }
      case 'ERR_US_02':
        return { mensaje: error.message, campo: 'correo' }
      case 'ERR_US_13':
      case 'ERR_SYS_01':
        return { mensaje: error.message }
    }
  }
  // Red, límite de peticiones, error inesperado: el mapeo común de la autenticación.
  const { mensaje, red } = mapearError(error)
  return { mensaje, red }
}
