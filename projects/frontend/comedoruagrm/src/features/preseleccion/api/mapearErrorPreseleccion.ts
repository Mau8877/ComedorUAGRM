import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorPreseleccion {
  mensaje: string
  /** Lo que la pantalla muestra ya no es cierto (hay otra versión o lote): conviene volver a pedir los datos. */
  refrescar?: boolean
  /** Error de conexión o del servidor: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'

/** Traduce un error de la API del modelo de preselección a lo que ve el Administrador (CU26). */
export function mapearErrorPreseleccion(error: unknown): ErrorPreseleccion {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_MOD_01':
        // El mensaje del backend dice qué parámetro falla ("Los parámetros no son válidos: …").
        return { mensaje: error.message }
      case 'ERR_MOD_02':
        return { mensaje: 'Todavía no hay una versión vigente del modelo. Guarda los parámetros primero.', refrescar: true }
      case 'ERR_MOD_03':
        return { mensaje: 'Ya hay un reprocesamiento en curso para esta convocatoria.', refrescar: true }
      case 'ERR_MOD_04':
        return {
          mensaje: 'Los resultados de esta convocatoria ya fueron publicados: no se pueden reprocesar los puntajes.',
          refrescar: true,
        }
      case 'ERR_MOD_05':
        return { mensaje: 'El reprocesamiento no existe.', refrescar: true }
      case 'ERR_MOD_06':
        return { mensaje: 'No hay postulaciones calificadas por reprocesar con la versión vigente.', refrescar: true }
      case 'ERR_MOD_07':
        return {
          mensaje: 'Otra versión se guardó al mismo tiempo. Vuelve a abrir los parámetros y reintenta.',
          refrescar: true,
        }
      case 'ERR_MOD_08':
        return { mensaje: 'La postulación no existe.' }
      case 'ERR_CV_01':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', refrescar: true }
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
