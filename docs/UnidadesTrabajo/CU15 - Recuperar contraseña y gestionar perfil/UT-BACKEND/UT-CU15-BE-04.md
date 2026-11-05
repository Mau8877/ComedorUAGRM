# UT-CU15-BE-04 — Token del enlace: generar y calcular su hash

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-04` |
| **Nombre** | Token del enlace: generar y calcular su hash |
| **Caso de uso** | CU15-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear)

## Solicitud de negocio

El enlace del correo debe llevar un valor imposible de adivinar, y el sistema solo puede guardar una huella suya, nunca el valor.

## Objetivo

Existe un componente que genera tokens aleatorios para los enlaces y calcula su hash SHA-256.

## Alcance

- `B/features/auth/service/TokenEnlace.java`: `@Component` con `String generar()` (32 bytes de `SecureRandom`, Base64 URL sin relleno, igual que el refresh token de `SesionService`) y `String hash(String token)` (SHA-256 en hexadecimal, 64 caracteres). No loguea nunca lo que genera ni lo que recibe.
- Tests: dos tokens generados son distintos; el largo y el alfabeto son los esperados (solo caracteres de URL); `hash` es determinista, de 64 caracteres hexadecimales, y distinto para tokens distintos; coincide con el SHA-256 de un valor conocido.

Archivos esperados (crear): `TokenEnlace.java` y su test.

## Fuera de alcance (no tocar)

- Refactorizar `SesionService` para que use `TokenEnlace` (follow-up del README).
- Guardar el hash o armar el enlace (UT-09).

## Resultado esperado

El `service` pide un token y su hash con una llamada cada uno.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
