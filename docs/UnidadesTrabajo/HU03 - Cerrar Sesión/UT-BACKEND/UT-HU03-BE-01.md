# UT-HU03-BE-01 — `RefreshTokenService`: agregar `clearCookie()`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-BE-01` |
| **Nombre** | `RefreshTokenService`: agregar `clearCookie()` |
| **Historia** | HU03-BE |
| **Depende de** | `security/RefreshTokenService.java` (ya existe — `issue`/`buildCookie` de HU01-BE, `validar`/`revocar` de HU02-BE) |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token).

## Solicitud de negocio

Al cerrar sesión, el navegador tiene que dejar de mandar la cookie
`refreshToken` — el backend la borra explícitamente en la respuesta.

## Objetivo

Agregar el único método que le falta a `RefreshTokenService` para esta HU.

## Alcance

### `RefreshTokenService` — **editar** `security/RefreshTokenService.java`

```java
// Cookie de borrado: mismo nombre/path que buildCookie, Max-Age=0.
public ResponseCookie clearCookie() {
    return ResponseCookie.from(COOKIE_NAME, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .secure(cookiesSecure)
            .maxAge(0)
            .build();
}
```

- Reusa `COOKIE_NAME` y el flag `cookiesSecure` ya definidos en el
  archivo (de `buildCookie`, HU01-BE) — no se duplican constantes.

### Archivos esperados (editar — ya existe)

- `security/RefreshTokenService.java`

## Fuera de alcance (no tocar)

- `issue`/`buildCookie` (HU01-BE) y `validar`/`revocar` (HU02-BE) — sin
  cambios
- `AuthService`/`AuthController` (UT-HU03-BE-02 — ahí se **usa**)

## Resultado esperado

`clearCookie()` disponible, con los mismos atributos de `buildCookie`
salvo `Max-Age=0` y valor vacío.

## Validación

1. `clearCookie().getMaxAge()` es `0`.
2. `clearCookie().getName()` es `"refreshToken"`, mismo nombre que
   `buildCookie(...)`.
3. `issue`/`buildCookie`/`validar`/`revocar` siguen funcionando sin
   cambios de comportamiento.
