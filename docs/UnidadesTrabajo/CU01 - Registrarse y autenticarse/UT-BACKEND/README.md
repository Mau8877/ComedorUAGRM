# Unidades de trabajo — CU01-BE

**Caso de uso:** Backend de registro de un Postulante, inicio de sesión con correo y contraseña (JWT propio), renovación y cierre de sesión por dispositivo, bloqueo por intentos fallidos, registro del acceso en la bitácora y catálogo de carreras para el formulario.
**Fuente CU:** [`CU01 - Registrarse y autenticarse`](../../../CasosDeUso/CU01%20-%20Registrarse%20y%20autenticarse.md)
**Diagramas de grado:** `images/038_CU01_Registrarse_Autenticarse.png` (contexto), `images/051_Analisis_CU01_Registrarse_Autenticarse.jpg` (análisis), `images/121_Secuencia_CU01_Registrarse_Autenticarse.jpg` (secuencia) y `images/134_Mockup_CU01_Registrarse_autenticarse.png` (mockup).
**Recurso HTTP / contrato:** `GET /api/v1/carreras` (pública), `POST /api/v1/auth/registro`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` (todas públicas) y `GET /api/v1/auth/usuario-actual`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/src/test/java/com/comedoruagrm/backend/`.

- **Cambio de rumbo: JWT propio en lugar de Firebase Auth.** La primera versión de estas fichas delegaba el login en Firebase Auth. Se decidió que la autenticación la implemente el propio backend; Firebase queda solo para las notificaciones push (FCM) de mobile. Todo lo que dependía de Firebase (verificación de su ID token, `firebase_uid`, registro idempotente por uid, "el backend no tiene login") se reemplazó por lo de abajo.
- **Diagramas de grado.** El análisis (051) y la secuencia (121) todavía muestran Firebase Auth como proveedor de identidad: **ganan el CU y estas UT**, y esos diagramas quedan por actualizar en la documentación de grado (también el diseño lógico 091, que ahora tiene `usuario.password_hash`, las columnas de bloqueo y la tabla `sesion`; `docs/DiagramaConceptual.mermaid` ya está al día). El mockup (134) sigue mostrando el diseño anterior (contraseña de mínimo 8 con letras y números, sin carrera, registro universitario ni teléfono, con casilla de términos y login/registro en pestañas): tampoco se corrige.
- **Token de acceso + refresh token.** El token de acceso es un JWT HS256 de 15 minutos (`JWT_SECRET`), con el código del usuario como `sub` y nada más: el rol y el estado de la cuenta se leen de la base en cada petición. El refresh token es una cadena aleatoria opaca de 7 días, **por dispositivo**, de la que la base guarda solo el hash SHA-256 (tabla `sesion`); cada renovación lo rota. Cerrar sesión revoca solo el refresh token recibido (CA19).
- **Cookies `HttpOnly` en la web, Bearer en mobile.** `registro`, `login` y `refresh` escriben las cookies `access_token` y `refresh_token` (`HttpOnly`, `SameSite=Strict`, `Secure` con `COOKIE_SECURE=true`) y devuelven igual los tokens en el cuerpo, para mobile. El token se lee del header `Authorization` o, si no viene, de la cookie; `refresh` y `logout` aceptan el refresh token en el cuerpo (mobile) o en la cookie (web, sin cuerpo). La web no guarda ningún token. Defensa CSRF: `SameSite=Strict` y CORS restringido.
- **Contraseñas con BCrypt.** `usuario.password_hash`; nunca en texto plano, en un DTO de salida ni en un log. Política: 6 a 72 caracteres (72 es el máximo que procesa BCrypt) y coincidir con su confirmación (CA05).
- **El bloqueo por intentos fallidos lo implementa este backend** (A6, CA10, CA11): 5 intentos consecutivos → 5 min, luego 15 min, 1 h y 24 h con cada bloqueo seguido (**escala por definir con la DUBSS**); un acierto reinicia. Responde `403` `ERR_AUTH_05` y se niega aunque la contraseña sea correcta. El conteo se guarda aunque el login termine en error (`noRollbackFor`).
- **Mismo mensaje ante credenciales incorrectas** (A3, CA08): `401` `ERR_AUTH_04` exista o no el correo; si no existe se compara igual contra un hash de relleno para no delatarlo por el tiempo de respuesta.
- **Registro idempotente por datos.** Repetir la misma petición (mismo correo, CI y contraseña) devuelve `200` con la cuenta existente y una sesión nueva, sin duplicar nada (A7, CA12). El correo se normaliza a minúsculas y sin espacios. El registro **inicia la sesión** (paso 6 del CU): la respuesta ya trae los tokens.
- **Rutas públicas** (`security/RutasPublicas`): registro, login, refresh, logout y `GET /carreras`. Esas rutas ignoran el header `Authorization`, para que un token vencido que el cliente sigue mandando no haga fallar el login.
- **Dependencia `security/` → `features/`.** `security/` no puede importar `features/` (regla de dependencia). El filtro que resuelve el usuario usa una interfaz que define `security/` (`UsuarioAutenticableProvider`) y que implementa `features/usuarios/`.
- **`GET /api/v1/carreras` es público.** El formulario de registro necesita la lista de carreras **antes** de que exista una cuenta ni token. Es solo lectura de un catálogo y se documenta en `SEGURIDAD_AUTH_BACKEND.md` (UT-CU01-BE-09).
- **Bitácora.** La tabla `bitacora` es del paquete P6 (CU25 la consulta), pero CU01 escribe en ella (CA20), así que se crea y se escribe acá. Registra `REGISTRO_USUARIO` e `INICIO_SESION` (también el que sigue a un registro). Los intentos fallidos no quedan en la bitácora: se cuentan en `usuario.intentos_fallidos`.
- **Módulos.** `features/usuarios/` (P1: `usuario`, `rol`, `postulante`, `carrera`, credenciales y bloqueo), `features/auth/` (controller y services de registro/sesión, y el model de `sesion`) y `features/bitacora/`. `auth` usa el `service` de `usuarios` y el de `bitacora`, nunca sus repositorios.
- **Códigos de error:** `ERR_AUTH_01` a `ERR_AUTH_06` (`security/` y sesión) y `ERR_US_01` a `ERR_US_04` (`usuarios`; `ERR_US_05` quedó retirado), cada uno documentado en `docs/errors/`.
- **Prerrequisito de configuración:** definir `JWT_SECRET` (mínimo 32 caracteres; `openssl rand -base64 48`) en el `.env` de la raíz y en el del backend. Sin él, el backend no arranca.
- **Longitudes de columna** que el diagrama de grado deja sin tamaño (`rol.nombre`, `carrera.nombre`, `carrera.facultad`) se fijan en `varchar(50)`, `varchar(150)` y `varchar(150)`.

