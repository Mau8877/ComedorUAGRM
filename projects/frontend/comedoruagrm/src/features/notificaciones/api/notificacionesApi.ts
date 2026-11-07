import { apiClient } from '@/store'

// Contrato de docs/pendientes/NOTIFICACIONES_WEB.md. `tipo` llega en mayúscula (SUCCESS, INFO, WARNING, ERROR).
export interface NotificacionApi {
  id: string
  tipo: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR'
  titulo: string
  detalle: string | null
  fecha: string
  leida: boolean
}

export interface PaginaNotificaciones {
  data: NotificacionApi[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

export async function listarNotificacionesApi(page: number, pageSize: number): Promise<PaginaNotificaciones> {
  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<PaginaNotificaciones>('/notificaciones/me', {
    params: { page, pageSize, sort: '-fecha' },
  })
  return respuesta.data
}

export async function obtenerResumenNotificacionesApi(): Promise<{ noLeidas: number }> {
  const respuesta = await apiClient.get<{ noLeidas: number }>('/notificaciones/me/resumen')
  return respuesta.data
}

export async function marcarLeidaApi(id: string): Promise<void> {
  await apiClient.post(`/notificaciones/${id}/leer`)
}

export async function marcarTodasLeidasApi(): Promise<void> {
  await apiClient.post('/notificaciones/leer-todo')
}

export async function eliminarTodasApi(): Promise<void> {
  await apiClient.delete('/notificaciones')
}
