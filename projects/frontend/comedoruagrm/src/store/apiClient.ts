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
})

// * La sesión es de Firebase Auth (ver SEGURIDAD_AUTH_BACKEND.md): el cliente
// * manda el ID token en `Authorization: Bearer` y no usa cookies (por eso
// * no hay `withCredentials`). Este módulo no importa el SDK de Firebase: el
// * flujo de autenticación registra acá cómo obtener el token
// * (`getIdToken()` del SDK, que lo renueva solo) y qué hacer ante un 401
// * (cerrar la sesión de Firebase y llevar a /login), así `store/` sigue sin
// * depender de ninguna feature.
type AuthTokenProvider = () => Promise<string | null>

let authTokenProvider: AuthTokenProvider | null = null
let unauthorizedHandler: (() => void) | null = null

export function setAuthTokenProvider(provider: AuthTokenProvider | null) {
  authTokenProvider = provider
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use(async (config) => {
  const token = authTokenProvider ? await authTokenProvider() : null
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>
    response.data = envelope.meta ? { data: envelope.data, meta: envelope.meta } : envelope.data
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      unauthorizedHandler?.()
    }
    const envelope = error.response?.data as ApiEnvelope<unknown> | undefined
    if (envelope?.error) {
      return Promise.reject(new ApiError(envelope.error, envelope.message))
    }
    return Promise.reject(error)
  },
)
