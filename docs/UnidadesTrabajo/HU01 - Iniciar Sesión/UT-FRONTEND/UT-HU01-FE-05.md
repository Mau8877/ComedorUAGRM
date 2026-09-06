# UT-HU01-FE-05 — Ruta `/login` + guard `_authenticated`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-05` |
| **Nombre** | Reemplazar el placeholder de `/login` y activar el guard real de `_authenticated` |
| **Historia** | HU01-FE |
| **Depende de** | UT-HU01-FE-02, UT-HU01-FE-04 |

## Antes de implementar

Leer [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
completo, en particular la nota sobre layouts pathless sin hijos.

## Solicitud de negocio

Hoy `/login` es un placeholder (`<h1>Iniciar sesión</h1>`) y
`_authenticated.tsx` tiene `isLoggedIn = true` hardcodeado — ninguno de
los dos refleja todavía el login real.

## Objetivo

Montar la screen `Login` real en la ruta `/login`, y hacer que el guard
`_authenticated` lea `useAuthStore` de verdad.

## Alcance

### `routes/login.tsx` — reemplazar el placeholder

**Archivo ya existente — editar, no crear uno nuevo.**

```tsx
import { createFileRoute } from '@tanstack/react-router'
import { Login } from '@/features/auth'

export const Route = createFileRoute('/login')({
  component: Login,
})
```

- Se retira el `<h1>` de prueba — el componente ahora es la screen real
  importada del barrel de la feature (nunca de un archivo interno, ver
  [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#indexts-qué-es-público-de-la-feature)).

### `routes/_authenticated.tsx` — activar el guard real

**Archivo ya existente — editar, no reescribir entero.** Reemplazar el
`isLoggedIn = true` hardcodeado:

```tsx
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: () => {
    const isAuthenticated = useAuthStore.getState().isAuthenticated

    if (!isAuthenticated) {
      throw redirect({ to: '/login' })
    }
  },
  component: AuthenticatedLayout,
})
```

- `useAuthStore.getState()` (no el hook `useAuthStore()`) porque
  `beforeLoad` no es un componente React — mismo motivo por el que el
  store se diseñó legible fuera de React (ver
  [UT-HU01-FE-02](./UT-HU01-FE-02.md)).
- Se borran los comentarios `TODO` existentes sobre este punto — ya no
  aplican.

### Redirección inversa: usuario ya logueado que visita `/login`

Agregar `beforeLoad` en `routes/login.tsx` para no mostrar el formulario
si ya hay sesión activa:

```tsx
export const Route = createFileRoute('/login')({
  beforeLoad: () => {
    if (useAuthStore.getState().isAuthenticated) {
      throw redirect({ to: '/panel' })
    }
  },
  component: Login,
})
```

### Archivos esperados (editar — ambos ya existen)

- `routes/login.tsx`
- `routes/_authenticated.tsx`

## Fuera de alcance (no tocar)

- `routes/_authenticated/panel.tsx` (sigue siendo el placeholder que ya
  es — no se reemplaza en esta HU)
- Layouts por rol (`AdminLayout`/`EstudianteLayout`) — fuera de alcance
- Interceptor de `Authorization` header (follow-up)

## Resultado esperado

Sin sesión, visitar cualquier ruta bajo `_authenticated` (ej. `/panel`)
redirige a `/login`. Tras un login exitoso, visitar `/login` de nuevo
redirige a `/panel` en vez de mostrar el formulario.

## Validación

1. Sin sesión (`useAuthStore` en estado inicial), navegar a `/panel` →
   termina en `/login`.
2. Completar el login con éxito → termina en `/panel`.
3. Con sesión activa, navegar manualmente a `/login` → redirige a
   `/panel` sin mostrar el formulario.
4. Recargar la página (F5) con sesión activa → como no hay `persist`
   (decisión de diseño), la sesión se pierde y `/panel` redirige de
   nuevo a `/login` — comportamiento esperado para esta HU, se resuelve
   en [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md).
