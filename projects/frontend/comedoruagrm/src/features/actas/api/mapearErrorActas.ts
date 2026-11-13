import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorActas {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una. */
  convocatoriaInexistente?: boolean
  /** La lista de pendientes quedó desactualizada (ERR_ACT_01 o 02): conviene volver a pedirla. */
  refrescar?: boolean
  /** Error de conexión o del servidor: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'

/** Traduce un error de la API del acta de adjudicación a lo que ve el usuario (CU08). */
export function mapearErrorActas(error: unknown): ErrorActas {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_PRI_01':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', convocatoriaInexistente: true }
      case 'ERR_ACT_01':
        return {
          mensaje: 'Los resultados de esta convocatoria aún no están publicados. Primero hay que publicarlos.',
          refrescar: true,
        }
      case 'ERR_ACT_02':
        return { mensaje: 'Esta convocatoria ya tiene acta de adjudicación.', refrescar: true }
      case 'ERR_ACT_03':
        return { mensaje: 'Esta convocatoria todavía no tiene acta.' }
      case 'ERR_ACT_04':
        return { mensaje: 'El archivo del acta no está disponible. Avisa a soporte.' }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      case 'ERR_SYS_00':
        return { mensaje: SERVIDOR, red: true }
      default:
        return { mensaje: getErrorMessage(error) }
    }
  }
  if (isAxiosError(error) && !error.response) {
    return { mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', red: true }
  }
  if (isAxiosError(error) && (error.response?.status ?? 0) >= 500) {
    return { mensaje: SERVIDOR, red: true }
  }
  return { mensaje: GENERICO }
}
