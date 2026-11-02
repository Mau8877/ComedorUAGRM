# UT-CU01-FE-04 — Sesión de Firebase conectada a la API (token, 401 y estado de sesión)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-04` |
| **Nombre** | Sesión de Firebase conectada a la API (token, 401 y estado de sesión) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-01`, `UT-CU01-FE-03` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

La API debe recibir siempre el token vigente de Firebase y la app debe reaccionar cuando la sesión deja de ser válida.

## Objetivo

Existe `AuthProvider` que registra el proveedor de token y el manejador de `401` en `apiClient` y expone el estado de sesión (`cargando`, `sin-sesion`, `con-sesion`).

## Alcance

- `src/features/auth/api/useFirebaseSession.ts`: hook sobre `onAuthStateChanged` que devuelve `{ estado, firebaseUser }`; y `esperarSesionInicial()` (`auth.authStateReady()`) para el guard del router.
- `src/features/auth/components/AuthProvider.tsx`: al montarse llama `setAuthTokenProvider(() => auth.currentUser?.getIdToken() ?? null)` y `setUnauthorizedHandler(...)`, que cierra la sesión de Firebase, limpia la caché de TanStack Query y lleva a `/login` (A8). Al desmontarse, los desregistra.
- `src/main.tsx`: montar `<AuthProvider>` dentro de `QueryProvider` y alrededor de `RouterProvider`.
- Tests: el token provider devuelve el token del usuario o `null`; un `401` dispara cerrar sesión y navegar a `/login`; el estado pasa de `cargando` a `con-sesion`/`sin-sesion` (con `firebase/auth` simulado).

Archivos esperados (crear/tocar): `useFirebaseSession.ts`, `AuthProvider.tsx`, `main.tsx`, `components/index.ts`, `api/index.ts` y tests.

## Fuera de alcance (no tocar)

- Iniciar o cerrar sesión desde un botón (`UT-CU01-FE-05`).
- El guard de rutas (`UT-CU01-FE-08`).
- Guardar el token en un store o en `localStorage`: nunca (lo guarda el SDK).

## Resultado esperado

Toda petición a la API lleva `Authorization: Bearer <token>` y una sesión inválida devuelve al usuario al login.

## Validación

1. `pnpm test` pasa.
2. En el navegador, con el stack y Firebase configurados, después de iniciar sesión las peticiones de la pestaña "Red" llevan el header `Authorization`.
3. `pnpm lint` y `pnpm build` sin errores.
