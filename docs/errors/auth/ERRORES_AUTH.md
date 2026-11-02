# Errores — Autenticación (`AUTH`)

Errores de la verificación del ID token de Firebase y del usuario asociado
a cada petición. Los escribe la capa de seguridad (`security/`) a mano, en
el sobre estándar de la API, antes de que la petición llegue a un
controller (ver
[SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).

## `ERR_AUTH_01`

- **HTTP:** 401
- **Excepción:** ninguna — lo escribe `ApiAuthenticationEntryPoint` directo
  (no pasa por `GlobalExceptionHandler`).
- **Significado:** La petición no trae un ID token de Firebase válido: falta
  el header `Authorization`, el token está mal firmado, es de otro proyecto
  de Firebase o ya venció.
- **Dónde se lanza:** `ApiAuthenticationEntryPoint.commence()`

## `ERR_AUTH_02`

- **HTTP:** 401
- **Excepción:** ninguna — lo escribe `FirebaseUsuarioFilter` directo (y
  `UnauthorizedException` desde `CurrentUserIdArgumentResolver` si un
  endpoint pide `@CurrentUserId` sin un usuario registrado).
- **Significado:** El token de Firebase es válido, pero no hay ningún usuario
  del sistema con ese `firebase_uid` (la persona creó su cuenta en Firebase
  pero no completó el registro). El único endpoint que acepta este caso es
  `POST /api/v1/auth/registro`.
- **Dónde se lanza:** `FirebaseUsuarioFilter.doFilterInternal()`

## `ERR_AUTH_03`

- **HTTP:** 403
- **Excepción:** ninguna — lo escribe `FirebaseUsuarioFilter` directo.
- **Significado:** El usuario existe pero su cuenta está desactivada
  (`usuario.activo = false`); debe contactar a la DUBSS. Se consulta en cada
  petición, así que desactivar una cuenta surte efecto de inmediato aunque
  su token siga vigente.
- **Dónde se lanza:** `FirebaseUsuarioFilter.doFilterInternal()`
