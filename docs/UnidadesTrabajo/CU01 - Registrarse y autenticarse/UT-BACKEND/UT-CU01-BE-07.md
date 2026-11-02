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

Tener un token válido de Firebase no basta: la persona debe tener una cuenta registrada y activa en el sistema, con su rol.

## Objetivo

Cada petición autenticada queda asociada a un `usuario` activo con su rol, o es rechazada con `ERR_AUTH_02` (sin cuenta) o `ERR_AUTH_03` (desactivada).

## Alcance

- `B/security/UsuarioAutenticableProvider.java` (interfaz, definida en `security/`): `Optional<DatosUsuario> buscarPorFirebaseUid(String uid)`, con el `record DatosUsuario(UUID codigo, String rol, boolean activo)`.
- `B/security/UsuarioAutenticado.java` (`record`): `UUID codigo`, `String rol`, `String firebaseUid`; y `B/security/FirebaseSinRegistro.java` (`record`): `String firebaseUid`, `String correo`.
- `B/security/FirebaseUsuarioFilter.java` (`OncePerRequestFilter`, registrado después del filtro de bearer token): con el `uid` del JWT consulta el provider. Existe y está activo → deja el `Authentication` con principal `UsuarioAutenticado` y `authorities` `ROLE_{rol}`. Existe y está desactivado → `403` `ERR_AUTH_03`. No existe → si la petición es `POST /api/v1/auth/registro`, deja pasar con principal `FirebaseSinRegistro` (el correo sale del claim `email`); en cualquier otra ruta, `401` `ERR_AUTH_02`.
- `B/features/usuarios/service/UsuarioAutenticableAdapter.java` (`@Service`): implementa el provider con `UsuarioRepository`.
- `SecurityConfig`: registrar el filtro.
- Tests con un provider simulado: activo (pasa, con el rol), desactivado (`403` `ERR_AUTH_03`), sin cuenta en ruta normal (`401` `ERR_AUTH_02`), sin cuenta en `POST /api/v1/auth/registro` (pasa).
- El filtro no loguea el token.

Archivos esperados (crear/tocar): los seis de arriba, `SecurityConfig.java` y los tests.

## Fuera de alcance (no tocar)

- `@CurrentUserId` (`UT-CU01-BE-08`).
- Los endpoints (`UT-CU01-BE-11` en adelante).
- Cachear el estado `activo` en Redis (follow-up si el costo importa).

## Resultado esperado

Los endpoints protegidos solo se alcanzan con una cuenta activa, y el registro es el único que acepta un token sin cuenta.

## Validación

1. `./mvnw test` pasa con los cuatro escenarios del filtro.
2. `./mvnw checkstyle:check` sin errores.
3. Revisar que `security/` no importa nada de `features/` (solo la interfaz propia).
