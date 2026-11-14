# Unidades de trabajo — CU15-FE

**Caso de uso:** Pantallas web para recuperar la contraseña (pedir el enlace y definir la contraseña nueva desde el enlace del correo) y para «Mi perfil» (ver los datos, actualizar correo y teléfono, y cambiar la contraseña), para cualquier rol con layout.
**Fuente CU:** [`CU15 - Recuperar contraseña y gestionar perfil`](../../../CasosDeUso/CU15%20-%20Recuperar%20contrase%C3%B1a%20y%20gestionar%20perfil.md)
**Diagramas de grado:** los del CU15 en la documentación de grado (análisis, secuencia y mockup 140). La carpeta no estaba disponible al redactar estas fichas; el mockup 140 muestra nombres, apellidos y carrera como editables, pero **gana el CU** (solo correo y teléfono). Manda el sistema de diseño ya hecho del panel.
**Recurso HTTP / contrato:** Rutas `/recuperar-contrasena`, `/restablecer-contrasena?token=…` (públicas) y `/perfil` (con sesión); consume `POST /auth/recuperar-contrasena`, `POST /auth/restablecer-contrasena`, `POST /auth/cambiar-contrasena`, `GET /perfil` y `PATCH /perfil` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Dónde vive cada cosa.** La recuperación de contraseña (pantallas públicas, mutations y schemas) va en la feature existente `features/auth/`, junto al login; el perfil es una feature nueva, `features/perfil/`, con la forma de [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature). El «cambiar contraseña» llama a `/auth/cambiar-contrasena` pero se muestra en el perfil, así que su hook vive en `features/perfil/api/`.
- **Sin tokens en el cliente** (regla del proyecto). La sesión sigue en cookies `HttpOnly`; el cambio de contraseña no manda el refresh token (el navegador manda la cookie a `/api/v1/auth`) y el backend conserva esta sesión y cierra las demás. Las llamadas públicas (`recuperar`, `restablecer`) **no** disparan la renovación ante un `401` (son públicas; el interceptor ya las excluye por ser de `/auth`; se confirma con un test).
- **El token del enlace** llega en la URL (`?token=…`). La pantalla lo lee con el `validateSearch` de TanStack Router, lo guarda en el estado del formulario y **lo quita de la barra de direcciones** (`replace`), para que no quede en el historial ni se filtre por el `Referer`. No se guarda en `localStorage`, en un store ni en la caché de TanStack Query; la mutación no deja el token en caché, y nada lo escribe en la consola.
- **Mensaje neutro.** Tras pedir el enlace, la pantalla muestra siempre el mismo mensaje, sin importar la respuesta (el backend ya responde igual). Un error de red sí se informa, con reintento (A10).
- **Enlace inválido.** `ERR_AUTH_08` se muestra como «El enlace ya no sirve» con un botón que lleva a pedir uno nuevo (A2). Tras restablecer, se lleva al login con un aviso de éxito y se limpia la caché de TanStack Query (el backend cerró todas las sesiones).
- **Schemas de Zod** con las mismas reglas que el backend (contraseña de 6 a 72, coincidir con la confirmación, correo con formato); el servidor vuelve a validar. Los errores van **inline** y solo si el campo fue tocado (`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`).
- **Perfil.** Una sola pantalla (`PerfilScreen`) dentro del layout del rol (`AdminLayout` o `EstudianteLayout`; los roles sin layout —Directivo y Personal del Comedor— no tienen perfil en la web hasta que existan sus módulos: la ruta los lleva a `/panel`). Muestra tres tarjetas: **Mis datos** (solo lectura: nombres, apellidos, CI, rol, estado y, si es Postulante, registro universitario y carrera), **Datos de contacto** (correo y, si es Postulante, teléfono; editables) y **Cambiar contraseña**. El CI se ve bloqueado, no es un campo editable (A4).
- **Correo con reautenticación.** Si el correo cambia, el formulario muestra el campo «Contraseña actual» (obligatorio) y solo entonces envía `contrasenaActual`. `ERR_AUTH_09` se marca en ese campo; `ERR_US_02` (`409`) en el campo correo; el teléfono de un no Postulante ni se muestra. Un perfil actualizado invalida `['auth','usuario-actual']` y `['perfil','detail']` (el nombre del header no cambia, pero el correo sí).
- **Acceso desde el header.** `AppHeader` ya trae los ítems «Mi perfil» y «Cambiar contraseña» sin acción; se les da una (`onPerfil`, `onCambiarContrasena`, o un único `perfilHref`), y las dos entradas llevan a `/perfil` (la segunda hace foco en la tarjeta de contraseña). Los layouts reciben el handler igual que `onCerrarSesion`. Se retira el ítem «Perfil» del `estudianteNavItems` si queda duplicado, o se apunta a `/perfil`.
- **Errores del backend → mensajes:** `ERR_SYS_01` y `ERR_AUTH_08`/`ERR_AUTH_09`/`ERR_US_13` muestran el mensaje del backend; `ERR_US_02` marca el correo; error de red o `5xx` → mensaje de conexión con reintento (A10); un `401` lo resuelve el flujo de sesión existente (renovar o `/login`, A8).
- **Textos** en español neutro. **Sin variables de entorno nuevas.**

