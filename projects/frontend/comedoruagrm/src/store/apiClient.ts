import type { InternalAxiosRequestConfig } from 'axios'

import { createBaseApi } from './createBaseApi'

// Sobre estándar de todas las respuestas del backend -- ver
// .claude/rules/backend/RESPONSES_BACKEND.md. `error` solo viene presente
// cuando status es "failed"; `meta` solo en listados paginados.
export interface ApiEnvelope<T> {
  status: 'success' | 'failed'
  data: T
  message: string
  timestamp: string
  error?: string
  meta?: {
    page: number
    pageSize: number
    totalItems: number
    totalPages: number
  }
}

export class ApiError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * Cliente HTTP único de la app -- todo lo que llame a la API del backend
 * pasa por acá, nunca por una instancia de axios propia.
 *
 * * Construido sobre createBaseApi (logging genérico) y le suma el
 * * desempaquetado del sobre {status,data,message,error,timestamp,meta}:
 * * en éxito, `response.data` queda como el `data` real del backend (y
 * * `meta` si vino); en error, se rechaza con un ApiError tipado en vez de
 * * un AxiosError crudo.
 */
export const apiClient = createBaseApi({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // La sesión viaja en cookies HttpOnly que pone el backend (el JavaScript nunca ve los tokens).
  withCredentials: true,
})

// * La sesión usa el JWT propio del backend (ver SEGURIDAD_AUTH_BACKEND.md). En
// * la web viaja en cookies HttpOnly (por eso `withCredentials`): el navegador
// * las manda solo y la app no guarda ningún token. El proveedor de token de
// * abajo es opcional y la web no lo usa (queda por si un cliente sin cookies
// * necesita `Authorization: Bearer`). Lo que sí registra el flujo de
// * autenticación es qué hacer ante un 401 (renovar con `/auth/refresh` o, si
// * no se puede, llevar a /login), así `store/` sigue sin depender de ninguna
// * feature.
type AuthTokenProvider = () => Promise<string | null>

let authTokenProvider: AuthTokenProvider | null = null
/**
 * Se llama ante un 401 de una petición. Devuelve `true` si logró renovar la
 * sesión (entonces la petición original se reintenta una sola vez) y
 * `false` si no hay nada que hacer (entonces el error sigue su camino).
 */
type UnauthorizedHandler = (config: InternalAxiosRequestConfig) => Promise<boolean>

let unauthorizedHandler: UnauthorizedHandler | null = null

// Marca de una petición ya reintentada tras renovar la sesión: evita reintentar en bucle.
type ReintentableConfig = InternalAxiosRequestConfig & { _reintentada?: boolean }

export function setAuthTokenProvider(provider: AuthTokenProvider | null) {
  authTokenProvider = provider
}

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use(async (config) => {
  const token = authTokenProvider ? await authTokenProvider() : null
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// * Un error de una descarga también llega como Blob: se lee su texto para recuperar el sobre de error
// * ({ error, message }) y poder rechazar con un ApiError, igual que cualquier otra petición.
async function leerSobreDeError(datos: unknown): Promise<ApiEnvelope<unknown> | undefined> {
  if (datos instanceof Blob) {
    try {
      return JSON.parse(await datos.text()) as ApiEnvelope<unknown>
    } catch {
      return undefined
    }
  }
  return datos as ApiEnvelope<unknown> | undefined
}

apiClient.interceptors.response.use(
  (response) => {
    // Una descarga (responseType 'blob') es el archivo mismo, no un sobre: se deja tal cual.
    if (response.config.responseType === 'blob') return response
    const envelope = response.data as ApiEnvelope<unknown>
    response.data = envelope.meta ? { data: envelope.data, meta: envelope.meta } : envelope.data
    return response
  },
  async (error) => {
    const config = error.config as ReintentableConfig | undefined
    if (error.response?.status === 401 && unauthorizedHandler && config && !config._reintentada) {
      config._reintentada = true
      if (await unauthorizedHandler(config)) {
        return apiClient.request(config)
      }
    }
    const envelope = await leerSobreDeError(error.response?.data)
    if (envelope?.error) {
      return Promise.reject(new ApiError(envelope.error, envelope.message))
    }
    return Promise.reject(error)
  },
)
