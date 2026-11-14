# UT-CU15-BE-12 — Cambiar la contraseña desde el perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-12` |
| **Nombre** | Cambiar la contraseña desde el perfil |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-01`, `UT-CU15-BE-06`, `UT-CU15-BE-07`, `UT-CU15-BE-08`, `UT-CU15-BE-11` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

Un usuario autenticado cambia su contraseña confirmando la actual; si alguien más tenía su cuenta abierta en otro dispositivo, esa sesión se cierra.

## Objetivo

`PerfilService.cambiarContrasena(...)` guarda la nueva contraseña, cierra las demás sesiones y conserva la actual.

## Alcance

- `B/features/usuarios/service/PerfilService.java`: `@Transactional @Auditar(accion = CAMBIO_CONTRASENA, entidad = "usuario") ResultadoAuditado<Void> cambiarContrasena(UUID usuarioCodigo, CambiarContrasenaRequest request, String refreshTokenActual)`:
  1. Resuelve la cuenta del token (`ERR_AUTH_02` si no existe).
  2. `UsuarioService.cambiarContrasena` (verifica la actual con `ERR_AUTH_09` y guarda el hash nuevo).
  3. `SesionService.revocarOtras(usuario, refreshTokenActual)`. `refreshTokenActual` lo resuelve el controller: el del cuerpo (mobile) gana sobre la cookie (web). Sin ninguno se cierran todas (ver README).
  4. Resultado auditado con actor y entidad = la cuenta y `detalle` vacío o con la cantidad de sesiones cerradas; nunca contraseñas.
- La contraseña actual incorrecta **no cuenta** como intento fallido de inicio de sesión ni bloquea la cuenta (la persona ya está autenticada); solo se rechaza con `400`.
- Tests: cambio correcto (hash nuevo válido, el viejo no, otras sesiones revocadas, la actual vigente, una fila de bitácora); actual incorrecta → `ERR_AUTH_09` sin cambiar nada ni cerrar sesiones; sin refresh token → todas las sesiones cerradas; el `detalle` no contiene contraseñas.

Archivos esperados (tocar): `PerfilService.java` y `PerfilServiceTest.java`.

## Fuera de alcance (no tocar)

- Emitir tokens nuevos tras el cambio: los vigentes siguen sirviendo.
- Avisar por correo del cambio de contraseña.
- Forzar el cambio de la contraseña temporal del personal (follow-up).

## Resultado esperado

Tras el cambio, solo la sesión desde la que se hizo sigue abierta y la contraseña anterior ya no entra.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
