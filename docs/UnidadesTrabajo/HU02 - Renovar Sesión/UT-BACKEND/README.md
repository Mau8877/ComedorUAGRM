# Unidades de trabajo — HU02-BE

**Historia:** Renovar la sesión — validar el refresh token (cookie `HttpOnly`), rotarlo, y emitir un nuevo access token + datos del usuario.
**Fuente HU:** [`../../HistoriasDeUsuario/HU02 - Renovar Sesión.md`](../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
**Recurso HTTP:** `POST /api/v1/auth/refresh`
**Requiere que ya exista:** [HU01-BE](../../HU01%20-%20Iniciar%20Sesión/UT-BACKEND/README.md) implementada completa (`Usuario`, `UsuarioRepository`, `UsuarioRolRepository`, `RefreshTokenService.issue`/`buildCookie`, `JwtService.generarAccessToken`, `AuthController`, `AuthErrorCodes`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — backend. Prioridad:
[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md),
[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md),
[RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md),
[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), y
[UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md)
(en particular la regla de no anticipar trabajo de otra HU).

## Decisión de diseño: esta HU **edita** archivos que ya existen, no los recrea

`RefreshTokenService`, `AuthController`, `AuthErrorCodes` y
`docs/errors/auth/ERRORES_AUTH.md` ya existen (los creó
[HU01-BE](../../HU01%20-%20Iniciar%20Sesión/UT-BACKEND/README.md)). Las UT
de esta HU los **editan** para sumar lo que el refresh necesita —
declarado así explícitamente en cada UT (nunca "crear", siempre "editar").

## Decisión de diseño: rotación del refresh token

Confirmado (ver [HU02 - Renovar Sesión.md](../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md#reglas-de-negocio)):
cada renovación exitosa **revoca** el refresh token usado y **emite uno
nuevo**, reseteando la cookie en la misma respuesta. Es la práctica
recomendada (OWASP) para que un refresh token filtrado solo sirva para
una renovación, no para toda su vida útil restante.

## Código de error nuevo (módulo `AUTH`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_AUTH_04` | 401 | `UnauthorizedException` | Refresh token ausente, con formato inválido, no encontrado/revocado en Redis, o de una cuenta con `estado != ACTIVO` |

## Recurso HTTP de esta HU

| Método | Path | HTTP éxito | `data` | Efecto adicional |
|--------|------|------------|--------|-------------------|
| `POST` | `/api/v1/auth/refresh` | 200 | `{ accessToken, usuario: { codigo, username, correo, roles } }` | Revoca el refresh token usado y resetea la cookie `refreshToken` con uno nuevo |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU02-BE-01](./UT-HU02-BE-01.md) | Código de error `ERR_AUTH_04` | — |
| [UT-HU02-BE-02](./UT-HU02-BE-02.md) | `RefreshTokenService`: agregar `validar`/`revocar` | — |
| [UT-HU02-BE-03](./UT-HU02-BE-03.md) | `AuthService.refresh()` + `AuthController` + whitelist en `SecurityConfig` | UT-HU02-BE-01, UT-HU02-BE-02 |
| [UT-HU02-BE-04](./UT-HU02-BE-04.md) | Pruebas unitarias | UT-HU02-BE-03 |

**Cadena:** `01, 02 → 03 → 04`.

## Criterios de aceptación cubiertos

| CA (HU02) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Refresh válido → nuevo accessToken + usuario + rotación | 02, 03 |
| CA02 | Refresh inválido/expirado/revocado → 401 | 01, 02, 03 |
| CA03 | Cookie ausente → mismo 401 | 03 |
| CA04 | Refresh con formato inválido → mismo 401 | 02, 03 |
| CA05 | Error inesperado → 500 (ya existente, sin cambios) | — |
| CA06 | Sobre de respuesta estándar | 03 |
| CA07 | Cuenta desactivada tras el login → 401 + revoca el token | 03 |

## Follow-ups (no se implementan en estas UT)

- Filtro de validación del access token en requests protegidos (sigue sin
  ningún consumidor real, mismo follow-up que dejó HU01-BE).
- Límite de refresh tokens activos simultáneos por usuario (ej. multi-
  dispositivo) — no hay requisito de negocio para esto todavía.

## Qué NO incluyen estas UT

- `/api/v1/auth/logout` ([HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md))
- `clearCookie()` sobre `RefreshTokenService` — la crea HU-03
- Cualquier cambio a `/api/v1/auth/login` (ya cerrado en HU01-BE)
- Código frontend (ver [`../UT-FRONTEND/README.md`](../UT-FRONTEND/README.md))
