# Historia de usuario

| Id    | Nombre corto de HU | Prioridad | PHU | Estado    |
| ----- | ------------------ | --------- | --- | --------- |
| HU-01 | Iniciar sesión     | Alta      |     | Pendiente |

**Como:** usuario del sistema
**Quiero:** poder iniciar sesión con mi usuario/correo y contraseña
**Para:** poder utilizar las funcionalidades que ofrece el sistema según mi rol

**Descripción:**
El usuario ingresa su `username` **o** `correo` (indistintamente) y su contraseña en el formulario de inicio de sesión. El sistema valida las credenciales contra el backend (`POST /api/v1/auth/login`) y, si son correctas, genera un access token (JWT) y un refresh token: el access token y los datos básicos del usuario (`codigo`, `username`, `correo`, `roles`) viajan en el body de la respuesta, y el refresh token se setea como **cookie `HttpOnly`** — nunca es accesible ni manejado por JavaScript en el cliente (ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). El nombre para mostrar (`nombres`/`apellidos`) no forma parte de esta respuesta — vive en una entidad `Perfil` separada, fuera del alcance de esta HU (ver [`docs/DiagramaConceptual.mermaid`](../../docs/DiagramaConceptual.mermaid)). Si las credenciales son incorrectas o existe algún impedimento (cuenta inactiva, cuenta bloqueada por intentos fallidos, etc.), el sistema informa un error específico sin exponer datos sensibles.

> La renovación automática del access token vencido usando el refresh token
> está fuera del alcance de esta HU — ver
> [HU-02: Renovar sesión](HU02%20-%20Renovar%20Sesión.md). El cierre
> explícito de sesión está fuera del alcance de esta HU — ver
> [HU-03: Cerrar sesión](HU03%20-%20Cerrar%20Sesión.md). Esta HU cubre
> únicamente el login inicial.

---

## Conversación/Reglas (opcional)

**Flujo general**

1. El usuario accede a la pantalla de inicio de sesión.
2. El usuario ingresa su `username` o `correo`, y su contraseña.
3. El usuario presiona el botón "Iniciar sesión".
4. El frontend envía las credenciales a `POST /api/v1/auth/login`.
5. El backend valida las credenciales:
   - Si son correctas, genera un **access token (JWT)** de corta duración y un **refresh token** de mayor duración. Responde `{ accessToken, usuario }` en el body (`usuario` incluye `codigo`, `username`, `correo` y `roles`) y setea la cookie `refreshToken` (`HttpOnly`) en la respuesta.
   - Si son incorrectas, o existe un impedimento (cuenta inactiva/bloqueada), retorna un error específico (ver Criterios de aceptación) y no setea ninguna cookie.
6. El frontend guarda el access token en memoria (store de sesión, ver [ESTADO_GLOBAL_FRONTEND.md](../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)) — el refresh token no lo maneja el frontend en ningún momento, vive únicamente en la cookie que el propio navegador adjunta solo.
7. El usuario es redirigido a la pantalla principal según su rol.

**Reglas de negocio**

- La contraseña nunca se almacena ni se transmite en texto plano (hash BCrypt + HTTPS).
- El access token (JWT) tiene una duración corta (ej. 15 min); el refresh token tiene una duración más larga (ej. 7 días) — el ciclo de vida completo del refresh token (renovación, revocación) es responsabilidad de [HU-02](HU02%20-%20Renovar%20Sesión.md) y [HU-03](HU03%20-%20Cerrar%20Sesión.md), no de esta HU.
- El refresh token se transporta como **cookie `HttpOnly`/`SameSite=Lax`** seteada por el backend, nunca en el body de la respuesta ni accesible por JavaScript — ver el detalle completo de atributos de la cookie en [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- **Bloqueo por intentos fallidos:** se lleva un contador de intentos fallidos consecutivos por identificador de login (`username` o `correo`, el que se haya usado) en **Redis** (clave `auth:failed-attempts:{identificador}`, con TTL igual a la ventana de conteo — ej. 15 minutos, se resetea sola si no hay más intentos fallidos en ese lapso). Al superar el máximo (ej. 5 intentos), se marca la cuenta como bloqueada con una clave separada con TTL igual al tiempo de bloqueo (ej. `auth:locked:{identificador}`, TTL 15 min). Se eligió Redis y no una columna en la tabla `users` porque es un dato puramente transitorio (se auto-expira solo) y Redis ya es la pieza que el proyecto usa para este tipo de estado (mismo mecanismo que el storage de refresh tokens, ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)) — no hace falta una migración de Flyway ni limpieza manual de un campo persistente. Es un mecanismo independiente del campo `Usuario.estado` (ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)).
- Un usuario con `estado` distinto de `ACTIVO` no puede iniciar sesión aunque sus credenciales sean correctas.
- El JWT debe incluir claims mínimos necesarios (`codigo` de usuario, lista de nombres de roles activos) y no debe incluir información sensible.
- Un usuario puede tener **más de un rol activo** a la vez (ver [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)) — una vez autenticado, accede a las funcionalidades permitidas para cualquiera de sus roles.
- Ni la contraseña ni los tokens completos se loguean en ningún nivel (INFO/WARN/ERROR) — ver [LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear).
- Todas las respuestas (éxito y error) siguen el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md) (`status`/`data`/`message`/`error`/`timestamp`) — no un formato ad-hoc para este endpoint.

---

## Criterios de aceptación

- **CA01:** Dado que el usuario ingresa credenciales correctas (`username` o `correo`) para una cuenta con `estado = ACTIVO` y no bloqueada, el sistema responde `200` con `status: "success"`, `data: { accessToken, usuario: { codigo, username, correo, roles } }` y la cookie `refreshToken` (`HttpOnly`) seteada en la respuesta.
- **CA02:** Dado que el usuario ingresa un `username`/`correo` inexistente, el sistema responde `401` (`UnauthorizedException`) con un código de error catalogado del módulo `AUTH` y `message: "Credenciales inválidas"` (sin indicar si el error es por identificador o por contraseña, para no dar pistas a un atacante).
- **CA03:** Dado que el usuario ingresa una contraseña incorrecta, el sistema responde el mismo `401` genérico de CA02 (mismo código de error, mismo mensaje).
- **CA04:** Dado que el usuario deja campos vacíos o con formato inválido, el sistema responde `400` (validación de `@Valid`, código `ERR_SYS_01`) indicando el/los campo(s) afectado(s).
- **CA05:** Dado que la cuenta del usuario tiene `estado` distinto de `ACTIVO`, el sistema responde `403` (`ForbiddenException`) con `message: "Cuenta inactiva"`.
- **CA06:** Dado que el usuario superó el número máximo de intentos fallidos consecutivos, el sistema responde `423` (`LockedException`, ver [EXCEPCIONES_BACKEND.md](../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)) con `message` indicando el tiempo restante de bloqueo.
- **CA07:** Dado un intento fallido que no llega al máximo, el contador de intentos se incrementa mas el login responde igual que CA02/CA03 (el usuario no ve el conteo, solo el mensaje genérico de credenciales inválidas).
- **CA08:** Dado un login exitoso, el contador de intentos fallidos de ese identificador en Redis se reinicia (borra la clave `auth:failed-attempts:{identificador}`).
- **CA09:** Dado un error inesperado del servidor, el sistema responde `500` (`ERR_SYS_00`) sin exponer detalles técnicos internos.
- **CA10:** Todas las respuestas (éxito y error) siguen exactamente el sobre estándar de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md) — no una estructura distinta inventada para este endpoint.

## Desarrollador

Mauro Boris Gutierrez Primintela
