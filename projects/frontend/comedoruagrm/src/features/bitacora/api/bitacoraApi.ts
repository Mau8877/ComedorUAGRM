import { apiClient } from '@/store'

import type { BitacoraParams, Pagina, RegistroBitacora } from '../types'

// * Solo lectura: la bitácora no se edita ni se elimina (RF-20). Con `meta`, el apiClient devuelve { data, meta }.

export async function listarBitacoraApi(params: BitacoraParams): Promise<Pagina<RegistroBitacora>> {
  const consulta: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.search) consulta.search = params.search
  if (params.accion) consulta['filter[accion]'] = params.accion
  if (params.desde) consulta['filter[desde]'] = params.desde
  if (params.hasta) consulta['filter[hasta]'] = params.hasta
  if (params.sort) consulta.sort = params.sort
  const respuesta = await apiClient.get<Pagina<RegistroBitacora>>('/bitacora', { params: consulta })
  return respuesta.data
}

export async function listarAccionesApi(): Promise<string[]> {
  const respuesta = await apiClient.get<string[]>('/bitacora/acciones')
  return respuesta.data
}
