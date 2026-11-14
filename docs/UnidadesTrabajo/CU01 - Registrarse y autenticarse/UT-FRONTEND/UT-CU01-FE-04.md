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

Existe `instalarManejadorDeSesion` que registra el manejador de `401` en `apiClient`, y el estado de sesión (`cargando`, `sin-sesion`, `con-sesion`) sale de consultar al backend.

## Alcance

- `renovarSesion()` (en `sesion.ts`) llama a `refreshApi()` (`POST /auth/refresh` **sin cuerpo**: el navegador manda la cookie `refresh_token`); devuelve `true` si renovó y `false` si el backend responde `401` (`ERR_AUTH_06`). **Una sola renovación a la vez** (promesa compartida): varias peticiones que fallan juntas con `401` esperan la misma.
- `src/features/auth/api/sesion.ts`: `instalarManejadorDeSesion({ alPerderSesion })` llama a `setUnauthorizedHandler(...)`: ante un `401` de una ruta **que no sea** `/auth/login`, `/auth/registro`, `/auth/refresh` ni `/auth/logout` intenta `renovarSesion()` y `apiClient` reintenta la petición; si no se puede renovar, llama a `alPerderSesion` (lleva a `/login` salvo en `/`, `/login` y `/registro`). **No hay token que registrar**: las cookies las manda el navegador. Se instala en `main.tsx` justo después de crear el router, **no** con un componente `AuthProvider`: los guards (`beforeLoad`) ya hacen peticiones antes de que se monte cualquier componente, así que el manejador tiene que existir antes.
- `src/features/auth/api/useSesion.ts` y `useUsuarioActual.ts`: como las cookies `HttpOnly` no se pueden leer desde JavaScript, el estado de sesión sale de `GET /auth/usuario-actual` (`usuarioActualApi` devuelve `null` si no hay sesión, sin lanzar): `useSesion()` da `cargando`/`con-sesion`/`sin-sesion`, y `esperarSesionInicial()` (`cargarUsuarioActual`) sirve al guard del router.
- `src/store/apiClient.ts`: `setUnauthorizedHandler` pasa a recibir la configuración de la petición fallida y devolver `Promise<boolean>`; si devuelve `true`, `apiClient` **reintenta la petición una sola vez** (marca interna para no reintentar en bucle). Sin importar nada de `features/`.
- `src/main.tsx`: llamar a `instalarManejadorDeSesion` tras `createRouter`.
- Tests (con `apiClient`/`refreshApi` simulados): un `401` renueva una sola vez aunque fallen tres peticiones a la vez y reintenta; si el refresh falla, limpia y navega a `/login`; un `401` de `/auth/login` **no** dispara renovación; el estado pasa de `cargando` a `con-sesion`/`sin-sesion`.

Archivos esperados (crear/tocar): `sesion.ts`, `useSesion.ts`, `useUsuarioActual.ts`, `apiClient.ts`, `main.tsx`, `api/index.ts` y tests.

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
