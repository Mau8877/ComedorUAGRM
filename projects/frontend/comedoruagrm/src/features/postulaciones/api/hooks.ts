import { keepPreviousData, useQuery } from '@tanstack/react-query'

import type { MisPostulacionesParams } from '../types'
import { listarMisPostulacionesApi, obtenerPostulacionApi } from './postulacionesApi'
import { postulacionesKeys } from './keys'

/** Una página de «Mis postulaciones»; conserva la anterior mientras llega la siguiente. */
export function useMisPostulaciones(params: MisPostulacionesParams) {
  return useQuery({
    queryKey: postulacionesKeys.mias(params),
    queryFn: () => listarMisPostulacionesApi(params),
    placeholderData: keepPreviousData,
  })
}

/** El detalle de una postulación propia; no consulta mientras no haya código. */
export function usePostulacion(codigo: string | undefined) {
  return useQuery({
    queryKey: postulacionesKeys.detalle(codigo as string),
    queryFn: () => obtenerPostulacionApi(codigo as string),
    enabled: Boolean(codigo),
  })
}
