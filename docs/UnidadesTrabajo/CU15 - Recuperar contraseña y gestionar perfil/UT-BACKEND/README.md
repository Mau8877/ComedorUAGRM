# Unidades de trabajo — CU15-BE

**Caso de uso:** Backend de la recuperación de contraseña por correo (solicitud del enlace y restablecimiento) y del perfil del usuario autenticado (consulta, actualización de correo y teléfono, y cambio de contraseña), con envío de correo por SMTP, cierre de sesiones y bitácora.
**Fuente CU:** [`CU15 - Recuperar contraseña y gestionar perfil`](../../../CasosDeUso/CU15%20-%20Recuperar%20contrase%C3%B1a%20y%20gestionar%20perfil.md)
**Diagramas de grado:** los del CU15 en la documentación de grado (contexto, análisis, secuencia y mockup 140; ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)). La carpeta de la documentación de grado no estaba disponible al redactar estas fichas: **antes de implementar la primera UT hay que abrir esos diagramas** y, si contradicen algo de acá, avisar (el mockup 140 ya se sabe que difiere: muestra nombres, apellidos y carrera como editables; gana el CU).
**Recurso HTTP / contrato:** `POST /api/v1/auth/recuperar-contrasena`, `POST /api/v1/auth/restablecer-contrasena` (públicos), `POST /api/v1/auth/cambiar-contrasena`, `GET /api/v1/perfil` y `PATCH /api/v1/perfil` (autenticados, cualquier rol).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend/`.

- **Hoy no hay envío de correo en el proyecto.** No existe `spring-boot-starter-mail`, ni un servidor SMTP en `docker-compose.yml`, ni variables `MAIL_*`. La `UT-CU15-BE-03` lo agrega: **Mailpit** (un servidor SMTP de pruebas que no entrega nada y muestra los correos en una página web, `http://localhost:8025`) para desarrollo. El código usa solo `JavaMailSender` de Spring, así que **pasar a un proveedor real es cambiar variables de entorno** (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM`), sin tocar código. Qué proveedor real se usa queda **por definir** (la DUBSS o la universidad deben dar el SMTP o la cuenta emisora).
- **Dos columnas nuevas en `usuario`, sin tabla aparte.** El enlace vigente de cada cuenta se guarda en `restablecimiento_token_hash` (SHA-256, nunca el token) y `restablecimiento_expira_en`, ambas nulas cuando no hay enlace. No están en el diseño lógico de la base de datos; según [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#fuente-de-verdad-del-modelo-de-datos) primero se agregan a `docs/DiagramaConceptual.mermaid` y a la documentación de grado (`UT-CU15-BE-02`; la copia de la documentación de grado la actualiza a mano la persona, está fuera de este repo: ver [`docs/pendientes/DOCUMENTACION_GRADO_CU15.md`](../../../pendientes/DOCUMENTACION_GRADO_CU15.md)). Un enlace sirve solo si el hash coincide y `restablecimiento_expira_en` es futuro. Se descartó una tabla de enlaces porque el CU pide un solo enlace vigente por cuenta y ningún historial.
- **El token del enlace** es una cadena aleatoria de 256 bits (`SecureRandom`, Base64 URL), igual que el refresh token del CU01. Vive solo en el correo; en la base y en los logs solo existe su hash. **Vigencia: 60 minutos** (`app.auth.reset-ttl-minutes`, por confirmar con la DUBSS).
- **El enlace** apunta al portal web: `{app.web.base-url}/restablecer-contrasena?token=…` (`FRONTEND_URL`, por defecto `http://localhost`, el proxy). La app móvil solo pide el enlace; el correo se abre en el navegador.
- **Respuesta neutra.** `POST /auth/recuperar-contrasena` responde siempre `200` con el mismo cuerpo (correo inexistente, cuenta desactivada, correo enviado o fallo de SMTP). El envío es **asíncrono** (`@Async`) para que el tiempo de respuesta tampoco delate si la cuenta existe; un fallo del servicio de correo se registra en el log (WARN, sin correo ni token) y no llega al cliente (A9).
- **Un solo enlace vigente por cuenta.** Al generar uno nuevo se **sobrescriben** las dos columnas de la cuenta, y el enlace anterior deja de servir sin ningún paso extra. Al usarlo se dejan en `null`.
- **Restablecer** (token + contraseña nueva): valida el enlace, guarda el hash BCrypt, reinicia `intentos_fallidos`, `bloqueos_consecutivos` y `bloqueado_hasta`, marca el enlace usado y **revoca todas las sesiones** de la cuenta (`SesionService.revocarTodas`, ya existe del CU14). Cualquier enlace malo (desconocido, vencido, usado o reemplazado) da el mismo `400` `ERR_AUTH_08`: no se distingue cuál fue.
- **Cambiar contraseña** vive en `/api/v1/auth/cambiar-contrasena` (y no bajo `/perfil`) porque la cookie `refresh_token` de la web solo viaja a `/api/v1/auth/**` (`Path=/api/v1/auth`); así el backend sabe cuál es la sesión actual y la conserva. En mobile el refresh token llega en el cuerpo (`refreshToken`); el cuerpo gana sobre la cookie, igual que en `/refresh` y `/logout`. Se revocan las demás sesiones (`SesionService.revocarOtras`); si no llega ningún refresh token no se puede saber cuál conservar y se cierran todas.
- **La contraseña actual incorrecta es `400`, no `401`:** un `401` haría que el cliente intente renovar el token y lo mande al login. Código `ERR_AUTH_09`, también cuando se pide cambiar el correo sin enviarla.
- **Perfil.** `GET /perfil` devuelve los datos del usuario del token; si es Postulante, además registro universitario, carrera y teléfono. `PATCH /perfil` acepta solo `correo`, `telefono` y `contrasenaActual`; el CI y cualquier otro campo que llegue **se ignora** (A4). Cambiar el correo exige `contrasenaActual`; el correo se normaliza y debe ser único (`409` `ERR_US_02`, ya existe). Un `telefono` de alguien que no es Postulante es `400` `ERR_US_13`. Un `PATCH` sin cambios reales responde `200` y no anota la bitácora.
- **Bitácora con `@Auditar`** (acciones nuevas en `BitacoraAcciones`): `RESTABLECIMIENTO_CONTRASENA`, `CAMBIO_CONTRASENA` y `ACTUALIZACION_PERFIL`. El actor es el dueño de la cuenta (en el restablecimiento no hay sesión: el actor sale del enlace). `ACTUALIZACION_PERFIL` guarda en `detalle` **qué campos** cambiaron (`["correo","telefono"]`), no sus valores. La solicitud del enlace **no** se anota. Nunca contraseñas, tokens ni el enlace.
- **Logs.** INFO al restablecer y al cambiar la contraseña, con `userId` y sin datos sensibles. Nunca el token, el enlace ni la contraseña (`LOGGING_BACKEND.md`).
- **Cuentas heredadas.** Las cuentas anteriores al JWT propio tienen `password_hash = '!'` (no pueden entrar); este CU les da la forma de obtener una contraseña. No requiere código aparte.
- **Un código de error por cada rechazo nuevo**, documentado en `docs/errors/`.

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md). Contraseñas: de 6 a 72 caracteres y coincidir con su confirmación (mismo mecanismo que `RegistroRequest`: Bean Validation, `400` `ERR_SYS_01`).

