# UT-CUX01-MOB-04 — Recepción del aviso: primer plano, segundo plano y canal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-MOB-04` |
| **Nombre** | Recepción del aviso: primer plano, segundo plano y canal |
| **Caso de uso** | CU-EXTRA-01-MOB |
| **Depende de** | UT-CUX01-MOB-01, UT-CUX01-MOB-02 |

## Antes de implementar

- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (tema y colores de marca)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)

## Solicitud de negocio

La persona tiene que ver el aviso aunque tenga la app abierta, en segundo plano o cerrada, y que tocarlo la lleve a la app.

## Objetivo

La app muestra los avisos en primer plano y deja que Android los muestre en segundo plano, con un canal propio de notificaciones.

## Alcance

- `M/android/app/src/main/AndroidManifest.xml`: permiso `android.permission.POST_NOTIFICATIONS` y `meta-data` `com.google.firebase.messaging.default_notification_channel_id` = `notificaciones` (el mismo canal que usa el backend).
- Crear el canal Android `notificaciones` (importancia alta) en código nativo, en `MainActivity.kt` (`onCreate`), sin sumar `flutter_local_notifications` solo para esto (decisión al implementar).
- Primer plano: escuchar `PushService.mensajes` y mostrar el título y el cuerpo con un `SnackBar` (o `MaterialBanner`) mediante un `GlobalKey<ScaffoldMessengerState>` expuesto como provider y pasado a `MaterialApp.router(scaffoldMessengerKey: ...)` en `main.dart`; los colores salen de `Theme`, sin hex sueltos. No cambia la pantalla actual.
- Segundo plano / app cerrada: no requiere código propio (los mensajes `notification` los pinta Android); confirmar que el handler de mensajes en segundo plano de Firebase no hace falta para este caso y no se registra uno.
- Tocar el aviso abre la app; no se navega a ninguna pantalla (no hay bandeja todavía).
- Tests: el provider del aviso en primer plano con `PushService` falso (un mensaje entrante produce el aviso con el texto esperado; sin mensaje no hay nada).

Archivos esperados (crear): `avisos_push_provider.dart`, `widgets/aviso_push.dart` y su test. (Tocar): `AndroidManifest.xml`, `MainActivity.kt`, `main.dart`.

## Fuera de alcance (no tocar)

- Pantalla de bandeja y navegación al tocar (follow-up).
- Notificaciones locales programadas y manejo de imágenes o acciones en el aviso.
- Ícono y color propios de la notificación.

## Resultado esperado

Con la app abierta se ve un aviso dentro de ella; con la app cerrada o en segundo plano, el sistema operativo lo muestra y al tocarlo se abre la app.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Con un push real (aviso de prueba del backend o la consola de Firebase): app abierta → aparece el aviso dentro de la app; app en segundo plano y cerrada → aparece en la bandeja del teléfono y abre la app al tocarlo.
