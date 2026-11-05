# UT-CU15-FE-02 — Llamadas a la API, hooks y mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-02` |
| **Nombre** | Llamadas a la API de recuperación y de perfil, hooks y mapeo de errores |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-01` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`, `apiClient`, sin tokens en el cliente)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
- Como referencia: `F/features/auth/api/` (`authApi.ts`, `mapearError.ts`, `useLogin.ts`)

## Solicitud de negocio

Las pantallas necesitan hablar con el backend y mostrar mensajes claros para cada rechazo, sin que los componentes armen llamadas ni claves de caché a mano.

## Objetivo

Existen las funciones de API, los hooks de TanStack Query y el mapeo de errores del CU.

## Alcance

- `F/features/auth/api/authApi.ts`: `solicitarRecuperacionApi(correo)` (`POST /auth/recuperar-contrasena`) y `restablecerContrasenaApi(request)` (`POST /auth/restablecer-contrasena`).
- `F/features/auth/api/useSolicitarRecuperacion.ts` y `useRestablecerContrasena.ts`: `useMutation`. Al restablecer con éxito, `queryClient.clear()` (el backend cerró todas las sesiones). El token no se guarda en caché ni se loguea.
- `F/features/auth/api/mapearError.ts`: casos nuevos: `ERR_AUTH_08` → `{ mensaje: 'El enlace ya no sirve. Solicita uno nuevo.', enlaceInvalido: true }`; el resto sigue el mapeo existente. Un error de red → el mensaje de conexión con reintento (A10).
- `F/features/perfil/api/keys.ts`: `perfilKeys` (`all: ['perfil']`, `detail: () => ['perfil','detail']`).
- `F/features/perfil/api/perfilApi.ts`, `usePerfil.ts`, `useActualizarPerfil.ts`, `useCambiarContrasena.ts` y `mapearError.ts`: `GET /perfil`, `PATCH /perfil` y `POST /auth/cambiar-contrasena` (sin cuerpo de refresh token: viaja en la cookie). `useActualizarPerfil` invalida `perfilKeys.detail()` y `['auth','usuario-actual']`. `useCambiarContrasena` no toca la sesión local (la actual sigue abierta). Mapeo: `ERR_AUTH_09` → campo `contrasenaActual`; `ERR_US_02` → campo `correo`; `ERR_US_13` y `ERR_SYS_01` → el mensaje del backend; red o `5xx` → conexión con reintento.
- Tests (mockeando `apiClient`): URL, método y cuerpo de cada llamada; que ninguna manda ni guarda tokens; el mapeo de cada código; que `useActualizarPerfil` invalida las claves esperadas; que un `401` de `recuperar` o `restablecer` no dispara la renovación de sesión.

Archivos esperados (crear): los archivos de `features/perfil/api/` y sus tests. (Tocar): `authApi.ts`, `mapearError.ts`, `features/auth/api/index.ts` y los hooks nuevos de `auth/api`.

## Fuera de alcance (no tocar)

- Pantallas y formularios.
- `apiClient`, `createBaseApi` y el manejador de `401` de `sesion.ts`.

## Resultado esperado

Las pantallas obtienen datos y mutan con un hook y reciben errores ya traducidos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
