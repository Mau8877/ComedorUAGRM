# Unidades de trabajo — CU01-FE

**Caso de uso:** Pantallas web de crear cuenta e iniciar sesión con Firebase Auth, conexión de la sesión de Firebase con la API, guard de rutas privadas y cierre de sesión.
**Fuente CU:** [`CU01 - Registrarse y autenticarse`](../../../CasosDeUso/CU01%20-%20Registrarse%20y%20autenticarse.md)
**Diagramas de grado:** `images/038_CU01_Registrarse_Autenticarse.png` (contexto) y `images/051_Analisis_CU01_Registrarse_Autenticarse.png` (análisis). No hay diagrama de secuencia ni mockup de este CU en la documentación de grado.
**Recurso HTTP / contrato:** Rutas `/registro` y `/login`; consume `GET /carreras`, `POST /auth/registro`, `POST /auth/sesion` y `GET /auth/usuario-actual` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (la feature nueva es `features/auth/`).

- **La sesión es de Firebase, no de un store.** El estado de sesión sale de `onAuthStateChanged` del SDK de Firebase; Zustand sigue siendo solo estado de UI. Los datos del usuario (nombre, rol) vienen del backend con TanStack Query (`GET /auth/usuario-actual`), no se copian a un store (`ESTADO_GLOBAL_FRONTEND.md`).
- **Flujo de registro en el cliente:** (1) validar el formulario con Zod; (2) crear la cuenta en Firebase con correo y contraseña; (3) `POST /auth/registro` con el ID token; (4) llevar al usuario a su módulo. Si Firebase responde que el correo ya existe, se intenta iniciar sesión con **el mismo correo y contraseña**: si funciona, se continúa con el paso 3 (recupera una cuenta de Firebase que quedó sin su registro en el sistema; el backend es idempotente) y si no, se informa "correo ya registrado" (A1). Si el backend responde `409`, se marca el campo en conflicto (CI, correo o registro universitario) y **no** se cierra la sesión de Firebase, para poder reintentar sin crear otra cuenta.
- **Flujo de inicio de sesión:** (1) validar; (2) iniciar sesión en Firebase; (3) `POST /auth/sesion` (una sola vez, registra el acceso); (4) llevar al módulo del rol. Si el backend responde `403` `ERR_AUTH_03` (cuenta desactivada) o `401` `ERR_AUTH_02` (sin cuenta en el sistema), se cierra la sesión de Firebase y se muestra el mensaje.
- **Errores de Firebase → mensajes** (la política de contraseña de 6 caracteres y el bloqueo por intentos son de Firebase; en el cliente solo se traducen): `auth/invalid-credential`, `auth/wrong-password`, `auth/user-not-found` → "Correo o contraseña incorrectos" (siempre el mismo, A3); `auth/too-many-requests` → "Demasiados intentos fallidos. Intenta de nuevo más tarde." (A6); `auth/user-disabled` → cuenta desactivada, comunicarse con la DUBSS (A4); `auth/network-request-failed` o cualquier error de red → error de conexión con opción de reintentar (A9).
- **Mientras no existan los módulos de cada rol, todos aterrizan en `/panel`** (el placeholder de `_authenticated/`). `rutaInicioPorRol(rol)` es el único lugar a actualizar cuando los CU de postulación, auditoría, dashboard y asistencia creen sus pantallas. Los roles `DIRECTIVO` y `PERSONAL_COMEDOR` no tienen layout todavía: ven una pantalla "tu módulo aún no está disponible" con el botón de cerrar sesión.
- **El enlace «¿Olvidó su contraseña?»** (A5, CA13) lo agrega el CU15, que crea su ruta; en este CU la pantalla de login no lo muestra todavía.
- **Variables de entorno nuevas:** `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_APP_ID`. Hace falta el proyecto de Firebase (prerrequisito manual, ver `UT-BACKEND`).
- Textos en español neutro, errores **inline debajo del campo** y solo si el campo fue tocado (`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`).

## Contrato de datos / recurso HTTP

**Rutas nuevas / modificadas**

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/registro` | Pública; con sesión activa redirige a su módulo | Formulario de registro del Postulante |
| `/login` | Pública; con sesión activa redirige a su módulo | Formulario de inicio de sesión |
| `/_authenticated/*` | Requiere sesión de Firebase y usuario activo | Rutas privadas (hoy `/panel`) |

**Formulario de registro** (`registroSchema`): `nombres`, `apellidos`, `ci`, `registroUniversitario`, `carreraCodigo` (select con `GET /carreras`), `telefono`, `correo`, `contrasena` (mínimo 6 caracteres, sin más exigencias), `confirmarContrasena` (igual a `contrasena`). Todos obligatorios. Al backend se envía todo **menos** `correo`, `contrasena` y `confirmarContrasena`.

**Formulario de login** (`loginSchema`): `correo` (formato de correo) y `contrasena` (no vacía).

**Respuesta del backend que consume la feature** (`UsuarioResponse`): `codigo`, `nombres`, `apellidos`, `correo`, `rol`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-FE-01](./UT-CU01-FE-01.md) | SDK de Firebase, configuración y variables de entorno | — |
| [UT-CU01-FE-02](./UT-CU01-FE-02.md) | Schemas de Zod de registro y login | 01 |
| [UT-CU01-FE-03](./UT-CU01-FE-03.md) | Hooks y llamadas a la API de cuentas (carreras, registro, sesión, usuario actual) | 01 |
| [UT-CU01-FE-04](./UT-CU01-FE-04.md) | Sesión de Firebase conectada a la API (token, 401 y estado de sesión) | 01, 03 |
| [UT-CU01-FE-05](./UT-CU01-FE-05.md) | Acciones de registro, inicio y cierre de sesión con mapeo de errores | 02, 03, 04 |
| [UT-CU01-FE-06](./UT-CU01-FE-06.md) | Pantalla de registro (/registro) | 02, 03, 05 |
| [UT-CU01-FE-07](./UT-CU01-FE-07.md) | Pantalla de inicio de sesión (/login) | 02, 05 |
| [UT-CU01-FE-08](./UT-CU01-FE-08.md) | Guard de rutas privadas, redirecciones y destino por rol | 04, 05 |
| [UT-CU01-FE-09](./UT-CU01-FE-09.md) | Cerrar sesión desde el header y usuario real en el layout | 05, 08 |

**Cadena:** `01 → 02, 03`; `01, 03 → 04`; `02, 03, 04 → 05`; `02, 03, 05 → 06`; `02, 05 → 07`; `04, 05 → 08`; `05, 08 → 09`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A, pasos 1 a 6 (registro) | Formulario, validación, creación de la cuenta y redirección | 02, 03, 05, 06 |
| Flujo B, pasos 7 a 12 (inicio de sesión) | Formulario, login, anotación del acceso y redirección por rol | 03, 05, 07, 08 |
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
| CA10, CA11 | Bloqueo temporal: se muestra el aviso de Firebase (el comportamiento exacto lo define Firebase) | 05, 07 |
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
- Mostrar el tiempo restante de bloqueo si Firebase lo permite.

## Qué NO incluyen estas UT

- Cualquier regla de contraseña, bloqueo o expiración propia: es de Firebase.
- Pantallas de gestión de usuarios (CU14) y de perfil (CU15).
- Mobile (ver `UT-MOBILE`) y backend (ver `UT-BACKEND`).
- Recordar la dirección a la que se quería entrar antes de iniciar sesión.
