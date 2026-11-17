import { apiClient } from '@/store'

import type { AsistenciaBecado, AsistenciasParams, MiBeca, Pagina } from '../types'

// * Solo lectura (RF-19): la beca es siempre la del usuario de la sesión, el cliente no manda ningún identificador.
// * Con `meta`, el apiClient devuelve { data, meta }.

export async function obtenerMiBecaApi(): Promise<MiBeca> {
  const respuesta = await apiClient.get<MiBeca>('/mi-beca')
  return respuesta.data
}

export async function listarAsistenciasApi(params: AsistenciasParams): Promise<Pagina<AsistenciaBecado>> {
  const consulta: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.desde) consulta['filter[desde]'] = params.desde
  if (params.hasta) consulta['filter[hasta]'] = params.hasta
  if (params.sort) consulta.sort = params.sort
  const respuesta = await apiClient.get<Pagina<AsistenciaBecado>>('/mi-beca/asistencias', { params: consulta })
  return respuesta.data
}
