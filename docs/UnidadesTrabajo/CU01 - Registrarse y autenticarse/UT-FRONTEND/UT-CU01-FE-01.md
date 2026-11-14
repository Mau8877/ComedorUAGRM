# UT-CU01-FE-01 — Cliente HTTP con cookies de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-01` |
| **Nombre** | Cliente HTTP con cookies de sesión |
| **Caso de uso** | CU01-FE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (sección "Cómo viajan los tokens")
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (sección de autenticación del cliente HTTP)

## Solicitud de negocio

La sesión de la web viaja en cookies `HttpOnly` que pone el backend: el JavaScript de la página nunca debe ver ni guardar los tokens, para que un XSS no pueda robar la sesión.

## Objetivo

`apiClient` manda las cookies del backend en cada petición (`withCredentials`) y la feature `auth` queda creada sin ningún almacén de tokens.

## Alcance

- `src/store/apiClient.ts`: `withCredentials: true` en la instancia (ya hecho) y comentario que explica que no hay token que guardar ni header `Authorization` en la web.
- `src/store/apiClient.test.ts`: una petición sale con `withCredentials`.
- `src/features/auth/index.ts` (barrel, por ahora exporta solo lo que necesite el resto de la app).
- Verificar que el origen del frontend está en `app.cors.allowed-origins` del backend (ya lo está: `http://localhost:5173` y `http://localhost`) y que web y API son el mismo *site* (`SameSite=Strict`).

Archivos esperados (crear/tocar): `apiClient.ts`, `apiClient.test.ts`, `auth/index.ts`.

## Fuera de alcance (no tocar)

- Llamar a la API o renovar la sesión (`UT-CU01-FE-03`, `UT-CU01-FE-04`).
- Registro, login y logout (`UT-CU01-FE-05`).
- Guardar tokens en `localStorage`, `sessionStorage`, cookies legibles por JavaScript o un store: **nunca**.
- Cualquier SDK de Firebase.

## Resultado esperado

Las peticiones de la web llevan las cookies de sesión del backend sin que la app toque ningún token.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Búsqueda en `src/`: ningún uso de `localStorage`/`sessionStorage` para tokens ni de `Authorization: Bearer`.
