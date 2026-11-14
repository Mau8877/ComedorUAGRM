import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/** Traduce un error de la API de comunicados a lo que ve el Administrador (CU21, A6, A8). */
export function mapearErrorComunicados(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_COM_01':
        return 'No hay destinatarios para ese comunicado. No se publicó nada.'
      case 'ERR_COM_02':
        return 'Para avisar a los finalistas o a los adjudicados hay que elegir una convocatoria.'
      case 'ERR_FIN_02':
        return 'La convocatoria ya no existe. Elige otra.'
      case 'ERR_AUTH_07':
        return 'No tienes permiso para publicar comunicados.'
      case 'ERR_SYS_01':
      case 'ERR_SYS_05':
        return error.message
      default:
        return GENERICO
    }
  }
  if (isAxiosError(error) && !error.response) {
    return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.'
  }
  return GENERICO
}
