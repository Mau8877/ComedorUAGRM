# Errores — Usuarios (`US`)

Errores del registro y la gestión de cuentas de usuario.

## `ERR_US_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe un usuario con ese CI (una cuenta eliminada
  lógicamente no cuenta: su CI se puede reutilizar).
- **Dónde se lanza:** `UsuarioService.registrarPostulante()`

## `ERR_US_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe un usuario con ese correo (el correo sale del
  token de Firebase, no del formulario).
- **Dónde se lanza:** `UsuarioService.registrarPostulante()`

## `ERR_US_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe un postulante con ese registro universitario.
- **Dónde se lanza:** `UsuarioService.registrarPostulante()`

## `ERR_US_04`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La carrera indicada en el registro no existe en el
  catálogo de carreras.
- **Dónde se lanza:** `UsuarioService.registrarPostulante()`

## `ERR_US_05`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La misma cuenta de Firebase se está registrando en otra
  solicitud simultánea (doble clic, reintento automático): una de las dos
  choca contra el índice único de `firebase_uid`. Reintentar con el mismo
  token responde 200 (registro idempotente).
- **Dónde se lanza:** `UsuarioService.registrarPostulante()`
