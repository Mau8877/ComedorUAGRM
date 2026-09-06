# Historia de usuario

| Id    | Nombre corto de HU | Prioridad | PHU | Estado    |
| ----- | ------------------ | --------- | --- | --------- |
| HU-03 | Cerrar sesión       | Media     |     | Pendiente |

**Como:** usuario autenticado
**Quiero:** poder cerrar mi sesión explícitamente
**Para:** invalidar mi acceso desde ese dispositivo y evitar que alguien más lo siga usando

**Descripción:**
El usuario presiona "Cerrar sesión" y el frontend llama a `POST /api/v1/auth/logout` (sin body — el navegador adjunta la cookie `refreshToken` sola). El backend lee esa cookie, revoca (borra) la entrada correspondiente en el storage de Redis, de forma que ese refresh token ya no pueda usarse para renovar el access token (ver [HU-02](HU02%20-%20Renovar%20Sesión.md)), y borra la cookie en la respuesta (`Max-Age=0`). El access token ya emitido sigue siendo técnicamente válido hasta su expiración natural — es stateless por diseño (ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)), no se invalida antes de tiempo.

---

## Conversación/Reglas (opcional)

**Flujo general**

1. El usuario presiona "Cerrar sesión" en la interfaz.
2. El frontend llama a `POST /api/v1/auth/logout` sin body — el navegador adjunta la cookie `refreshToken` automáticamente.
3. El backend lee el refresh token de la cookie (`@CookieValue`) y borra/marca inválida esa entrada en Redis.
4. El backend responde `200` sin contenido, y borra la cookie `refreshToken` en la respuesta (`Max-Age=0`).
5. El frontend limpia el access token en memoria y redirige a la pantalla de login.

**Reglas de negocio**

- El access token **no** se invalida en el logout — sigue siendo válido hasta que expira solo (duración corta, ej. 15 min), por ser stateless. Solo se revoca el refresh token.
- El endpoint es **idempotente**: cerrar sesión sin cookie, o con un refresh token que ya fue revocado, responde igual `200` de éxito, no un error — evita que un doble-click, un reintento de red, o un logout con sesión ya vencida rompan el flujo.
- El logout no requiere reenviar credenciales ni ningún body — solo la cookie `refreshToken` de la sesión activa (si existe).
- El refresh token no se loguea completo en ningún nivel — ver [LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear).
- Todas las respuestas siguen el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md).

---

## Criterios de aceptación

- **CA01:** Dado un refresh token válido y vigente en la cookie, al cerrar sesión el sistema revoca esa entrada en Redis, responde `200` con `data: null` y `message: "Sesión cerrada correctamente"`, y borra la cookie `refreshToken` en la respuesta.
- **CA02:** Dado que la request no trae la cookie `refreshToken` (ausente o ya revocada), al cerrar sesión el sistema responde igual `200` de éxito (operación idempotente), sin lanzar error.
- **CA03:** *(No aplica — el logout no recibe body; se retiró junto con el transporte por body del refresh token, ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).)*
- **CA04:** Dado un error inesperado del servidor, el sistema responde `500` (`ERR_SYS_00`) sin exponer detalles técnicos internos.
- **CA05:** Luego de un logout exitoso, un intento de renovar sesión ([HU-02](HU02%20-%20Renovar%20Sesión.md)) con esa misma cookie (si el cliente la conservara manualmente) responde `401 - Sesión expirada`.
- **CA06:** Todas las respuestas (éxito y error) siguen exactamente el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md).

## Desarrollador

Mauro Boris Gutierrez Primintela
