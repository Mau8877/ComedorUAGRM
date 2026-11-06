import { apiClient } from '@/store'

import type { Carrera } from './types'

// * El catálogo de carreras lo usan el registro (features/auth) y la lista priorizada
// * (features/priorizacion): por eso vive acá y no en una sola feature.
export async function listarCarrerasApi(): Promise<Carrera[]> {
  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<{ data: Carrera[] }>('/carreras', { params: { pageSize: 200 } })
  return respuesta.data.data
}
