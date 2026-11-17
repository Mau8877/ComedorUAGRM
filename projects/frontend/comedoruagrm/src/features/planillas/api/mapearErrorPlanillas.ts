import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorPlanillas {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una. */
  convocatoriaInexistente?: boolean
  /** Lo que la pantalla muestra ya no es cierto (el padrón cambió): conviene volver a pedir el resumen. */
  refrescar?: boolean
  /** Error de conexión o del servidor: la pantalla ofrece reintentar. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'

/** Traduce un error de la API de la planilla de asistencia a lo que ve el usuario (CU09). */
export function mapearErrorPlanillas(error: unknown): ErrorPlanillas {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_PRI_01':
        return { mensaje: 'La convocatoria no existe o fue eliminada.', convocatoriaInexistente: true }
      case 'ERR_PLA_01':
        return { mensaje: 'La fecha final no puede ser anterior a la inicial.' }
      case 'ERR_PLA_02':
        return { mensaje: 'El periodo debe estar dentro de la gestión académica de la convocatoria.' }
      case 'ERR_PLA_03':
        return { mensaje: 'El periodo no puede superar los 31 días: genera una planilla por mes.' }
      case 'ERR_PLA_04':
        return {
          mensaje: 'La convocatoria no tiene becas vigentes: no hay a quién incluir en la planilla.',
          refrescar: true,
        }
      case 'ERR_PLA_05':
        return { mensaje: 'La planilla no existe o fue eliminada.', refrescar: true }
      case 'ERR_PLA_06':
        return { mensaje: 'El archivo de la planilla no está disponible. Avisa a soporte.' }
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
