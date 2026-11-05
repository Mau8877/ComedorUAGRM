# UT-CU15-BE-11 — Consultar y actualizar el perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-11` |
| **Nombre** | Consultar y actualizar el perfil |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-01`, `UT-CU15-BE-06`, `UT-CU15-BE-08` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok)

## Solicitud de negocio

Cada usuario ve sus datos y mantiene al día su correo y, si es Postulante, su teléfono, sin poder tocar su CI.

## Objetivo

`PerfilService` devuelve el perfil del usuario autenticado y actualiza su correo y teléfono con las protecciones del CU.

## Alcance

- `B/features/usuarios/model/Postulante.java`: `@Setter` **solo** sobre `telefono`.
- `B/features/usuarios/repository/PostulanteRepository.java`: `findByUsuarioId(Long)` si no existe ya.
- `B/features/usuarios/service/PerfilService.java` (`@Service`, `@RequiredArgsConstructor`):
  - `@Transactional(readOnly = true) PerfilResponse obtener(UUID usuarioCodigo)`: resuelve la cuenta del token (si no existe, `UnauthorizedException` `ERR_AUTH_02`, igual que `UsuarioService.obtenerPorCodigo`) y arma `PerfilResponse` con el rol y, si es Postulante, `registroUniversitario`, carrera y teléfono.
  - `@Transactional @Auditar(accion = ACTUALIZACION_PERFIL, entidad = "usuario") ResultadoAuditado<PerfilResponse> actualizar(UUID usuarioCodigo, ActualizarPerfilRequest request)`:
    - **Correo**: si viene y es distinto del actual (normalizado), exige `contrasenaActual` correcta (`UsuarioService.verificarContrasenaActual`, `ERR_AUTH_09`) y que no pertenezca a otra cuenta (`existsByCorreo` → `ConflictException` `ERR_US_02`, y se traduce la violación del índice único `uq_usuario_correo` si dos cambios chocan, igual que `UsuarioService`). Un correo igual al actual no cuenta como cambio.
    - **Teléfono**: si viene y la cuenta no es Postulante → `ValidationException` `ERR_US_13`; si es Postulante y es distinto, se guarda.
    - El CI, nombres, apellidos y demás **no se tocan**.
    - La respuesta trae el perfil resultante. `detalle` = `{"campos": ["correo", "telefono"]}` solo con los campos que cambiaron de verdad; **sin valores**. Si no cambió nada, `auditar()` es `false` (no hay fila en la bitácora) y responde igual `200`.
  - Un solo `PATCH` con correo inválido por conflicto no deja guardado el teléfono (todo en la misma transacción).
- Tests de servicio: obtener de Postulante y de Administrador; cambio de teléfono sin contraseña actual; cambio de correo con la actual correcta, incorrecta y ausente (`ERR_AUTH_09`); correo ajeno (`ERR_US_02`) sin guardar nada; correo con mayúsculas se normaliza; teléfono de un no Postulante (`ERR_US_13`); `PATCH` sin cambios → no audita; el `detalle` no lleva valores ni contraseñas.

Archivos esperados (crear): `PerfilService.java` y su test. (Tocar): `Postulante.java`, `PostulanteRepository.java`.

## Fuera de alcance (no tocar)

- Cambiar la contraseña (UT-12) y el endpoint (UT-13).
- Editar nombres, apellidos, registro universitario o carrera.
- Verificar el correo nuevo con un enlace (follow-up).

## Resultado esperado

Un Postulante puede actualizar su teléfono y su correo (con su contraseña), y el sistema rechaza cada caso de A4 a A7.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
