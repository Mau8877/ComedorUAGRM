# UT-CUX01-BE-07 — Reintentos y ejecutor de notificaciones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-07` |
| **Nombre** | Reintentos y ejecutor de notificaciones |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- `B/config/CorreoConfig.java` (ejecutor de correo existente, que se deja como está)

## Solicitud de negocio

Un fallo pasajero de Firebase o del correo no debería perder el aviso, pero tampoco debe demorar ni romper la operación de negocio que lo originó.

## Objetivo

Un helper de reintentos con espera creciente y un ejecutor asíncrono propio para los envíos de notificaciones.

## Alcance

- `B/features/notificaciones/service/Reintentos.java`: `ejecutar(Supplier<T> accion, Predicate<Exception> reintentable)` con intentos y esperas tomados de `app.notificaciones.reintentos` (3) y `app.notificaciones.esperas-ms` (`2000,8000`); sin dependencias nuevas. Devuelve el resultado o relanza el último error; las esperas se pueden sustituir en los tests por 0.
- `B/config/NotificacionesConfig.java`: ejecutor `ejecutorNotificaciones` (4 hilos, cola de 500, prefijo `notif-`), con `CallerRunsPolicy` **no**: si la cola se llena, el envío se descarta y se registra un WARN, para no frenar la operación de negocio.
- `application.properties`: las propiedades anteriores con valores por defecto.
- Tests: `ReintentosTest` (éxito al primer intento, éxito al tercero, agotamiento, error no reintentable corta de inmediato, cantidad de intentos y esperas).

Archivos esperados (crear): `Reintentos.java`, `NotificacionesConfig.java` y `ReintentosTest.java`. (Tocar): `application.properties`.

## Fuera de alcance (no tocar)

- Reintentos persistentes que sobrevivan a un reinicio (follow-up).
- Usar el helper en el correo y el push (`UT-CUX01-BE-08` y `BE-10`).

## Resultado esperado

Hay un mecanismo único y probado de reintento y un ejecutor aparte para los envíos.

## Validación

1. `./mvnw test` pasa y `ReintentosTest` no tarda segundos (esperas en 0).
2. `./mvnw checkstyle:check` sin errores.
