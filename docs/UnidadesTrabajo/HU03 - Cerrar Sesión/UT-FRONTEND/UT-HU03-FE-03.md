# UT-HU03-FE-03 — Pruebas unitarias de cierre de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-FE-03` |
| **Nombre** | Pruebas unitarias de `logoutSession`, `clearSession` y `LogoutButton` |
| **Historia** | HU03-FE |
| **Depende de** | UT-HU03-FE-01, UT-HU03-FE-02 |

## Antes de implementar

Leer [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Solicitud de negocio

Evitar regresiones en que el logout siempre limpie la sesión local, con
o sin éxito de la request al backend.

## Objetivo

Implementar specs de `logoutSession`, `clearSession`, y `LogoutButton`.

## Alcance

### `features/auth/tests/useAuthStore.test.ts` — **editar** (agregar caso, archivo de HU01-FE)

| # | Caso | Assert |
|---|------|--------|
| 1 | `clearSession()` tras `setSession(...)` | vuelve al estado inicial |

### `features/auth/tests/logoutSession.test.ts` (nuevo)

| # | Caso | Assert |
|---|------|--------|
| 2 | `apiClient.post` resuelve | `logoutSession()` resuelve sin valor |
| 3 | `apiClient.post` rechaza | `logoutSession()` propaga el rechazo (el manejo de "igual limpiar" es responsabilidad de quien lo llama, no de esta función) |

### `features/auth/tests/LogoutButton.test.tsx` (nuevo)

Mock de `logoutSession`, `useAuthStore`, `useNavigate`.

| # | Caso | Assert |
|---|------|--------|
| 4 | Click, `logoutSession` resuelve | `clearSession` invocado; `navigate` llamado con `{ to: '/login' }` |
| 5 | Click, `logoutSession` rechaza | igual `clearSession` invocado y `navigate` llamado (no se propaga el error a la UI) |
| 6 | Durante la llamada en curso | el botón está `disabled` |

### Archivos esperados (crear/editar)

- Editar: `features/auth/tests/useAuthStore.test.ts`
- Crear: `features/auth/tests/logoutSession.test.ts`
- Crear: `features/auth/tests/LogoutButton.test.tsx`

## Fuera de alcance (no tocar)

- Tests de HU01-FE/HU02-FE ya existentes (salvo el caso agregado arriba)

## Resultado esperado

`pnpm test` verde para toda la feature `auth`, cubriendo login, refresh y
logout en el mismo run.

## Validación

1. `pnpm test` pasa.
2. Los 6 casos de las tablas están cubiertos.
3. Los tests de HU01-FE/HU02-FE siguen pasando.
