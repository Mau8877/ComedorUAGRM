import { apiClient } from '@/store'

import type {
  Finalista,
  FinalistasParams,
  NotificarFinalistasRequest,
  RegistrarEntregaRequest,
  ResultadoNotificacion,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient
// * compartido. El listado NUNCA manda `sort` (el orden es fijo, por posición) y solo acepta
// * `filter[convocatoriaId]`: el backend rechaza cualquier otro con ERR_FIN_05.

export interface PaginaFinalistas {
  data: Finalista[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

export async function listarFinalistasApi(params: FinalistasParams): Promise<PaginaFinalistas> {
  const query: Record<string, string | number> = {
    'filter[convocatoriaId]': params.convocatoriaId,
    page: params.page,
    pageSize: params.pageSize,
  }
  if (params.search) query.search = params.search

  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<PaginaFinalistas>('/finalistas', { params: query })
  return respuesta.data
}

export async function notificarFinalistasApi(pedido: NotificarFinalistasRequest): Promise<ResultadoNotificacion> {
  const respuesta = await apiClient.post<ResultadoNotificacion>('/finalistas/notificaciones', pedido)
  return respuesta.data
}

export async function registrarEntregaApi(codigo: string, pedido: RegistrarEntregaRequest): Promise<Finalista> {
  const respuesta = await apiClient.patch<Finalista>(`/finalistas/${codigo}/entrega`, pedido)
  return respuesta.data
}
