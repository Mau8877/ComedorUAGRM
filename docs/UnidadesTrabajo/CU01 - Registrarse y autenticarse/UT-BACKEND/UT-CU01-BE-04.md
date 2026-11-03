# UT-CU01-BE-04 — Bitácora: Model, repositorio y service para registrar eventos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-04` |
| **Nombre** | Bitácora: Model, repositorio y service para registrar eventos |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-01`, `UT-CU01-BE-03` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

Todo registro o inicio de sesión exitoso debe quedar anotado para poder auditarlo después (CU25), sin guardar datos sensibles.

## Objetivo

Existe `BitacoraService.registrar(...)`, que inserta una fila append-only en `bitacora`.

## Alcance

- `B/features/bitacora/model/Bitacora.java`: extiende `BaseModel`, `@Table(name = "bitacora")`, `@AttributeOverride(... "codigo_bitacora")`. Campos: `usuario` (`@ManyToOne` LAZY, nullable), `accion`, `entidad`, `entidadId`, `detalle` (`Map<String, Object>` con `@JdbcTypeCode(SqlTypes.JSON)`), `ip` (`@JdbcTypeCode(SqlTypes.INET)`). **Sin setters** (append-only; nunca se actualiza ni se borra).
- `B/features/bitacora/BitacoraAcciones.java`: constantes `REGISTRO_USUARIO` e `INICIO_SESION`.
- `B/features/bitacora/repository/BitacoraRepository.java`.
- `B/features/bitacora/service/BitacoraService.java`: `registrar(Usuario usuario, String accion, String entidad, Long entidadId, Map<String, Object> detalle, String ip)`, con `@Transactional` (se une a la transacción de quien lo llama). Loguea a nivel INFO el evento **sin** datos sensibles.
- Tests: unitario del service con `BitacoraRepository` mockeado (se guarda lo indicado); con Docker, que `detalle` e `ip` se guardan y leen.

Archivos esperados (crear): los cuatro de arriba y sus tests.

## Fuera de alcance (no tocar)

- Consultar o listar la bitácora (CU25).
- Registrar intentos fallidos de inicio de sesión (se cuentan en `usuario.intentos_fallidos`, no en la bitácora).
- Llamar al service: lo hacen `UT-CU01-BE-11` y `UT-CU01-BE-13`.

## Resultado esperado

Cualquier service puede anotar un evento en la bitácora dentro de su transacción.

## Validación

1. `./mvnw test` pasa: el test unitario verifica que `registrar` guarda una `Bitacora` con la acción, la entidad y la IP recibidas.
2. Con Docker: el test de integración guarda y relee `detalle` (jsonb) e `ip` (inet).
3. `./mvnw checkstyle:check` sin errores.
