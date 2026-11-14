# UT-CU14-BE-04 — Revocar todas las sesiones de una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-04` |
| **Nombre** | Revocar todas las sesiones de una cuenta |
| **Caso de uso** | CU14-BE |
| **Depende de** | — (edita el `SesionService` creado en `UT-CU01-BE-15`) |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

Al desactivar una cuenta o cambiarle el rol, la persona no debe poder seguir usando sus sesiones abiertas (A3, A4).

## Objetivo

`SesionService.revocarTodas(usuario)` revoca todos los refresh tokens vigentes de esa cuenta.

## Alcance

- `B/features/auth/repository/SesionRepository.java`: una consulta de actualización que marca `revocada_en = ahora` en las sesiones de un usuario que todavía no están revocadas, y devuelve cuántas cerró.
- `B/features/auth/service/SesionService.java`: `revocarTodas(Usuario usuario)`, con `@Transactional`, que usa esa consulta. Idempotente: si no hay sesiones vigentes no es un error. Loguea a nivel `INFO` (`module=auth`, `userId`, cantidad de sesiones cerradas); **nunca** un token ni su hash.
- Tests: con la base real, tras `revocarTodas` el refresh token de cada dispositivo ya no se puede canjear (`ERR_AUTH_06`); las sesiones de **otra** cuenta quedan intactas; llamarlo dos seguidas no falla.

Archivos esperados (crear/tocar): `SesionRepository.java`, `SesionService.java` y sus tests.

## Fuera de alcance (no tocar)

- Revocar desde `UsuarioPersonalService`: lo hacen `UT-CU14-BE-10` y `UT-CU14-BE-11`.
- El cierre de sesión por dispositivo (`cerrar`, CU01) y la limpieza periódica de sesiones vencidas.
- Invalidar los tokens de acceso ya emitidos: viven 15 minutos y `UsuarioActivoFilter` ya niega el acceso a una cuenta desactivada en cada petición.

## Resultado esperado

Un service puede cerrar de una vez todas las sesiones de una cuenta.

## Validación

1. `./mvnw test` pasa.
2. El test de integración abre dos sesiones de la misma cuenta, llama `revocarTodas` y comprueba que ambas fallan al renovar y que una tercera cuenta no se vio afectada.
3. `./mvnw checkstyle:check` sin errores.
