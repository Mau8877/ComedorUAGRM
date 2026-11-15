import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorDocumentos {
  mensaje: string
  /** La postulación o el documento ya no existen o no son del usuario: la pantalla actualiza la lista. */
  refrescar?: boolean
  /** La postulación ya no se puede modificar (enviada, o la convocatoria cerró): la pantalla actualiza la lista. */
  noEditable?: boolean
  /** Error de conexión o del servidor: el usuario puede reintentar sin perder los demás documentos (A4). */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'
const CONEXION = 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo; tus otros documentos siguen guardados.'

/** Traduce un error de la API de documentos (CU16) a lo que ve el usuario. Los `400` traen su mensaje en español. */
export function mapearErrorDocumentos(error: unknown): ErrorDocumentos {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_DOC_04':
      case 'ERR_DOC_09':
        return { mensaje: 'El documento o la postulación ya no existe.', refrescar: true }
      case 'ERR_DOC_06':
        return { mensaje: 'La postulación ya fue enviada: sus documentos no se pueden modificar.', noEditable: true }
      case 'ERR_DOC_07':
        return { mensaje: 'La convocatoria ya no está abierta.', noEditable: true }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      case 'ERR_SYS_00':
        return { mensaje: SERVIDOR, red: true }
      default:
        // ERR_DOC_01/02/03/05/08 y ERR_SYS_05/06: el backend ya explica el motivo en español.
        return { mensaje: getErrorMessage(error) }
    }
  }
  if (isAxiosError(error) && !error.response) return { mensaje: CONEXION, red: true }
  if (isAxiosError(error) && (error.response?.status ?? 0) >= 500) return { mensaje: SERVIDOR, red: true }
  return { mensaje: GENERICO }
}
