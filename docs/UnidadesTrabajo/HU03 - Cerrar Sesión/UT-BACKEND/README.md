# Unidades de trabajo — HU03-BE

**Historia:** Cerrar sesión — revocar el refresh token (cookie `HttpOnly`) y borrar la cookie en la respuesta, de forma idempotente.
**Fuente HU:** [`../../HistoriasDeUsuario/HU03 - Cerrar Sesión.md`](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
**Recurso HTTP:** `POST /api/v1/auth/logout`
**Requiere que ya exista:** [HU01-BE](../../HU01%20-%20Iniciar%20Sesión/UT-BACKEND/README.md) y [HU02-BE](../../HU02%20-%20Renovar%20Sesión/UT-BACKEND/README.md) implementadas completas (`RefreshTokenService.revocar` ya existe desde HU02-BE)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — backend, y
[UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md).

## Decisión de diseño: sin código de error nuevo

A diferencia de HU01/HU02, esta HU no cataloga ningún `ERR_AUTH_0N`
nuevo — el logout es idempotente por diseño (ver
[HU03 - Cerrar Sesión.md](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)):
nunca responde un error de negocio, solo `200` (éxito) o `500` (el
catch-all genérico ya existente, `ERR_SYS_00`).

## Recurso HTTP de esta HU

| Método | Path | HTTP éxito | `data` | Efecto adicional |
|--------|------|------------|--------|-------------------|
| `POST` | `/api/v1/auth/logout` | 200 | `null` | Revoca el refresh token (si existía) y borra la cookie `refreshToken` (`Max-Age=0`) |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU03-BE-01](./UT-HU03-BE-01.md) | `RefreshTokenService`: agregar `clearCookie()` | — |
| [UT-HU03-BE-02](./UT-HU03-BE-02.md) | `AuthService.logout()` + `AuthController` + whitelist en `SecurityConfig` | UT-HU03-BE-01 |
| [UT-HU03-BE-03](./UT-HU03-BE-03.md) | Pruebas unitarias | UT-HU03-BE-02 |

**Cadena:** `01 → 02 → 03`.

## Criterios de aceptación cubiertos

| CA (HU03) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Logout con cookie válida → revoca + borra cookie | 01, 02 |
| CA02 | Logout sin cookie/ya revocada → igual 200 (idempotente) | 02 |
| CA03 | *(no aplica, ver HU03.md)* | — |
| CA04 | Error inesperado → 500 (ya existente) | — |
| CA05 | Tras logout, un refresh con ese token → 401 | 02 (verificado contra HU02-BE) |
| CA06 | Sobre de respuesta estándar | 02 |

## Follow-ups (no se implementan en estas UT)

- Botón de "Cerrar sesión en todos los dispositivos" (revocar todos los
  refresh tokens de un usuario, no solo el de la sesión actual) — no hay
  requisito de negocio para esto todavía; hoy cada usuario tiene un único
  refresh token activo a la vez (no hay multi-sesión documentada).

## Qué NO incluyen estas UT

- Cualquier cambio a `/api/v1/auth/login` o `/api/v1/auth/refresh` (ya
  cerrados por HU01-BE/HU02-BE)
- Código frontend (ver [`../UT-FRONTEND/README.md`](../UT-FRONTEND/README.md))
