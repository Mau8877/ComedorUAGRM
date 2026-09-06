# UT-HU01-BE-07 — DTOs + `AuthService.login()` + `AuthController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-07` |
| **Nombre** | Contrato REST de login: DTOs, `AuthService.login()`, `AuthController` |
| **Historia** | HU01-BE |
| **Depende de** | UT-HU01-BE-01, UT-HU01-BE-02, UT-HU01-BE-03, UT-HU01-BE-04, UT-HU01-BE-05, UT-HU01-BE-06 |

## Antes de implementar

Leer [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md),
[HU01 - Iniciar Sesión.md](../../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)
completa (los 10 CA de esta UT salen de ahí).

## Solicitud de negocio

Exponer `POST /api/v1/auth/login` orquestando todas las piezas ya
construidas por las UT anteriores: valida credenciales (`username` o
`correo`), chequea bloqueo, emite ambos tokens con los roles activos del
usuario, y responde con el sobre estándar.

## Objetivo

Implementar el contrato REST completo del login: DTOs de request/response,
`AuthService.login()` con toda la lógica de negocio, y `AuthController`
delegando en él y seteando la cookie.

## Alcance

### DTOs — `features/auth/dto/`

```java
// LoginRequest.java
public record LoginRequest(
    @NotBlank(message = "El usuario o correo es obligatorio") String identificador,
    @NotBlank(message = "La contraseña es obligatoria") String password
) {}

// UsuarioResumenResponse.java
public record UsuarioResumenResponse(UUID codigo, String username, String correo, List<String> roles) {}

// LoginResponse.java -- contrato público, NUNCA incluye el refresh token
public record LoginResponse(String accessToken, UsuarioResumenResponse usuario) {}
```

- `identificador` **no** lleva `@Email` — acepta tanto `username` como
  `correo`, no se puede validar formato de email sobre un campo que
  también puede ser un username.

### Resultado interno del service (no es un DTO de API)

