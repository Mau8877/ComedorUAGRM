import { apiClient } from '@/store'

import type {
  ConvocatoriaPriorizable,
  FiltrosLista,
  ListaPriorizadaParams,
  PostulacionPriorizada,
  ResumenListaPriorizada,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient
// * compartido. La lista NUNCA manda `sort`: el orden es fijo (puntaje de mayor a menor) y el
// * backend rechaza cualquier `sort` con ERR_PRI_02.

export interface PaginaListaPriorizada {
  data: PostulacionPriorizada[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

export async function listarConvocatoriasApi(): Promise<ConvocatoriaPriorizable[]> {
  const respuesta = await apiClient.get<ConvocatoriaPriorizable[]>('/lista-priorizada/convocatorias')
  return respuesta.data
}

export async function obtenerResumenApi(convocatoriaId: string): Promise<ResumenListaPriorizada> {
  const respuesta = await apiClient.get<ResumenListaPriorizada>('/lista-priorizada/resumen', {
    params: { 'filter[convocatoriaId]': convocatoriaId },
  })
  return respuesta.data
}

export async function listarListaPriorizadaApi(params: ListaPriorizadaParams): Promise<PaginaListaPriorizada> {
  const query: Record<string, string | number> = {
    'filter[convocatoriaId]': params.convocatoriaId,
    page: params.page,
    pageSize: params.pageSize,
  }
  if (params.carreraId) query['filter[carreraId]'] = params.carreraId
  if (params.search) query.search = params.search

  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<PaginaListaPriorizada>('/lista-priorizada', { params: query })
  return respuesta.data
}

export interface PdfExportado {
  archivo: Blob
  nombre: string
}

/** El nombre que manda el servidor en `Content-Disposition`; si no viene, uno genérico. */
export function nombreDeArchivo(contentDisposition: unknown): string {
  const coincidencia = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(String(contentDisposition ?? ''))
  return coincidencia ? decodeURIComponent(coincidencia[1]) : 'lista-priorizada.pdf'
}

/** Exporta la lista completa (no la página) con los mismos filtros de la pantalla. */
export async function exportarListaApi(filtros: FiltrosLista): Promise<PdfExportado> {
  const cuerpo: Record<string, string> = { convocatoriaId: filtros.convocatoriaId }
  if (filtros.carreraId) cuerpo.carreraId = filtros.carreraId
  if (filtros.search) cuerpo.search = filtros.search

  const respuesta = await apiClient.post<Blob>('/lista-priorizada/exportaciones', cuerpo, { responseType: 'blob' })
  return { archivo: respuesta.data, nombre: nombreDeArchivo(respuesta.headers['content-disposition']) }
}
