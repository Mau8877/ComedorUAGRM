# UT-HU01-FE-02 — `useAuthStore` (sesión en memoria)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-02` |
| **Nombre** | `useAuthStore`: access token + usuario en memoria |
| **Historia** | HU01-FE |
| **Depende de** | — |

## Antes de implementar

Leer [ESTADO_GLOBAL_FRONTEND.md](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
completo, y la [decisión de diseño](../README.md#decisión-de-diseño-la-sesión-vive-en-un-store-de-zustand-no-en-tanstack-query)
de este `README` — esta UT es exactamente esa decisión implementada.

## Solicitud de negocio

Tras un login exitoso, el access token y los datos básicos del usuario
tienen que quedar disponibles de forma síncrona para el guard de rutas y
(a futuro) el interceptor HTTP — un hook de TanStack Query no sirve para
eso.

## Objetivo

Crear `store/useAuthStore.ts` (global, no específico de `features/auth/`)
con el estado de sesión y sus acciones.

## Alcance

### `store/useAuthStore.ts`

```ts
import { create } from 'zustand'
import type { UsuarioSesion } from '@/features/auth'

interface AuthState {
  accessToken: string | null
  usuario: UsuarioSesion | null
  isAuthenticated: boolean
  setSession: (accessToken: string, usuario: UsuarioSesion) => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: null,
  usuario: null,
  isAuthenticated: false,
  setSession: (accessToken, usuario) => set({ accessToken, usuario, isAuthenticated: true }),
}))
```

- **Sin** middleware `persist` — a propósito (ver decisión de diseño en el
  `README`, recuperar sesión tras F5 es de HU-02).
- **Sin `clearSession` todavía.** Esta HU (login) nunca cierra una
  sesión — esa acción la agrega
  [HU-03: Cerrar sesión](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
  sobre este mismo archivo (lo declara como archivo a **editar**, no a
  crear, en su propia UT) cuando se desglose, no acá — ver
  [UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md#una-ut-nunca-construye-trabajo-de-otra-hu-aunque-sea-trivial-agregarlo-ahora).
- `UsuarioSesion` se importa desde el barrel de la feature `auth`
  (`@/features/auth`), no desde el archivo interno de tipos — mismo
  criterio de ["qué es público de la feature"](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#indexts-qué-es-público-de-la-feature).
  Exportar `UsuarioSesion` desde `features/auth/index.ts` si todavía no lo
  hace UT-HU01-FE-01 (barrel).

### Archivos esperados (crear/tocar)

- `store/useAuthStore.ts`
- `features/auth/index.ts` (agregar el export de `UsuarioSesion` si falta)

## Fuera de alcance (no tocar)

- Consumo real del store (login screen: UT-HU01-FE-04; guard: UT-HU01-FE-05)
- `clearSession` — se agrega en la UT de
  [HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md), no acá
- Interceptor de `Authorization` header (follow-up)
- Persistencia entre recargas (follow-up de HU-02)

## Resultado esperado

`useAuthStore.getState()` expone `accessToken`/`usuario`/`isAuthenticated`
y `setSession`, consumible tanto desde componentes React
(`useAuthStore()`) como desde código fuera de React (`useAuthStore.getState()`
en el `beforeLoad` del router, que no es un componente).

## Validación

1. `useAuthStore.getState().isAuthenticated` es `false` en el estado
   inicial.
2. Llamar `setSession(token, usuario)` deja `isAuthenticated: true` y los
   dos valores accesibles.
3. El store es legible desde un módulo `.ts` plano (no un componente),
   confirmando que sirve para el guard de `_authenticated.tsx`.