`features/auth/service/AuthResult.java` — solo para pasar el refresh
token plano del `service` al `controller` sin exponerlo en el JSON de
`LoginResponse`. Genérico para que [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
lo reutilice con su propio tipo de respuesta:

```java
public record AuthResult<T>(T response, String refreshToken) {}
```

### `AuthService` — `features/auth/service/AuthService.java`

Inyecta: `AuthenticationManager`, `UsuarioRepository`, `UsuarioRolRepository`,
`JwtService`, `RefreshTokenService`, `LoginAttemptService`. Clase anotada
`@Service` + `@RequiredArgsConstructor` (Lombok, ver
[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok))
sobre esos seis campos `private final` — no se escribe el constructor a mano.

```java
public AuthResult<LoginResponse> login(LoginRequest request) {
    loginAttemptService.tiempoRestanteBloqueo(request.identificador()).ifPresent(restante -> {
        throw new LockedException(AuthErrorCodes.CUENTA_BLOQUEADA,
                "Cuenta bloqueada temporalmente. Intente nuevamente en "
                        + restante.toMinutes() + " minutos.");
    });

    try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identificador(), request.password()));
    } catch (DisabledException ex) {
        LOG.warn("Intento de login a cuenta inactiva"); // sin loguear la contraseña
        throw new ForbiddenException(AuthErrorCodes.CUENTA_INACTIVA, "Cuenta inactiva");
    } catch (BadCredentialsException ex) {
        loginAttemptService.registrarFallo(request.identificador());
        LOG.warn("Intento de login con credenciales inválidas");
        throw new UnauthorizedException(AuthErrorCodes.CREDENCIALES_INVALIDAS, "Credenciales inválidas");
    }

    Usuario usuario = usuarioRepository.findByUsernameOrCorreo(request.identificador())
            .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.CREDENCIALES_INVALIDAS, "Credenciales inválidas"));

    List<String> roles = usuarioRolRepository.findRolesActivosDeUsuario(usuario.getId()).stream()
            .map(ur -> ur.getRol().getNombre())
            .toList();

    loginAttemptService.reiniciar(request.identificador());

    String accessToken = jwtService.generarAccessToken(usuario.getCodigo(), roles);
    String refreshToken = refreshTokenService.issue(usuario.getCodigo());

    LOG.info("Login exitoso"); // module=auth, userId=usuario.getCodigo() -- NUNCA password/tokens

    LoginResponse response = new LoginResponse(accessToken,
            new UsuarioResumenResponse(usuario.getCodigo(), usuario.getUsername(), usuario.getCorreo(), roles));
    return new AuthResult<>(response, refreshToken);
}
```

- Orden de las validaciones: **bloqueo primero**, después credenciales
  (`authenticationManager.authenticate`), y la excepción de cuenta
  inactiva/credenciales inválidas se resuelve dentro de ese mismo
  `try/catch` (ver nota de UT-HU01-BE-06 sobre el orden que usa Spring
  Security internamente).
- Logging: `WARN` en credenciales inválidas y cuenta inactiva (casos
  anómalos esperados), `INFO` en éxito — nunca loguear `request.password()`
  ni los tokens completos (ver
  [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear)).
  El campo `module` del log JSON es `"auth"`.

### `AuthController` — `features/auth/controller/AuthController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor // Lombok -- ver CONVENCIONES_JAVA_BACKEND.md#lombok
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResult<LoginResponse> result = authService.login(request);
        ResponseCookie cookie = refreshTokenService.buildCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.response(), "Inicio de sesión exitoso"));
    }
}
```

- El controller **no** contiene lógica de negocio ni `try/catch` de
  excepciones de negocio (regla de controllers delgados de
  [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados))
  — solo delega en `AuthService` y arma la cookie con el helper ya
  construido en UT-HU01-BE-04.

### Archivos esperados (crear)

- `features/auth/dto/LoginRequest.java`
- `features/auth/dto/UsuarioResumenResponse.java`
- `features/auth/dto/LoginResponse.java`
- `features/auth/service/AuthResult.java`
- `features/auth/service/AuthService.java`
- `features/auth/controller/AuthController.java`

## Fuera de alcance (no tocar)

- `/api/v1/auth/refresh`, `/api/v1/auth/logout` (HU-02/HU-03)
- Tests (UT-HU01-BE-08)
- `SecurityConfig`, `UserDetailsServiceImpl` (ya cerrados en UT-HU01-BE-06)

## Resultado esperado

`POST /api/v1/auth/login` funcional de punta a punta contra la base real:
credenciales correctas (por `username` o por `correo`) devuelven `200` +
cookie; cada caso de error de la HU devuelve el HTTP/código
correspondiente con el sobre estándar.

## Validación

1. Login con usuario activo y credenciales correctas usando `username` →
   `200`, `data.accessToken` presente, `data.usuario` (con `roles`) sin
   `passwordHash`, cookie `refreshToken` en la respuesta (`Set-Cookie`
   header). Repetir usando `correo` en vez de `username` — mismo resultado.
2. `username`/`correo` inexistente → `401`, `error: "ERR_AUTH_01"`.
3. Password incorrecta → mismo `401`/`ERR_AUTH_01` que el punto 2 (mensaje
   idéntico).
4. Usuario con `estado=INACTIVO` → `403`, `error: "ERR_AUTH_02"`.
5. Body con `identificador`/`password` vacíos → `400`, `error: "ERR_SYS_01"`.
6. 5 intentos fallidos consecutivos → 6º intento (aunque la contraseña sea
   correcta) → `423`, `error: "ERR_AUTH_03"`.
7. Tras un login exitoso, el contador de intentos fallidos de ese
   identificador queda en cero (siguiente fallo no dispara bloqueo
   inmediato).
8. Un usuario con dos roles activos recibe `data.usuario.roles` con
   longitud 2, y el JWT decodificado también trae ambos.
9. Ningún log de esta request contiene la contraseña ni los tokens
   completos.
