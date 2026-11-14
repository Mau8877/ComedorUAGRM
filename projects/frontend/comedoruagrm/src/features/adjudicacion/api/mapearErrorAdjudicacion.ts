import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorAdjudicacion {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una. */
  convocatoriaInexistente?: boolean
  /** El panel quedó desactualizado (ERR_ADJ_01 o ERR_ADJ_05): conviene volver a pedirlo. */
  refrescar?: boolean
  /** Error de conexión: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/** Traduce un error de la API del panel y la selección de finalistas a lo que ve el Administrador (CU05). */
export function mapearErrorAdjudicacion(error: unknown): ErrorAdjudicacion {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_PRI_01':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', convocatoriaInexistente: true }
      case 'ERR_ADJ_01':
        return {
          mensaje: 'Los resultados de esta convocatoria ya se publicaron; no se pueden seleccionar finalistas.',
          refrescar: true,
        }
      case 'ERR_ADJ_05':
        return {
          mensaje: 'La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.',
          refrescar: true,
        }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
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
