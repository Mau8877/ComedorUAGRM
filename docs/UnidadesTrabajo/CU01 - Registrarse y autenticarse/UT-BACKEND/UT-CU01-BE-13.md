# UT-CU01-BE-13 — Login, refresh, logout y usuario actual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-13` |
| **Nombre** | Login, refresh, logout y usuario actual |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-04`, `UT-CU01-BE-08`, `UT-CU01-BE-11`, `UT-CU01-BE-15` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Cualquier usuario con cuenta debe poder iniciar sesión con su correo y contraseña, mantener la sesión sin volver a escribirla, cerrarla en su dispositivo y recuperarla al recargar; la cuenta debe protegerse de intentos repetidos.

## Objetivo

`POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` y `GET /auth/usuario-actual` cumplen los flujos B y C del CU, con bloqueo temporal por intentos fallidos.

## Alcance

- DTOs: `LoginRequest` (`correo`, `contrasena`; solo `@NotBlank`: cualquier dato que no coincida da el mismo `401`), `RefreshTokenRequest` (`refreshToken`) y `SesionResponse` (`accessToken`, `refreshToken`, `tokenType` = `Bearer`, `expiresIn` en segundos, `usuario`).
- `UsuarioService.validarCredenciales(correo, contrasena)`, con `@Transactional(noRollbackFor = BusinessException.class)` para que el conteo de fallos se guarde aunque se lance el error. Orden: cuenta bloqueada → `403` `ERR_AUTH_05` (aunque la contraseña sea correcta); contraseña incorrecta o correo inexistente → `401` `ERR_AUTH_04` con el **mismo mensaje** (si el correo no existe se compara igual contra un hash de relleno); cuenta desactivada → `403` `ERR_AUTH_03`. Al **5.º intento fallido consecutivo** bloquea la cuenta (5 min, luego 15 min, 1 h, 24 h con cada bloqueo seguido; **escala por definir con la DUBSS**); un acierto reinicia intentos y escalada.
- `AuthService`: `iniciarSesion(LoginRequest, ip)` (también con `noRollbackFor`; abre la sesión con `SesionService.abrir` y anota `INICIO_SESION`), `renovar(refreshToken)` (rota el refresh token; **no** anota un acceso nuevo), `cerrarSesion(refreshToken)` y `usuarioActual(codigo)` (solo lectura).
- `AuthController`: `POST /api/v1/auth/login` (`200`), `POST /api/v1/auth/refresh` (`200`), `POST /api/v1/auth/logout` (`200`, `data: null`, idempotente) y `GET /api/v1/auth/usuario-actual` (`200`, con `@CurrentUserId`). Los tres primeros son públicos (`RutasPublicas`).
- Códigos `ERR_AUTH_04`, `ERR_AUTH_05` y `ERR_AUTH_06` en `AuthErrorCodes`, documentados en `docs/errors/auth/ERRORES_AUTH.md` y su índice.
- Tests: de service (bloqueo al 5.º intento, duración creciente, bloqueada aunque acierte, mismo mensaje con correo inexistente, desactivada, acierto reinicia), de controller (contrato del sobre y códigos) y de `AuthService` (anota exactamente un evento por login; `refresh` y `usuario-actual` ninguno).

Archivos esperados (crear/tocar): los DTOs, `UsuarioService.java`, `AuthService.java`, `AuthController.java`, `AuthErrorCodes.java`, `docs/errors/`, y sus tests.

## Fuera de alcance (no tocar)

- Recuperar contraseña y verificación de correo (CU15).
- Cierre de sesión en **todos** los dispositivos (CU01 pide solo el del dispositivo actual, CA19).
- Registrar los intentos fallidos en la bitácora (solo se cuentan en `usuario`).

## Resultado esperado

Un usuario inicia sesión, renueva y cierra su sesión por dispositivo; tras 5 contraseñas incorrectas la cuenta queda bloqueada y el acceso se niega aunque acierte.

## Validación

1. `./mvnw test` pasa.
2. El test verifica que `login` crea una fila `INICIO_SESION` y que `refresh` y `usuario-actual` no crean ninguna.
3. `ERR_AUTH_04` devuelve el mismo mensaje con contraseña incorrecta y con correo inexistente.
4. `./mvnw checkstyle:check` sin errores.
