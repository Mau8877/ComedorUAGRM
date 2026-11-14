# Unidades de trabajo — CU01-FE

**Caso de uso:** Pantallas web de crear cuenta e iniciar sesión con el JWT propio del backend, conexión de la sesión con la API (token de acceso y renovación), guard de rutas privadas y cierre de sesión.
**Fuente CU:** [`CU01 - Registrarse y autenticarse`](../../../CasosDeUso/CU01%20-%20Registrarse%20y%20autenticarse.md)
**Diagramas de grado:** `images/038_CU01_Registrarse_Autenticarse.png` (contexto), `images/051_Analisis_CU01_Registrarse_Autenticarse.jpg` (análisis), `images/121_Secuencia_CU01_Registrarse_Autenticarse.jpg` (secuencia) y `images/134_Mockup_CU01_Registrarse_autenticarse.png` (mockup).
**Recurso HTTP / contrato:** Rutas `/registro` y `/login`; consume `GET /carreras`, `POST /auth/registro`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` y `GET /auth/usuario-actual` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (la feature nueva es `features/auth/`).

- **Cambio de rumbo: JWT propio en lugar de Firebase Auth.** La primera versión de estas fichas usaba el SDK de Firebase. Ahora el login lo resuelve el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)); la web **no** instala el SDK de Firebase ni necesita variables `VITE_FIREBASE_*`. Firebase queda solo para push en mobile.
- **Diagramas de grado.** El análisis (051) y la secuencia (121) todavía muestran Firebase; el mockup (134) sigue mostrando el diseño anterior (contraseña de mínimo 8 con letras y números, sin carrera, registro universitario ni teléfono, con casilla de términos y login/registro en pestañas): **ganan el CU y estas UT**.
- **Dónde viven los tokens: en ningún lado del JavaScript.** El backend los pone en cookies `HttpOnly` (`access_token` y `refresh_token`, `SameSite=Strict`) y el navegador las manda solo (`withCredentials`); la web no guarda ni lee ningún token, así un XSS no puede robar la sesión. Para saber si hay sesión se pregunta al backend (`GET /auth/usuario-actual`). Zustand sigue siendo solo estado de UI y los datos del usuario (nombre, rol) vienen del backend con TanStack Query (`GET /auth/usuario-actual`), no se copian a un store (`ESTADO_GLOBAL_FRONTEND.md`).
- **Renovación automática.** Ante un `401` de una ruta protegida se renueva con `POST /auth/refresh` sin cuerpo (una sola renovación a la vez aunque fallen varias peticiones juntas) y se reintenta la petición; si el refresh token tampoco sirve, se limpia la caché y se lleva a `/login` (A8). Un `401` de `login`, `registro` o `refresh` **no** dispara renovación: es un error del formulario.
- **Flujo de registro:** (1) validar el formulario con Zod; (2) `POST /auth/registro` — el backend crea la cuenta y deja la sesión abierta con las cookies —; (3) llevar al usuario a su módulo. Si el backend responde `409`, se marca el campo en conflicto (CI, correo o registro universitario, A1).
- **Flujo de inicio de sesión:** (1) validar; (2) `POST /auth/login`; (3) llevar al módulo del rol. El backend anota el acceso en la bitácora al iniciar sesión.
- **Errores del backend → mensajes** (la política de contraseña, el bloqueo y el mensaje único de credenciales los aplica el backend; el cliente solo los muestra): `ERR_AUTH_04` → "Correo o contraseña incorrectos" (siempre el mismo, A3); `ERR_AUTH_05` → cuenta bloqueada temporalmente, con el mensaje del backend, que ya dice cuántos minutos faltan (A6); `ERR_AUTH_03` → cuenta desactivada, comunicarse con la DUBSS (A4); error de red o `5xx` → error de conexión con opción de reintentar (A9).
- **Mientras no existan los módulos de cada rol, todos aterrizan en `/panel`** (el placeholder de `_authenticated/`). `rutaInicioPorRol(rol)` es el único lugar a actualizar cuando los CU de postulación, auditoría, dashboard y asistencia creen sus pantallas. Los roles `DIRECTIVO` y `PERSONAL_COMEDOR` no tienen layout todavía: ven una pantalla "tu módulo aún no está disponible" con el botón de cerrar sesión.
- **El enlace «¿Olvidó su contraseña?»** (A5, CA13) lo agrega el CU15, que crea su ruta; en este CU la pantalla de login no lo muestra todavía.
- **Sin variables de entorno nuevas:** el cliente solo necesita `VITE_API_BASE_URL`, que ya existe.
- Textos en español neutro, errores **inline debajo del campo** y solo si el campo fue tocado (`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`).

## Contrato de datos / recurso HTTP

**Rutas nuevas / modificadas**

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/registro` | Pública; con sesión activa redirige a su módulo | Formulario de registro del Postulante |
| `/login` | Pública; con sesión activa redirige a su módulo | Formulario de inicio de sesión |
| `/_authenticated/*` | Requiere sesión y usuario activo | Rutas privadas (hoy `/panel`) |

