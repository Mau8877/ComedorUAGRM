# UT-CU15-BE-08 — DTOs de recuperación, cambio de contraseña y perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-08` |
| **Nombre** | DTOs de recuperación, cambio de contraseña y perfil |
| **Caso de uso** | CU15-BE |
| **Depende de** | — |

## Antes de implementar

- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`)
- Como referencia de validación: `B/features/auth/dto/RegistroRequest.java` (política de contraseña y confirmación)

## Solicitud de negocio

El contrato de los cuatro flujos tiene que ser claro y rechazar datos mal formados antes de llegar a la lógica.

## Objetivo

Existen los DTOs de entrada y salida del CU15, con sus validaciones.

## Alcance

En `B/features/auth/dto/`:

- `RecuperarContrasenaRequest(correo)`: obligatorio, formato de correo, máximo 150.
- `RestablecerContrasenaRequest(token, contrasena, confirmacionContrasena)`: `token` obligatorio; la contraseña de 6 a 72 caracteres y la confirmación igual (`@AssertTrue`, mismo patrón y mensajes que `RegistroRequest`).
- `CambiarContrasenaRequest(contrasenaActual, contrasenaNueva, confirmacionContrasena, refreshToken)`: la actual obligatoria; la nueva de 6 a 72 y igual a su confirmación; `refreshToken` opcional (lo manda mobile).

En `B/features/usuarios/dto/`:

- `ActualizarPerfilRequest(correo, telefono, contrasenaActual)`: los tres opcionales; si viene `correo`, formato válido y máximo 150; si viene `telefono`, no en blanco y máximo 20. **No** lleva `ci`, nombres, apellidos, registro ni carrera: si el cliente los manda se ignoran (Jackson ya descarta los campos desconocidos; hay un test que lo prueba).
- `PerfilResponse(codigo, nombres, apellidos, ci, correo, rol, activo, postulante)` y `PerfilPostulanteResponse(registroUniversitario, carrera, telefono)`, con `CarreraResponse` (ya existe).

Tests (`RecuperarContrasenaRequestTest`, etc., con el `Validator` de Bean Validation): correo vacío o inválido; contraseña corta (5), de 6 y de 73 caracteres; confirmación distinta; la actual vacía; teléfono en blanco o de 21 caracteres; un cuerpo con `ci` o `rol` extra se deserializa sin error y sin esos campos.

Archivos esperados (crear): los cinco DTOs y sus tests.

## Fuera de alcance (no tocar)

- Mapear entidades a `PerfilResponse` (UT-11).
- Servicios y controllers.

## Resultado esperado

Un cuerpo inválido se rechaza con `400` `ERR_SYS_01` antes de llegar al `service`.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
