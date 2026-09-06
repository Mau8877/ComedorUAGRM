# UT-HU03-FE-01 — `logoutSession()` + `clearSession` en `useAuthStore`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-FE-01` |
| **Nombre** | `logoutSession()` y `clearSession` en `useAuthStore` |
| **Historia** | HU03-FE |
| **Depende de** | `store/useAuthStore.ts` (ya existe, de HU01-FE, sin `clearSession`) |

## Antes de implementar

Leer [ESTADO_GLOBAL_FRONTEND.md](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md).

## Solicitud de negocio

Cerrar sesión necesita avisarle al backend (para revocar el refresh
token) y limpiar el estado local — ninguna de las dos cosas existe
todavía del lado del frontend.

## Objetivo

Agregar `clearSession` al store existente, y crear la función que llama
al endpoint de logout.

## Alcance

### `useAuthStore` — **editar** `store/useAuthStore.ts`

Agregar la acción que faltaba (declarada explícitamente como pendiente
por HU01-FE):

```ts
interface AuthState {
  // ...accessToken, usuario, isAuthenticated, setSession ya existentes...
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  // ...estado y setSession ya existentes...
  clearSession: () => set({ accessToken: null, usuario: null, isAuthenticated: false }),
}))
```

> Si [UT-HU02-FE-01](../../HU02%20-%20Renovar%20Sesión/UT-FRONTEND/UT-HU02-FE-01.md)
> ya se implementó y usa un `useAuthStore.setState({...})` directo como
> solución temporal dentro del interceptor de `apiClient` (documentado ahí
> como nota de orden), esta UT **reemplaza** esa línea por
> `useAuthStore.getState().clearSession()` — se declara acá como archivo a
> editar.

### `features/auth/api/logoutSession.ts` (nuevo)

```ts
export async function logoutSession(): Promise<void> {
  await apiClient.post('/api/v1/auth/logout')
}
```

- No propaga con fuerza el error: si la request de red falla (no la
  respuesta del backend, que siempre es `200`, sino p. ej. sin conexión),
  el consumidor (UT-HU03-FE-02) decide igual limpiar la sesión local —
  ver esa UT.

### Archivos esperados (crear/editar)

- Crear: `features/auth/api/logoutSession.ts`
- Editar: `store/useAuthStore.ts`
- Editar (si aplica, ver nota arriba): `src/store/apiClient.ts`

## Fuera de alcance (no tocar)

- El botón de UI y su wiring (UT-HU03-FE-02)
- `setSession` (sin cambios, ya existente)

## Resultado esperado

`useAuthStore.getState().clearSession()` vuelve al estado inicial;
`logoutSession()` llama al endpoint real.

## Validación

1. `setSession(...)` seguido de `clearSession()` deja
   `isAuthenticated === false`, `accessToken === null`, `usuario === null`.
2. `logoutSession()` hace un `POST` a `/api/v1/auth/logout` sin body.
3. Si UT-HU02-FE-01 ya existía con el `setState` temporal, ese código
   ahora llama a `clearSession()` en su lugar (sin duplicar lógica de
   limpieza de sesión en dos lugares distintos).
