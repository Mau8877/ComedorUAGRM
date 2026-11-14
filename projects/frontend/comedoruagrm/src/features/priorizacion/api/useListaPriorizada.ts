import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'

import type { FiltrosLista, ListaPriorizadaParams } from '../types'
import { priorizacionKeys } from './keys'
import {
  exportarListaApi,
  listarConvocatoriasApi,
  listarListaPriorizadaApi,
  obtenerResumenApi,
} from './listaPriorizadaApi'

/** Convocatorias que se pueden consultar (la más reciente primero). */
export function useConvocatoriasPriorizables() {
  return useQuery({ queryKey: priorizacionKeys.convocatorias(), queryFn: listarConvocatoriasApi })
}

/** Cuántas postulaciones hay calificadas y cuántas pendientes; solo corre con una convocatoria elegida. */
export function useResumenLista(convocatoriaId: string | undefined) {
  return useQuery({
    queryKey: priorizacionKeys.resumen(convocatoriaId ?? ''),
    queryFn: () => obtenerResumenApi(convocatoriaId as string),
    enabled: Boolean(convocatoriaId),
  })
}

/** Una página de la lista; conserva la anterior mientras llega la siguiente. */
export function useListaPriorizada(params: ListaPriorizadaParams | undefined, habilitado = true) {
  return useQuery({
    queryKey: priorizacionKeys.list(params as ListaPriorizadaParams),
    queryFn: () => listarListaPriorizadaApi(params as ListaPriorizadaParams),
    enabled: Boolean(params) && habilitado,
    placeholderData: keepPreviousData,
  })
}

/** El PDF no se guarda en la caché: es una mutación que devuelve el archivo a quien la llama. */
export function useExportarLista() {
  return useMutation({ mutationFn: (filtros: FiltrosLista) => exportarListaApi(filtros) })
}
