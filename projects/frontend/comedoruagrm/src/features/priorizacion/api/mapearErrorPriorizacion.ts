import { isAxiosError } from 'axios'

import { ApiError } from '@/store'

export interface ErrorPriorizacion {
  mensaje: string
  /** La convocatoria ya no existe: hay que volver a elegir una (A10). */
  convocatoriaInexistente?: boolean
  /** No hay nada que exportar (A12). */
  listaVacia?: boolean
  /** Error de conexión: la pantalla ofrece reintentar (A8). */
  red?: boolean
}

const GENERICO = 'Ocurrió un error inesperado. Inténtalo de nuevo.'

/** Traduce un error de la API a lo que ve el Administrador (CU06). Nunca incluye datos sensibles. */
export function mapearErrorPriorizacion(error: unknown): ErrorPriorizacion {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'ERR_PRI_01':
        return { mensaje: 'La convocatoria ya no existe. Elige otra.', convocatoriaInexistente: true }
      case 'ERR_PRI_03':
        return { mensaje: 'No hay postulaciones para exportar.', listaVacia: true }
      case 'ERR_AUTH_07':
        return { mensaje: 'No tienes permiso para ver la lista priorizada.' }
      case 'ERR_PRI_02':
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
