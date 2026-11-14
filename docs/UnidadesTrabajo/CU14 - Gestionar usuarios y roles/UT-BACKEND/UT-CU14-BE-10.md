# UT-CU14-BE-10 — Cambiar el rol de una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-10` |
| **Nombre** | Cambiar el rol de una cuenta |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-03`, `UT-CU14-BE-04`, `UT-CU14-BE-06`, `UT-CU14-BE-09` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)

## Solicitud de negocio

Cuando una persona cambia de funciones, el Administrador le cambia el rol, sin dejar al sistema sin Administradores y dejando registro de quién lo hizo (flujo D, A3, A5).

## Objetivo

`UsuarioPersonalService.cambiarRol(...)` cambia el rol de una cuenta del personal, cierra sus sesiones y anota el cambio con el rol anterior y el nuevo.

## Alcance

- `B/features/usuarios/model/Usuario.java`: agrega `@Setter` en `rol`.
- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `CAMBIO_ROL`.
- `UsuarioPersonalService.cambiarRol(UUID actorCodigo, UUID codigo, CambiarRolRequest)`, con `@Transactional`, `@PreAuthorize` y `@Auditar(accion = CAMBIO_ROL, entidad = "usuario")`. Valida **en este orden**: la cuenta existe y no es de un Postulante (`ERR_US_06`); el rol pedido es uno de los tres del personal (`ValidationException` `ERR_US_10`); no es la propia cuenta del administrador (`ForbiddenException` `ERR_US_08`); la cuenta no tiene ya ese rol (`ConflictException` `ERR_US_12`); si la cuenta es un Administrador **activo** y el rol nuevo no es Administrador, que no sea el último (`validarNoUltimoAdministrador`: cuenta `UsuarioRepository.bloquearAdministradoresActivos()` dentro de la transacción; si queda en cero, `ForbiddenException` `ERR_US_09`).
- Si pasa las validaciones: guarda el rol nuevo y llama a `SesionService.revocarTodas` (A3: la persona entra de nuevo y aterriza en el módulo de su rol).
- Devuelve `ResultadoAuditado<UsuarioPersonalResponse>` con `detalle = {rolAnterior, rolNuevo}`.
- Loguea un `INFO` (`module=usuarios`, `userId`, roles anterior y nuevo).
- Tests: cambio válido entre los tres roles (con `revocarTodas` verificado y el `detalle` correcto); propia cuenta → `ERR_US_08` (aunque sea la última); mismo rol → `ERR_US_12`; rol `POSTULANTE` → `ERR_US_10`; el último Administrador activo no puede dejar de serlo → `ERR_US_09`; sí se puede si hay otro activo, y sí se puede si el Administrador al que se le cambia el rol ya estaba desactivado; ante cualquier rechazo no se revocan sesiones ni se anota nada.

Archivos esperados (crear/tocar): `Usuario.java`, `BitacoraAcciones.java`, `UsuarioPersonalService.java` y sus tests.

## Fuera de alcance (no tocar)

- Desactivar y reactivar (`UT-CU14-BE-11`, que reutiliza `validarNoUltimoAdministrador`).
- Convertir una cuenta de Postulante en personal o al revés.
- Forzar que la persona vea el cambio sin volver a iniciar sesión.

## Resultado esperado

El Administrador cambia el rol de una cuenta, la persona debe iniciar sesión de nuevo y el cambio queda en la bitácora.

## Validación

1. `./mvnw test` pasa.
2. Un test comprueba que, en cualquier rechazo, la cuenta conserva su rol anterior y sus sesiones.
3. `./mvnw checkstyle:check` sin errores.
