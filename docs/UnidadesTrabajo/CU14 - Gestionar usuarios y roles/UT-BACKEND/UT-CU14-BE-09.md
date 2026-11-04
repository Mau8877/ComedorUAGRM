# UT-CU14-BE-09 — Editar los datos de una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-09` |
| **Nombre** | Editar los datos de una cuenta |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-06`, `UT-CU14-BE-08` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok) (setters por campo)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)

## Solicitud de negocio

Los datos de un funcionario cambian (un apellido mal escrito, un correo nuevo) y el Administrador tiene que poder corregirlos sin tocar su CI ni su rol (flujo C).

## Objetivo

`UsuarioPersonalService.editar(...)` actualiza nombres, apellidos y correo de una cuenta del personal y anota la edición en la bitácora.

## Alcance

- `B/features/usuarios/model/Usuario.java`: agrega `@Setter` **solo** en `nombres`, `apellidos` y `correo`. No en `ci`, `passwordHash` ni `rol` (el rol lo cambia `UT-CU14-BE-10`).
- `B/features/bitacora/BitacoraAcciones.java`: agrega `EDICION_USUARIO`.
- `UsuarioPersonalService.editar(UUID actorCodigo, UUID codigo, ActualizarUsuarioPersonalRequest)`, con `@Transactional`, `@PreAuthorize` y `@Auditar(accion = EDICION_USUARIO, entidad = "usuario")`. Busca la cuenta con `findPersonalByCodigo` (si no existe o es de un Postulante, `NotFoundException` `ERR_US_06`; método privado que reutilizan las UT 10 y 11); aplica solo los campos que llegan; normaliza el correo; si el correo cambia y ya lo usa **otra** cuenta, `ConflictException` `ERR_US_02`. Un administrador puede editar sus propios datos.
- Devuelve `ResultadoAuditado<UsuarioPersonalResponse>` con `detalle = {camposModificados: [nombres, ...]}` (solo los **nombres** de los campos). Si el `Request` no cambia nada, `auditar = false` (no se anota una edición vacía) y responde igual la cuenta.
- Loguea un `INFO` (`module=usuarios`, `userId`). Los lecturas y los no-cambios no se loguean.
- Tests: cambia solo lo enviado; correo duplicado → `ERR_US_02`; correo propio sin cambio no es conflicto; cuenta inexistente y cuenta de Postulante → `ERR_US_06`; sin cambios → `auditar = false`; el `detalle` no contiene valores de los campos.

Archivos esperados (crear/tocar): `Usuario.java`, `BitacoraAcciones.java`, `UsuarioPersonalService.java` y sus tests.

## Fuera de alcance (no tocar)

- Editar el CI o el rol, la contraseña y los datos del Postulante.
- El endpoint (`UT-CU14-BE-12`).
- Cerrar las sesiones al editar: editar nombres o correo no las invalida.

## Resultado esperado

El Administrador corrige los datos de una cuenta del personal y queda anotado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
