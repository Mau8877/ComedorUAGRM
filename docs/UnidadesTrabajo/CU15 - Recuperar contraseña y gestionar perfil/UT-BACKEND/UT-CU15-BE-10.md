# UT-CU15-BE-10 — Restablecer la contraseña con el enlace

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-10` |
| **Nombre** | Restablecer la contraseña con el enlace |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-01`, `UT-CU15-BE-02`, `UT-CU15-BE-04`, `UT-CU15-BE-06`, `UT-CU15-BE-07`, `UT-CU15-BE-08`, `UT-CU15-BE-09` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Quien abre el enlace de su correo define una contraseña nueva y recupera el acceso a su cuenta; el enlace no puede usarse dos veces.

## Objetivo

`RestablecimientoContrasenaService.restablecer(token, contrasenaNueva)` valida el enlace y deja la cuenta con contraseña nueva, desbloqueada y sin sesiones.

## Alcance

- `B/features/auth/service/RestablecimientoContrasenaService.java`: `@Transactional @Auditar(accion = RESTABLECIMIENTO_CONTRASENA, entidad = "usuario")` `ResultadoAuditado<Void> restablecer(String token, String contrasenaNueva)`:
  1. Busca la cuenta por `UsuarioRepository.findByRestablecimientoTokenHash(TokenEnlace.hash(token))`. Si no hay ninguna, el enlace está vencido (`restablecimiento_expira_en` pasado) o fue usado o reemplazado (el hash ya no coincide) → `ValidationException` `ERR_AUTH_08` «El enlace ya no es válido. Solicita uno nuevo.» (mismo mensaje en todos los casos, WARN sin token).
  2. Si la cuenta está **desactivada** → el mismo `ERR_AUTH_08` (no se recupera una cuenta desactivada).
  3. `UsuarioService.restablecerContrasena(usuario, contrasenaNueva)` (guarda hash y desbloquea).
  4. `usuario.limpiarRestablecimiento()` (el enlace ya no sirve).
  5. `SesionService.revocarTodas(usuario)`.
  6. Devuelve el resultado auditado: actor = la cuenta, `entidad_id` = la cuenta, `detalle` sin contraseña ni token.
- Un fallo en cualquier paso deshace todo (el enlace **no** queda limpio si la contraseña no se guardó).
- Tests: enlace vigente → contraseña nueva válida contra BCrypt, vieja ya no sirve (`passwordEncoder.matches`), cuenta desbloqueada, columnas del enlace en `null`, todas las sesiones revocadas; enlace desconocido / vencido / ya usado / reemplazado / de cuenta desactivada → `ERR_AUTH_08` sin cambios; segundo uso del mismo enlace rechazado; el `detalle` auditado no contiene la contraseña ni el token.

Archivos esperados (tocar): `RestablecimientoContrasenaService.java` y su test.

## Fuera de alcance (no tocar)

- Verificar la política de la contraseña (ya lo hace el DTO de UT-08).
- El endpoint (UT-13) y la pantalla.
- Iniciar sesión automáticamente tras restablecer: el usuario vuelve al login (flujo B, paso 11).

## Resultado esperado

Con un enlace bueno, la persona puede iniciar sesión con la contraseña nueva y con ninguna otra, y no queda ninguna sesión abierta.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
