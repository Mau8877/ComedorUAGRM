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

## `ERR_US_06`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La cuenta indicada no existe, está eliminada o es la de un
  Postulante (los Postulantes no se gestionan en el CU14: para esa
  operación no existen).
- **Dónde se lanza:** `UsuarioPersonalService.buscarPersonal()` (usado por
  `editar()`, `cambiarRol()`, `desactivar()` y `reactivar()`)

## `ERR_US_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La cuenta ya está en el estado pedido: se intentó
  desactivar una cuenta ya desactivada o reactivar una ya activa.
- **Dónde se lanza:** `UsuarioPersonalService.desactivar()` y
  `UsuarioPersonalService.reactivar()`

## `ERR_US_08`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El Administrador intentó desactivar su propia cuenta o
  cambiarse a sí mismo el rol.
- **Dónde se lanza:** `UsuarioPersonalService.cambiarRol()` y
  `UsuarioPersonalService.desactivar()`

## `ERR_US_09`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** La operación dejaría al sistema sin ningún Administrador
  DUBSS activo (se intentó desactivar o quitarle el rol Administrador a la
  cuenta del último Administrador activo).
- **Dónde se lanza:** `UsuarioPersonalService.validarNoUltimoAdministrador()`
  (llamado desde `cambiarRol()` y `desactivar()`)

## `ERR_US_10`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El rol indicado no es uno de los tres del personal
  (Administrador DUBSS, Directivo o Personal del Comedor): no existe o es
  Postulante. También se usa al filtrar el listado por un rol inválido.
- **Dónde se lanza:** `UsuarioPersonalService.rolDelPersonal()` (usado por
  `listar()`, `crear()` y `cambiarRol()`)

## `ERR_US_11`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El listado de cuentas recibió un `sort` por un campo no
  soportado o un `filter[...]` que no existe (los soportados son
  `filter[rol]` y `filter[activo]`) o con un valor inválido.
- **Dónde se lanza:** `UsuarioPersonalService.listar()` y
  `UsuarioController.listar()`

## `ERR_US_12`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se pidió cambiar el rol de una cuenta al mismo rol que ya
  tiene.
- **Dónde se lanza:** `UsuarioPersonalService.cambiarRol()`
