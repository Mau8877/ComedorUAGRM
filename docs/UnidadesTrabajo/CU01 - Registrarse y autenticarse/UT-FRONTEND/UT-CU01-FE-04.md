# UT-CU01-FE-04 — Sesión conectada a la API (renovación ante 401 y estado de sesión)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-04` |
| **Nombre** | Sesión conectada a la API (renovación ante 401 y estado de sesión) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-01`, `UT-CU01-FE-03` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

La sesión debe sobrevivir a una recarga de la página, renovarse sola cuando el token de acceso vence y la app debe reaccionar cuando deja de ser válida.

## Objetivo

Existe `AuthProvider` que registra el manejador de `401` en `apiClient`, sabe si hay sesión consultando al backend y expone el estado (`cargando`, `sin-sesion`, `con-sesion`).

## Alcance

- `src/features/auth/api/renovarSesion.ts`: `renovarSesion()` llama a `refreshApi()` (`POST /auth/refresh` **sin cuerpo**: el navegador manda la cookie `refresh_token`); devuelve `true` si renovó y `false` si el backend responde `401` (`ERR_AUTH_06`). **Una sola renovación a la vez** (promesa compartida): varias peticiones que fallan juntas con `401` esperan la misma.
- `src/features/auth/components/AuthProvider.tsx`: al montarse llama `setUnauthorizedHandler(...)`: ante un `401` de una ruta **que no sea** `/auth/login`, `/auth/registro` ni `/auth/refresh` intenta `renovarSesion()` y reintenta la petición; si no se puede renovar, limpia la caché de TanStack Query y lleva a `/login` (A8). Al desmontarse, lo desregistra. **No hay token que registrar**: las cookies las manda el navegador.
- `src/features/auth/api/useSesion.ts`: como las cookies `HttpOnly` no se pueden leer desde JavaScript, el estado de sesión sale de `GET /auth/usuario-actual` (`useUsuarioActual`, que ante un `401` ya pasa por la renovación): `cargando` mientras consulta, `con-sesion` si responde el usuario, `sin-sesion` si no. También `esperarSesionInicial()` para el guard del router.
- `src/store/apiClient.ts`: ajustar `setUnauthorizedHandler` para que el manejador pueda **reintentar la petición** original tras renovar (hoy solo se notifica); sin importar nada de `features/`.
- `src/main.tsx`: montar `<AuthProvider>` dentro de `QueryProvider` y alrededor de `RouterProvider`.
- Tests (con `apiClient`/`refreshApi` simulados): un `401` renueva una sola vez aunque fallen tres peticiones a la vez y reintenta; si el refresh falla, limpia y navega a `/login`; un `401` de `/auth/login` **no** dispara renovación; el estado pasa de `cargando` a `con-sesion`/`sin-sesion`.

Archivos esperados (crear/tocar): `renovarSesion.ts`, `AuthProvider.tsx`, `useSesion.ts`, `apiClient.ts`, `main.tsx`, `components/index.ts`, `api/index.ts` y tests.

## Fuera de alcance (no tocar)

- Iniciar o cerrar sesión desde un botón (`UT-CU01-FE-05`).
- El guard de rutas (`UT-CU01-FE-08`).
- Guardar tokens en cualquier lado (`localStorage`, store, memoria): nunca.

## Resultado esperado

La sesión se mantiene al recargar, el token de acceso vencido se renueva solo y una sesión inválida devuelve al usuario al login, sin que el JavaScript maneje un solo token.

## Validación

1. `pnpm test` pasa.
2. En el navegador, con el stack levantado, después de iniciar sesión la pestaña "Aplicación → Cookies" muestra `access_token` y `refresh_token` con `HttpOnly`; recargar la página mantiene la sesión.
3. `pnpm lint` y `pnpm build` sin errores.
