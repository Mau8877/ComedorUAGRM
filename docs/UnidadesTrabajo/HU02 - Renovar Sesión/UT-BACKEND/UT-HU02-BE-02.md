# UT-HU02-BE-02 — `RefreshTokenService`: agregar `validar`/`revocar`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-BE-02` |
| **Nombre** | `RefreshTokenService`: agregar `validar(token)` y `revocar(token)` |
| **Historia** | HU02-BE |
| **Depende de** | `security/RefreshTokenService.java` (ya existe, creado por HU01-BE con `issue`/`buildCookie`) |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token).

## Solicitud de negocio

El refresh necesita poder verificar si un token es válido (existe en
Redis, no fue revocado) y, si va a rotarlo, revocar el token usado.

## Objetivo

Sumar los dos métodos que le faltan a `RefreshTokenService` para que HU02
pueda validar y rotar.

## Alcance

### `RefreshTokenService` — **editar** `security/RefreshTokenService.java`

Agregar sobre la clase que ya existe (no se toca `issue`/`buildCookie`,
ya cerrados por HU01-BE):

```java
// Optional.empty() si el token no existe/fue revocado; si existe, el
// codigo del usuario asociado.
public Optional<UUID> validar(String token) {
    String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
    return Optional.ofNullable(value).map(UUID::fromString);
}

// Borra la entrada de Redis -- token ya no sirve para nada después de esto.
public void revocar(String token) {
    redisTemplate.delete(KEY_PREFIX + token);
}
```

- Reusa la misma constante `KEY_PREFIX` ya definida en el archivo.
- **No** se toca `buildCookie`/`issue` — siguen siendo exactamente los de
  HU01-BE.

### Archivos esperados (editar — ya existe)

- `security/RefreshTokenService.java`

## Fuera de alcance (no tocar)

- `clearCookie()` — no forma parte de esta HU, la agrega
  [HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
- `AuthService`/`AuthController` (UT-HU02-BE-03 — ahí se **usan** estos
  métodos)
- `JwtService` (sin cambios en esta HU)

## Resultado esperado

`validar(token)` y `revocar(token)` funcionales sobre el mismo storage
que ya usa `issue`.

## Validación

1. `validar(token)` sobre un token recién emitido con `issue(codigo)`
   devuelve `Optional.of(codigo)`.
2. `validar(token)` sobre un token inexistente devuelve `Optional.empty()`.
3. `revocar(token)` borra la entrada; `validar(token)` sobre ese mismo
   token después devuelve `Optional.empty()`.
4. `issue`/`buildCookie` (de HU01-BE) siguen funcionando sin cambios de
   comportamiento.
