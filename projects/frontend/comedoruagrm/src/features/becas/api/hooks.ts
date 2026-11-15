import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ParamsPadron } from '../types'
import {
  darDeAltaApi,
  darDeBajaApi,
  listarPadronApi,
  obtenerCandidatoApi,
  obtenerResumenApi,
  reemplazarApi,
  suspenderApi,
} from './becasApi'
import { becasKeys } from './keys'

/** El resumen del padrón de una convocatoria; sin código no consulta. */
export function useResumenPadron(convocatoria: string | undefined) {
  return useQuery({
    queryKey: becasKeys.resumen(convocatoria as string),
    queryFn: () => obtenerResumenApi(convocatoria as string),
    enabled: Boolean(convocatoria),
  })
}

/** Una página del padrón; conserva la anterior mientras llega la siguiente. Sin código no consulta. */
export function usePadron(convocatoria: string | undefined, params: ParamsPadron, enabled = true) {
  return useQuery({
    queryKey: becasKeys.list(convocatoria as string, params),
    queryFn: () => listarPadronApi(convocatoria as string, params),
    enabled: Boolean(convocatoria) && enabled,
    placeholderData: keepPreviousData,
  })
}

/**
 * El candidato de un reemplazo. Nunca se reutiliza una respuesta vieja (`gcTime: 0`, sin reintentos): quien confirma
 * tiene que ver al postulante que el backend propone ahora.
 */
export function useCandidatoReemplazo(beca: string | undefined, habilitado: boolean) {
  return useQuery({
    queryKey: becasKeys.candidato(beca as string),
    queryFn: () => obtenerCandidatoApi(beca as string),
    enabled: Boolean(beca) && habilitado,
    gcTime: 0,
    retry: false,
  })
}

// Toda mutación del padrón vuelve a pedir listados, resúmenes y candidatos: un cambio mueve los tres.
function useInvalidarBecas() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: becasKeys.all })
}

export function useDarDeAlta() {
  const invalidar = useInvalidarBecas()
  return useMutation({
    mutationFn: (convocatoria: string) => darDeAltaApi(convocatoria),
    onSuccess: invalidar,
  })
}

export function useDarDeBaja() {
  const invalidar = useInvalidarBecas()
  return useMutation({
    mutationFn: ({ beca, motivo }: { beca: string; motivo: string }) => darDeBajaApi(beca, motivo),
    onSuccess: invalidar,
  })
}

export function useSuspender() {
  const invalidar = useInvalidarBecas()
  return useMutation({
    mutationFn: ({ beca, motivo }: { beca: string; motivo: string }) => suspenderApi(beca, motivo),
    onSuccess: invalidar,
  })
}

export function useReemplazar() {
  const invalidar = useInvalidarBecas()
  return useMutation({
    mutationFn: ({ beca, postulanteCodigo }: { beca: string; postulanteCodigo: string }) =>
      reemplazarApi(beca, postulanteCodigo),
    onSuccess: invalidar,
  })
}
