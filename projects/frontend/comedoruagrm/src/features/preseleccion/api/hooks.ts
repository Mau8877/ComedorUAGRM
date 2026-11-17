import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { EstadoLote, ParamsHistorial } from '../types'
import {
  guardarModeloApi,
  listarVersionesApi,
  obtenerLoteApi,
  obtenerPrecalificacionApi,
  obtenerResumenReprocesamientoApi,
  obtenerVigenteApi,
  reprocesarApi,
} from './preseleccionApi'
import type { GuardarModeloPayload } from './preseleccionApi'
import { preseleccionKeys } from './keys'

/** Cada cuántos milisegundos se pide el avance de un lote en curso. */
export const INTERVALO_AVANCE_MS = 2000

const EN_CURSO: EstadoLote[] = ['PENDIENTE', 'EN_PROCESO']

export function estaEnCurso(estado: EstadoLote | undefined): boolean {
  return estado !== undefined && EN_CURSO.includes(estado)
}

/** La versión vigente (`null` si no hay ninguna). */
export function useModeloVigente() {
  return useQuery({ queryKey: preseleccionKeys.vigente(), queryFn: obtenerVigenteApi })
}

/** Una página del historial de versiones; conserva la anterior mientras llega la siguiente. */
export function useHistorialVersiones(params: ParamsHistorial) {
  return useQuery({
    queryKey: preseleccionKeys.list(params),
    queryFn: () => listarVersionesApi(params),
    placeholderData: keepPreviousData,
  })
}

/** Lo que se muestra antes de reprocesar; sin código no consulta. */
export function useResumenReprocesamiento(convocatoria: string | undefined) {
  return useQuery({
    queryKey: preseleccionKeys.resumen(convocatoria as string),
    queryFn: () => obtenerResumenReprocesamientoApi(convocatoria as string),
    enabled: Boolean(convocatoria),
  })
}

/** El avance de un lote: se vuelve a pedir cada pocos segundos mientras sigue en curso. Sin código no consulta. */
export function useLoteReprocesamiento(codigo: string | undefined) {
  return useQuery({
    queryKey: preseleccionKeys.lote(codigo as string),
    queryFn: () => obtenerLoteApi(codigo as string),
    enabled: Boolean(codigo),
    refetchInterval: (consulta) => (estaEnCurso(consulta.state.data?.estado) ? INTERVALO_AVANCE_MS : false),
  })
}

/** Guardar una versión mueve la vigente, el historial y lo que falta por recalcular de cada convocatoria. */
export function useGuardarModelo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: GuardarModeloPayload) => guardarModeloApi(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: preseleccionKeys.vigente() })
      void queryClient.invalidateQueries({ queryKey: preseleccionKeys.lists() })
      void queryClient.invalidateQueries({ queryKey: preseleccionKeys.resumenes() })
    },
  })
}

/** Arranca el reprocesamiento; el resumen se vuelve a pedir para mostrar el lote en curso. */
export function useReprocesar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (convocatoria: string) => reprocesarApi(convocatoria),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: preseleccionKeys.resumenes() }),
  })
}

/** Al terminar un lote cambian los pendientes de recalcular: el resumen se vuelve a pedir. */
export function useRefrescarResumenes() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: preseleccionKeys.resumenes() })
}

/** La precalificación con IA de una postulación (CU03). Es una lectura: no cambia nada. */
export function usePrecalificacion(postulacion: string) {
  return useQuery({
    queryKey: preseleccionKeys.precalificacion(postulacion),
    queryFn: () => obtenerPrecalificacionApi(postulacion),
  })
}
