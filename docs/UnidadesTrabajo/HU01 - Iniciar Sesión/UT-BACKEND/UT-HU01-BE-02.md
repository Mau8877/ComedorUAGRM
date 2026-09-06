# UT-HU01-BE-02 — Excepciones y catálogo de errores del módulo `AUTH`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-02` |
| **Nombre** | Excepciones y catálogo de errores del módulo `AUTH` |
| **Historia** | HU01-BE |
| **Depende de** | — |

## Antes de implementar

Leer [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md),
[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#nombres-de-clases).

## Solicitud de negocio

El login puede fallar de tres formas catalogables (credenciales inválidas,
cuenta inactiva, cuenta bloqueada) — cada una necesita su propio código de
error documentado, y la cuenta bloqueada necesita una excepción HTTP que
todavía no existe en el proyecto (`423`).

## Objetivo

Crear `LockedException` (global, `common/exception/`) y el catálogo
`AuthErrorCodes` del módulo `AUTH`, documentando cada código nuevo en
`docs/errors/` en el mismo cambio (regla estricta de
[GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)).

## Alcance

### `LockedException` — `common/exception/LockedException.java`

Ya está documentada en
[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
(`423`) pero la clase todavía no existe. Mismo patrón que
`UnauthorizedException`/`ForbiddenException` (ver esos archivos como
referencia exacta de forma):

```java
package com.comedoruagrm.backend.common.exception;

import org.springframework.http.HttpStatus;

public class LockedException extends BusinessException {

    public LockedException(String errorCode, String message) {
        super(errorCode, HttpStatus.LOCKED, message);
    }
}
```

No hace falta tocar `GlobalExceptionHandler` — el `@ExceptionHandler(BusinessException.class)`
ya es genérico y cubre cualquier subtipo nuevo (confirmado leyendo
`GlobalExceptionHandler.java` actual).

### `AuthErrorCodes` — `features/auth/AuthErrorCodes.java`

Suelto en la raíz del paquete del módulo (no en una subcarpeta), mismo
patrón que describe `CONVENCIONES_JAVA_BACKEND.md`:

```java
package com.comedoruagrm.backend.features.auth;

public interface AuthErrorCodes {
    String CREDENCIALES_INVALIDAS = "ERR_AUTH_01";
    String CUENTA_INACTIVA = "ERR_AUTH_02";
    String CUENTA_BLOQUEADA = "ERR_AUTH_03";
}
```

### Documentación en `docs/errors/`

Carpeta nueva `docs/errors/auth/` (no existe todavía — se crea recién
ahora porque este es el primer error real del módulo, ver
[docs/errors/README.md](../../../../docs/errors/README.md)):

`docs/errors/auth/ERRORES_AUTH.md`:

```markdown
# Errores — Autenticación (`AUTH`)

## `ERR_AUTH_01`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** El `username`/`correo` no corresponde a ningún usuario,
  o la contraseña no coincide con el hash almacenado. Se usa el mismo
  código y mensaje para ambos casos a propósito, para no revelar cuál de
  los dos falló.
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_02`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** Las credenciales son correctas, pero la cuenta tiene
  `estado` distinto de `ACTIVO`.
- **Dónde se lanza:** `AuthService.login()`

## `ERR_AUTH_03`

- **HTTP:** 423
- **Excepción:** `LockedException`
- **Significado:** La cuenta superó el máximo de intentos fallidos
  consecutivos configurado (`app.auth.max-intentos-fallidos`) y está
  bloqueada temporalmente.
- **Dónde se lanza:** `AuthService.login()`
```

Y agregar la sección `### Autenticación (AUTH)` con los tres links en
[`docs/errors/README.md`](../../../../docs/errors/README.md), mismo
formato que la sección `### Sistema (SYS)` ya existente.

### Archivos esperados (crear)

- `common/exception/LockedException.java`
- `features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- Editar: `docs/errors/README.md` (agregar sección + links)

## Fuera de alcance (no tocar)

- `GlobalExceptionHandler.java` (no necesita cambios, confirmado arriba)
- `AuthService`/`AuthController` (UT-HU01-BE-07, ahí se **usan** estos
  códigos, no se definen)
- Otros módulos de `docs/errors/`

## Resultado esperado

`LockedException` disponible para lanzar con HTTP 423, y los tres códigos
`ERR_AUTH_0N` catalogados tanto en Java como en `docs/errors/`, listos
para que UT-HU01-BE-07 los use.

## Validación

1. Lanzar `new LockedException(AuthErrorCodes.CUENTA_BLOQUEADA, "...")`
   desde un controller cualquiera de prueba responde `423` con el sobre
   estándar (`ApiResponse.error(...)`) sin tocar `GlobalExceptionHandler`.
2. Los tres códigos aparecen en `docs/errors/auth/ERRORES_AUTH.md` con las
   4 líneas obligatorias (HTTP/Excepción/Significado/Dónde se lanza).
3. `docs/errors/README.md` enlaza a los tres, con ancla en minúscula
   (`auth/errores_auth.md#err_auth_01`).