## Contrato de datos / recurso HTTP

**Rutas nuevas**

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/recuperar-contrasena` | Pública; con sesión redirige al módulo | Formulario con el correo y el mensaje neutro |
| `/restablecer-contrasena` | Pública (con `?token=`); con sesión redirige al módulo | Formulario de contraseña nueva y confirmación, o el aviso de enlace inválido |
| `/perfil` | Con sesión (`Administrador DUBSS` y `Postulante`) | Mis datos, datos de contacto y cambiar contraseña |

**Formularios (Zod):**

| Schema | Campos |
|--------|--------|
| `recuperarContrasenaSchema` | `correo` (obligatorio, hasta 150, formato) |
| `restablecerContrasenaSchema` | `contrasena` (6 a 72), `confirmarContrasena` (igual); el `token` no es un campo del formulario |
| `actualizarPerfilSchema` | `correo` (obligatorio), `telefono` (solo Postulante: obligatorio, hasta 20), `contrasenaActual` (obligatoria **solo** si el correo cambió) |
| `cambiarContrasenaSchema` | `contrasenaActual`, `contrasenaNueva` (6 a 72), `confirmarContrasena` (igual a la nueva) |

**Respuestas:** `Perfil` (`codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo`, `postulante`: `null` o `{ registroUniversitario, carrera: { codigo, nombre, facultad }, telefono }`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU15-FE-01](./UT-CU15-FE-01.md) | Schemas de Zod y tipos del CU | — |
| [UT-CU15-FE-02](./UT-CU15-FE-02.md) | Llamadas a la API de recuperación y de perfil, hooks y mapeo de errores | 01 |
| [UT-CU15-FE-03](./UT-CU15-FE-03.md) | Pantalla de solicitud de recuperación y enlace en el login | 01, 02 |
| [UT-CU15-FE-04](./UT-CU15-FE-04.md) | Pantalla de restablecimiento desde el enlace | 01, 02 |
| [UT-CU15-FE-05](./UT-CU15-FE-05.md) | Ruta `/perfil`, consulta del perfil y acceso desde el header | 02 |
| [UT-CU15-FE-06](./UT-CU15-FE-06.md) | Actualizar los datos de contacto | 01, 02, 05 |
| [UT-CU15-FE-07](./UT-CU15-FE-07.md) | Cambiar la contraseña desde el perfil | 01, 02, 05 |

**Cadena:** `01 → 02`; `01, 02 → 03, 04`; `02 → 05`; `01, 02, 05 → 06, 07`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (pasos 1 a 5) | «¿Olvidó su contraseña?», formulario del correo y mensaje neutro | 01, 02, 03 |
| Flujo B (pasos 6 a 11) | Enlace del correo, contraseña nueva y regreso al login | 01, 02, 04 |
| Flujo C (pasos 12 y 13) | «Mi perfil» con el CI solo de lectura | 02, 05 |
| Flujo D (pasos 14 a 18) | Correo y teléfono, con la contraseña actual al cambiar el correo | 01, 02, 06 |
| Flujo E (pasos 19 a 22) | Cambio de contraseña desde el perfil | 01, 02, 07 |
| A2 | Enlace vencido o usado: aviso y nueva solicitud | 02, 04 |
| A3 | Contraseña fuera de la política o sin coincidir | 01, 04, 07 |
| A4 | CI bloqueado | 05 |
| A5 | Correo ya usado, marcado en el campo | 02, 06 |
| A6, A7 | Contraseña actual incorrecta; el correo nuevo la exige | 01, 02, 06, 07 |
| A8 | Sesión inválida → login | 02 (flujo existente) |
| A9, A10 | Mensaje neutro ante fallo de correo; error de conexión con reintento | 02, 03, 04, 06, 07 |
| CA01, CA02, CA18 | Mismo mensaje neutro en cualquier caso | 03 |
| CA03–CA06 | Restablecer, vencido, usado, reemplazado | 04 |
| CA07 | Requisitos de la contraseña visibles y `400` mostrado | 01, 04, 07 |
| CA10, CA11 | Perfil con CI bloqueado | 05 |
| CA12–CA15 | Teléfono, correo, conflicto y contraseña actual incorrecta | 06 |
| CA16 | Cambio de contraseña conservando esta sesión | 07 |
| CA17 | Sin sesión: login | 02 (flujo existente) |
| CA20 | Mismo comportamiento que en el móvil | 03–07 (ver UT-MOBILE) |
| CA21 | Enlace «¿Olvidó su contraseña?» en el login | 03 |

## Follow-ups (no se implementan en estas UT)

- Perfil para `DIRECTIVO` y `PERSONAL_COMEDOR` en la web (cuando existan sus layouts).
- Medidor de fuerza de contraseña o más reglas de política (la política es la del CU01).
- Verificar el correo nuevo con un enlace (por definir con la DUBSS).

## Qué NO incluyen estas UT

- Mobile (ver [`UT-MOBILE`](../UT-MOBILE/README.md)) y backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Editar nombres, apellidos, registro universitario o carrera.
- Forzar el cambio de la contraseña temporal en el primer acceso.
- La consulta de la bitácora (CU25).
