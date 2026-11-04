# UT-CU14-BE-11 — Desactivar y reactivar una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-11` |
| **Nombre** | Desactivar y reactivar una cuenta |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-03`, `UT-CU14-BE-04`, `UT-CU14-BE-06`, `UT-CU14-BE-10` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (borrado lógico)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)

## Solicitud de negocio

Cuando alguien deja el cargo, su cuenta se desactiva sin perder lo que hizo, y si vuelve se reactiva con su mismo rol (flujos E y F, A4, A5, A6).

## Objetivo

`UsuarioPersonalService.desactivar(...)` y `reactivar(...)` cambian `activo` de una cuenta del personal, con las protecciones del CU y la bitácora.

## Alcance

- `B/features/bitacora/BitacoraAcciones.java`: agrega `DESACTIVACION_USUARIO` y `REACTIVACION_USUARIO`.
- `UsuarioPersonalService.desactivar(UUID actorCodigo, UUID codigo)`, con `@Transactional`, `@PreAuthorize` y `@Auditar(accion = DESACTIVACION_USUARIO, entidad = "usuario")`. Valida **en este orden**, siguiendo el diagrama de secuencia: la cuenta existe y no es de un Postulante (`ERR_US_06`); no es la propia cuenta (`ForbiddenException` `ERR_US_08`); no está ya desactivada (`ConflictException` `ERR_US_07`); si es Administrador, que no sea el último activo (`validarNoUltimoAdministrador`, reutilizado de `UT-CU14-BE-10`, `ERR_US_09`). Después marca `activo = false`, llama a `SesionService.revocarTodas` y **no elimina nada** (`eliminado_en` queda en `NULL`).
- `UsuarioPersonalService.reactivar(UUID actorCodigo, UUID codigo)`, con `@Transactional`, `@PreAuthorize` y `@Auditar(accion = REACTIVACION_USUARIO, entidad = "usuario")`: la cuenta existe y no es de un Postulante (`ERR_US_06`); si ya está activa, `ConflictException` `ERR_US_07`; marca `activo = true`. **No cambia el rol**, que es el que tenía (A6), ni restaura sesiones: la persona inicia sesión de nuevo.
- Ambos devuelven `ResultadoAuditado<UsuarioPersonalResponse>` con `detalle` vacío (el rol de la cuenta se ve en su respuesta) y loguean un `INFO` (`module=usuarios`, `userId`).
- Tests: desactivar una cuenta normal (verifica `activo`, `revocarTodas` y la bitácora); propia cuenta → `ERR_US_08`; ya desactivada → `ERR_US_07`; el último Administrador activo → `ERR_US_09` y con otro Administrador activo sí se puede; reactivar una desactivada recupera el acceso y conserva el rol; reactivar una activa → `ERR_US_07`; en cada rechazo no cambia nada ni se anota nada.

Archivos esperados (crear/tocar): `BitacoraAcciones.java`, `UsuarioPersonalService.java` y sus tests.

## Fuera de alcance (no tocar)

- Eliminar (borrado lógico con `eliminado_en`) una cuenta: este CU solo desactiva.
- Reiniciar el contador de bloqueo por intentos fallidos al reactivar.
- El endpoint (`UT-CU14-BE-12`).

## Resultado esperado

Se puede desactivar y reactivar una cuenta del personal sin perder su historial y sin dejar al sistema sin Administradores.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
