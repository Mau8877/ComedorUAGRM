import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { resultadosKeys } from '@/features/resultados'

import type { ActasParams, PendientesParams } from '../types'
import {
  generarActaApi,
  listarActasApi,
  listarPendientesApi,
  obtenerAccesoActaApi,
  obtenerActaApi,
  obtenerVistaPreviaApi,
} from './actasApi'
import { actasKeys } from './keys'

/** Una página de actas generadas; conserva la anterior mientras llega la siguiente. */
export function useActas(params: ActasParams) {
  return useQuery({
    queryKey: actasKeys.list(params),
    queryFn: () => listarActasApi(params),
    placeholderData: keepPreviousData,
  })
}

/** Las convocatorias con resultados publicados y sin acta (alimenta el selector del panel). */
export function useActasPendientes(params: PendientesParams) {
  return useQuery({
    queryKey: actasKeys.pendientes(params),
    queryFn: () => listarPendientesApi(params),
  })
}

/** El acta de una convocatoria; no consulta mientras no haya una elegida. */
export function useActa(convocatoriaCodigo: string | undefined) {
  return useQuery({
    queryKey: actasKeys.detail(convocatoriaCodigo as string),
    queryFn: () => obtenerActaApi(convocatoriaCodigo as string),
    enabled: Boolean(convocatoriaCodigo),
  })
}

/**
 * La dirección de descarga es una mutación: se pide al hacer clic, vence en minutos, anota la descarga en la
 * bitácora y por eso nunca se guarda en la caché.
 */
export function useAccesoActa() {
  return useMutation({ mutationFn: (convocatoria: string) => obtenerAccesoActaApi(convocatoria) })
}

/** Genera el acta (CU08) y refresca las actas y los resultados publicados. */
export function useGenerarActa() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (convocatoria: string) => generarActaApi(convocatoria),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: actasKeys.all })
      void queryClient.invalidateQueries({ queryKey: resultadosKeys.all })
    },
  })
}

/** La vista previa es una mutación que devuelve el `Blob`: el PDF nunca se guarda en la caché. */
export function useVistaPreviaActa() {
  return useMutation({ mutationFn: (convocatoria: string) => obtenerVistaPreviaApi(convocatoria) })
}
