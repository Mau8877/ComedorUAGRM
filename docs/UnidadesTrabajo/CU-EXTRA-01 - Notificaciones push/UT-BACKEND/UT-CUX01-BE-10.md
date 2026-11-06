# UT-CUX01-BE-10 — Servicio de notificaciones: guardar y despachar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-10` |
| **Nombre** | Servicio de notificaciones: guardar y despachar |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-02, UT-CUX01-BE-04, UT-CUX01-BE-05, UT-CUX01-BE-07, UT-CUX01-BE-08, UT-CUX01-BE-09 |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (un módulo de negocio llamará a este `service`, nunca a su repository)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cualquier módulo del sistema debe poder avisar a un usuario con una sola llamada, sin preocuparse de Firebase, del correo ni de qué pasa si fallan.

## Objetivo

`NotificacionService.notificar(...)` guarda el aviso y, terminada la transacción de quien lo pidió, lo despacha por push y correo con reintentos.

## Alcance

- `B/features/notificaciones/service/NotificacionService.java`: `notificar(Usuario destinatario, EventoNotificacion evento, Map<String,String> datos)`:
  1. Renderiza la plantilla (`PlantillaNotificacion`); si es incompleta, registra ERROR y no guarda ni envía nada (A10).
  2. Guarda la fila de `notificacion` en la transacción de quien llama (`@Transactional`, `REQUIRED`).
  3. Publica un evento de Spring con lo necesario (sin entidades): id de la notificación, id del usuario, correo, título, mensaje y evento.
- `B/features/notificaciones/service/DespachoNotificaciones.java`: `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` + `@Async("ejecutorNotificaciones")`:
  - Push: obtiene los tokens del usuario (`DispositivoFcmService.tokensDe`); sin tokens, lo omite (A3). Envía con `CanalPush` dentro de `Reintentos`, reintentando solo los `tokensPasajeros`; los `tokensInvalidos` se eliminan con `DispositivoFcmService.eliminarInvalidos` (A4). `data` del push: `codigoNotificacion` y `evento`.
  - Correo: `CorreoNotificacionService.enviar` con el título como asunto.
  - Al terminar, marca `enviada_en` (en su propia transacción corta) **si al menos una vía se entregó**.
  - Cualquier error se captura y se registra; nada se propaga a quien notificó.
- Logging INFO por aviso despachado (`userId`, evento, cuántos teléfonos, correo sí/no) y WARN por vías fallidas; nunca tokens, correo ni mensaje.
- Tests: `NotificacionServiceTest` y `DespachoNotificacionesTest` con todo simulado (dos teléfonos, sin teléfonos, token inválido, fallo pasajero que se recupera, push agotado y correo bien, plantilla incompleta, `enviada_en` según las vías).

Archivos esperados (crear): `NotificacionService.java`, `DespachoNotificaciones.java`, el record del evento interno y sus tests.

## Fuera de alcance (no tocar)

- Eventos de negocio reales y los módulos que llaman a `notificar`.
- Leer, marcar como leída o eliminar avisos (endpoints de la bandeja).
- Envío masivo por lotes a muchos usuarios.

## Resultado esperado

Llamar a `notificar` desde cualquier `service` deja una fila y, tras el commit, envía el push y el correo sin bloquear ni romper nada.

## Validación

1. `./mvnw test` pasa.
2. Con un rollback de la transacción llamadora, no queda fila ni se envía nada (cubierto en `BE-12`).
3. `./mvnw checkstyle:check` sin errores.
