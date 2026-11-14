# UT-CU14-BE-06 — DTOs de la gestión de cuentas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-06` |
| **Nombre** | DTOs de la gestión de cuentas |
| **Caso de uso** | CU14-BE |
| **Depende de** | — |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (tests de Bean Validation)

## Solicitud de negocio

El Administrador necesita un contrato claro para crear, editar y mostrar las cuentas del personal, con los datos obligatorios validados antes de tocar la base.

## Objetivo

Existen los `record` de entrada y salida de los endpoints de `/api/v1/usuarios`, con sus validaciones.

## Alcance

- `B/features/usuarios/dto/CrearUsuarioPersonalRequest.java`: `nombres`, `apellidos`, `ci`, `correo`, `rol`. `@NotBlank`; `nombres` y `apellidos` hasta 100 caracteres, `ci` hasta 20, `correo` hasta 150 y `@Email`. El rol se valida contra los tres del personal en el `service` (no acá), para devolver `ERR_US_10`.
- `B/features/usuarios/dto/ActualizarUsuarioPersonalRequest.java`: `nombres`, `apellidos`, `correo`, todos opcionales (PATCH: lo que no llega no cambia), con los mismos límites y `@Email`. Un valor que llega no puede estar en blanco. **No** incluye `ci` ni `rol`.
- `B/features/usuarios/dto/CambiarRolRequest.java`: `rol` (`@NotBlank`).
- `B/features/usuarios/dto/UsuarioPersonalResponse.java`: `codigo` (UUID), `nombres`, `apellidos`, `ci`, `correo`, `rol` (nombre del rol) y `activo`. Nunca el `id` interno, el hash ni los campos de bloqueo.
- `B/features/usuarios/dto/UsuarioCreadoResponse.java`: `UsuarioPersonalResponse` más `contrasenaTemporal`. Se usa **solo** en la respuesta del alta.
- Tests de validación: campos en blanco, correo inválido y límites de largo en los tres `Request`.

Archivos esperados (crear): los cinco `record` y su test.

## Fuera de alcance (no tocar)

- El `UsuarioResponse` existente (CU01): se mantiene como está.
- Normalizar el correo y validar el rol: lo hacen los `service` de las UT 08 a 10.
- Los `ErrorCodes` y la lógica de negocio.

## Resultado esperado

Los controllers y services de las UT siguientes comparten estos DTOs.

## Validación

1. `./mvnw test` pasa.
2. Un test comprueba que un `Request` con un campo obligatorio en blanco no pasa la validación.
3. `./mvnw checkstyle:check` sin errores.
