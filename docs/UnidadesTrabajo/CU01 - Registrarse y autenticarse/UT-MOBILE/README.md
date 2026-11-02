# Unidades de trabajo — CU01-MOB

**Caso de uso:** Pantallas de la app móvil para crear cuenta e iniciar sesión con Firebase Auth, conexión de la sesión con la API, redirección por rol y cierre de sesión.
**Fuente CU:** [`CU01 - Registrarse y autenticarse`](../../../CasosDeUso/CU01%20-%20Registrarse%20y%20autenticarse.md)
**Diagramas de grado:** `images/038_CU01_Registrarse_Autenticarse.png` (contexto) y `images/051_Analisis_CU01_Registrarse_Autenticarse.png` (análisis). No hay diagrama de secuencia ni mockup de este CU en la documentación de grado.
**Recurso HTTP / contrato:** Rutas `/login`, `/registro` y `/sin-modulo` en `go_router`; consume `GET /carreras`, `POST /auth/registro`, `POST /auth/sesion` y `GET /auth/usuario-actual` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `FORMULARIOS_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/mobile/lib/` (la feature es `features/auth/`). Esta capa reproduce, con las herramientas de Flutter, lo mismo que [`UT-FRONTEND`](../UT-FRONTEND/README.md): la misma regla de negocio, los mismos mensajes y el mismo flujo (CA14).

- **La sesión es de Firebase.** El estado de sesión sale de `FirebaseAuth.instance.authStateChanges()`; los datos del usuario (nombre, rol) vienen del backend (`GET /auth/usuario-actual`). No se guarda ningún token a mano (ni en `flutter_secure_storage`): lo persiste el SDK.
- **Flujo de registro:** validar el formulario → crear la cuenta en Firebase → `POST /auth/registro` con el ID token → ir al módulo. Si Firebase dice que el correo ya existe, se prueba iniciar sesión con el mismo correo y contraseña: si funciona se continúa con el registro (recupera una cuenta de Firebase que quedó sin su registro en el sistema); si no, "correo ya registrado" (A1). Un `409` del backend marca el campo en conflicto y **no** cierra la sesión de Firebase.
- **Flujo de inicio de sesión:** validar → iniciar sesión en Firebase → `POST /auth/sesion` (una sola vez, anota el acceso) → ir al módulo del rol. Ante `403` `ERR_AUTH_03` (desactivada) o `401` `ERR_AUTH_02` (sin cuenta en el sistema) se cierra la sesión de Firebase y se muestra el mensaje.
- **Mensajes de error** (la política de 6 caracteres y el bloqueo son de Firebase; acá solo se traducen): `invalid-credential`, `wrong-password`, `user-not-found` → "Correo o contraseña incorrectos" (siempre el mismo, A3); `too-many-requests` → "Demasiados intentos fallidos. Intenta de nuevo más tarde." (A6); `user-disabled` → cuenta desactivada, comunicarse con la DUBSS (A4); `network-request-failed` o error de red → mensaje de conexión con reintento (A9).
- **Configuración de Firebase desde `.env`**, sin `google-services.json` ni `GoogleService-Info.plist`: `FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_MESSAGING_SENDER_ID` y `FIREBASE_PROJECT_ID` arman el `FirebaseOptions`. Así se mantiene el mismo mecanismo que `API_BASE_URL` y no se commitean archivos de la consola de Firebase. Prioridad Android; iOS se verifica después.
- **Convención de modelos (DTOs Dart) que faltaba definir:** una carpeta `models/` por feature con clases inmutables y `fromJson`/`toJson` escritos a mano (sin generación de código). Se escribe en `CONSUMO_API_MOBILE.md` en `UT-CU01-MOB-02`.
- **Destino por rol:** `POSTULANTE` → `/estudiante` (`EstudianteShell`); `ADMINISTRADOR_DUBSS` → `/admin` (`AdminShell`); `DIRECTIVO` y `PERSONAL_COMEDOR` no tienen shell todavía → `/sin-modulo` ("Tu módulo aún no está disponible", con botón de cerrar sesión). Cuando los CU de asistencia (CU10) y dashboard (CU12) tengan sus pantallas se actualiza el mapa en un solo lugar.
- **Las rutas de demostración** (`/ingredientes`, `/diagnostico`, `/`) pasan a ser privadas junto con el resto; `/` redirige al módulo del rol o al login.
- **El enlace «¿Olvidó su contraseña?»** (A5, CA13) lo agrega el CU15; en este CU la pantalla de login no lo muestra.
- Los tests siguen el criterio de `TESTING_MOBILE.md`: se prueba la lógica (mapeo de errores, providers, servicio, redirect); no hay widget tests obligatorios para cada pantalla.

## Contrato de datos / recurso HTTP

