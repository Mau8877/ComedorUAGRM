# UT-HU03-BE-02 — `AuthService.logout()` + `AuthController` + whitelist

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-BE-02` |
| **Nombre** | `AuthService.logout()`, endpoint `POST /api/v1/auth/logout`, whitelist en `SecurityConfig` |
| **Historia** | HU03-BE |
| **Depende de** | UT-HU03-BE-01 |

## Antes de implementar

Leer [HU03 - Cerrar Sesión.md](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
completa.

## Solicitud de negocio

Exponer `POST /api/v1/auth/logout`: revoca el refresh token si existe y
borra la cookie, sin fallar nunca por falta o invalidez del token
(idempotente).

## Objetivo

Implementar el endpoint completo de cierre de sesión.

## Alcance

### `AuthService` — **editar** `features/auth/service/AuthService.java`

```java
public void logout(String refreshTokenCookie) {
    if (refreshTokenCookie != null) {
        refreshTokenService.revocar(refreshTokenCookie);
    }
    LOG.info("Logout ejecutado"); // sin loguear el valor del token
}
```

- **No** lanza ninguna excepción si el token es `null`, no existe, o ya
  estaba revocado — `revocar` sobre una key inexistente en Redis es un
  no-op seguro (`DELETE` de una key que no existe no falla).

### `AuthController` — **editar** `features/auth/controller/AuthController.java`

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {

    authService.logout(refreshTokenCookie);
    ResponseCookie cookie = refreshTokenService.clearCookie();

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.success(null, "Sesión cerrada correctamente"));
}
```

### `SecurityConfig` — **editar** `config/SecurityConfig.java`

Agregar a la whitelist ya existente (junto a `login`/`refresh`) —
necesario para que el logout funcione incluso si el access token ya
expiró (el logout no depende de un `Authorization` válido, solo de la
cookie):

```java
.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
```

### Archivos esperados (editar — todos ya existen)

- `features/auth/service/AuthService.java`
- `features/auth/controller/AuthController.java`
- `config/SecurityConfig.java`

## Fuera de alcance (no tocar)

- `login()`/`refresh()` (sin cambios de comportamiento)
- Tests (UT-HU03-BE-03)

## Resultado esperado

`POST /api/v1/auth/logout` siempre responde `200` (salvo un 500
inesperado), revocando el token si había uno válido, y borra la cookie en
la respuesta en todos los casos.

## Validación

1. Login exitoso (HU01) → `POST /api/v1/auth/logout` con la cookie
   adjunta → `200`, `data: null`, `Set-Cookie` con `Max-Age=0`.
2. Tras el punto 1, un `POST /api/v1/auth/refresh` con ese mismo refresh
   token → `401`/`ERR_AUTH_04` (confirma que quedó revocado, CA05 de
   HU03).
3. `POST /api/v1/auth/logout` **sin** cookie `refreshToken` → igual `200`
   (idempotente, CA02).
4. `POST /api/v1/auth/logout` con una cookie que nunca existió en Redis →
   igual `200`.
5. Ningún log de esta request contiene el valor del refresh token.
