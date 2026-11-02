# UT-CU01-BE-08 — Anotación @CurrentUserId y seguridad por método

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-08` |
| **Nombre** | Anotación @CurrentUserId y seguridad por método |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-07` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)

## Solicitud de negocio

Los controllers necesitan saber quién es el usuario de la petición sin leer el token a mano, y el proyecto debe poder proteger por rol en los services.

## Objetivo

Existe `@CurrentUserId UUID` para los parámetros de controller y está habilitada la seguridad por método (`@PreAuthorize`).

## Alcance

- `B/security/CurrentUserId.java` (anotación de parámetro) y `CurrentUserIdArgumentResolver.java` (`HandlerMethodArgumentResolver` que devuelve `UsuarioAutenticado.codigo()`; `401` `ERR_AUTH_02` si el principal no es un `UsuarioAutenticado`).
- Registrarlo en una `WebMvcConfigurer` en `B/config/WebConfig.java`.
- `@EnableMethodSecurity` en `SecurityConfig`.
- Test con un controller de prueba (solo en `T/`) que recibe `@CurrentUserId UUID` y devuelve el valor.

Archivos esperados (crear/tocar): `CurrentUserId.java`, `CurrentUserIdArgumentResolver.java`, `WebConfig.java`, `SecurityConfig.java`, test.

## Fuera de alcance (no tocar)

- Usar `@PreAuthorize` en algún endpoint (ninguno de este CU lo necesita).
- Una anotación `@CurrentUser` con más datos.

## Resultado esperado

Un controller declara `@CurrentUserId UUID id` y recibe el `codigo` del usuario autenticado.

## Validación

1. `./mvnw test` pasa: el controller de prueba devuelve el `codigo` del usuario simulado.
2. `./mvnw checkstyle:check` sin errores.
