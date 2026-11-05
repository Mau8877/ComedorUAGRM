# UT-CU15-BE-06 — Contraseña de la cuenta: verificar, cambiar y restablecer

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-06` |
| **Nombre** | Contraseña de la cuenta: verificar, cambiar y restablecer |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-01` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok) (setters por campo, nunca `@Data`)

## Solicitud de negocio

El sistema debe poder comprobar la contraseña actual de una persona y guardar una nueva, sin que el hash quede expuesto a modificaciones arbitrarias.

## Objetivo

`UsuarioService` sabe verificar la contraseña actual, cambiarla y restablecerla (desbloqueando la cuenta).

## Alcance

- `B/features/usuarios/model/Usuario.java`: método `cambiarPasswordHash(String nuevoHash)` (no un `@Setter` del campo: el hash sigue sin setter público genérico).
- `B/features/usuarios/service/UsuarioService.java`, tres métodos, todos `@Transactional`:
  - `verificarContrasenaActual(Usuario usuario, String contrasenaActual)`: si es nula, vacía o no coincide con el hash (`PasswordEncoder.matches`), lanza `ValidationException` `ERR_AUTH_09` «La contraseña actual es incorrecta» (WARN sin datos personales, solo el código).
  - `cambiarContrasena(Usuario usuario, String contrasenaActual, String contrasenaNueva)`: verifica la actual y guarda el hash BCrypt de la nueva.
  - `restablecerContrasena(Usuario usuario, String contrasenaNueva)`: guarda el hash de la nueva y deja `intentosFallidos = 0`, `bloqueosConsecutivos = 0` y `bloqueadoHasta = null`. No pide la actual.
- Ninguno loguea contraseñas ni hashes.
- Tests (con repositorio y encoder mockeados o reales según el caso): actual correcta / incorrecta / nula; el hash guardado no es la contraseña y valida contra BCrypt; `restablecerContrasena` desbloquea una cuenta con intentos y bloqueo; las cuentas heredadas con hash `'!'` pueden restablecerse.

Archivos esperados (tocar): `Usuario.java`, `UsuarioService.java` y `UsuarioServiceTest.java`.

## Fuera de alcance (no tocar)

- Cerrar sesiones (UT-07, UT-10, UT-12) y anotar la bitácora (los métodos que llaman a estos).
- Validar la política de la contraseña nueva: es Bean Validation de los DTOs (UT-08).
- El flujo de inicio de sesión (`validarCredenciales`), que no cambia.

## Resultado esperado

Los servicios de recuperación y de perfil reutilizan estos tres métodos sin tocar el hash a mano.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
