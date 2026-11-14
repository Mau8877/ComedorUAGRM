# UT-CUX01-MOB-03 — Registro y baja del teléfono según la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-MOB-03` |
| **Nombre** | Registro y baja del teléfono según la sesión |
| **Caso de uso** | CU-EXTRA-01-MOB |
| **Depende de** | UT-CUX01-MOB-02 |

## Antes de implementar

- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)
- `M/lib/features/auth/providers/auth_sesion_provider.dart` y `auth_provider.dart` (la sesión y el cierre de sesión)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

El teléfono debe empezar a recibir los avisos de quien inicia sesión y dejar de recibirlos cuando esa persona cierra sesión.

## Objetivo

El token se registra solo cuando hay sesión y se da de baja antes de cerrarla.

## Alcance

- `M/lib/features/notificaciones/providers/push_registro_provider.dart`: notifier que escucha `authSesionProvider`; cuando pasa a haber usuario, si hay permiso (`pedirPermiso`) obtiene el token y llama a `registrar`; escucha `tokenRenovado` y vuelve a registrar mientras haya sesión. Guarda en memoria el último token registrado. Sin permiso o sin push, no hace nada y no vuelve a pedirlo (A1). Cualquier error de red se traga y se reintenta en el próximo inicio de sesión o renovación.
- Se activa desde `M/lib/main.dart` (`ref.watch` del provider en `MyApp`).
- `M/lib/features/auth/providers/auth_provider.dart`: en `cerrarSesion`, **antes** de `authSesionProvider.notifier.cerrar()`, llama a `darDeBaja` del token registrado dentro del mismo `try/catch` que ya ignora fallos (A8); un error no impide cerrar sesión.
- Tests (`M/test/features/notificaciones/`): con `PushService` y `DispositivosService` falsos: registra al abrir sesión, no registra sin sesión ni sin permiso, vuelve a registrar al renovarse el token, da de baja al cerrar sesión y cierra igual si la baja falla.

Archivos esperados (crear): `push_registro_provider.dart` y sus tests. (Tocar): `main.dart`, `auth_provider.dart`.

## Fuera de alcance (no tocar)

- Mostrar avisos recibidos (`UT-CUX01-MOB-04`).
- Cambiar el flujo de login o de restauración de sesión más allá de lo anterior.

## Resultado esperado

Iniciar sesión en el teléfono deja el token registrado en el backend; cerrar sesión lo elimina.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Con backend y Firebase reales: tras iniciar sesión, la base tiene una fila en `dispositivo_fcm`; tras cerrar sesión queda eliminada (verificar por SQL o con `BE-12`).
