import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorBecas {
  mensaje: string
  /** Lo que la pantalla muestra ya no es cierto (la beca cambió o desapareció): conviene volver a pedir los datos. */
  refrescar?: boolean
  /** Error de conexión o del servidor: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'

/** Traduce un error de la API del padrón de becados a lo que ve el Administrador (CU23). */
export function mapearErrorBecas(error: unknown): ErrorBecas {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_BEC_01':
        return { mensaje: 'La beca no existe o fue eliminada.', refrescar: true }
      case 'ERR_BEC_02':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', refrescar: true }
      case 'ERR_BEC_03':
        return {
          mensaje: 'Los resultados de esta convocatoria aún no están publicados. Primero hay que publicarlos.',
          refrescar: true,
        }
      case 'ERR_BEC_04':
        return { mensaje: 'No hay adjudicados pendientes de alta: todos ya figuran en el padrón.', refrescar: true }
      case 'ERR_BEC_05':
        return { mensaje: 'La beca ya no está vigente: no se puede dar de baja ni suspender.', refrescar: true }
      case 'ERR_BEC_06':
        return { mensaje: 'Solo se puede reemplazar una beca que fue dada de baja.', refrescar: true }
      case 'ERR_BEC_07':
        return { mensaje: 'No hay un postulante disponible para ocupar el cupo.' }
      case 'ERR_BEC_08':
        return {
          mensaje: 'El postulante propuesto cambió mientras confirmabas. Vuelve a abrir el reemplazo.',
          refrescar: true,
        }
      case 'ERR_BEC_09':
        return { mensaje: 'Esta beca ya fue reemplazada.', refrescar: true }
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
