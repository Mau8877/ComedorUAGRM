# UT-HU01-FE-06 — Pruebas unitarias de login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-06` |
| **Nombre** | Pruebas unitarias de la feature `auth` (`.test.ts`/`.test.tsx`) |
| **Historia** | HU01-FE |
| **Depende de** | UT-HU01-FE-01, UT-HU01-FE-02, UT-HU01-FE-03, UT-HU01-FE-04, UT-HU01-FE-05 |

## Antes de implementar

Leer [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
completo — los tests de esta feature van en `features/auth/tests/`, no
al lado del archivo (criterio distinto al de `src/utils`/`src/components/ui`).

## Solicitud de negocio

Evitar regresiones en la validación del formulario, el store de sesión, y
el guard de rutas — son las tres piezas con lógica real de esta HU.

## Objetivo

Implementar specs con Vitest + Testing Library, mockeando `apiClient`
(nunca contra un backend real).

## Alcance

### `features/auth/tests/loginSchema.test.ts`

| # | Caso | Assert |
|---|------|--------|
| 1 | `identificador` y `password` válidos | `loginSchema.safeParse(...).success === true` |
| 2 | `identificador` vacío | error `"El usuario o correo es obligatorio"` |
| 3 | `identificador` con un `username` cualquiera (no email) | `success === true` (no se exige formato de email) |
| 4 | Password vacío | error `"La contraseña es obligatoria"` |

### `features/auth/tests/LoginForm.test.tsx`

| # | Caso | Assert |
|---|------|--------|
| 5 | Blur en `identificador` vacío | aparece el error inline de ese campo |
| 6 | Escribir en `password` sin tocar `identificador` | el error de `identificador` **no** aparece todavía (regla de `isTouched`) |
| 7 | Completar ambos campos válidos y enviar | `onSubmit` se invoca con los valores correctos |
| 8 | `isSubmitting=true` | el botón de envío está deshabilitado |
| 9 | `errorMessage="Credenciales inválidas"` | el texto se renderiza en pantalla |

### `features/auth/tests/useAuthStore.test.ts`

| # | Caso | Assert |
|---|------|--------|
| 10 | Estado inicial | `isAuthenticated === false`, `accessToken === null` |
| 11 | `setSession(token, usuario)` | `isAuthenticated === true`, ambos valores seteados |

### `features/auth/tests/Login.test.tsx`

Mock de `apiClient.post` (no de `axios` global) y de `useNavigate` de
`@tanstack/react-router`.

| # | Caso | Assert |
|---|------|--------|
| 13 | `apiClient.post` resuelve con `{ accessToken, usuario }` | `useAuthStore.getState().isAuthenticated === true`; `navigate` llamado con `{ to: '/panel' }` |
| 14 | `apiClient.post` rechaza con `ApiError('ERR_AUTH_01', 'Credenciales inválidas')` | el texto del error se muestra; `navigate` **no** se llama |
| 15 | `apiClient.post` rechaza con `ApiError('ERR_AUTH_03', 'Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.')` | ese texto exacto se muestra |

### Archivos esperados (crear)

- `features/auth/tests/loginSchema.test.ts`
- `features/auth/tests/LoginForm.test.tsx`
- `features/auth/tests/useAuthStore.test.ts`
- `features/auth/tests/Login.test.tsx`

## Fuera de alcance (no tocar)

- Tests E2E (Playwright/Cypress — el proyecto no los tiene configurados)
- Tests del guard `_authenticated.tsx` en aislamiento (`beforeLoad` de
  TanStack Router no se testea unitariamente en este proyecto todavía —
  se verifica manualmente, ver Validación de UT-HU01-FE-05)
- Tests de backend (ver [`../UT-BACKEND/UT-HU01-BE-08.md`](../UT-BACKEND/UT-HU01-BE-08.md))

## Resultado esperado

`pnpm test` verde para toda la feature `auth`, sin pegarle a un backend
real.

## Validación

1. `pnpm test` pasa sin warnings de act()/effects pendientes.
2. Los 15 casos de las tablas de arriba están cubiertos.
3. Ningún test depende de un backend corriendo (`apiClient.post` siempre
   mockeado).