| Método | Ruta | Acceso | Cuerpo | Respuesta | Errores |
|--------|------|--------|--------|-----------|---------|
| `POST` | `/api/v1/auth/recuperar-contrasena` | Público | `correo` | `200` `data: null`, mensaje neutro | `400` `ERR_SYS_01` (correo vacío o mal formado) |
| `POST` | `/api/v1/auth/restablecer-contrasena` | Público | `token`, `contrasena`, `confirmacionContrasena` | `200` `data: null` | `400` `ERR_SYS_01` / `ERR_AUTH_08` |
| `POST` | `/api/v1/auth/cambiar-contrasena` | Sesión | `contrasenaActual`, `contrasenaNueva`, `confirmacionContrasena`, `refreshToken` (opcional, mobile) | `200` `data: null` | `400` `ERR_SYS_01` / `ERR_AUTH_09`; `401` `ERR_AUTH_01` |
| `GET` | `/api/v1/perfil` | Sesión | — | `200` `PerfilResponse` | `401` `ERR_AUTH_01` |
| `PATCH` | `/api/v1/perfil` | Sesión | `correo`, `telefono`, `contrasenaActual` (todos opcionales) | `200` `PerfilResponse` | `400` `ERR_SYS_01` / `ERR_AUTH_09` / `ERR_US_13`; `409` `ERR_US_02`; `401` `ERR_AUTH_01` |

