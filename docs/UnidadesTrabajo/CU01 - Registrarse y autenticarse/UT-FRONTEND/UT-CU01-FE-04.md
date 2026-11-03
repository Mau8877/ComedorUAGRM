# UT-CU01-FE-04 — Sesión conectada a la API (token, renovación ante 401 y estado de sesión)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-04` |
| **Nombre** | Sesión conectada a la API (token, renovación ante 401 y estado de sesión) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-01`, `UT-CU01-FE-03` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

La API debe recibir siempre el token de acceso vigente, la sesión debe sobrevivir a una recarga de la página y la app debe reaccionar cuando la sesión deja de ser válida.

## Objetivo

Existe `AuthProvider` que registra el proveedor de token y el manejador de `401` en `apiClient`, restaura la sesión al cargar la app y expone el estado de sesión (`cargando`, `sin-sesion`, `con-sesion`).

## Alcance

- `src/features/auth/api/renovarSesion.ts`: `renovarSesion()` lee el refresh token de `tokenStorage`, llama a `refreshApi` y guarda el par nuevo; si el backend responde `401` (`ERR_AUTH_06`) o no hay refresh token, borra la sesión y devuelve `false`. **Una sola renovación a la vez** (promesa compartida): varias peticiones que fallan juntas con `401` esperan la misma.
- `src/features/auth/components/AuthProvider.tsx`: al montarse llama `setAuthTokenProvider(() => getAccessToken())` y `setUnauthorizedHandler(...)`: ante un `401` de una ruta **que no sea** `/auth/login`, `/auth/registro` ni `/auth/refresh` intenta `renovarSesion()` y reintenta la petición; si no se puede renovar, limpia la caché de TanStack Query y lleva a `/login` (A8). Al desmontarse, los desregistra. Al arrancar, si hay refresh token y no hay token de acceso, restaura la sesión con `renovarSesion()` antes de dejar el estado en `con-sesion`/`sin-sesion`.
- `src/features/auth/api/useSesion.ts`: hook que devuelve `{ estado }` y `esperarSesionInicial()` para el guard del router.
- `src/store/apiClient.ts`: ajustar `setUnauthorizedHandler` para que el manejador pueda **reintentar la petición** original tras renovar (hoy solo se notifica); sin importar nada de `features/`.
- `src/main.tsx`: montar `<AuthProvider>` dentro de `QueryProvider` y alrededor de `RouterProvider`.
- Tests (con `apiClient`/`refreshApi` simulados): el token provider devuelve el token de acceso; un `401` renueva una sola vez aunque fallen tres peticiones a la vez y reintenta; si el refresh falla, borra la sesión y navega a `/login`; un `401` de `/auth/login` **no** dispara renovación; el estado pasa de `cargando` a `con-sesion`/`sin-sesion`.

Archivos esperados (crear/tocar): `renovarSesion.ts`, `AuthProvider.tsx`, `useSesion.ts`, `apiClient.ts`, `main.tsx`, `components/index.ts`, `api/index.ts` y tests.

## Fuera de alcance (no tocar)

- Iniciar o cerrar sesión desde un botón (`UT-CU01-FE-05`).
- El guard de rutas (`UT-CU01-FE-08`).
- Guardar los tokens en un store de Zustand o en cookies: nunca.

## Resultado esperado

Toda petición a la API lleva `Authorization: Bearer <token>`, el token de acceso vencido se renueva solo y una sesión inválida devuelve al usuario al login.

## Validación

1. `pnpm test` pasa.
2. En el navegador, con el stack levantado, después de iniciar sesión las peticiones de la pestaña "Red" llevan el header `Authorization`; recargar la página mantiene la sesión.
3. `pnpm lint` y `pnpm build` sin errores.