**Formulario de registro** (`registroSchema`): `nombres`, `apellidos`, `ci`, `registroUniversitario`, `carreraCodigo` (select con `GET /carreras`), `telefono`, `correo`, `contrasena` (de 6 a 72 caracteres, sin más exigencias), `confirmarContrasena` (igual a `contrasena`). Todos obligatorios. Al backend se envían todos, con la confirmación como `confirmacionContrasena`.

**Formulario de login** (`loginSchema`): `correo` (formato de correo) y `contrasena` (no vacía).

**Respuesta de login/registro/refresh** (`Sesion`): trae `accessToken`, `refreshToken`, `tokenType` y `expiresIn` (son para mobile: la web los **ignora**) y `usuario` (`codigo`, `nombres`, `apellidos`, `correo`, `rol`), que es lo único que usa la feature.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-FE-01](./UT-CU01-FE-01.md) | Cliente HTTP con cookies de sesión | — |
| [UT-CU01-FE-02](./UT-CU01-FE-02.md) | Schemas de Zod de registro y login | — |
| [UT-CU01-FE-03](./UT-CU01-FE-03.md) | Hooks y llamadas a la API de cuentas (carreras, registro, login, refresh, logout, usuario actual) | 01 |
| [UT-CU01-FE-04](./UT-CU01-FE-04.md) | Sesión conectada a la API (renovación ante 401 y estado de sesión) | 01, 03 |
| [UT-CU01-FE-05](./UT-CU01-FE-05.md) | Acciones de registro, inicio y cierre de sesión con mapeo de errores | 02, 03, 04 |
| [UT-CU01-FE-06](./UT-CU01-FE-06.md) | Pantalla de registro (/registro) | 02, 03, 05 |
| [UT-CU01-FE-07](./UT-CU01-FE-07.md) | Pantalla de inicio de sesión (/login) | 02, 05 |
| [UT-CU01-FE-08](./UT-CU01-FE-08.md) | Guard de rutas privadas, redirecciones y destino por rol | 04, 05 |
| [UT-CU01-FE-09](./UT-CU01-FE-09.md) | Cerrar sesión desde el header y usuario real en el layout | 05, 08 |

**Cadena:** `01 → 03 → 04`; `02, 03, 04 → 05`; `02, 03, 05 → 06`; `02, 05 → 07`; `04, 05 → 08`; `05, 08 → 09`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A, pasos 1 a 6 (registro) | Formulario, validación, creación de la cuenta, sesión y redirección | 02, 03, 05, 06 |
| Flujo B, pasos 7 a 12 (inicio de sesión) | Formulario, login y redirección por rol | 03, 05, 07, 08 |
| Flujo C (cierre de sesión) | Opción «Cerrar sesión», pantalla de acceso | 05, 09 |
| A1, A2 | Conflictos y errores del formulario marcados en el campo | 02, 05, 06 |
| A3, A4, A6 | Mensajes de credenciales, cuenta desactivada y bloqueo | 05, 07 |
| A7 | Doble envío del registro sin duplicar | 05, 06 |
| A8, A10 | Sesión inválida o ya cerrada → pantalla de acceso | 04, 08 |
| A9 | Error de conexión con mensaje y reintento | 05, 06, 07 |
| CA01, CA06, CA12 | Registro correcto, siempre Postulante, sin duplicar | 05, 06 |
| CA02, CA03, CA21 | Conflictos marcados en CI, correo y registro universitario | 05, 06 |
| CA04, CA05 | Campos obligatorios, contraseña de al menos 6 caracteres y confirmación | 02, 06 |
| CA07, CA08, CA09 | Login por rol, credenciales incorrectas, cuenta desactivada | 05, 07, 08 |
| CA10, CA11 | Bloqueo temporal: se muestra el mensaje del backend con los minutos que faltan | 05, 07 |
| CA13 | Enlace «¿Olvidó su contraseña?» | — (CU15) |
| CA14 | Mismo comportamiento en web y móvil | 05–09 (ver UT-MOBILE) |
| CA15, CA18 | Sesión inválida o cerrada no deja ver pantallas privadas | 04, 08 |
| CA16 | El personal no tiene opción de crear cuenta: «Crear cuenta» solo existe como registro de Postulante | 06 |
| CA17, CA19 | Cierre de sesión solo de este dispositivo | 05, 09 |
| CA20 | Sin contraseñas ni tokens en consola ni en logs | 01, 03, 05 |

## Follow-ups (no se implementan en estas UT)

- Enlace y pantalla de recuperar contraseña: CU15.
- Pantallas reales de cada rol (postulación, auditoría, dashboard, asistencia) y su layout: CU02, CU05, CU12, CU10 y los demás.
- Verificación del correo (la documentación no la pide por ahora).
- Si el frontend pasa a otro dominio (otro *site*), replantear `SameSite=Strict` y la defensa CSRF.

## Qué NO incluyen estas UT

- Cualquier regla de contraseña, bloqueo o expiración propia del cliente: son del backend.
- Pantallas de gestión de usuarios (CU14) y de perfil (CU15).
- Mobile (ver `UT-MOBILE`) y backend (ver `UT-BACKEND`).
- Recordar la dirección a la que se quería entrar antes de iniciar sesión.