`PerfilResponse` = `codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo` y `postulante` (`null` si no es Postulante; si lo es: `registroUniversitario`, `carrera` {`codigo`, `nombre`, `facultad`}, `telefono`).

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_AUTH_08` | 400 | `ValidationException` | El enlace de restablecimiento no sirve: desconocido, vencido, ya usado o reemplazado por uno más nuevo |
| `ERR_AUTH_09` | 400 | `ValidationException` | La contraseña actual es incorrecta, o falta al cambiar el correo |
| `ERR_US_13` | 400 | `ValidationException` | Solo el Postulante tiene teléfono: no se puede actualizar en otra cuenta |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU15-BE-01](./UT-CU15-BE-01.md) | Códigos de error `ERR_AUTH_08`, `ERR_AUTH_09`, `ERR_US_13` y acciones de bitácora | — |
| [UT-CU15-BE-02](./UT-CU15-BE-02.md) | Columnas del enlace de restablecimiento en `usuario` | — |
| [UT-CU15-BE-03](./UT-CU15-BE-03.md) | Envío de correo por SMTP: Mailpit en desarrollo y configuración | — |
| [UT-CU15-BE-04](./UT-CU15-BE-04.md) | Token del enlace: generar y calcular su hash | — |
| [UT-CU15-BE-05](./UT-CU15-BE-05.md) | Correo del enlace de restablecimiento (asíncrono y tolerante a fallos) | 03 |
| [UT-CU15-BE-06](./UT-CU15-BE-06.md) | Contraseña de la cuenta: verificar, cambiar y restablecer | 01 |
| [UT-CU15-BE-07](./UT-CU15-BE-07.md) | Revocar las sesiones de una cuenta menos la actual | — |
| [UT-CU15-BE-08](./UT-CU15-BE-08.md) | DTOs de recuperación, cambio de contraseña y perfil | — |
| [UT-CU15-BE-09](./UT-CU15-BE-09.md) | Solicitar el enlace de restablecimiento | 02, 04, 05, 08 |
| [UT-CU15-BE-10](./UT-CU15-BE-10.md) | Restablecer la contraseña con el enlace | 01, 02, 04, 06, 07, 08, 09 |
| [UT-CU15-BE-11](./UT-CU15-BE-11.md) | Consultar y actualizar el perfil | 01, 06, 08 |
| [UT-CU15-BE-12](./UT-CU15-BE-12.md) | Cambiar la contraseña desde el perfil | 01, 06, 07, 08 |
| [UT-CU15-BE-13](./UT-CU15-BE-13.md) | Endpoints, rutas públicas y reglas | 09, 10, 11, 12 |
| [UT-CU15-BE-14](./UT-CU15-BE-14.md) | Pruebas de integración con base real | 13 |

**Cadena:** `03 → 05`; `02, 04, 05, 08 → 09 → 10` (con `01, 06, 07`); `01, 06, 08 → 11`; `01, 06, 07, 08 → 12`; `09, 10, 11, 12 → 13 → 14`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (pasos 1 a 5) | Solicitud del enlace, un solo enlace vigente, respuesta neutra | 02, 03, 04, 05, 08, 09, 13 |
| Flujo B (pasos 6 a 11) | Verificar el enlace, guardar la contraseña, desbloquear, cerrar sesiones y anotar | 02, 04, 06, 07, 10, 13 |
| Flujo C (pasos 12 y 13) | Consultar el perfil | 08, 11, 13 |
| Flujo D (pasos 14 a 18) | Actualizar correo y teléfono con reautenticación | 06, 08, 11, 13 |
| Flujo E (pasos 19 a 22) | Cambiar la contraseña conservando la sesión actual | 06, 07, 08, 12, 13 |
| A1, A11 | Correo inexistente o cuenta desactivada; varias solicitudes seguidas | 09 |
| A2 | Enlace vencido, usado, reemplazado o inválido | 01, 10 |
| A3 | Contraseña fuera de la política o sin coincidir | 08 |
| A4 | CI y otros datos no editables | 08, 11 |
| A5 | Correo ya usado por otra cuenta (`409`) | 11 |
| A6, A7 | Contraseña actual incorrecta; correo nuevo exige la actual | 01, 06, 11, 12 |
| A8 | Sin sesión (`401`) | 13 |
| A9 | Servicio de correo caído | 05, 09 |
| CA01–CA02 | Enlace enviado / mismo mensaje sin correo | 05, 09 |
| CA03–CA06 | Restablecer, vencido, usado, solo el último | 02, 10 |
| CA07 | `400` por política o confirmación | 08 |
| CA08, CA09 | Sesiones cerradas y cuenta desbloqueada | 07, 10 |
| CA10–CA11 | Perfil con CI solo de lectura | 08, 11 |
| CA12–CA15 | Teléfono, correo, conflicto y contraseña actual incorrecta | 11 |
| CA16 | Cambio de contraseña conservando la sesión actual | 07, 12 |
| CA17 | `401` sin sesión | 13 |
| CA18 | Servicio de correo no disponible | 05, 09 |
| CA19 | Bitácora y sin secretos en logs | 01, 05, 09, 10, 11, 12, 14 |
| CA20, CA21 | Mismo comportamiento en web y móvil | 13 (ver UT-FRONTEND y UT-MOBILE) |

## Follow-ups (no se implementan en estas UT)

- Elegir el proveedor SMTP real y cargar sus credenciales (solo variables de entorno).
- Limitar cuántos correos de recuperación se mandan por cuenta o por IP (hoy solo rige el rate limit general de `/api/v1/**`).
- Verificar el correo nuevo con un enlace al correo nuevo, o avisar al anterior (por definir con la DUBSS).
- Obligar al personal a cambiar la contraseña temporal del CU14 en su primer acceso (por definir con la DUBSS; necesita una columna nueva en `usuario`).
- Limpieza periódica de sesiones vencidas (los enlaces vencidos no hacen falta limpiarlos: se pisan al pedir uno nuevo).
- Historial de enlaces emitidos, solo si algún día se quiere auditarlos (hoy el CU no lo pide).
- Extraer el hash SHA-256 de `SesionService` y de `TokenEnlace` a una sola clase.

## Qué NO incluyen estas UT

- Las pantallas (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md) y [`UT-MOBILE`](../UT-MOBILE/README.md)).
- Notificaciones al usuario (CU de notificaciones): esto solo envía el correo del enlace.
- Editar nombres, apellidos, registro universitario o carrera: nombres del personal los corrige el Administrador (CU14).
- Consultar la bitácora (CU25).
