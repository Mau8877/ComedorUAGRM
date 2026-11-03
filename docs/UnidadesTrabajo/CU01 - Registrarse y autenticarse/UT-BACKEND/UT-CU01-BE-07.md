# UT-CU01-BE-07 — Filtro que resuelve el usuario autenticado y controla cuenta activa

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-07` |
| **Nombre** | Filtro que resuelve el usuario autenticado y controla cuenta activa |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-03`, `UT-CU01-BE-05`, `UT-CU01-BE-06` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)

## Solicitud de negocio

Tener un token válido no basta: la persona debe tener una cuenta existente y activa en el sistema, con su rol, y las rutas de acceso (registro, login, refresh, logout) deben poder usarse sin sesión.

## Objetivo

Cada petición autenticada queda asociada a un `usuario` activo con su rol, o es rechazada con `ERR_AUTH_02` (el usuario ya no existe) o `ERR_AUTH_03` (desactivada); las rutas públicas ignoran el header `Authorization`.

## Alcance

- `B/security/UsuarioAutenticableProvider.java` (interfaz, definida en `security/`): `Optional<DatosUsuario> buscarPorCodigo(UUID codigo)`, con el `record DatosUsuario(UUID codigo, String rol, boolean activo)`.
- `B/security/UsuarioAutenticado.java` (`record`): `UUID codigo`, `String rol`.
- `B/security/UsuarioActivoFilter.java` (`OncePerRequestFilter`, registrado después del filtro de bearer token): toma el `sub` del JWT (un `sub` que no es un UUID → `401` `ERR_AUTH_01`) y consulta el provider. Existe y está activo → deja el `Authentication` con principal `UsuarioAutenticado` y `authorities` `ROLE_{rol}`. Existe y está desactivado → `403` `ERR_AUTH_03`. No existe → `401` `ERR_AUTH_02`. Solo actúa sobre `/api/v1/**`.
- `B/security/RutasPublicas.java`: lista de los endpoints sin sesión (`GET /carreras`, `POST /auth/registro|login|refresh|logout`) y `esPublica(request)`. `SecurityConfig` los usa en `permitAll` y en un `BearerTokenResolver` que **ignora el header `Authorization` en esas rutas**: así un token vencido que el cliente sigue mandando no hace fallar el login.
- `B/features/usuarios/service/UsuarioAutenticableAdapter.java` (`@Service`): implementa el provider con `UsuarioRepository`.
- `SecurityConfig`: registrar el filtro y el resolver.
- Tests con un provider simulado: activo (pasa, con el rol), desactivado (`403` `ERR_AUTH_03`), usuario inexistente (`401` `ERR_AUTH_02`), `sub` inválido (`401` `ERR_AUTH_01`), sin autenticación previa (continúa sin consultar).
- El filtro no loguea el token.

Archivos esperados (crear/tocar): los de arriba, `SecurityConfig.java` y los tests.

## Fuera de alcance (no tocar)

- `@CurrentUserId` (`UT-CU01-BE-08`).
- Los endpoints (`UT-CU01-BE-11` en adelante).
- Cachear el estado `activo` en Redis (follow-up si el costo importa).

## Resultado esperado

Los endpoints protegidos solo se alcanzan con una cuenta activa, y las rutas públicas funcionan aunque el cliente mande un token vencido.

## Validación

1. `./mvnw test` pasa con los escenarios del filtro.
2. `./mvnw checkstyle:check` sin errores.
3. Revisar que `security/` no importa nada de `features/` (solo la interfaz propia).
