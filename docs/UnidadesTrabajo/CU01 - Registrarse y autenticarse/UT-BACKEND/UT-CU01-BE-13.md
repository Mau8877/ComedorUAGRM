# UT-CU01-BE-13 — POST /api/v1/auth/sesion y GET /api/v1/auth/usuario-actual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-13` |
| **Nombre** | POST /api/v1/auth/sesion y GET /api/v1/auth/usuario-actual |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-04`, `UT-CU01-BE-08`, `UT-CU01-BE-11` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

Tras iniciar sesión en Firebase, el cliente necesita saber quién es la persona y a qué módulo llevarla, y el acceso debe quedar anotado una sola vez.

## Objetivo

`POST /auth/sesion` devuelve el usuario y anota `INICIO_SESION` en la bitácora; `GET /auth/usuario-actual` lo devuelve sin anotar nada.

## Alcance

- `AuthService.iniciarSesion(UUID codigoUsuario, String ip)` (registra `INICIO_SESION` en la bitácora con `ip` y `detalle`) y `AuthService.usuarioActual(UUID codigoUsuario)` (solo lectura) — ambos delegan en `UsuarioService` (nuevo método `obtenerResumen(UUID codigo)`).
- `AuthController`: `POST /api/v1/auth/sesion` y `GET /api/v1/auth/usuario-actual`, ambos con `@CurrentUserId`. Respuesta `UsuarioResponse` (`200`).
- Tests de service y de controller: `sesion` anota exactamente un evento y `usuario-actual` ninguno; sin token `401`; cuenta desactivada `403` `ERR_AUTH_03` y sin cuenta `401` `ERR_AUTH_02` (a través del filtro).

Archivos esperados (tocar): `AuthService.java`, `UsuarioService.java`, `AuthController.java` y sus tests.

## Fuera de alcance (no tocar)

- Cerrar sesión (no existe endpoint: el backend es stateless).
- Registrar intentos fallidos.
- Datos adicionales del perfil (CU15).

## Resultado esperado

Después del login el cliente obtiene el usuario y su rol; cada inicio de sesión queda anotado una vez.

## Validación

1. `./mvnw test` pasa.
2. El test verifica que llamar dos veces a `usuario-actual` no crea filas en `bitacora` y llamar a `sesion` crea una.
3. `./mvnw checkstyle:check` sin errores.
