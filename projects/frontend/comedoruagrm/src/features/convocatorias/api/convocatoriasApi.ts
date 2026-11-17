import { apiClient } from '@/store'

import type { ConvocatoriaRequest, GestionRequest } from '../schemas'
import type {
  Convocatoria,
  ConvocatoriaDetalle,
  ConvocatoriasListParams,
  GestionAcademica,
  GestionesListParams,
  Pagina,
  TipoDocumento,
  TiposDocumentoListParams,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido (no se
// * crea otra instancia de axios). Con `meta`, el apiClient devuelve { data, meta }.

export async function listarConvocatoriasApi(params: ConvocatoriasListParams): Promise<Pagina<Convocatoria>> {
  const query: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.search) query.search = params.search
  if (params.estado) query['filter[estado]'] = params.estado
  if (params.gestion) query['filter[gestion]'] = params.gestion
  if (params.sort) query.sort = params.sort

  const respuesta = await apiClient.get<Pagina<Convocatoria>>('/convocatorias', { params: query })
  return respuesta.data
}

export async function obtenerConvocatoriaApi(codigo: string): Promise<ConvocatoriaDetalle> {
  const respuesta = await apiClient.get<ConvocatoriaDetalle>(`/convocatorias/${codigo}`)
  return respuesta.data
}

export async function crearConvocatoriaApi(request: ConvocatoriaRequest): Promise<ConvocatoriaDetalle> {
  const respuesta = await apiClient.post<ConvocatoriaDetalle>('/convocatorias', request)
  return respuesta.data
}

/** PUT: viaja la convocatoria completa; los opcionales en `null` se vacían. */
export async function editarConvocatoriaApi(codigo: string, request: ConvocatoriaRequest): Promise<ConvocatoriaDetalle> {
  const respuesta = await apiClient.put<ConvocatoriaDetalle>(`/convocatorias/${codigo}`, request)
  return respuesta.data
}

export async function abrirConvocatoriaApi(codigo: string): Promise<ConvocatoriaDetalle> {
  const respuesta = await apiClient.post<ConvocatoriaDetalle>(`/convocatorias/${codigo}/abrir`)
  return respuesta.data
}

export async function cerrarConvocatoriaApi(codigo: string): Promise<ConvocatoriaDetalle> {
  const respuesta = await apiClient.post<ConvocatoriaDetalle>(`/convocatorias/${codigo}/cerrar`)
  return respuesta.data
}

export async function eliminarConvocatoriaApi(codigo: string): Promise<void> {
  await apiClient.delete(`/convocatorias/${codigo}`)
}

export async function listarGestionesApi(params: GestionesListParams): Promise<Pagina<GestionAcademica>> {
  const query: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.search) query.search = params.search
  if (params.sort) query.sort = params.sort

  const respuesta = await apiClient.get<Pagina<GestionAcademica>>('/gestiones-academicas', { params: query })
  return respuesta.data
}

export async function crearGestionApi(request: GestionRequest): Promise<GestionAcademica> {
  const respuesta = await apiClient.post<GestionAcademica>('/gestiones-academicas', request)
  return respuesta.data
}

/** PUT: viaja la gestión completa. */
export async function editarGestionApi(codigo: string, request: GestionRequest): Promise<GestionAcademica> {
  const respuesta = await apiClient.put<GestionAcademica>(`/gestiones-academicas/${codigo}`, request)
  return respuesta.data
}

export async function listarTiposDocumentoApi(params: TiposDocumentoListParams): Promise<Pagina<TipoDocumento>> {
  const query: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.search) query.search = params.search
  if (params.sort) query.sort = params.sort

  const respuesta = await apiClient.get<Pagina<TipoDocumento>>('/tipos-documento', { params: query })
  return respuesta.data
}
