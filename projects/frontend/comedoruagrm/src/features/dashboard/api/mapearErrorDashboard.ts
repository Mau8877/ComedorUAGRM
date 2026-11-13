import { isAxiosError } from 'axios'

import { ApiError } from '@/store'
import { getErrorMessage } from '@/components/ui/tanstack-table'

export interface ErrorDashboard {
  mensaje: string
  /** La gestión o la convocatoria ya no existe: hay que volver a la selección vigente. */
  seleccionInexistente?: boolean
  /** No hay datos que exportar. */
  sinDatos?: boolean
  /** Error de conexión: la pantalla ofrece reintentar. */
  red?: boolean
}

/** Traduce un error de la API a lo que ve el Directivo (CU12). Nunca incluye datos sensibles. */
export function mapearErrorDashboard(error: unknown): ErrorDashboard {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_DAS_01':
        return { mensaje: 'La gestión o la convocatoria elegida ya no existe.', seleccionInexistente: true }
      case 'ERR_DAS_02':
        return { mensaje: 'No hay datos para exportar.', sinDatos: true }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para ver el dashboard.' }
      default:
        return { mensaje: getErrorMessage(error) }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  return { mensaje: getErrorMessage(error) }
}
