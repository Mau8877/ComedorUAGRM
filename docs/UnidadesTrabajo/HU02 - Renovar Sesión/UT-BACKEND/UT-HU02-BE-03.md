# UT-HU02-BE-03 — `AuthService.refresh()` + `AuthController` + whitelist

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-BE-03` |
| **Nombre** | `AuthService.refresh()`, endpoint `POST /api/v1/auth/refresh`, whitelist en `SecurityConfig` |
| **Historia** | HU02-BE |
| **Depende de** | UT-HU02-BE-01, UT-HU02-BE-02 |

## Antes de implementar

Leer [HU02 - Renovar Sesión.md](../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
completa (los 7 CA de esta UT salen de ahí),
[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Exponer `POST /api/v1/auth/refresh`, orquestando la validación del
refresh token, la verificación de que la cuenta siga activa, la rotación,
y la emisión del nuevo access token.

## Objetivo

Implementar el endpoint completo de renovación de sesión.

## Alcance

### DTO nuevo — `features/auth/dto/RefreshResponse.java`

```java
public record RefreshResponse(String accessToken, UsuarioResumenResponse usuario) {}
```

(`UsuarioResumenResponse` ya existe, creado por HU01-BE — se reutiliza,
no se duplica.)

### `AuthService` — **editar** `features/auth/service/AuthService.java`

Agregar el método `refresh` sobre el service que ya existe (inyecta
además `UsuarioRolRepository`, ya inyectado por `login`; no se necesita
ninguna dependencia nueva):

```java
public AuthResult<RefreshResponse> refresh(String refreshTokenCookie) {
    UUID codigo = refreshTokenService.validar(refreshTokenCookie)
            .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.SESION_EXPIRADA, "Sesión expirada"));

    Usuario usuario = usuarioRepository.findByCodigo(codigo).orElse(null);
    if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO) {
        refreshTokenService.revocar(refreshTokenCookie);
        LOG.warn("Intento de refresh con cuenta inexistente o inactiva");
        throw new UnauthorizedException(AuthErrorCodes.SESION_EXPIRADA, "Sesión expirada");
    }

    List<String> roles = usuarioRolRepository.findRolesActivosDeUsuario(usuario.getId()).stream()
            .map(ur -> ur.getRol().getNombre())
            .toList();

    refreshTokenService.revocar(refreshTokenCookie);
    String nuevoRefreshToken = refreshTokenService.issue(usuario.getCodigo());
    String accessToken = jwtService.generarAccessToken(usuario.getCodigo(), roles);

    LOG.info("Sesión renovada"); // module=auth, userId=usuario.getCodigo()

    RefreshResponse response = new RefreshResponse(accessToken,
            new UsuarioResumenResponse(usuario.getCodigo(), usuario.getUsername(), usuario.getCorreo(), roles));
    return new AuthResult<>(response, nuevoRefreshToken);
}
```

> `AuthResult<T>` ya quedó genérico desde HU01-BE (`AuthResult<T>(T response,
> String refreshToken)`) — esta UT no necesita tocar ese archivo, solo lo
> reutiliza con `RefreshResponse` como tipo.

### `AuthController` — **editar** `features/auth/controller/AuthController.java`

Agregar el endpoint sobre el controller que ya existe:

```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {

    if (refreshTokenCookie == null) {
        throw new UnauthorizedException(AuthErrorCodes.SESION_EXPIRADA, "Sesión expirada");
    }

    AuthResult<RefreshResponse> result = authService.refresh(refreshTokenCookie);
    ResponseCookie cookie = refreshTokenService.buildCookie(result.refreshToken());

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.success(result.response(), "Sesión renovada correctamente"));
}
```

- `@CookieValue(required = false)` — si no llega cookie, el controller
  lanza `UnauthorizedException` directo (caso borde de "no autenticado en
  absoluto", no amerita entrar al service).

### `SecurityConfig` — **editar** `config/SecurityConfig.java`

Agregar a la whitelist ya existente (al lado de `/api/v1/auth/login`):

```java
.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
```

### Archivos esperados (crear/editar)

- Crear: `features/auth/dto/RefreshResponse.java`
- Editar: `features/auth/service/AuthService.java`, `features/auth/controller/AuthController.java`, `config/SecurityConfig.java`

## Fuera de alcance (no tocar)

- `/api/v1/auth/logout` ([HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md))
- `login()` (sin cambios de comportamiento, solo se le agrega un método
  hermano al service/controller)
- Tests (UT-HU02-BE-04)

## Resultado esperado

`POST /api/v1/auth/refresh` funcional: con cookie válida, rota el token y
responde `200`; con cookie ausente/inválida/de cuenta inactiva, responde
`401` con `ERR_AUTH_04`.

## Validación

1. Login exitoso (HU01) seguido de `POST /api/v1/auth/refresh` (cookie
   adjunta automáticamente por el cliente de test) → `200`,
   `data.accessToken` nuevo, `data.usuario` presente, `Set-Cookie` con un
   valor de `refreshToken` **distinto** al original.
2. El refresh token original, tras el punto 1, ya no es válido (un
   segundo refresh con el token viejo → `401`/`ERR_AUTH_04`) — confirma la
   rotación.
3. Request sin cookie `refreshToken` → `401`/`ERR_AUTH_04`.
4. Cookie con un valor que nunca existió en Redis → `401`/`ERR_AUTH_04`.
5. Cambiar el `estado` del usuario a `INACTIVO` después del login, y usar
   su refresh token vigente → `401`/`ERR_AUTH_04`, y ese token queda
   revocado (un segundo intento con el mismo token también falla).
6. Un usuario con dos roles activos recibe `data.usuario.roles` con
   longitud 2 en la respuesta de refresh.