## Contrato de datos / recurso HTTP

**Endpoints** (todos bajo `/api/v1`, respuestas en el sobre de `RESPONSES_BACKEND.md`):

| Método | Ruta | Auth | Body | Éxito | Errores |
|--------|------|------|------|-------|---------|
| `GET` | `/carreras` | Pública | — | `200` `data`: array de `{codigo, nombre, facultad}` + `meta` (`?pageSize=200` para el selector) | — |
| `POST` | `/auth/registro` | Pública | `RegistroRequest` | `201` con `SesionResponse`; `200` si la misma petición ya se había procesado | `400` `ERR_SYS_01`; `404` `ERR_US_04`; `409` `ERR_US_01`/`02`/`03` |
| `POST` | `/auth/login` | Pública | `LoginRequest` | `200` con `SesionResponse` y acceso anotado en la bitácora | `401` `ERR_AUTH_04`; `403` `ERR_AUTH_03`/`05` |
| `POST` | `/auth/refresh` | Pública | `RefreshTokenRequest` o la cookie `refresh_token` | `200` con `SesionResponse` (refresh token rotado) | `401` `ERR_AUTH_06`; `403` `ERR_AUTH_03` |
| `POST` | `/auth/logout` | Pública | `RefreshTokenRequest` o la cookie `refresh_token` | `200`, `data: null`; idempotente | — |
| `GET` | `/auth/usuario-actual` | Token de acceso + `usuario` activo | — | `200` con `UsuarioResponse` | `401` `ERR_AUTH_01`/`02`; `403` `ERR_AUTH_03` |

