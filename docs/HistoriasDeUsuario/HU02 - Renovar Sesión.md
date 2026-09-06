# Historia de usuario

| Id    | Nombre corto de HU | Prioridad | PHU | Estado    |
| ----- | ------------------- | --------- | --- | --------- |
| HU-02 | Renovar sesión       | Alta      |     | Pendiente |

**Como:** usuario ya autenticado
**Quiero:** que mi access token se renueve automáticamente cuando expira, usando mi refresh token
**Para:** seguir usando el sistema sin tener que volver a ingresar mis credenciales cada 15 minutos

**Descripción:**
Cuando el access token (JWT) expira, o cuando la aplicación se recarga (F5) y el frontend perdió el access token de memoria pero el navegador todavía tiene la cookie `refreshToken` vigente, el frontend llama a `POST /api/v1/auth/refresh`. El navegador adjunta automáticamente la cookie `refreshToken` (`HttpOnly`, seteada en el login — ver [HU-01](HU01%20-%20Iniciar%20Sesión.md)) — el frontend nunca lee ni envía su valor explícitamente. El backend valida ese refresh token contra el storage en Redis (no solo su formato), lo **rota** (revoca el usado y emite uno nuevo, práctica recomendada para minimizar el daño de un token filtrado) y, si es válido, emite un nuevo access token junto con los datos básicos del usuario (necesarios para que el frontend pueda restaurar la sesión completa tras un F5, no solo el token). Si el refresh token es inválido, expiró, fue revocado, o la cuenta fue desactivada desde el login, el backend rechaza la solicitud y el frontend debe forzar un nuevo login.

---

## Conversación/Reglas (opcional)

**Flujo general**

1. El access token del usuario expira (o el frontend recibe `401` de un endpoint de negocio por token vencido).
2. El frontend llama a `POST /api/v1/auth/refresh` **sin body** — el navegador adjunta la cookie `refreshToken` automáticamente (requiere `withCredentials: true` en el cliente HTTP, ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
3. El backend lee el refresh token de la cookie (`@CookieValue`, nunca del body) y lo valida:
   - Verifica el formato.
   - Verifica que la entrada siga existiendo (no revocada) en el storage de Redis.
   - Verifica que la cuenta del usuario siga existiendo y activa (pudo desactivarse después del login original).
4. Si es válido, el backend **rota** el refresh token (revoca el usado, emite uno nuevo) y responde `200` con `{ accessToken, usuario: { codigo, username, correo, roles } }`, reseteando la cookie `refreshToken` con el nuevo valor.
5. Si no es válido (expiró, fue revocado, no existe en Redis, no llegó ninguna cookie, o la cuenta ya no está activa), el backend responde `401` y el frontend cierra la sesión localmente y redirige al login.
6. El usuario no percibe este intercambio — sucede en segundo plano mientras sigue usando el sistema, o al recargar la página si todavía hay una cookie `refreshToken` vigente.

**Reglas de negocio**

- El refresh token es opaco (no JWT), de larga duración (ej. 7 días), validado contra el storage en Redis — la sola validez de formato no alcanza si la entrada fue borrada/revocada.
- Este endpoint **solo** acepta el refresh token vía cookie — nunca credenciales de usuario/contraseña, y nunca el token en el body (ese transporte quedó descartado, ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- El access token emitido en la renovación tiene los mismos claims mínimos (`codigo` de usuario, lista de nombres de roles activos) que el emitido en el login original (ver [HU-01](HU01%20-%20Iniciar%20Sesión.md)).
- **Rotación obligatoria:** cada renovación exitosa revoca el refresh token usado y emite uno nuevo (nunca se reutiliza el mismo valor) — reduce la ventana de uso de un token filtrado a una sola renovación. La cookie se resetea con el nuevo valor en la misma respuesta.
- La respuesta incluye los datos básicos del usuario (no solo el access token) para que el frontend pueda restaurar la sesión completa (`username`/`correo`/`roles`) tras perder el estado en memoria (ej. F5), sin una llamada adicional.
- Ni el refresh token ni el access token se loguean completos en ningún nivel — ver [LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear).
- Todas las respuestas siguen el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md).

---

## Criterios de aceptación

- **CA01:** Dado un refresh token válido y no revocado en la cookie `refreshToken`, para una cuenta con `estado = ACTIVO`, el sistema responde `200` con `data: { accessToken, usuario: { codigo, username, correo, roles } }`, revoca el refresh token usado y setea uno nuevo en la cookie.
- **CA02:** Dado un refresh token inválido, expirado o revocado, el sistema responde `401` (`UnauthorizedException`) con `message: "Sesión expirada"`.
- **CA03:** Dado que la request no incluye la cookie `refreshToken` (ausente), el sistema responde el mismo `401` de CA02.
- **CA04:** Dado un refresh token con formato inválido (manipulado), el sistema responde el mismo `401` de CA02, sin distinguir el motivo exacto en el mensaje.
- **CA05:** Dado un error inesperado del servidor, el sistema responde `500` (`ERR_SYS_00`) sin exponer detalles técnicos internos.
- **CA06:** Todas las respuestas (éxito y error) siguen exactamente el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md).
- **CA07:** Dado un refresh token técnicamente válido pero cuya cuenta tiene `estado` distinto de `ACTIVO` después del login, el sistema responde el mismo `401` de CA02 y revoca ese refresh token (no queda utilizable ni siquiera para un futuro intento).

## Desarrollador

Mauro Boris Gutierrez Primintela
