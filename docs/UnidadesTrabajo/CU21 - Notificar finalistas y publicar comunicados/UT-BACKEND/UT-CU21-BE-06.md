# UT-CU21-BE-06 — Notificar a los finalistas (con reenvío a los fallidos)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-06` |
| **Nombre** | Notificar a los finalistas (con reenvío a los fallidos) |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-02`, `UT-CU21-BE-03` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).
- Como referencia: `ListaPriorizadaPdfService.exportar` (`@Auditar` con `ResultadoAuditado`) y `NotificacionService`.

## Solicitud de negocio

El administrador avisa a los finalistas qué documentos entregar, dónde y hasta cuándo.

## Objetivo

`FinalistaService.notificar` guarda un aviso por finalista (con push y correo), los marca como notificados y lo anota en la bitácora.

## Alcance

- `FinalistaService.notificar(actorCodigo, NotificarFinalistasRequest)` con `@PreAuthorize` del administrador, `@Transactional` y `@Auditar(accion = NOTIFICACION_FINALISTAS, entidad = "finalista")`, devolviendo `ResultadoAuditado<ResultadoNotificacionResponse>`.
- Valida: `finalistas` (códigos, al menos uno), `plazoEntrega`, `lugarHorario` y `documentos` obligatorios (`ERR_SYS_01`); el plazo debe ser **posterior a hoy** en `America/La_Paz` (`ERR_FIN_04`); un código que no existe → `404` `ERR_FIN_01` y no se envía nada.
- Por cada finalista: `NotificacionService.notificar(usuario, FINALISTA_NOTIFICADO, datos, postulacionId, null)`, fija `plazo_entrega` y `notificado_en` **solo del finalista cuyo aviso se registró** (si no se pudo registrar, queda sin marcar) y cuenta `enviados` o `fallidos`. El resultado del push o del correo no se espera: lo despacha CU-EXTRA-01 (A1, A13). Reenviar a los fallidos es repetir la llamada solo con ellos (A2).
- Bitácora: cantidad y **códigos** de los finalistas, plazo, lugar/horario y documentos; nunca nombres, CI ni correos. Log `INFO` con la cantidad, sin datos personales.
- No cambia el estado de ninguna postulación.
- Tests unitarios (validaciones, plazo de hoy y de mañana, finalista inexistente, cuenta de fallidos, detalle de la bitácora sin datos personales).

Archivos esperados (tocar): `FinalistaService` y su test.

## Fuera de alcance (no tocar)

- El envío (push, correo, reintentos): CU-EXTRA-01.
- Elegir finalistas (CU-05) y devolver o archivar (CU-19, CU-20).
- Endpoint (UT-10).

## Resultado esperado

Tras notificar, cada finalista tiene su aviso en la bandeja y queda con plazo y `notificado_en`.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
