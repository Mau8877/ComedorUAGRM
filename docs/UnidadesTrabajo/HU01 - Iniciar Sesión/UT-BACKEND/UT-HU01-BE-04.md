# UT-HU01-BE-04 — Refresh token: emisión, storage en Redis y cookie

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-04` |
| **Nombre** | Refresh token: emisión, storage en Redis y cookie `HttpOnly` (`security/RefreshTokenService`) |
| **Historia** | HU01-BE |
| **Depende de** | — |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token)
completo — esta UT implementa exactamente el mecanismo de cookie descrito
ahí (solo la parte de emisión; validación y revocación son de otras HU,
ver "Fuera de alcance").

## Solicitud de negocio

El login necesita emitir un refresh token opaco, guardarlo en Redis, y
transportarlo como cookie `HttpOnly` — nunca en el body de la respuesta.

## Objetivo

Implementar `RefreshTokenService` en `security/` con la emisión, el
storage en Redis, y el armado de la cookie de respuesta — únicamente lo
que el login (esta HU) necesita.

## Alcance

### Configuración (`application.properties`)

Agregar la sección "7. REFRESH TOKEN" documentada en el
[README](./README.md#configuración-nueva-applicationproperties):
`app.jwt.refresh-token-expiration-days`, `app.cookies.secure`.

### `RefreshTokenService` — `security/RefreshTokenService.java`

```java
@Component
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";
    private static final String COOKIE_NAME = "refreshToken";

    // Genera un UUID aleatorio, lo guarda en Redis (key = KEY_PREFIX + token,
    // value = codigoUsuario.toString(), TTL = app.jwt.refresh-token-expiration-days)
    // y devuelve el token plano (para que el controller arme la cookie).
    public String issue(UUID codigoUsuario) { ... }

    // Arma la cookie de respuesta con el token plano (usada por
    // AuthController en UT-HU01-BE-07).
    public ResponseCookie buildCookie(String token) { ... }
}
```

- Usa `StringRedisTemplate` (ya autoconfigurado por
  `spring-boot-starter-data-redis`, sin wiring adicional — mismo
  mecanismo que ya documenta `SEGURIDAD_AUTH_BACKEND.md`).
- `buildCookie` usa `org.springframework.http.ResponseCookie` con los
  atributos exactos documentados en
  [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token):
  `httpOnly(true)`, `sameSite("Lax")`, `path("/api/v1/auth")`,
  `secure(app.cookies.secure)`, `maxAge(...)` (duración configurada).
- **No** se agregan acá `validar(...)`/`revocar(...)`/`clearCookie()` —
  esta HU (login) solo emite el token, nunca lo valida ni lo revoca. Esos
  métodos se agregan sobre este mismo archivo cuando se desglosen
  [HU-02: Renovar sesión](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
  (`validar`) y [HU-03: Cerrar sesión](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
  (`revocar`, `clearCookie`) en sus propias UT — ver la regla de
  [UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md#una-ut-nunca-construye-trabajo-de-otra-hu-aunque-sea-trivial-agregarlo-ahora)
  sobre no anticipar trabajo de otra HU.

### Archivos esperados (crear/tocar)

- `security/RefreshTokenService.java`
- `application.properties` (nueva sección de config)

## Fuera de alcance (no tocar)

- `validar(...)`, `revocar(...)`, `clearCookie()` sobre `RefreshTokenService`
  — se agregan en las UT de
  [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)/[HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
  cuando esas HU se desglosen, no acá
- `JwtService` (UT-HU01-BE-03, es un componente distinto)
- `AuthController` (UT-HU01-BE-07 — ahí se **usa** `issue`/`buildCookie`)

## Resultado esperado

`RefreshTokenService.issue(codigo)` persiste el token en Redis con el TTL
correcto y devuelve el valor plano; `buildCookie(token)` arma una cookie
con los atributos exactos de la rule.

## Validación

1. `issue(codigo)` deja una key `refresh-token:{token}` en Redis con TTL ≈
   `app.jwt.refresh-token-expiration-days` días, y el valor guardado es el
   `codigo` correcto.
2. `buildCookie(token).toString()` contiene `HttpOnly`, `SameSite=Lax`,
   `Path=/api/v1/auth`, y `Secure` solo si `app.cookies.secure=true`.
3. `buildCookie(token).getMaxAge()` coincide con
   `app.jwt.refresh-token-expiration-days` convertido a segundos.
