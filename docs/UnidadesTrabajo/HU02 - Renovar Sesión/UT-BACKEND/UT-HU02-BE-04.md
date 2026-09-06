# UT-HU02-BE-04 — Pruebas unitarias de renovación de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-BE-04` |
| **Nombre** | Pruebas unitarias de `AuthService.refresh()`/`AuthController.refresh()` |
| **Historia** | HU02-BE |
| **Depende de** | UT-HU02-BE-03 |

## Antes de implementar

Leer [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Evitar regresiones en la rotación del refresh token y en el chequeo de
cuenta activa al renovar.

## Objetivo

Extender `AuthServiceTest`/`AuthControllerTest` (ya existentes, de
HU01-BE) con los casos de `refresh`.

## Alcance

### `AuthServiceTest` — **editar** (agregar casos, no reescribir la clase)

| # | Caso | Assert |
|---|------|--------|
| 1 | Refresh con token válido, cuenta activa | `AuthResult` con `accessToken`/`usuario` correctos; `refreshTokenService.revocar` invocado con el token viejo; `refreshTokenService.issue` invocado una vez |
| 2 | Refresh con token inexistente en Redis | `UnauthorizedException` `ERR_AUTH_04`; `revocar`/`issue` **no** invocados |
| 3 | Refresh con cuenta `estado=INACTIVO` | `UnauthorizedException` `ERR_AUTH_04`; `revocar` **sí** invocado (se invalida igual) |
| 4 | Refresh con cuenta que ya no existe (`findByCodigo` vacío) | mismo resultado que el caso 3 |
| 5 | Refresh exitoso, usuario con dos roles activos | `usuario.roles()` de la respuesta con 2 elementos |

### `AuthControllerTest` — **editar** (agregar casos)

| # | Caso | Assert |
|---|------|--------|
| 5 | Request con cookie `refreshToken` válida (mock de `AuthService.refresh`) | `200`; `Set-Cookie` presente con un valor de `refreshToken` |
| 6 | Request sin cookie `refreshToken` | `401`; `error:"ERR_AUTH_04"`; `authService.refresh` **no** invocado |
| 7 | `AuthService.refresh` lanza `UnauthorizedException` | `401`; `error:"ERR_AUTH_04"` |

### Archivos esperados (editar — ya existen)

- `src/test/java/.../features/auth/service/AuthServiceTest.java`
- `src/test/java/.../features/auth/controller/AuthControllerTest.java`

## Fuera de alcance (no tocar)

- Tests de `login()` (ya existentes, de HU01-BE — no se tocan)
- Tests de logout ([HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md))

## Resultado esperado

Suite verde, incluyendo los casos de `login` (HU01) y `refresh` (HU02) en
las mismas clases de test.

## Validación

1. `mvn test -Dtest=AuthServiceTest,AuthControllerTest` pasa.
2. Los 7 CA de HU02 tienen al menos un test que los cubre.
3. Los tests de `login` de HU01-BE siguen pasando sin modificación de su
   propio código (solo se agregaron casos nuevos al archivo).
