# UT-HU02-FE-03 — Pruebas unitarias de renovación de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-FE-03` |
| **Nombre** | Pruebas unitarias de `refreshSession` e interceptores |
| **Historia** | HU02-FE |
| **Depende de** | UT-HU02-FE-01, UT-HU02-FE-02 |

## Antes de implementar

Leer [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Solicitud de negocio

Evitar regresiones en el reintento automático y en la restauración de
sesión tras F5 — es lógica con estado compartido (interceptores, un
`Promise` en módulo) fácil de romper sin darse cuenta.

## Objetivo

Implementar specs para `refreshSession`, los interceptores de `apiClient`,
y el `beforeLoad` de `_authenticated`.

## Alcance

### `features/auth/tests/refreshSession.test.ts`

| # | Caso | Assert |
|---|------|--------|
| 1 | `apiClient.post` resuelve | `refreshSession()` devuelve `{ accessToken, usuario }` |
| 2 | `apiClient.post` rechaza | `refreshSession()` propaga el rechazo |

### `features/auth/tests/apiClient.interceptors.test.ts`

Mock de `axios`/`apiClient` a nivel de request (no HTTP real).

| # | Caso | Assert |
|---|------|--------|
| 3 | Sesión activa en `useAuthStore` | la request saliente lleva `Authorization: Bearer <token>` |
| 4 | Sin sesión | la request saliente **no** lleva `Authorization` |
| 5 | Respuesta `401` + `refreshSession` exitoso | la request original se reintenta y resuelve |
| 6 | Tres `401` simultáneos | `refreshSession` (mockeado) se invoca una sola vez |
| 7 | Respuesta `401` + `refreshSession` también falla | la sesión se limpia; el error se propaga |
| 8 | Un `401` del propio endpoint `/api/v1/auth/refresh` | no dispara un segundo refresh (sin loop) |

### `features/auth/tests/authGuard.test.ts`

Mock de `refreshSession` y de `useAuthStore`.

| # | Caso | Assert |
|---|------|--------|
| 9 | Sin sesión, `refreshSession` exitoso | `beforeLoad` de `_authenticated` no lanza `redirect`; `setSession` invocado |
| 10 | Sin sesión, `refreshSession` falla | `beforeLoad` lanza `redirect({ to: '/login' })` |
| 11 | `/login` con sesión ya activa | `beforeLoad` lanza `redirect({ to: '/panel' })` sin llamar a `refreshSession` |
| 12 | `/login` sin sesión, `refreshSession` exitoso | `beforeLoad` lanza `redirect({ to: '/panel' })` |

### Archivos esperados (crear)

- `features/auth/tests/refreshSession.test.ts`
- `features/auth/tests/apiClient.interceptors.test.ts`
- `features/auth/tests/authGuard.test.ts`

## Fuera de alcance (no tocar)

- Tests de HU01-FE (login, formulario, store) — ya existentes, no se
  tocan salvo que un cambio de esta HU los rompa (no debería)
- Tests E2E

## Resultado esperado

`pnpm test` verde para toda la feature `auth`, cubriendo login (HU01) y
renovación (HU02) en el mismo run.

## Validación

1. `pnpm test` pasa.
2. Los 12 casos de las tablas están cubiertos.
3. Los tests de HU01-FE (login) siguen pasando sin cambios.
