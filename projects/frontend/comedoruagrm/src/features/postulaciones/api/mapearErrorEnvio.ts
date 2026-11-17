import { isAxiosError } from 'axios'

import { getErrorMessage } from '@/components/ui/tanstack-table'
import { ApiError } from '@/store'

export interface ErrorEnvio {
  mensaje: string
  /** No hay convocatoria abierta (ERR_ENV_01): la pantalla lo muestra como aviso, no como fallo. */
  sinConvocatoria?: boolean
  /** Ya tiene una postulación vigente o no es elegible (ERR_VAL_01/02): no puede postular. */
  noPuedePostular?: boolean
  /** La convocatoria se cerró (ERR_VAL_03) o la postulación ya se envió (ERR_ENV_03): el plazo finalizó, no hay reintento. */
  cerrada?: boolean
  /** Error de conexión o del servidor: el usuario puede reintentar sin perder lo que escribió. */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
const SERVIDOR = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.'
const CONEXION = 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo; lo que escribiste se conserva.'

/** Traduce un error del envío de la postulación (CU-02) a lo que ve el usuario. Los `400` y `409` traen su mensaje en español. */
export function mapearErrorEnvio(error: unknown): ErrorEnvio {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_ENV_01':
        return { mensaje: getErrorMessage(error), sinConvocatoria: true }
      case 'ERR_VAL_01':
      case 'ERR_VAL_02':
        return { mensaje: getErrorMessage(error), noPuedePostular: true }
      case 'ERR_VAL_03':
        return { mensaje: 'La convocatoria ya no recibe postulaciones: el plazo finalizó.', cerrada: true }
      case 'ERR_ENV_02':
        return { mensaje: 'La postulación no existe o no es tuya.', noPuedePostular: true }
      case 'ERR_ENV_03':
        return { mensaje: 'Esta postulación ya no se puede enviar.', cerrada: true }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para esta acción.' }
      case 'ERR_SYS_00':
        return { mensaje: SERVIDOR, red: true }
      default:
        // ERR_ENV_04 (falta algo) y ERR_ENV_05 (dato inválido): el backend explica qué en español.
        return { mensaje: getErrorMessage(error) }
    }
  }
  if (isAxiosError(error) && !error.response) return { mensaje: CONEXION, red: true }
  if (isAxiosError(error) && (error.response?.status ?? 0) >= 500) return { mensaje: SERVIDOR, red: true }
  return { mensaje: GENERICO }
}
