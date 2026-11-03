# UT-CU01-BE-10 — DTOs de registro y respuesta, y códigos ERR_US

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-10` |
| **Nombre** | DTOs de registro y respuesta, y códigos ERR_US |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-02` |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)

## Solicitud de negocio

El registro necesita un contrato claro de entrada y salida y errores catalogados para cada conflicto, de modo que el cliente pueda marcar el campo en conflicto.

## Objetivo

Existen `RegistroRequest`, `UsuarioResponse` y `UsuarioErrorCodes`, con los códigos documentados en `docs/errors/`.

## Alcance

- `B/features/auth/dto/RegistroRequest.java` (`record`) con las validaciones de Bean Validation del contrato del README (`@NotBlank`, `@Size` máximas, `@NotNull` en `carreraCodigo`, `@Email` en `correo`, contraseña de 6 a 72 caracteres y `@AssertTrue` en `confirmacionCoincide`), mensajes en español. `registroUniversitario` con `@Pattern(regexp = "^[1-9]\\d{8}$")` (exactamente 9 dígitos y sin cero inicial, mensaje en español); sin formato de CI (por definir con la DUBSS).
- `B/features/usuarios/dto/UsuarioResponse.java` (`record`): `codigo`, `nombres`, `apellidos`, `correo`, `rol`.
- `B/features/usuarios/UsuarioErrorCodes.java`: `ERR_US_01` a `ERR_US_04` (ver tabla del README).
- `docs/errors/usuarios/ERRORES_USUARIOS.md` (cuatro secciones, formato de `GUIA_ERRORES_BACKEND.md`) y sus links en `docs/errors/README.md`.
- Test de validación (`spring-boot-starter-validation-test`): campos vacíos y largos excedidos producen violaciones; contraseña de 5 caracteres, de más de 72 o que no coincide con su confirmación, y un correo sin formato, también; un request completo no.

Archivos esperados (crear/tocar): los tres Java, `docs/errors/usuarios/ERRORES_USUARIOS.md`, `docs/errors/README.md`, test.

## Fuera de alcance (no tocar)

- Lanzar los errores (`UT-CU01-BE-11`).
- Un campo `rol` en el request (nunca se acepta del cliente: siempre es `POSTULANTE`).

## Resultado esperado

Contrato y catálogo de errores del registro definidos y documentados.

## Validación

1. `./mvnw test` pasa (incluido el test de validación).
2. Cada código de `UsuarioErrorCodes` tiene su sección en `docs/errors/usuarios/ERRORES_USUARIOS.md` y su link en el índice.
3. `./mvnw checkstyle:check` sin errores.
