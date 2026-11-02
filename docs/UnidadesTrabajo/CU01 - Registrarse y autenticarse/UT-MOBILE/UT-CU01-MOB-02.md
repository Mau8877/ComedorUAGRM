# UT-CU01-MOB-02 — Modelos de la feature y convención de DTOs

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-02` |
| **Nombre** | Modelos de la feature y convención de DTOs |
| **Caso de uso** | CU01-MOB |
| **Depende de** | — |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Antes de escribir el primer servicio que parsea respuestas hay que definir dónde viven los DTOs y cómo se escriben.

## Objetivo

Queda definida la convención de `models/` en la rule y existen los modelos del CU01.

## Alcance

- `CONSUMO_API_MOBILE.md` y la nota de `ARQUITECTURA_MOBILE.md` ("Modelos de datos... todavía sin convención"): documentar la decisión (carpeta `models/` por feature, clases inmutables, `fromJson`/`toJson` manuales).
- `lib/features/auth/models/usuario.dart` (`codigo`, `nombres`, `apellidos`, `correo`, `rol`), `carrera.dart` (`codigo`, `nombre`, `facultad`) y `registro_request.dart` (`toJson()` con `nombres`, `apellidos`, `ci`, `registroUniversitario`, `carreraCodigo`, `telefono`; **sin** correo ni contraseñas).
- Tests: `fromJson` de cada modelo con un `data` de ejemplo del backend; `RegistroRequest.toJson` no contiene `correo` ni contraseñas.

Archivos esperados (crear/tocar): los tres modelos, las dos rules y los tests.

## Fuera de alcance (no tocar)

- Servicios y providers.
- Generación de código (`json_serializable`, `freezed`).

## Resultado esperado

Los modelos del CU01 existen y la convención queda escrita para el resto de los CU.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
