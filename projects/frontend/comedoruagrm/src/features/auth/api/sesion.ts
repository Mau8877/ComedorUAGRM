import { setUnauthorizedHandler } from '@/store'

import { refreshApi } from './authApi'

// * Renovación de la sesión ante un 401. El token de acceso vive en una cookie
// * HttpOnly y vence a los 15 minutos; el refresh token (otra cookie) permite
// * pedir uno nuevo con POST /auth/refresh sin cuerpo.

/** Rutas donde un 401 es un error del formulario ("credenciales incorrectas"), no una sesión vencida. */
const RUTAS_DE_ACCESO = ['/auth/login', '/auth/registro', '/auth/refresh', '/auth/logout']

export function esRutaDeAcceso(url: string | undefined): boolean {
  return RUTAS_DE_ACCESO.some((ruta) => url?.includes(ruta))
}

// Una sola renovación a la vez: varias peticiones que fallan juntas con 401 esperan la misma.
let renovando: Promise<boolean> | null = null

/** `true` si el backend entregó una sesión nueva; `false` si el refresh token no sirve (o no hay). */
export function renovarSesion(): Promise<boolean> {
  renovando ??= refreshApi()
    .then(() => true)
    .catch(() => false)
    .finally(() => {
      renovando = null
    })
  return renovando
}

/**
 * Conecta el apiClient con la sesión: ante un 401 de una ruta protegida intenta
 * renovar y, si no se puede, avisa con `alPerderSesion` (quien monta la app
 * decide cómo llevar al usuario a /login). Se llama una vez, al armar el router.
 */
export function instalarManejadorDeSesion({ alPerderSesion }: { alPerderSesion: () => void }) {
  setUnauthorizedHandler(async (config) => {
    if (esRutaDeAcceso(config.url)) return false
    const renovada = await renovarSesion()
    if (!renovada) alPerderSesion()
    return renovada
  })
}
