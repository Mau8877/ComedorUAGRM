# UT-CU01-FE-01 — Almacén de la sesión (tokens)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-01` |
| **Nombre** | Almacén de la sesión (tokens) |
| **Caso de uso** | CU01-FE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (sección de autenticación del cliente HTTP)

## Solicitud de negocio

La sesión usa los tokens que emite el backend: el cliente necesita un lugar único donde guardarlos, leerlos y borrarlos, sin esparcir `localStorage` por la app.

## Objetivo

Existe `tokenStorage`: guarda el token de acceso **en memoria** y el refresh token en `localStorage`, y nadie más toca esos dos lugares.

## Alcance

- `src/features/auth/api/tokenStorage.ts`: `getAccessToken()`, `getRefreshToken()`, `guardarSesion({ accessToken, refreshToken })` y `borrarSesion()`. El token de acceso vive en una variable de módulo (se pierde al recargar y se recupera con el refresh token); el refresh token en `localStorage` con una clave propia (`comedoruagrm.refresh`). Todo acceso a `localStorage` va en `try/catch` (modo privado o almacenamiento bloqueado → la sesión simplemente no sobrevive a la recarga).
- `src/features/auth/index.ts` (barrel, por ahora exporta solo lo que necesite el resto de la app).
- Test `src/features/auth/tests/tokenStorage.test.ts`: guardar, leer y borrar; con `localStorage` que lanza, no rompe y el token de acceso sigue disponible en memoria.

Archivos esperados (crear/tocar): `tokenStorage.ts`, `auth/index.ts`, test.

## Fuera de alcance (no tocar)

- Llamar a la API o renovar tokens (`UT-CU01-FE-03`, `UT-CU01-FE-04`).
- Funciones de registro, login y logout (`UT-CU01-FE-05`).
- Cualquier SDK de Firebase: no se usa para la autenticación.

## Resultado esperado

El resto de la feature guarda y lee los tokens solo a través de `tokenStorage`.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Ningún otro archivo de `src/` usa `localStorage` para los tokens (búsqueda de `comedoruagrm.refresh`).
