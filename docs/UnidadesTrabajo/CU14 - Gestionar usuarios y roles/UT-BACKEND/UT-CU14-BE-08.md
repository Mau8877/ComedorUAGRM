# UT-CU14-BE-08 — Crear una cuenta del personal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-08` |
| **Nombre** | Crear una cuenta del personal |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-05`, `UT-CU14-BE-06` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

El personal no se autorregistra: el Administrador crea la cuenta de cada funcionario con su rol y le entrega una contraseña temporal (flujo B).

## Objetivo

`UsuarioPersonalService.crear(...)` crea una cuenta activa con uno de los tres roles del personal y devuelve la contraseña temporal una sola vez, anotando el alta en la bitácora.

## Alcance

- `B/common/auditoria/ResultadoAuditado.java`: `record ResultadoAuditado<T>(T datos, Long actorId, UUID actorCodigo, Long entidadId, Map<String, Object> detalle, boolean auditar) implements Auditable`. Lleva el DTO que el controller devuelve y los datos que el aspecto necesita, **sin** entidades JPA. Lo reutilizan las UT 09 a 11 (`entidadId` es la cuenta afectada; `actorId` es el administrador).
- `B/features/bitacora/BitacoraAcciones.java`: agrega `ALTA_PERSONAL`.
- `UsuarioPersonalService.crear(UUID actorCodigo, CrearUsuarioPersonalRequest)`, con `@Transactional`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` y `@Auditar(accion = ALTA_PERSONAL, entidad = "usuario")`. Sigue el orden del diagrama de secuencia: normaliza el correo (minúsculas, sin espacios); valida que el `rol` sea uno de los tres del personal (si no, `ValidationException` `ERR_US_10`); verifica que el CI no exista (`ConflictException` `ERR_US_01`) y que el correo no exista (`ConflictException` `ERR_US_02`); busca el `Rol`; **recién entonces** genera la contraseña temporal (`GeneradorContrasenaTemporal`), guarda solo su hash BCrypt y crea el `Usuario` activo, **sin** fila de `postulante`. Una violación de los índices únicos por carrera de dos altas simultáneas se traduce al mismo `409` que `registrarPostulante`.
- Devuelve `ResultadoAuditado<UsuarioCreadoResponse>` con `detalle = {rol}` y la contraseña temporal solo dentro del DTO.
- Resuelve al administrador que actúa (`actorCodigo`, el `sub` del token) a su `Usuario` con un método privado reutilizable por las UT 09 a 11; si no existe, `UnauthorizedException` `ERR_AUTH_02`.
- Loguea un `INFO` (`module=usuarios`, `userId` de la cuenta creada). **Nunca** la contraseña temporal, ni su hash, ni el `Request` completo.
- Tests (repositorios y generador mockeados): crea con cada uno de los tres roles; rol `POSTULANTE` o inexistente → `ERR_US_10`; CI duplicado → `ERR_US_01` y correo duplicado → `ERR_US_02` sin crear nada ni generar contraseña; el hash guardado no es la contraseña temporal; el resultado marca `auditar = true` y no lleva la contraseña en `detalle`; reintentar el mismo alta da `409`.

Archivos esperados (crear/tocar): `ResultadoAuditado.java`, `BitacoraAcciones.java`, `UsuarioPersonalService.java` y sus tests.

## Fuera de alcance (no tocar)

- Mostrar la contraseña en la respuesta HTTP y el `Cache-Control: no-store`: es de `UT-CU14-BE-12`.
- Forzar el cambio en el primer acceso, el enlace de activación y el correo.
- Editar, cambiar rol y cambiar de estado (UT 09 a 11).

## Resultado esperado

Se puede crear la cuenta de un Administrador, un Directivo o un Personal del Comedor; el alta queda en la bitácora con el administrador como actor.

## Validación

1. `./mvnw test` pasa.
2. Un test comprueba que ninguna salida del `service` (log capturado, `detalle`) contiene la contraseña temporal.
3. `./mvnw checkstyle:check` sin errores.
