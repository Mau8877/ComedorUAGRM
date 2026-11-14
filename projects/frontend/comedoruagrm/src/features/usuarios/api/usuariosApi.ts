import { apiClient } from '@/store'

import type { EditarUsuarioPersonalRequest, CrearUsuarioPersonalRequest } from '../schemas'
import type { UsuarioCreado, UsuarioPersonal, UsuariosListParams } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van
// * por el apiClient compartido (no se crea otra instancia de axios).

export interface PaginaUsuarios {
  data: UsuarioPersonal[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

export async function listarUsuariosApi(params: UsuariosListParams): Promise<PaginaUsuarios> {
  const query: Record<string, string | number | boolean> = { page: params.page, pageSize: params.pageSize }
  if (params.search) query.search = params.search
  if (params.rol) query['filter[rol]'] = params.rol
  if (params.activo !== undefined) query['filter[activo]'] = params.activo
  if (params.sort) query.sort = params.sort

  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<PaginaUsuarios>('/usuarios', { params: query })
  return respuesta.data
}

export async function crearUsuarioApi(request: CrearUsuarioPersonalRequest): Promise<UsuarioCreado> {
  const respuesta = await apiClient.post<UsuarioCreado>('/usuarios', request)
  return respuesta.data
}

/** PATCH: solo viajan los campos que cambiaron. */
export async function editarUsuarioApi(
  codigo: string,
  cambios: Partial<EditarUsuarioPersonalRequest>,
): Promise<UsuarioPersonal> {
  const respuesta = await apiClient.patch<UsuarioPersonal>(`/usuarios/${codigo}`, cambios)
  return respuesta.data
}

export async function cambiarRolApi(codigo: string, rol: string): Promise<UsuarioPersonal> {
  const respuesta = await apiClient.post<UsuarioPersonal>(`/usuarios/${codigo}/cambiar-rol`, { rol })
  return respuesta.data
}

export async function desactivarUsuarioApi(codigo: string): Promise<UsuarioPersonal> {
  const respuesta = await apiClient.post<UsuarioPersonal>(`/usuarios/${codigo}/desactivar`)
  return respuesta.data
}

export async function reactivarUsuarioApi(codigo: string): Promise<UsuarioPersonal> {
  const respuesta = await apiClient.post<UsuarioPersonal>(`/usuarios/${codigo}/reactivar`)
  return respuesta.data
}
