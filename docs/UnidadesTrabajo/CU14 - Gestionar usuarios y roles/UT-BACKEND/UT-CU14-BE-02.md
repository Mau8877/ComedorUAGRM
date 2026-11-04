# UT-CU14-BE-02 — `403` en el sobre por falta de rol (`ERR_AUTH_07`)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-02` |
| **Nombre** | `403` en el sobre por falta de rol (`ERR_AUTH_07`) |
| **Caso de uso** | CU14-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Un usuario que no es Administrador DUBSS no debe poder gestionar cuentas (CA19), y la respuesta tiene que ser la misma del resto de la API, no una página vacía.

## Objetivo

Cuando un usuario autenticado llama a un método protegido con `@PreAuthorize` y no tiene el rol, la API responde `403` con el sobre estándar y el código `ERR_AUTH_07`.

## Alcance

- `B/security/AuthErrorCodes.java`: agrega `ERR_AUTH_07` (403, el usuario no tiene el rol necesario).
- `B/security/ApiAccessDeniedHandler.java`: `AccessDeniedHandler` que escribe el `403` con el mismo `ErrorWriter` que ya usan `ApiAuthenticationEntryPoint` y `UsuarioActivoFilter`, y loguea un `WARN` (`module=auth`, `errorCode`, `userId`; nunca el token).
- `B/config/SecurityConfig.java`: registra el handler en `exceptionHandling(...)`, al lado del `authenticationEntryPoint`.
- `docs/errors/auth/ERRORES_AUTH.md` y `docs/errors/README.md`: documentan `ERR_AUTH_07`.
- `.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md`: una línea en [Roles y permisos](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) que dice que la falta de rol responde `403` `ERR_AUTH_07`.
- Tests: con un endpoint de prueba (o un `service` anotado) protegido por rol, un usuario con otro rol recibe `403` con `status = failed` y `error = ERR_AUTH_07`; con el rol correcto, `200`; sin token, `401` `ERR_AUTH_01` (sin cambio).

Archivos esperados (crear/tocar): los de arriba y su test.

## Fuera de alcance (no tocar)

- Aplicar `@PreAuthorize` a los métodos de `UsuarioPersonalService`: lo hacen las UT 07 a 11.
- El `403` de cuenta desactivada (`ERR_AUTH_03`) y el de bloqueo (`ERR_AUTH_05`).
- Permisos por endpoint en `SecurityConfig` (la regla del proyecto es proteger en el `service`).

## Resultado esperado

Todo `AccessDeniedException` por falta de rol sale como `403` `ERR_AUTH_07` en el sobre de la API.

## Validación

1. `./mvnw test` pasa.
2. El test comprueba el cuerpo completo de la respuesta (`status`, `error`, `message`, `timestamp`), no solo el código HTTP.
3. `./mvnw checkstyle:check` sin errores.