**`RegistroRequest`:** `nombres` (≤100), `apellidos` (≤100), `ci` (≤20), `registroUniversitario` (9 dígitos, no empieza por 0), `carreraCodigo` (UUID), `telefono` (≤20), `correo` (válido, ≤150), `contrasena` (6 a 72) y `confirmacionContrasena` (igual a la contraseña); todos obligatorios. Sin `rol`: siempre es `POSTULANTE` (CA06). El formato del CI está **por definir con la DUBSS**: por ahora solo se valida que no esté vacío y su largo máximo.

**`LoginRequest`:** `correo` y `contrasena` (obligatorios; sin más validación). **`RefreshTokenRequest`:** `refreshToken` (opcional: la web lo manda por cookie).

**`SesionResponse`:** `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` (segundos de vida del token de acceso) y `usuario` (`UsuarioResponse`).

**`UsuarioResponse`:** `codigo` (UUID), `nombres`, `apellidos`, `correo`, `rol` (nombre del rol).

**Códigos de error:**

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_AUTH_01` | 401 | — (lo escribe el entry point) | Token de acceso ausente, mal firmado o vencido |
| `ERR_AUTH_02` | 401 | — (lo escribe el filtro) | Token válido pero el usuario ya no existe |
| `ERR_AUTH_03` | 403 | `ForbiddenException` / el filtro | El `usuario` existe pero está desactivado |
| `ERR_AUTH_04` | 401 | `UnauthorizedException` | Correo o contraseña incorrectos |
| `ERR_AUTH_05` | 403 | `ForbiddenException` | Cuenta bloqueada temporalmente por intentos fallidos |
| `ERR_AUTH_06` | 401 | `UnauthorizedException` | Refresh token inválido, vencido o revocado |
| `ERR_US_01` | 409 | `ConflictException` | El CI ya está registrado |
| `ERR_US_02` | 409 | `ConflictException` | El correo ya está registrado |
| `ERR_US_03` | 409 | `ConflictException` | El registro universitario ya está registrado |
| `ERR_US_04` | 404 | `NotFoundException` | La carrera indicada no existe |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-BE-01](./UT-CU01-BE-01.md) | Migraciones de tablas de cuentas, acceso y sesiones | — |
| [UT-CU01-BE-02](./UT-CU01-BE-02.md) | Models y repositorios de los catálogos Rol y Carrera | 01 |
| [UT-CU01-BE-03](./UT-CU01-BE-03.md) | Models y repositorios de Usuario y Postulante | 01, 02 |
| [UT-CU01-BE-04](./UT-CU01-BE-04.md) | Bitácora: Model, repositorio y service para registrar eventos | 01, 03 |
| [UT-CU01-BE-05](./UT-CU01-BE-05.md) | JWT propio: firma, verificación y hash de contraseñas | — |
| [UT-CU01-BE-06](./UT-CU01-BE-06.md) | Respuestas 401 en el sobre de la API y catálogo ERR_AUTH | 05 |
| [UT-CU01-BE-07](./UT-CU01-BE-07.md) | Filtro que resuelve el usuario autenticado y controla cuenta activa | 03, 05, 06 |
| [UT-CU01-BE-08](./UT-CU01-BE-08.md) | Anotación @CurrentUserId y seguridad por método | 07 |
| [UT-CU01-BE-09](./UT-CU01-BE-09.md) | GET /api/v1/carreras (catálogo público de carreras) | 02, 05 |
| [UT-CU01-BE-10](./UT-CU01-BE-10.md) | DTOs de registro y respuesta, y códigos ERR_US | 02 |
| [UT-CU01-BE-11](./UT-CU01-BE-11.md) | Lógica de registro de un Postulante | 03, 04, 05, 10, 15 |
| [UT-CU01-BE-12](./UT-CU01-BE-12.md) | POST /api/v1/auth/registro | 07, 10, 11 |
| [UT-CU01-BE-15](./UT-CU01-BE-15.md) | Sesiones por dispositivo: refresh tokens | 01, 03, 05 |
| [UT-CU01-BE-13](./UT-CU01-BE-13.md) | Login, refresh, logout y usuario actual | 04, 08, 11, 15 |
| [UT-CU01-BE-14](./UT-CU01-BE-14.md) | Pruebas de integración con base real del registro y la sesión | 09, 12, 13 |

**Cadena:** `01 → 02 → 03 → 04`; `05 → 06 → 07 → 08`; `02, 05 → 09`; `02 → 10`; `01, 03, 05 → 15`; `03, 04, 05, 10, 15 → 11`; `07, 10, 11 → 12`; `04, 08, 11, 15 → 13`; `09, 12, 13 → 14`. (La UT-15 se agregó al cambiar a JWT propio; por eso su número va después de la 14 aunque se ejecute antes de la 11.)

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A, pasos 4 a 6 (registro) | Validación de datos, contraseña y unicidad, creación con rol Postulante, sesión abierta | 10, 11, 12, 15 |
| Flujo B, pasos 9 a 12 (inicio de sesión) | Credenciales, cuenta activa y no bloqueada, rol, bitácora, sesión | 04, 05, 07, 13, 15 |
| Flujo C (cierre de sesión) | Revocar el refresh token del dispositivo | 13, 15 |
| A1 | CI, correo o registro universitario ya registrados → 409 | 11, 12 |
| A2 | Datos del formulario o contraseña inválidos → 400 | 10, 12 |
| A3 | Credenciales incorrectas → 401 con el mismo mensaje | 13 |
| A4 | Cuenta desactivada → 403 | 07, 13 |
| A6 | Bloqueo temporal por intentos fallidos | 13 |
| A7 | Registro repetido idempotente | 11 |
| A8 | Token inválido o vencido → 401 (y refresh) | 05, 06, 07, 13, 15 |
| A9 | Error inesperado → 500 con mensaje genérico (ya existe `GlobalExceptionHandler`) | — (existente) |
| A10 | Cerrar una sesión ya cerrada no da error | 13, 15 |
| CA01, CA06, CA12 | Registro correcto, siempre como Postulante, una sola cuenta | 11, 12 |
| CA02, CA03, CA21 | Conflictos de CI, correo y registro universitario | 03, 11, 12, 14 |
| CA04, CA05 | Campos obligatorios, formato y contraseña | 10, 12 |
| CA07, CA09 | Login correcto; cuenta desactivada → 403 | 07, 13 |
| CA08 | Mismo mensaje con correo inexistente o contraseña incorrecta | 13 |
| CA10, CA11 | Bloqueo y bloqueos de duración creciente | 13 |
| CA14 | Mismo comportamiento web y móvil: es la misma API | 12, 13 |
| CA15 | Sesión inválida → 401 | 05, 06, 07, 15 |
| CA17 a CA19 | Cerrar sesión solo en este dispositivo | 13, 15 |
| CA20 | Acceso en la bitácora; sin contraseñas ni tokens en logs | 04, 11, 13, 15 |

## Follow-ups (no se implementan en estas UT)

- Mantenimiento posterior del catálogo de carreras (altas, bajas, cambios): se define cuando un caso de uso lo pida.
- Formato del CI y escala del bloqueo — por definir con la DUBSS.
- Consulta de la bitácora desde el panel: CU25.
- Alta de cuentas del personal con su contraseña inicial: CU14.
- Recuperar contraseña (y, por tanto, reasignar la de las cuentas que existían antes del cambio a JWT): CU15.
- Limpieza periódica de las sesiones vencidas o revocadas, y detección de reutilización de un refresh token ya rotado.
- Protección por rol con `@PreAuthorize` en endpoints de negocio: la primera UT de otro CU que lo necesite. En este CU solo queda habilitada la seguridad por método (UT-CU01-BE-08).

## Qué NO incluyen estas UT

- Recuperar contraseña y gestionar perfil (CU15), gestionar usuarios y roles (CU14).
- Verificación del correo electrónico.
- Notificaciones push con Firebase (FCM): son de otro CU; Firebase no participa en la autenticación.
- Cualquier endpoint de postulación, convocatoria o beca.
- Frontend y mobile (sus propias UT).
