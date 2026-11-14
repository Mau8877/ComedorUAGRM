# UT-CUX01-MOB-02 — Servicio de push y de dispositivos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-MOB-02` |
| **Nombre** | Servicio de push y de dispositivos |
| **Caso de uso** | CU-EXTRA-01-MOB |
| **Depende de** | UT-CUX01-MOB-01 |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (`services/`, el `ApiClient` compartido)
- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (convención de nombres de providers)
- `M/lib/features/auth/services/auth_service.dart` (referencia de estilo)

## Solicitud de negocio

La app necesita saber cuál es el token de este teléfono y poder avisárselo al backend, sin ensuciar el resto del código con Firebase.

## Objetivo

Dos servicios: uno que envuelve a Firebase Messaging y otro que habla con los endpoints de dispositivos.

## Alcance

- `M/lib/features/notificaciones/services/push_service.dart`: interfaz `PushService` y su implementación `firebase_push_service.dart` (`FirebasePushService`, la única que importa `firebase_messaging`; usa `getToken`/`onTokenRefresh` aunque figuren deprecados en v17, porque su reemplazo entrega el Firebase Installation ID y no se verificó que el Admin SDK lo acepte) con `Future<bool> pedirPermiso()` (Android 13+). **No guarda una marca propia de «ya se pidió»** (decisión al implementar): Android ya limita cuántas veces muestra el diálogo y después responde «denegado» sin molestar, `Future<String?> obtenerToken()`, `Stream<String> get tokenRenovado` y `Stream<MensajePush> get mensajes` (primer plano). Si Firebase no está disponible, todo devuelve «sin push» sin lanzar.
- `M/lib/features/notificaciones/services/dispositivos_service.dart`: `DispositivosService(Dio)` con `registrar(String token)` → `POST /dispositivos-fcm` (`plataforma: 'ANDROID'`) y `darDeBaja(String token)` → `POST /dispositivos-fcm/baja`.
- Modelo mínimo `MensajePush` (`titulo`, `cuerpo`, `codigoNotificacion`, `evento`).
- Providers de acceso en `M/lib/features/notificaciones/providers/`: `pushServiceProvider` y `dispositivosServiceProvider` (sustituibles en tests con `overrideWithValue`).
- Ninguno de los dos escribe el token en logs ni lo muestra en pantalla.

Archivos esperados (crear): los 2 servicios, el modelo y los 2 providers de acceso.

## Fuera de alcance (no tocar)

- Cuándo registrar y dar de baja (`UT-CUX01-MOB-03`).
- Mostrar avisos (`UT-CUX01-MOB-04`).

## Resultado esperado

Con servicios sustituibles, la lógica de las UT siguientes puede probarse sin Firebase ni red.

## Validación

1. `flutter analyze` sin avisos.
2. Test de `DispositivosService` con el adaptador falso de `test/support/fake_adapter.dart` (cuerpo, ruta y respuesta correctos).
