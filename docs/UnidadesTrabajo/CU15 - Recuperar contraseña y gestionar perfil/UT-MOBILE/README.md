# Unidades de trabajo — CU15-MOB

**Caso de uso:** Pantallas de la app móvil para pedir el enlace de recuperación de contraseña y para «Mi perfil» (ver los datos, actualizar correo y teléfono y cambiar la contraseña), con el mismo comportamiento que la web.
**Fuente CU:** [`CU15 - Recuperar contraseña y gestionar perfil`](../../../CasosDeUso/CU15%20-%20Recuperar%20contrase%C3%B1a%20y%20gestionar%20perfil.md)
**Diagramas de grado:** los del CU15 en la documentación de grado (análisis, secuencia y mockup 140). La carpeta no estaba disponible al redactar estas fichas; el mockup 140 muestra nombres, apellidos y carrera como editables, pero **gana el CU** (solo correo y teléfono).
**Recurso HTTP / contrato:** Rutas `/recuperar-contrasena` (pública) y `/perfil` (privada) en `go_router`; consume `POST /auth/recuperar-contrasena`, `POST /auth/cambiar-contrasena`, `GET /perfil` y `PATCH /perfil` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `FORMULARIOS_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/mobile/lib/` (en estas fichas, `M/`). Esta capa reproduce, con las herramientas de Flutter, lo mismo que [`UT-FRONTEND`](../UT-FRONTEND/README.md): la misma regla de negocio, los mismos mensajes y el mismo flujo (CA20).

