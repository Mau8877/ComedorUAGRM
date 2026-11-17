import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { PeriodoPlanilla, PlanillasParams } from '../types'
import { planillasKeys } from './keys'
import {
  generarPlanillaApi,
  listarPlanillasApi,
  obtenerAccesoPlanillaApi,
  obtenerResumenPlanillaApi,
  obtenerVistaPreviaPlanillaApi,
} from './planillasApi'

/** Cuántos becados vigentes entrarían y los límites de la gestión; no consulta sin convocatoria elegida. */
export function useResumenPlanilla(convocatoria: string | undefined) {
  return useQuery({
    queryKey: planillasKeys.resumen(convocatoria as string),
    queryFn: () => obtenerResumenPlanillaApi(convocatoria as string),
    enabled: Boolean(convocatoria),
  })
}

/** Una página de planillas de la convocatoria; conserva la anterior mientras llega la siguiente. */
export function usePlanillas(convocatoria: string | undefined, params: PlanillasParams) {
  return useQuery({
    queryKey: planillasKeys.list(convocatoria as string, params),
    queryFn: () => listarPlanillasApi(convocatoria as string, params),
    enabled: Boolean(convocatoria),
    placeholderData: keepPreviousData,
  })
}

/** Genera la planilla (CU09) y refresca la lista y el resumen de la convocatoria. */
export function useGenerarPlanilla() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ convocatoria, periodo }: { convocatoria: string; periodo: PeriodoPlanilla }) =>
      generarPlanillaApi(convocatoria, periodo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: planillasKeys.all })
    },
  })
}

/**
 * La dirección de descarga es una mutación: se pide al hacer clic, vence en minutos, anota la descarga en la
 * bitácora y por eso nunca se guarda en la caché.
 */
export function useAccesoPlanilla() {
  return useMutation({ mutationFn: (planilla: string) => obtenerAccesoPlanillaApi(planilla) })
}

/** La vista previa es una mutación que devuelve el `Blob`: el PDF nunca se guarda en la caché. */
export function useVistaPreviaPlanilla() {
  return useMutation({
    mutationFn: ({ convocatoria, periodo }: { convocatoria: string; periodo: PeriodoPlanilla }) =>
      obtenerVistaPreviaPlanillaApi(convocatoria, periodo),
  })
}
