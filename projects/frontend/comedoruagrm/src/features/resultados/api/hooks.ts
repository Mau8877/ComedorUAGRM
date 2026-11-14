import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { adjudicacionKeys } from '@/features/adjudicacion'
import { finalistasKeys } from '@/features/finalistas'

import type { AdjudicadosParams, DecisionesParams, PublicadosParams } from '../types'
import { resultadosKeys } from './keys'
import {
  listarAdjudicadosApi,
  listarConvocatoriasPublicadasApi,
  listarDecisionesApi,
  obtenerMiResultadoApi,
  obtenerResumenApi,
  publicarResultadosApi,
} from './resultadosApi'

/** El resumen de la publicación; no consulta mientras no haya una convocatoria elegida. */
export function useResumenPublicacion(convocatoriaCodigo: string | undefined) {
  return useQuery({
    queryKey: resultadosKeys.resumen(convocatoriaCodigo as string),
    queryFn: () => obtenerResumenApi(convocatoriaCodigo as string),
    enabled: Boolean(convocatoriaCodigo),
  })
}

/** Una página de decisiones del Administrador; conserva la anterior mientras llega la siguiente. */
export function useDecisionesResultado(convocatoriaCodigo: string | undefined, params: DecisionesParams) {
  return useQuery({
    queryKey: resultadosKeys.decisiones(convocatoriaCodigo as string, params),
    queryFn: () => listarDecisionesApi(convocatoriaCodigo as string, params),
    enabled: Boolean(convocatoriaCodigo),
    placeholderData: keepPreviousData,
  })
}

/** Las convocatorias con resultados publicados, la más reciente primero. */
export function useConvocatoriasPublicadas(params: PublicadosParams) {
  return useQuery({
    queryKey: resultadosKeys.publicados(params),
    queryFn: () => listarConvocatoriasPublicadasApi(params),
  })
}

/** La lista pública de adjudicados; conserva la página anterior mientras llega la siguiente. */
export function useAdjudicadosPublicos(convocatoriaCodigo: string | undefined, params: AdjudicadosParams) {
  return useQuery({
    queryKey: resultadosKeys.adjudicados(convocatoriaCodigo as string, params),
    queryFn: () => listarAdjudicadosApi(convocatoriaCodigo as string, params),
    enabled: Boolean(convocatoriaCodigo),
    placeholderData: keepPreviousData,
  })
}

/** El resultado propio del postulante en la convocatoria. */
export function useMiResultado(convocatoriaCodigo: string | undefined) {
  return useQuery({
    queryKey: resultadosKeys.mio(convocatoriaCodigo as string),
    queryFn: () => obtenerMiResultadoApi(convocatoriaCodigo as string),
    enabled: Boolean(convocatoriaCodigo),
  })
}

/**
 * Publica los resultados (CU22) y refresca todo lo que depende de ellos: los resultados, el panel de
 * adjudicación (queda con `resultadosPublicados`) y la lista de finalistas.
 */
export function usePublicarResultados() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (convocatoria: string) => publicarResultadosApi(convocatoria),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: resultadosKeys.all })
      void queryClient.invalidateQueries({ queryKey: adjudicacionKeys.all })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}
