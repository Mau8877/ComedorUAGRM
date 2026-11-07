import { apiClient } from '@/store'

import type { Comunicado, PublicarComunicadoRequest } from '../types'

export interface PaginaComunicados {
  data: Comunicado[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

export async function publicarComunicadoApi(pedido: PublicarComunicadoRequest): Promise<Comunicado> {
  const respuesta = await apiClient.post<Comunicado>('/comunicados', pedido)
  return respuesta.data
}

export async function listarComunicadosApi(params: { page: number; pageSize: number }): Promise<PaginaComunicados> {
  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<PaginaComunicados>('/comunicados', { params })
  return respuesta.data
}
