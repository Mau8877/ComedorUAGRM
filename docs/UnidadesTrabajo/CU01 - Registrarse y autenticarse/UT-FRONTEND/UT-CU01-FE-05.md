# UT-CU01-FE-05 — Acciones de registro, inicio y cierre de sesión con mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-05` |
| **Nombre** | Acciones de registro, inicio y cierre de sesión con mapeo de errores |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-02`, `UT-CU01-FE-03`, `UT-CU01-FE-04` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El registro, el login y el cierre de sesión combinan la API y el almacén de tokens; esa lógica y la traducción de errores deben vivir en un solo lugar, probada.

## Objetivo

Existen `useRegistro`, `useLogin` y `useCerrarSesion` (mutations) que implementan los flujos A, B y C, devuelven errores ya traducidos a mensajes y existe `rutaInicioPorRol`.

## Alcance

- `src/features/auth/api/mapearError.ts`: función pura que recibe un `ApiError` (o un error de red) y devuelve `{ mensaje, campo? }` según la tabla de decisiones del README: `ERR_AUTH_04` → mensaje genérico de credenciales; `ERR_AUTH_05` → cuenta bloqueada con el mensaje del backend (trae los minutos que faltan); `ERR_AUTH_03` → cuenta desactivada, comunicarse con la DUBSS; `ERR_US_01`/`02`/`03` → el campo en conflicto (`ci`, `correo`, `registroUniversitario`); red → error de conexión con reintento.
- `useRegistro.ts`: flujo A del README (`POST /auth/registro`, guarda los tokens de la respuesta con `tokenStorage` y deja la sesión abierta). Evita el doble envío mientras está en curso (A7).
- `useLogin.ts`: flujo B (`POST /auth/login`, guarda los tokens). Al terminar precarga `authKeys.usuarioActual` con el usuario de la respuesta y lo devuelve para decidir el destino.
- `useCerrarSesion.ts`: `POST /auth/logout` con el refresh token (se ignora cualquier error de red o `401`: A10), `borrarSesion()`, `queryClient.clear()` y navegación a `/login`; solo cierra este dispositivo.
- `src/features/auth/rutaInicioPorRol.ts`: `rutaInicioPorRol(rol)` → `/panel` para los 4 roles por ahora (rol desconocido → `/login`); es el único lugar a actualizar cuando cada rol tenga su módulo.
- Tests unitarios con `apiClient` simulado: cada fila de la tabla de errores; `rutaInicioPorRol` con los 4 roles y uno desconocido; registro feliz guarda tokens; `409` marca el campo; cerrar sesión limpia aunque el backend falle; ninguna contraseña ni token aparece en `console`.

Archivos esperados (crear): `mapearError.ts`, `useRegistro.ts`, `useLogin.ts`, `useCerrarSesion.ts`, `rutaInicioPorRol.ts` y tests.

## Fuera de alcance (no tocar)

- Las pantallas (`UT-CU01-FE-06`, `07`).
- Reglas de contraseña o de bloqueo: las aplica el backend; acá solo se muestran sus mensajes.
- Recuperar contraseña (CU15).

## Resultado esperado

La lógica del CU01 en el cliente está completa y probada; las pantallas solo la invocan.

## Validación

1. `pnpm test` pasa con todos los escenarios.
2. `pnpm lint` y `pnpm build` sin errores.
3. Revisar que ningún `console.log` imprime contraseñas ni tokens.
