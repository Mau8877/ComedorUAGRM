import { apiClient } from '@/store'

import type { ActualizarPerfilRequest, CambiarContrasenaRequest } from '../schemas'
import type { Perfil } from '../types'

// * Funciones sin hook: las usan las mutations/queries de esta carpeta. La sesión viaja en
// * cookies HttpOnly: ninguna manda ni guarda tokens (ni siquiera `cambiar-contrasena`: el
// * navegador manda la cookie del refresh token a /auth y el backend conserva esa sesión).

export async function obtenerPerfilApi(): Promise<Perfil> {
  const respuesta = await apiClient.get<Perfil>('/perfil')
  return respuesta.data
}

/** Solo viaja lo que cambió; el CI nunca. */
export async function actualizarPerfilApi(request: ActualizarPerfilRequest): Promise<Perfil> {
  const respuesta = await apiClient.patch<Perfil>('/perfil', request)
  return respuesta.data
}

/** El backend cierra las sesiones de los demás dispositivos y deja abierta la actual. */
export async function cambiarContrasenaApi(request: CambiarContrasenaRequest): Promise<void> {
  await apiClient.post('/auth/cambiar-contrasena', request)
}
