# UT-CU15-BE-07 — Revocar las sesiones de una cuenta menos la actual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-07` |
| **Nombre** | Revocar las sesiones de una cuenta menos la actual |
| **Caso de uso** | CU15-BE |
| **Depende de** | — (edita `SesionService` y `SesionRepository`, que ya existen) |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Al cambiar la contraseña desde el perfil, el resto de los dispositivos deben quedar sin sesión, pero el dispositivo desde el que se hizo el cambio no debe sacar al usuario.

## Objetivo

`SesionService` puede cerrar las sesiones de una cuenta conservando una (la actual).

## Alcance

- `B/features/auth/repository/SesionRepository.java`: `@Modifying(flushAutomatically = true)` `revocarTodasMenos(usuarioId, tokenHashConservado, ahora)`, que revoca las sesiones de la cuenta sin revocar cuyo `tokenHash` es distinto del recibido; devuelve cuántas cerró. Sin `clearAutomatically`, igual que `revocarTodasDe`.
- `B/features/auth/service/SesionService.java`: `revocarOtras(Usuario usuario, String refreshTokenActual)`. Si el token es nulo o vacío no se puede saber cuál conservar y revoca **todas** (`revocarTodas`). Si llega, calcula su hash y llama al repositorio. Loguea INFO con `userId` y la cantidad, nunca el token ni su hash.
- Tests: con tres sesiones de una cuenta, `revocarOtras` deja vigente solo la indicada; sin token revoca las tres; no toca las sesiones de otra cuenta; con un token desconocido revoca todas; idempotente (la segunda llamada cierra 0).

Archivos esperados (tocar): `SesionRepository.java`, `SesionService.java` y sus tests (`SesionRepositoryTest`, `SesionServiceTest`).

## Fuera de alcance (no tocar)

- `revocarTodas`, `cerrar` y `renovar`, que no cambian.
- Endpoint o lógica de cambio de contraseña (UT-12).

## Resultado esperado

El cambio de contraseña puede cerrar los demás dispositivos y mantener el actual con una llamada.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
