# UT-CU01-FE-08 — Guard de rutas privadas, redirecciones y destino por rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-08` |
| **Nombre** | Guard de rutas privadas, redirecciones y destino por rol |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-04`, `UT-CU01-FE-05` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Las pantallas privadas no deben verse sin sesión, y cada rol debe llegar a su módulo al entrar.

## Objetivo

`_authenticated.tsx` valida la sesión real (tokens del backend) y `/login` y `/registro` redirigen al módulo si ya hay sesión.

## Alcance

- `src/routes/_authenticated.tsx`: `beforeLoad` espera `esperarSesionInicial()`; sin sesión (ni token de acceso ni refresh token que se pueda canjear) → `redirect({ to: '/login' })`; con sesión, asegura el usuario actual (`ensureQueryData` de `useUsuarioActual`); si el backend responde `401`/`403` el manejador de `UT-CU01-FE-04` cierra la sesión y lleva a `/login`. Sustituye el `isLoggedIn = true` provisorio y su `TODO`.
- `src/routes/login.tsx` y `registro.tsx`: `beforeLoad` que, con sesión activa, redirige a `rutaInicioPorRol(rol)`.
- `src/routes/_authenticated/panel.tsx`: deja de ser un título suelto; muestra el saludo con el nombre del usuario (sin layout por ahora) y, para `DIRECTIVO` y `PERSONAL_COMEDOR`, el aviso "tu módulo aún no está disponible". Sigue siendo la ruta hija que evita el conflicto del layout vacío (ver `RUTAS_NAVEGACION_FRONTEND.md`).
- Tests: sin sesión redirige a `/login`; con sesión no redirige; `/login` con sesión redirige a `/panel`.

Archivos esperados (tocar): `_authenticated.tsx`, `login.tsx`, `registro.tsx`, `_authenticated/panel.tsx` y tests.

## Fuera de alcance (no tocar)

- Los layouts por rol dentro de `_authenticated/` (los crean los CU de cada rol).
- Recordar la dirección a la que se quería entrar.

## Resultado esperado

Sin sesión no se ve ninguna pantalla privada; con sesión, cada rol aterriza donde corresponde.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan (el build regenera `routeTree.gen.ts` sin conflictos).
2. En el navegador: abrir `/panel` sin sesión redirige a `/login`; con sesión, `/login` redirige a `/panel`.
3. Recargar `/panel` con sesión no pide credenciales otra vez.
