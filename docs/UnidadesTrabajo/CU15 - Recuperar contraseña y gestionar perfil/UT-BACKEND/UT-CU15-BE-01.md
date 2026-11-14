# UT-CU15-BE-01 — Códigos de error y acciones de bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-01` |
| **Nombre** | Códigos de error `ERR_AUTH_08`, `ERR_AUTH_09`, `ERR_US_13` y acciones de bitácora |
| **Caso de uso** | CU15-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)

## Solicitud de negocio

Todo rechazo que ve el cliente tiene un código catalogado y documentado, y todo cambio de contraseña o de perfil queda en la bitácora.

## Objetivo

Existen las constantes de error y de acción de bitácora del CU15, y los tres códigos están documentados en el catálogo.

## Alcance

- `B/security/AuthErrorCodes.java`: `ERR_AUTH_08` (400, enlace de restablecimiento no válido: desconocido, vencido, usado o reemplazado) y `ERR_AUTH_09` (400, contraseña actual incorrecta o ausente).
- `B/features/usuarios/exception/UsuarioErrorCodes.java`: `ERR_US_13` (400, solo el Postulante tiene teléfono).
- `B/features/bitacora/constant/BitacoraAcciones.java`: `RESTABLECIMIENTO_CONTRASENA`, `CAMBIO_CONTRASENA` y `ACTUALIZACION_PERFIL`.
- `docs/errors/auth/ERRORES_AUTH.md` y `docs/errors/usuarios/ERRORES_USUARIOS.md`: una sección por código con el formato de la guía (HTTP, excepción, significado, dónde se lanza; las clases todavía no existen: se indica la que las lanzará, `RestablecimientoContrasenaService.restablecer()`, `UsuarioService.verificarContrasenaActual()` y `PerfilService.actualizar()`).
- `docs/errors/README.md`: los tres links en el índice.

Archivos esperados (tocar): los cinco de arriba. No hay tests: son constantes y documentación.

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT-02 a UT-12).
- Cambiar los códigos existentes (`ERR_US_02` se reutiliza tal cual para el correo repetido).

## Resultado esperado

Las constantes existen, compilan y los códigos tienen su sección enlazada desde el índice.

## Validación

1. `./mvnw clean package` compila.
2. Cada código nuevo aparece en `docs/errors/README.md` y su ancla abre la sección correcta.
