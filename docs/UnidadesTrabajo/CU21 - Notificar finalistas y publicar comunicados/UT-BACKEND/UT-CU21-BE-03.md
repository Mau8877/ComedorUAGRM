# UT-CU21-BE-03 — Códigos de error, acciones de bitácora, DTOs y eventos de notificación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-03` |
| **Nombre** | Códigos de error, acciones de bitácora, DTOs y eventos de notificación |
| **Caso de uso** | CU21-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- Como referencia: `PriorizacionErrorCodes`, `EventoNotificacion` y `NotificacionService` (CU06 y CU-EXTRA-01).

## Solicitud de negocio

Cada rechazo tiene un código reconocible, las tres acciones quedan en la bitácora y los dos avisos nuevos tienen su plantilla.

## Objetivo

Quedan definidos y documentados los códigos, las acciones, los DTOs y los eventos, y el servicio de notificaciones puede ligar un aviso a su postulación o comunicado y enviar a muchos usuarios.

## Alcance

- `FinalistaErrorCodes` (`B/features/finalistas/exception/`): `ERR_FIN_01` (404, finalista no existe), `ERR_FIN_02` (404, convocatoria no existe), `ERR_FIN_03` (400, fecha de recepción futura), `ERR_FIN_04` (400, plazo no posterior a hoy), `ERR_FIN_05` (400, `filter`/`sort` no soportado).
- `ComunicadoErrorCodes` (`B/features/comunicados/exception/`): `ERR_COM_01` (400, destinatarios no soportados o sin destinatarios), `ERR_COM_02` (400, `FINALISTAS` sin convocatoria).
- `NotificacionErrorCodes` (`B/features/notificaciones/exception/`): `ERR_NOT_01` (404, aviso inexistente o ajeno), `ERR_NOT_02` (400, `sort` no soportado).
- `docs/errors/finalistas/`, `comunicados/` y `notificaciones/` (`ERRORES_*.md` en el formato de la guía) y sus links en `docs/errors/README.md`.
- `BitacoraAcciones`: `NOTIFICACION_FINALISTAS`, `ENTREGA_DOCUMENTACION_REGISTRADA`, `COMUNICADO_PUBLICADO`.
- `EventoNotificacion`: `FINALISTA_NOTIFICADO` (INFO, push + correo; `{nombres}`, `{plazo}`, `{lugar}`, `{documentos}`) y `COMUNICADO` (INFO, solo bandeja y push; `{titulo}`, `{contenido}`); el enum gana `enviaCorreo()` y `DespachoNotificaciones` no manda correo si es `false`.
- `NotificacionService`: `notificar(...)` devuelve `boolean` (si se registró) y tiene una variante con `postulacionId`/`comunicadoId`; nuevo `notificarUsuarios(ids, evento, datos, comunicadoId)` que guarda los avisos por lotes (solo eventos sin correo) y deja el despacho al asíncrono de siempre.
- DTOs (`record`): `FinalistaResponse` (`codigo`, `posicion`, `nombres`, `apellidos`, `ci`, `puntaje`, `estadoNotificacion`, `plazoEntrega`, `notificadoEn`, `documentacionRecibidaEn`; las fechas de día como `LocalDate`), `NotificarFinalistasRequest`, `ResultadoNotificacionResponse` (`solicitados`, `enviados`, `fallidos`), `RegistrarEntregaRequest`, `PublicarComunicadoRequest`, `ComunicadoResponse` (con `totalDestinatarios`), `NotificacionResponse` y `ResumenNotificacionesResponse`. Nunca llevan `id` internos.
- Tests: plantillas de los dos eventos (marcadores, título ≤ 150), `enviaCorreo`, `notificar` con contexto, `notificarUsuarios` y Bean Validation de los requests.

Archivos esperados (crear): los tres `*ErrorCodes`, los DTOs, `docs/errors/*` y los tests. (Tocar): `BitacoraAcciones`, `EventoNotificacion`, `NotificacionService`, `DespachoNotificaciones`, `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Lanzar las excepciones y usar las acciones (UT-05 a 09).
- El mecanismo de envío (push, correo, reintentos) de CU-EXTRA-01.

## Resultado esperado

Códigos, acciones, DTOs y eventos existen y están documentados; el envío de avisos existente sigue igual.

## Validación

1. `./mvnw test` pasa (incluidos los tests de CU-EXTRA-01 sin cambios de comportamiento).
2. `./mvnw checkstyle:check` sin errores.
3. Cada código está en `docs/errors/` y en el índice.
