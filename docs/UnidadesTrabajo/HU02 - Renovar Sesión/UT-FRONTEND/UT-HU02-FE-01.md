# UT-HU02-FE-01 — `refreshSession()` + interceptores de `apiClient`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-FE-01` |
| **Nombre** | `refreshSession()`, interceptor de `Authorization`, y reintento automático en `401` |
| **Historia** | HU02-FE |
| **Depende de** | `src/store/apiClient.ts`, `store/useAuthStore.ts` (ya existen, de HU01-FE) |

## Antes de implementar

Leer [TANSTACK_QUERY_FRONTEND.md](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md#cliente-http-centralizado-ya-implementado).

## Solicitud de negocio

Para que la sesión se renueve sola cuando el access token expira, el
cliente HTTP necesita mandar el token en cada request y, si el backend lo
rechaza por vencido, intentar renovarlo una sola vez y reintentar la
request original sin que el usuario lo note.

## Objetivo

Agregar a `apiClient` (archivo existente) un interceptor de request que
adjunte `Authorization`, y uno de response que reintente tras un refresh
exitoso; y crear `refreshSession()` como función reutilizable tanto por
ese interceptor como por el guard (UT-HU02-FE-02).

## Alcance

### `features/auth/api/refreshSession.ts` (nuevo)

Función plana (no un hook — la consume un interceptor de axios, que no es
un componente React):

```ts
export async function refreshSession(): Promise<RefreshResponse> {
  const { data } = await apiClient.post<RefreshResponse>('/api/v1/auth/refresh')
  return data
}
```

`RefreshResponse` (tipo nuevo, `features/auth/types/auth.types.ts`,
**editar** el archivo existente de HU01-FE):

```ts
export interface RefreshResponse {
  accessToken: string
  usuario: UsuarioSesion
}
```

### `apiClient` — **editar** `src/store/apiClient.ts`

Interceptor de **request**: adjunta el header si hay un access token en
memoria.

```ts
apiClient.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})
```

Interceptor de **response** (en el mismo archivo, se agrega a la cadena
que ya desempaqueta el sobre `{status,data,...}` — no se reemplaza esa
lógica, se agrega antes de que el error se convierta en `ApiError`):

```ts
let refreshEnProgreso: Promise<RefreshResponse> | null = null

apiClient.interceptors.response.use(
  (response) => { /* ...ya existente, sin cambios... */ },
  async (error) => {
    const originalRequest = error.config
    const esRefreshEndpoint = originalRequest?.url === '/api/v1/auth/refresh'

    if (error.response?.status === 401 && !originalRequest._retry && !esRefreshEndpoint) {
      originalRequest._retry = true
      try {
        refreshEnProgreso ??= refreshSession().finally(() => { refreshEnProgreso = null })
        const data = await refreshEnProgreso
        useAuthStore.getState().setSession(data.accessToken, data.usuario)
        return apiClient(originalRequest)
      } catch {
        useAuthStore.getState().clearSession() // agregado recién por HU-03 -- si esta UT se
        // implementa antes de que exista, ver nota abajo
      }
    }

    /* ...manejo de ApiError ya existente, sin cambios... */
  },
)
```

> **Nota de orden:** `useAuthStore.getState().clearSession` todavía no
> existe si HU-03 no se implementó antes que esta UT (`clearSession` es
> alcance de [HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)).
> Si HU-03 no está implementada todavía, esta UT usa en su lugar
> `useAuthStore.setState({ accessToken: null, usuario: null, isAuthenticated: false })`
> directo (válido porque Zustand expone `setState` siempre), y se
> reemplaza por `clearSession()` en la propia UT de HU-03 cuando esta
> exista (queda declarado ahí como archivo a **editar**).
- `refreshEnProgreso` deduplica: si varias requests fallan con `401` casi
  al mismo tiempo, solo se dispara **un** `refreshSession()`, todas
  esperan la misma promesa.
- `_retry` evita loop infinito si el propio reintento vuelve a fallar con
  `401`.

### Archivos esperados (crear/editar)

- Crear: `features/auth/api/refreshSession.ts`
- Editar: `features/auth/types/auth.types.ts` (agregar `RefreshResponse`)
- Editar: `src/store/apiClient.ts`

## Fuera de alcance (no tocar)

- El guard `_authenticated`/ruta `/login` (UT-HU02-FE-02 — ahí se **usa**
  `refreshSession` para el intento al recargar la página)
- `clearSession()` real (HU-03 — ver nota de orden arriba)
- Cola de reintentos más allá del dedupe simple de un refresh en vuelo

## Resultado esperado

Toda request de `apiClient` manda `Authorization` si hay sesión; un `401`
dispara un único intento de refresh + reintento automático de la request
original.

## Validación

1. Con sesión activa, cualquier request de `apiClient` lleva el header
   `Authorization: Bearer <token>`.
2. Mockear una respuesta `401` seguida de un `refreshSession()` exitoso →
   la request original se reintenta y resuelve con éxito, sin que el
   código que la llamó vea el error.
3. Disparar 3 requests en paralelo que fallan con `401` al mismo tiempo →
   `refreshSession` se invoca **una sola vez** (dedupe).
4. Si `refreshSession()` también falla → la sesión se limpia (según la
   nota de orden) y el error original se propaga como `ApiError`/rechazo,
   no queda una promesa colgada.
5. Una request al propio `/api/v1/auth/refresh` que falla con `401` **no**
   dispara otro refresh (evita loop infinito).
