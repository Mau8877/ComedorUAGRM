# UT-CU14-BE-05 — Generador de contraseña temporal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-05` |
| **Nombre** | Generador de contraseña temporal |
| **Caso de uso** | CU14-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear)

## Solicitud de negocio

Al crear la cuenta de un funcionario, el sistema necesita una contraseña inicial segura que el Administrador pueda entregarle.

## Objetivo

Existe un componente que genera contraseñas temporales aleatorias que cumplen la política de contraseñas del sistema.

## Alcance

- `B/features/usuarios/service/GeneradorContrasenaTemporal.java`: `@Component` con `String generar()`. Usa `SecureRandom`, produce 12 caracteres de un alfabeto de letras (mayúsculas y minúsculas) y dígitos **sin** los que se confunden al leerlos (`0`, `O`, `1`, `l`, `I`), y garantiza al menos una letra y un dígito. Cumple la política del CU01 (6 a 72 caracteres) con holgura.
- No loguea nunca lo que genera, ni a nivel `DEBUG`.
- Tests: longitud 12; solo caracteres del alfabeto permitido; al menos una letra y un dígito; 1000 generaciones consecutivas sin repetirse.

Archivos esperados (crear): `GeneradorContrasenaTemporal.java` y su test.

## Fuera de alcance (no tocar)

- Guardar el hash y mostrarla una vez: lo hace `UT-CU14-BE-08`.
- Forzar el cambio en el primer acceso (por definir con la DUBSS).
- Enlaces de activación por correo (CU15).

## Resultado esperado

El `service` puede pedir una contraseña temporal aleatoria con una sola llamada.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
