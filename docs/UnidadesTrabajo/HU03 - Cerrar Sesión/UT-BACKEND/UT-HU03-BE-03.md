# UT-HU03-BE-03 — Pruebas unitarias de cierre de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-BE-03` |
| **Nombre** | Pruebas unitarias de `AuthService.logout()`/`AuthController.logout()` |
| **Historia** | HU03-BE |
| **Depende de** | UT-HU03-BE-02 |

## Antes de implementar

Leer [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Evitar regresiones en la idempotencia del logout y en que la cookie
siempre se borre.

## Objetivo

Extender `AuthServiceTest`/`AuthControllerTest` (ya existentes) con los
casos de `logout`.

## Alcance

### `AuthServiceTest` — **editar** (agregar casos)

| # | Caso | Assert |
|---|------|--------|
| 1 | `logout` con token válido | `refreshTokenService.revocar` invocado con ese token |
| 2 | `logout` con `refreshTokenCookie = null` | `revocar` **no** invocado; no lanza excepción |

### `AuthControllerTest` — **editar** (agregar casos)

| # | Caso | Assert |
|---|------|--------|
| 3 | Request con cookie válida | `200`; `data: null`; `Set-Cookie` con `Max-Age=0` |
| 4 | Request sin cookie | igual `200`; `Set-Cookie` con `Max-Age=0` |

### Archivos esperados (editar — ya existen)

- `src/test/java/.../features/auth/service/AuthServiceTest.java`
- `src/test/java/.../features/auth/controller/AuthControllerTest.java`

## Fuera de alcance (no tocar)

- Tests de `login`/`refresh` (ya existentes, de HU01-BE/HU02-BE)

## Resultado esperado

Suite verde, cubriendo login, refresh y logout en las mismas clases de
test.

## Validación

1. `mvn test -Dtest=AuthServiceTest,AuthControllerTest` pasa.
2. Los 6 CA de HU03 (o los aplicables al backend) están cubiertos.
3. Los tests de HU01-BE/HU02-BE siguen pasando sin modificación de su
   propio código.
