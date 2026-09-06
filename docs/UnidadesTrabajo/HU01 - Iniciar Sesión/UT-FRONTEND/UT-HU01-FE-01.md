# UT-HU01-FE-01 — Contratos TS + `useLoginMutation` + `withCredentials`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-01` |
| **Nombre** | Contratos TypeScript, `useLoginMutation` y `withCredentials` en `apiClient` |
| **Historia** | HU01-FE |
| **Depende de** | — |

## Antes de implementar

Leer [TANSTACK_QUERY_FRONTEND.md](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
completo, en particular la sección "Cliente HTTP centralizado".

## Solicitud de negocio

El frontend necesita un contrato tipado para `POST /api/v1/auth/login` y
que el cliente HTTP mande/reciba la cookie `HttpOnly` del refresh token
(ver [decisión de diseño](../README.md) del backend).

## Objetivo

Crear el scaffold de `features/auth/` (mismo formato que
`features/home/`), los tipos de request/response de login, el hook de
mutación, y habilitar `withCredentials` en `apiClient`.

## Alcance

### Scaffold de la feature — `features/auth/`

Mismas subcarpetas que `features/home/` (ver
[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature)):
`api/`, `components/`, `schemas/`, `screens/`, `tests/`, `types/`,
`index.ts`.

### Tipos — `features/auth/types/auth.types.ts`

```ts
export interface UsuarioSesion {
  codigo: string
  username: string
  correo: string
  roles: string[]
}

export interface LoginRequest {
  identificador: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  usuario: UsuarioSesion
}
```

- `roles` es un array (`string[]`) porque un usuario puede tener más de
  un rol activo — ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos).
  No hay un tipo `Rol` cerrado del lado del frontend (los nombres de rol
  son datos, no un enum fijo).
- `identificador` acepta tanto `username` como `correo` — el backend
  resuelve contra cualquiera de las dos columnas (ver
  [UT-BACKEND](../UT-BACKEND/UT-HU01-BE-01.md)).

### `apiClient` — habilitar `withCredentials`

Editar `src/store/apiClient.ts` (archivo existente, **no** crear una
instancia nueva de axios): agregar `withCredentials: true` al
`createBaseApi({...})` — necesario para que el browser envíe/acepte la
cookie `refreshToken` en requests cross-origin (dev: frontend en
`:5173`, backend en `:8080`).

```ts
export const apiClient = createBaseApi({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
})
```

### `useLoginMutation` — `features/auth/api/useLogin.ts`

```ts
export function useLoginMutation() {
  return useMutation({
    mutationFn: async (request: LoginRequest) => {
      const { data } = await apiClient.post<LoginResponse>('/api/v1/auth/login', request)
      return data
    },
  })
}
```

- No usa `queryKey` (es una mutación, no una query cacheada) — no hace
  falta un `api/keys.ts` para esta UT.
- El consumo del resultado (`onSuccess` guardando la sesión) queda para
  UT-HU01-FE-04, que orquesta form + mutation + store — esta UT solo deja
  el hook listo.

### Archivos esperados (crear/tocar)

- `features/auth/{api,components,schemas,screens,tests,types}/index.ts` (barrels vacíos, patrón `home`)
- `features/auth/index.ts`
- `features/auth/types/auth.types.ts`
- `features/auth/api/useLogin.ts`
- Editar: `src/store/apiClient.ts`

## Fuera de alcance (no tocar)

- `useAuthStore` (UT-HU01-FE-02)
- Formulario/schema (UT-HU01-FE-03)
- Interceptor de `Authorization` header (follow-up, ver [README](../README.md#follow-ups-no-se-implementan-en-estas-ut))

## Resultado esperado

`useLoginMutation()` disponible para usar desde la screen; `apiClient`
manda/acepta cookies en requests cross-origin.

## Validación

1. Inspeccionar la request de red de un `POST` de prueba a `/api/v1/auth/login`
   → header `Cookie` se envía si existe una cookie previa del dominio del
   backend (verificable en DevTools → Network → Request Headers).
2. `useLoginMutation().mutateAsync({ identificador, password })` tipa el
   resultado como `LoginResponse` (autocompletado de `accessToken`/`usuario`
   en el editor).
