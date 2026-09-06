# UT-HU02-FE-02 — Guard `_authenticated` y ruta `/login`: refresh silencioso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-FE-02` |
| **Nombre** | Intento de refresh silencioso en el guard `_authenticated` y en `/login` |
| **Historia** | HU02-FE |
| **Depende de** | UT-HU02-FE-01 |

## Antes de implementar

Leer [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#guard-genérico-para-rutas-protegidas).

## Solicitud de negocio

Tras un F5, el access token en memoria se pierde pero la cookie
`refreshToken` puede seguir vigente — sin este intento, el usuario
quedaría deslogueado de la nada aunque su sesión real siga activa.

## Objetivo

Hacer que el guard intente restaurar la sesión antes de decidir si
redirige a `/login`.

## Alcance

### `routes/_authenticated.tsx` — **editar** (ya existe, con el guard real de HU01-FE)

```tsx
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async () => {
    if (useAuthStore.getState().isAuthenticated) return

    try {
      const data = await refreshSession()
      useAuthStore.getState().setSession(data.accessToken, data.usuario)
    } catch {
      throw redirect({ to: '/login' })
    }
  },
  component: AuthenticatedLayout,
})
```

- `beforeLoad` ya soporta `async` nativamente en TanStack Router — el
  router espera la promesa antes de resolver la navegación, no hace falta
  ningún mecanismo de "loading global" aparte.
- Si `refreshSession()` falla (sin cookie válida, o inválida/revocada), se
  redirige a `/login` — mismo comportamiento que sin esta UT, solo que
  ahora se intentó antes.

### `routes/login.tsx` — **editar** (ya existe, con el `beforeLoad` de HU01-FE)

Mismo intento, para no mostrar el formulario si la cookie todavía es
válida:

```tsx
export const Route = createFileRoute('/login')({
  beforeLoad: async () => {
    if (useAuthStore.getState().isAuthenticated) {
      throw redirect({ to: '/panel' })
    }

    try {
      const data = await refreshSession()
      useAuthStore.getState().setSession(data.accessToken, data.usuario)
      throw redirect({ to: '/panel' })
    } catch {
      // Sin sesión recuperable -- se queda en /login, comportamiento normal.
    }
  },
  component: Login,
})
```

> Cuidado: `throw redirect(...)` dentro de un `try` cuyo `catch` es
> genérico puede terminar atrapando el propio `redirect` (TanStack Router
> lo lanza como una forma de control de flujo, no como un error real) —
> verificar contra la versión instalada de `@tanstack/react-router` si
> hace falta re-lanzar el `redirect` explícitamente dentro del `catch`
> (`if (isRedirect(err)) throw err`) para no tragárselo por accidente.

### Archivos esperados (editar — ambos ya existen)

- `routes/_authenticated.tsx`
- `routes/login.tsx`

## Fuera de alcance (no tocar)

- `refreshSession()`/interceptores de `apiClient` (UT-HU02-FE-01, ya
  cerrados)
- Un indicador visual de "cargando sesión" mientras se resuelve el
  `beforeLoad` — no hay requisito de UX específico para esto en la HU;
  si hace falta, es una mejora de UX a futuro, no bloqueante

## Resultado esperado

Recargar la página con una cookie `refreshToken` vigente restaura la
sesión sin mostrar el login; sin cookie vigente, redirige a `/login`
normalmente.

## Validación

1. Con sesión activa, recargar la página (F5) estando en `/panel` →
   permanece en `/panel` (sesión restaurada por el guard).
2. Sin cookie válida, recargar en `/panel` → termina en `/login`.
3. Con cookie vigente, navegar manualmente a `/login` → redirige a
   `/panel` sin mostrar el formulario.
4. Sin cookie válida, navegar a `/login` → se muestra el formulario
   normalmente (sin quedar colgado esperando el intento fallido).
