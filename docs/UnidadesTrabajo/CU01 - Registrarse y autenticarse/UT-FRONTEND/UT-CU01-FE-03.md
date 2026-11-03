# UT-CU01-FE-03 — Hooks y llamadas a la API de cuentas (carreras, registro, login, refresh, logout, usuario actual)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-03` |
| **Nombre** | Hooks y llamadas a la API de cuentas (carreras, registro, login, refresh, logout, usuario actual) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-01` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Las pantallas necesitan hablar con el backend sin armar peticiones a mano ni inventar claves de caché.

## Objetivo

Existen los hooks y funciones del contrato HTTP del CU01 usando `apiClient`, con claves de `queryKey` según la convención.

## Alcance

- `src/features/auth/types/index.ts`: `Usuario` (`UsuarioResponse`), `Sesion` (`SesionResponse`: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `usuario`), `Carrera`.
- `src/features/auth/api/keys.ts`: `authKeys` (`all: ['auth']`, `usuarioActual`) y `carrerasKeys` (`['carreras', 'list', params]`).
- `useCarreras.ts`: `useQuery` de `GET /carreras?pageSize=200`.
- Funciones sin hook (las usan las mutations de `UT-CU01-FE-05` y el manejador de `401` de `UT-CU01-FE-04`): `registrarApi(request)` → `POST /auth/registro` (devuelve la `Sesion` y si fue creada `201` o ya existía `200`), `loginApi(correo, contrasena)` → `POST /auth/login`, `refreshApi(refreshToken)` → `POST /auth/refresh` y `logoutApi(refreshToken)` → `POST /auth/logout`.
- `useUsuarioActual({ enabled })`: `useQuery` de `GET /auth/usuario-actual` (`retry: false`, para que un `401` no reintente).
- Tests (mockeando `apiClient`, sin backend real): cada llamada pega a la ruta y el método correctos y devuelve el `data` desempaquetado; un `ApiError` con `ERR_US_01` o `ERR_AUTH_04` se propaga con su `code`.

Archivos esperados (crear): `types/index.ts`, `api/keys.ts`, `api/useCarreras.ts`, `api/authApi.ts`, `api/useUsuarioActual.ts`, `api/index.ts` y tests.

## Fuera de alcance (no tocar)

- Guardar los tokens (`UT-CU01-FE-01`) y renovarlos ante un `401` (`UT-CU01-FE-04`).
- Mutations con navegación o mensajes: son de `UT-CU01-FE-05`.

## Resultado esperado

La feature tiene toda su capa de acceso a la API, tipada y probada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Ningún componente arma un `queryKey` a mano (las keys salen de `keys.ts`).
