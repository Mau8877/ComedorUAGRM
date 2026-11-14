import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorResultados {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una. */
  convocatoriaInexistente?: boolean
  /** El resumen quedó desactualizado (ERR_PUB_01, 02 o 03): conviene volver a pedirlo. */
  refrescar?: boolean
  /** Error de conexión: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/** Traduce un error de la API de la publicación de resultados a lo que ve el usuario (CU22). */
export function mapearErrorResultados(error: unknown): ErrorResultados {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_PRI_01':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', convocatoriaInexistente: true }
      case 'ERR_PUB_01':
        // El backend dice cuántas decisiones faltan ("Faltan N decisiones ...").
        return { mensaje: error.message, refrescar: true }
      case 'ERR_PUB_02':
        return { mensaje: 'Los resultados de esta convocatoria ya estaban publicados.', refrescar: true }
      case 'ERR_PUB_03':
        return {
          mensaje: 'La convocatoria debe estar cerrada para publicar sus resultados.',
          refrescar: true,
        }
      case 'ERR_PUB_04':
        return { mensaje: 'Los resultados aún no están publicados.' }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      default:
        return { mensaje: getErrorMessage(error) }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  return { mensaje: GENERICO }
}
