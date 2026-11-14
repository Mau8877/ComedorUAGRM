# UT-CU01-BE-06 — Respuestas 401 en el sobre de la API y catálogo ERR_AUTH

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-06` |
| **Nombre** | Respuestas 401 en el sobre de la API y catálogo ERR_AUTH |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-05` |

## Antes de implementar

- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

Quien consume la API debe recibir siempre el mismo formato de error, también cuando falla la autenticación, con un código que se pueda buscar en el catálogo.

## Objetivo

Los rechazos de autenticación devuelven el sobre `{status: "failed", error, message}` con `ERR_AUTH_01`, y quedan catalogados los códigos `ERR_AUTH_01` a `ERR_AUTH_03`.

## Alcance

- `B/security/AuthErrorCodes.java`: constantes `ERR_AUTH_01` (token ausente, inválido o vencido, 401), `ERR_AUTH_02` (sin `usuario` registrado, 401) y `ERR_AUTH_03` (cuenta desactivada, 403).
- `B/security/SecurityErrorWriter.java`: escribe a mano el sobre JSON (`ApiResponse.error(...)`, `Content-Type: application/json`, status dado), igual que `RateLimitFilter`, porque estos rechazos ocurren antes del `DispatcherServlet`.
- `B/security/ApiAuthenticationEntryPoint.java`: `AuthenticationEntryPoint` que responde `401` + `ERR_AUTH_01` con el mensaje "Tu sesión no es válida. Inicia sesión de nuevo." Se registra en `SecurityConfig` (`oauth2ResourceServer(... authenticationEntryPoint(...))`).
- `docs/errors/auth/ERRORES_AUTH.md` con las tres secciones (formato de `GUIA_ERRORES_BACKEND.md`: HTTP, excepción — "ninguna, lo escribe el filtro/entry point" —, significado, dónde se lanza) y los tres links en `docs/errors/README.md`.
- Tests: sin token, con token mal firmado y con token vencido → `401`, cuerpo con `status = failed` y `error = ERR_AUTH_01`.

Archivos esperados (crear/tocar): `AuthErrorCodes.java`, `SecurityErrorWriter.java`, `ApiAuthenticationEntryPoint.java`, `SecurityConfig.java`, `docs/errors/auth/ERRORES_AUTH.md`, `docs/errors/README.md`, tests.

## Fuera de alcance (no tocar)

- Lanzar `ERR_AUTH_02` y `ERR_AUTH_03` (lo hace el filtro de `UT-CU01-BE-07`; acá solo quedan definidos y documentados).
- Manejo de `AccessDeniedException` por rol: lo agrega el primer CU que use `@PreAuthorize`.

## Resultado esperado

Toda petición sin sesión válida recibe un `401` en el formato estándar con un código documentado.

## Validación

1. `./mvnw test` pasa: los tres casos de token inválido devuelven el sobre esperado.
2. Verificar que `docs/errors/README.md` enlaza las tres secciones y los anclajes funcionan.
3. `curl -i localhost:8080/api/v1/carreras` sin token devuelve JSON con `"error":"ERR_AUTH_01"`.
