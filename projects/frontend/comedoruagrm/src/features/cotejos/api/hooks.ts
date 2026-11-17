import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ListadoParams, RegistrarCotejoRequest } from '../types'
import { cotejosKeys } from './keys'
import { listarCotejosApi, listarRegistrosApi, obtenerDatosCotejoApi, registrarCotejoApi } from './cotejosApi'

/** La planilla y cuántos registros digitales hay en su periodo; no consulta sin planilla elegida. */
export function useDatosCotejo(planilla: string | undefined) {
  return useQuery({
    queryKey: cotejosKeys.datos(planilla as string),
    queryFn: () => obtenerDatosCotejoApi(planilla as string),
    enabled: Boolean(planilla),
  })
}

/** Una página de registros digitales del periodo; conserva la anterior mientras llega la siguiente. */
export function useRegistrosDigitales(planilla: string | undefined, params: ListadoParams) {
  return useQuery({
    queryKey: cotejosKeys.registrosDe(planilla as string, params),
    queryFn: () => listarRegistrosApi(planilla as string, params),
    enabled: Boolean(planilla),
    placeholderData: keepPreviousData,
  })
}

/** Una página de los cotejos ya registrados de la planilla. */
export function useCotejos(planilla: string | undefined, params: ListadoParams) {
  return useQuery({
    queryKey: cotejosKeys.list(planilla as string, params),
    queryFn: () => listarCotejosApi(planilla as string, params),
    enabled: Boolean(planilla),
    placeholderData: keepPreviousData,
  })
}

/** Registra el cotejo (CU11) y refresca el historial de cotejos. No toca los registros digitales. */
export function useRegistrarCotejo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ planilla, cotejo }: { planilla: string; cotejo: RegistrarCotejoRequest }) =>
      registrarCotejoApi(planilla, cotejo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cotejosKeys.lists() })
    },
  })
}