**Rutas** (todas en el `GoRouter` único de `core/routing/app_router.dart`):

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/login` | Pública; con sesión redirige al módulo | Inicio de sesión |
| `/registro` | Pública; con sesión redirige al módulo | Registro del Postulante |
| `/estudiante`, `/admin` | Privada | Shells existentes por rol |
| `/sin-modulo` | Privada | Aviso para los roles sin módulo todavía |

**Formulario de registro:** `nombres`, `apellidos`, `ci`, `registroUniversitario`, `carreraCodigo` (selector con `GET /carreras?pageSize=200`), `telefono`, `correo`, `contrasena` (al menos 6 caracteres, sin más exigencias) y `confirmarContrasena` (igual a `contrasena`). Todos obligatorios. Al backend se envía todo **menos** `correo`, `contrasena` y su confirmación.

**Formulario de login:** `correo` (formato válido) y `contrasena` (no vacía).

**Respuesta del backend que consume la feature** (`UsuarioResponse`): `codigo`, `nombres`, `apellidos`, `correo`, `rol`.

**Variables nuevas en `.env`:** `FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_MESSAGING_SENDER_ID`, `FIREBASE_PROJECT_ID`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-MOB-01](./UT-CU01-MOB-01.md) | Dependencias de Firebase y su inicialización | — |
| [UT-CU01-MOB-02](./UT-CU01-MOB-02.md) | Modelos de la feature y convención de DTOs | — |
| [UT-CU01-MOB-03](./UT-CU01-MOB-03.md) | ApiClient: token de Firebase en cada petición y manejo de 401 | 01 |
| [UT-CU01-MOB-04](./UT-CU01-MOB-04.md) | AuthService: llamadas a la API de cuentas | 02, 03 |
| [UT-CU01-MOB-05](./UT-CU01-MOB-05.md) | Providers de sesión y de usuario actual | 01, 03, 04 |
| [UT-CU01-MOB-06](./UT-CU01-MOB-06.md) | Acciones de registro, inicio y cierre de sesión con mapeo de errores | 01, 04, 05 |
| [UT-CU01-MOB-07](./UT-CU01-MOB-07.md) | Pantalla de registro | 02, 05, 06 |
| [UT-CU01-MOB-08](./UT-CU01-MOB-08.md) | Pantalla de inicio de sesión | 05, 06 |
| [UT-CU01-MOB-09](./UT-CU01-MOB-09.md) | Router: guard de sesión, rutas de acceso y destino por rol | 05, 06, 07, 08 |
| [UT-CU01-MOB-10](./UT-CU01-MOB-10.md) | Cerrar sesión desde los shells | 06, 09 |

**Cadena:** `01 → 03`; `02, 03 → 04`; `01, 03, 04 → 05`; `01, 04, 05 → 06`; `02, 05, 06 → 07`; `05, 06 → 08`; `05, 06, 07, 08 → 09`; `06, 09 → 10`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A, pasos 1 a 6 (registro) | Formulario, validación, creación de la cuenta y redirección | 02, 04, 06, 07 |
| Flujo B, pasos 7 a 12 (inicio de sesión) | Formulario, login, anotación del acceso y redirección por rol | 04, 06, 08, 09 |
| Flujo C (cierre de sesión) | Opción «Cerrar sesión» y pantalla de acceso | 06, 10 |
| A1, A2 | Conflictos y errores del formulario marcados en el campo | 06, 07 |
| A3, A4, A6 | Mensajes de credenciales, cuenta desactivada y bloqueo | 06, 08 |
| A7 | Doble envío del registro sin duplicar | 06, 07 |
| A8, A10 | Sesión inválida o ya cerrada → pantalla de acceso | 03, 09 |
| A9 | Error de conexión con mensaje y reintento | 06, 07, 08 |
| CA01, CA06, CA12 | Registro correcto, siempre Postulante, sin duplicar | 06, 07 |
| CA02, CA03, CA21 | Conflictos marcados en CI, correo y registro universitario | 06, 07 |
| CA04, CA05 | Campos obligatorios, contraseña de al menos 6 caracteres y confirmación | 07 |
| CA07, CA08, CA09 | Login por rol, credenciales incorrectas, cuenta desactivada | 06, 08, 09 |
| CA10, CA11 | Bloqueo temporal: se muestra el aviso de Firebase | 06, 08 |
| CA13 | Enlace «¿Olvidó su contraseña?» | — (CU15) |
| CA14 | Mismo comportamiento en web y móvil | 06–10 (ver UT-FRONTEND) |
| CA15, CA18 | Sesión inválida o cerrada no deja ver pantallas privadas | 03, 09 |
| CA16 | El personal no tiene opción de crear cuenta | 07 |
| CA17, CA19 | Cierre de sesión solo de este dispositivo | 06, 10 |
| CA20 | Sin contraseñas ni tokens en logs | 01, 03, 06 |

## Follow-ups (no se implementan en estas UT)

- Enlace y pantalla de recuperar contraseña: CU15.
- Pantallas reales de cada rol y los shells de `DIRECTIVO` y `PERSONAL_COMEDOR`: CU12, CU10 y demás.
- Verificar en iOS (la prioridad es Android).
- Verificación del correo (la documentación no la pide por ahora).

## Qué NO incluyen estas UT

- Cualquier regla de contraseña, bloqueo o expiración propia: es de Firebase.
- Gestión de usuarios (CU14) y de perfil (CU15).
- Web (ver `UT-FRONTEND`) y backend (ver `UT-BACKEND`).
- Notificaciones push (FCM) y biometría.
