# Unidades de trabajo — CU-EXTRA-01-MOB

**Caso de uso:** App móvil (Android) lista para recibir notificaciones push: integración con Firebase, permiso, registro y baja del token del teléfono ligados a la sesión, y recepción del aviso con la app abierta, en segundo plano o cerrada.
**Fuente CU:** [`CU-EXTRA-01 - Notificaciones push`](../../../CasosDeUso/CU-EXTRA-01%20-%20Notificaciones%20push.md)
**Diagramas de grado:** ninguno (no figura entre los 26 CU). No hay mockup: no hay pantalla nueva.
**Recurso HTTP / contrato:** consume `POST /api/v1/dispositivos-fcm` y `POST /api/v1/dispositivos-fcm/baja` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)). Estas UT necesitan esos endpoints (`UT-CUX01-BE-06`) funcionando para la prueba completa.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `M/` = `projects/mobile/`. Feature nueva: `M/lib/features/notificaciones/` (`services/`, `providers/`, `widgets/` solo si hace falta).

- **Solo Android.** El proyecto no tiene carpeta `ios/`; iOS necesita una cuenta de Apple Developer y queda fuera. `plataforma` se envía siempre como `ANDROID`.
- **Compilar sin credenciales de Firebase tiene que seguir siendo posible (CA16).** El plugin de Gradle de Google Services falla el build si falta `google-services.json`; por eso se aplica **solo si el archivo existe**, y `Firebase.initializeApp()` se envuelve en un `try/catch` que deja la app sin push en vez de tumbarla. Sin el archivo, la app funciona igual, solo que sin push.
- **El token se registra con la sesión abierta y se da de baja antes de cerrarla.** Al haber sesión (login, registro o sesión restaurada) se pide permiso, se obtiene el token y se hace `POST /dispositivos-fcm`; `onTokenRefresh` repite el registro. Al cerrar sesión, `AuthNotifier.cerrarSesion` llama a la baja **antes** de borrar los tokens de sesión (la baja necesita autenticación) y, como el resto del cierre (CU01 A10), un fallo de red no impide cerrar.
- **Pide permiso una sola vez por instalación.** Si el usuario lo rechaza, la app no vuelve a pedirlo en cada inicio; queda la opción de los ajustes del teléfono (CA05). El permiso de Android 13+ es `POST_NOTIFICATIONS`.
- **Segundo plano y app cerrada: lo muestra el sistema.** Los mensajes llegan como `notification` y Android los pinta en su bandeja; tocarlos abre la app. **Primer plano:** Firebase no pinta nada, así que la app muestra un aviso dentro de la propia app (un `SnackBar`/banner por un `ScaffoldMessenger` global) sin interrumpir la pantalla actual.
- **Tocar el aviso solo abre la app.** No hay todavía una pantalla de bandeja a la cual navegar; cuando exista, se enlaza con `data.codigoNotificacion`.
- **Capa delgada sobre Firebase.** Un `PushService` envuelve a `firebase_messaging` (permiso, token, renovación, mensajes entrantes) para poder sustituirlo por uno falso en los tests; nada fuera de él importa Firebase.
- **Dependencia entre features.** `features/auth` llama a `notificaciones` solo para la baja al cerrar sesión; es la relación mínima que lo justifica (el orden con la sesión importa).

## Contrato de datos / recurso HTTP

| Llamada | Cuerpo | Respuesta |
|---------|--------|-----------|
| `POST /dispositivos-fcm` | `{token, plataforma: "ANDROID"}` | `201`/`200` con `{codigo, plataforma}` (se ignora) |
| `POST /dispositivos-fcm/baja` | `{token}` | `200`, `data: null` |

El push trae `notification.title`, `notification.body` y `data.codigoNotificacion`, `data.evento`; canal Android `notificaciones`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CUX01-MOB-01](./UT-CUX01-MOB-01.md) | Firebase en la app Android | — |
| [UT-CUX01-MOB-02](./UT-CUX01-MOB-02.md) | Servicio de push y de dispositivos | 01 |
| [UT-CUX01-MOB-03](./UT-CUX01-MOB-03.md) | Registro y baja del teléfono según la sesión | 02 |
| [UT-CUX01-MOB-04](./UT-CUX01-MOB-04.md) | Recepción del aviso: primer plano, segundo plano y canal | 01, 02 |
| [UT-CUX01-MOB-05](./UT-CUX01-MOB-05.md) | Pruebas y verificación de punta a punta | 03, 04 |

**Cadena:** `01 → 02 → 03`; `01, 02 → 04`; `03, 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (pasos 1 a 5) | Permiso, token, registro y renovación | 01, 02, 03 |
| Flujo B (pasos 6 a 8) | Baja al cerrar sesión | 02, 03 |
| Flujo D (pasos 13 y 14) | Recepción y toque del aviso | 04 |
| A1 | Permiso rechazado | 02, 03 |
| A8 | Baja sin conexión o sin sesión | 03 |
| CA01, CA03, CA04 | Registro, renovación y baja | 02, 03, 05 |
| CA05 | Sin permiso la app funciona | 02, 03 |
| CA11, CA12 | Aviso en segundo plano y en primer plano | 04, 05 |
| CA16 | Compilar sin `google-services.json` | 01 |

## Follow-ups (no se implementan en estas UT)

- iOS (cuenta de Apple Developer, APNs y `GoogleService-Info.plist`).
- Pantalla de bandeja de notificaciones y navegación desde el toque del aviso.
- Ícono y color propios de la notificación (hoy el predeterminado de la app).
- Pedir el permiso en un momento más oportuno que justo al iniciar sesión.

## Qué NO incluyen estas UT

- Ninguna pantalla nueva ni la bandeja.
- Push en la web (otro proyecto).
- Eventos de negocio concretos.
