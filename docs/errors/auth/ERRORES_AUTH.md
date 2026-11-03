# Errores — Autenticación (`AUTH`)

Errores de la autenticación con JWT propio: verificación del token de acceso,
usuario asociado a cada petición, inicio de sesión y refresh token. Los de la
capa de seguridad (`security/`) se escriben a mano, en el sobre estándar de la
API, antes de que la petición llegue a un controller; los del inicio de sesión
y el refresh los lanzan los services como excepciones de negocio (ver
[SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).

## `ERR_AUTH_01`

- **HTTP:** 401
- **Excepción:** ninguna — lo escribe `ApiAuthenticationEntryPoint` directo
  (no pasa por `GlobalExceptionHandler`); también `UsuarioActivoFilter` si el
  `sub` del token no es un código de usuario.
- **Significado:** La petición no trae un token de acceso válido: falta el
  header `Authorization`, el token está mal firmado, es de otro emisor o ya
  venció. El cliente debe renovarlo con `POST /api/v1/auth/refresh`.
- **Dónde se lanza:** `ApiAuthenticationEntryPoint.commence()`

## `ERR_AUTH_02`

- **HTTP:** 401
- **Excepción:** ninguna — lo escribe `UsuarioActivoFilter` directo (y
  `UnauthorizedException` desde `CurrentUserIdArgumentResolver` y
  `UsuarioService.obtenerPorCodigo()`).
- **Significado:** El token de acceso es válido, pero el usuario al que
  pertenece ya no existe en el sistema (por ejemplo, la cuenta fue eliminada
  después de emitir el token).
- **Dónde se lanza:** `UsuarioActivoFilter.doFilterInternal()`

## `ERR_AUTH_03`

- **HTTP:** 403
- **Excepción:** `ForbiddenException` (en el login y el refresh); en el resto
  de las peticiones lo escribe `UsuarioActivoFilter` directo.
- **Significado:** El usuario existe pero su cuenta está desactivada
  (`usuario.activo = false`); debe contactar a la DUBSS. Se consulta en cada
  petición, así que desactivar una cuenta surte efecto de inmediato aunque
  su token de acceso siga vigente, y tampoco puede iniciar sesión ni renovar.
- **Dónde se lanza:** `UsuarioActivoFilter.doFilterInternal()`,
  `UsuarioService.validarCredenciales()`, `SesionService.renovar()`

## `ERR_AUTH_04`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** Correo o contraseña incorrectos en el inicio de sesión. El
  mensaje es el mismo exista o no el correo (no revela cuál dato falló).
  Cada fallo suma un intento fallido a la cuenta.
- **Dónde se lanza:** `UsuarioService.validarCredenciales()`

## `ERR_AUTH_05`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** La cuenta está bloqueada temporalmente por 5 intentos
  fallidos de inicio de sesión consecutivos; se niega el acceso aunque la
  contraseña sea correcta, hasta que termine el bloqueo. La duración crece
  con cada bloqueo seguido (5 min, 15 min, 1 h, 24 h). El mensaje indica
  cuántos minutos faltan.
- **Dónde se lanza:** `UsuarioService.validarCredenciales()`

## `ERR_AUTH_06`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** El refresh token no sirve: no existe, venció, o ya fue
  revocado (por un logout o porque ya se usó para renovar la sesión). El
  cliente debe llevar al usuario a la pantalla de inicio de sesión.
- **Dónde se lanza:** `SesionService.renovar()`
