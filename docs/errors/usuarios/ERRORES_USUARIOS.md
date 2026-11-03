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
- **Significado:** Ya existe un usuario con ese correo (se compara en
  minúsculas y sin espacios). Si llega de nuevo la misma petición completa
  (mismo correo, CI y contraseña) no es un error: responde 200 (registro
  idempotente).
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
- **Significado:** **Retirado.** Se lanzaba cuando la misma cuenta de
  Firebase se registraba dos veces a la vez (índice único de
  `firebase_uid`). Con la autenticación propia con JWT ya no existe ese
  índice; un doble registro simultáneo cae en `ERR_US_01`/`ERR_US_02`. El
  número no se reutiliza.
- **Dónde se lanza:** (ya no se lanza)