- **La app solo pide el enlace; el enlace se abre en la web.** El correo trae un enlace al portal web (decisión del CU), donde se define la contraseña nueva. La pantalla de la app, tras pedirlo, lo dice con claridad: «Abre el enlace desde tu correo; se abre en el navegador». No hay pantalla de restablecimiento en la app ni se registra un esquema de enlaces profundos (`deep links`); si más adelante se quiere abrirlo dentro de la app, es otro trabajo.
- **Feature nueva `features/perfil/`** con la forma de [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md#estructura-de-un-feature-libfeaturesfeature): `models/`, `providers/`, `screens/`, `services/` y `widgets/`. La solicitud de recuperación va en `features/auth/` (junto al login), igual que en el panel web. Los modelos siguen la convención de `CONSUMO_API_MOBILE.md` (clases inmutables con `fromJson`/`toJson` a mano, sin generación de código).
- **Sin tokens fuera de `TokenStorage`.** El token de acceso sigue en memoria y el refresh token en `flutter_secure_storage`. Al cambiar la contraseña, la app **manda el refresh token en el cuerpo** (`refreshToken`) para que el backend conserve esta sesión y cierre las demás; ese token se lee de `TokenStorage` justo en ese momento y no se guarda ni se muestra en otro lado. No se escribe nunca en consola ni en logs.
- **Mismos mensajes que la web.** Mensaje neutro tras pedir el enlace; `ERR_AUTH_09` («La contraseña actual es incorrecta») en el campo de la contraseña actual; `ERR_US_02` en el correo; `ERR_SYS_01`/`ERR_US_13` con el mensaje del backend; red o `5xx` → conexión con «Reintentar». La política de contraseña (6 a 72, coincide con la confirmación) y los mensajes reutilizan `auth_validators.dart` para que no diverjan.
- **Un `400` nunca cierra la sesión.** La contraseña actual incorrecta es `400` precisamente para que `ApiClient` no intente renovar ni mande al login. Un `401` real lo resuelve el flujo de sesión existente (renovar una vez o ir a `/login`, A8).
- **Perfil.** Una pantalla (`PerfilScreen`) con tres secciones: **Mis datos** (solo lectura; el CI bloqueado), **Datos de contacto** (correo y teléfono si es Postulante) y **Cambiar contraseña**. Disponible para **todos los roles** (la app, a diferencia de la web, ya tiene el menú de cuenta en todos los shells y en `/sin-modulo`). Se abre desde «Mi perfil» del menú de cuenta (`MenuUsuario`) y, para el Postulante, también desde la sección «Perfil» de `EstudianteShell`, que muestra el mismo contenido.
- **Correo con reautenticación.** Si el correo cambia, aparece el campo «Contraseña actual» (obligatorio); la regla es la del backend y la web.
- **Sin variables de entorno nuevas** (solo `API_BASE_URL`).
- Los tests siguen el criterio de `TESTING_MOBILE.md`: se prueba la lógica (modelos, servicios, mapeo de errores, providers, redirect); no hay widget tests obligatorios para cada pantalla.

## Contrato de datos / recurso HTTP

**Rutas** (todas en `core/routing/app_router.dart`):

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/recuperar-contrasena` | Pública; con sesión redirige al módulo | Formulario con el correo y el mensaje neutro |
| `/perfil` | Privada, todos los roles | Mis datos, datos de contacto y cambiar contraseña |

**Formularios:** recuperar (`correo` con formato válido); actualizar contacto (`correo`, `telefono` si es Postulante, hasta 20 caracteres, y `contrasenaActual` solo si el correo cambió); cambiar contraseña (`contrasenaActual`, `contrasenaNueva` de 6 a 72, `confirmacionContrasena` igual). Al backend se envía la confirmación como `confirmacionContrasena`.

**Respuesta de perfil** (`Perfil`): `codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo` y `postulante` (`null` o `registroUniversitario`, `carrera` {`codigo`, `nombre`, `facultad`}, `telefono`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU15-MOB-01](./UT-CU15-MOB-01.md) | Modelos y servicios del CU | — |
| [UT-CU15-MOB-02](./UT-CU15-MOB-02.md) | Providers del perfil y mapeo de errores | 01 |
| [UT-CU15-MOB-03](./UT-CU15-MOB-03.md) | Pantalla de solicitud de recuperación y enlace en el login | 01, 02 |
| [UT-CU15-MOB-04](./UT-CU15-MOB-04.md) | Pantalla «Mi perfil» y acceso desde el menú de cuenta | 02 |
| [UT-CU15-MOB-05](./UT-CU15-MOB-05.md) | Actualizar los datos de contacto | 02, 04 |
| [UT-CU15-MOB-06](./UT-CU15-MOB-06.md) | Cambiar la contraseña desde el perfil | 02, 04 |

**Cadena:** `01 → 02`; `01, 02 → 03`; `02 → 04 → 05, 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (pasos 1 a 5) | «¿Olvidó su contraseña?», correo y mensaje neutro | 01, 02, 03 |
| Flujo B (pasos 6 a 11) | Se hace en el portal web con el enlace del correo | — (ver UT-FRONTEND) |
| Flujo C (pasos 12 y 13) | «Mi perfil» con el CI solo de lectura | 01, 02, 04 |
| Flujo D (pasos 14 a 18) | Correo y teléfono, con la contraseña actual al cambiar el correo | 02, 05 |
| Flujo E (pasos 19 a 22) | Cambio de contraseña conservando esta sesión | 02, 06 |
| A3 | Contraseña fuera de la política o sin coincidir | 06 |
| A4 | CI bloqueado | 04 |
| A5 | Correo ya usado, marcado en el campo | 02, 05 |
| A6, A7 | Contraseña actual incorrecta; el correo nuevo la exige | 02, 05, 06 |
| A8 | Sesión inválida → login | 02 (flujo existente) |
| A9, A10 | Mensaje neutro ante fallo de correo; error de conexión con reintento | 02, 03, 05, 06 |
| CA01, CA02, CA18 | Mismo mensaje neutro en cualquier caso | 03 |
| CA07 | Requisitos visibles y `400` mostrado | 06 |
| CA10, CA11 | Perfil con CI bloqueado | 04 |
| CA12–CA15 | Teléfono, correo, conflicto y contraseña actual incorrecta | 05 |
| CA16 | Cambio de contraseña conservando esta sesión | 06 |
| CA17 | Sin sesión: login | 02 (flujo existente) |
| CA20 | Mismo comportamiento que en la web | 03–06 (ver UT-FRONTEND) |
| CA21 | Enlace «¿Olvidó su contraseña?» en el login | 03 |

## Follow-ups (no se implementan en estas UT)

- Abrir el enlace del correo dentro de la app (`deep links` / `app links`) y definir ahí la contraseña nueva.
- Verificar iOS (la prioridad es Android).
- Verificar el correo nuevo con un enlace (por definir con la DUBSS).

## Qué NO incluyen estas UT

- Restablecer la contraseña desde la app: se hace en la web con el enlace.
- Editar nombres, apellidos, registro universitario o carrera.
- Web (ver `UT-FRONTEND`) y backend (ver `UT-BACKEND`).
- Notificaciones push y biometría.
