import { apiClient, ApiError } from '@/store'

import type { RegistroRequest, RestablecerContrasenaRequest } from '../schemas'
import type { Carrera, Sesion, Usuario } from '../types'

// * Funciones sin hook: las usan las mutations de useRegistro/useLogin/
// * useCerrarSesion y el manejador de 401 (sesion.ts). La sesión viaja en
// * cookies HttpOnly que pone el backend, así que ninguna manda ni guarda
// * tokens (refresh y logout no llevan cuerpo: el navegador manda la cookie).

/** Códigos de error que significan "no hay una sesión utilizable" (ver docs/errors/auth). */
const ERRORES_DE_SESION = ['ERR_AUTH_01', 'ERR_AUTH_02', 'ERR_AUTH_03', 'ERR_AUTH_06']

export function esErrorDeSesion(error: unknown): boolean {
  return error instanceof ApiError && ERRORES_DE_SESION.includes(error.code)
}

export async function listarCarrerasApi(): Promise<Carrera[]> {
  // Con `meta` el apiClient devuelve { data, meta }.
  const respuesta = await apiClient.get<{ data: Carrera[] }>('/carreras', { params: { pageSize: 200 } })
  return respuesta.data.data
}

/** `creado` es false si la misma petición ya se había procesado (el backend responde 200). */
export async function registrarApi(request: RegistroRequest): Promise<{ sesion: Sesion; creado: boolean }> {
  const respuesta = await apiClient.post<Sesion>('/auth/registro', request)
  return { sesion: respuesta.data, creado: respuesta.status === 201 }
}

export async function loginApi(correo: string, contrasena: string): Promise<Sesion> {
  const respuesta = await apiClient.post<Sesion>('/auth/login', { correo, contrasena })
  return respuesta.data
}

export async function refreshApi(): Promise<Sesion> {
  const respuesta = await apiClient.post<Sesion>('/auth/refresh')
  return respuesta.data
}

export async function logoutApi(): Promise<void> {
  await apiClient.post('/auth/logout')
}

/**
 * Pide el enlace de recuperación (CU15, flujo A). El backend responde siempre igual exista o no la
 * cuenta: acá no se lee nada de la respuesta, para no poder distinguir los casos.
 */
export async function solicitarRecuperacionApi(correo: string): Promise<void> {
  await apiClient.post('/auth/recuperar-contrasena', { correo })
}

/** Define la contraseña nueva con el token del enlace (CU15, flujo B). El backend cierra todas las sesiones. */
export async function restablecerContrasenaApi(request: RestablecerContrasenaRequest): Promise<void> {
  await apiClient.post('/auth/restablecer-contrasena', request)
}

/** `null` si no hay sesión (sin cookie, vencida o cuenta desactivada); cualquier otro error se propaga. */
export async function usuarioActualApi(): Promise<Usuario | null> {
  try {
    const respuesta = await apiClient.get<Usuario>('/auth/usuario-actual')
    return respuesta.data
  } catch (error) {
    if (esErrorDeSesion(error)) return null
    throw error
  }
}
