# UT-CU21-BE-08 — Publicar comunicados y listarlos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-08` |
| **Nombre** | Publicar comunicados y listarlos |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-02`, `UT-CU21-BE-03` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

La DUBSS informa a todos los postulantes, o solo a los finalistas, con un comunicado.

## Objetivo

`ComunicadoService.publicar` guarda el comunicado, deja un aviso en la bandeja de cada destinatario (con push) y lo anota en la bitácora; `listar` devuelve los ya publicados.

## Alcance

- `ComunicadoService.publicar(actorCodigo, PublicarComunicadoRequest)` con `@PreAuthorize`, `@Transactional` y `@Auditar(accion = COMUNICADO_PUBLICADO, entidad = "comunicado")`.
- Valida título (≤ 150), contenido y destinatarios (`ERR_SYS_01`); `ADJUDICADOS` o un grupo sin destinatarios → `400` `ERR_COM_01` sin publicar; `FINALISTAS` sin convocatoria → `ERR_COM_02`; convocatoria inexistente → `404` `ERR_FIN_02`.
- Destinatarios (consulta JDBC en el repositorio del módulo): `TODOS` = cuentas activas de Postulante, y si hay convocatoria solo quienes tienen una postulación enviada (no `borrador`) en ella; `FINALISTAS` = los finalistas de la convocatoria.
- Guarda el comunicado y llama a `NotificacionService.notificarUsuarios(..., COMUNICADO, ..., comunicadoId)`: los avisos se guardan por lotes, sin correo, y el despacho es el asíncrono de CU-EXTRA-01, sin bloquear la respuesta (CA20). Devuelve el `ComunicadoResponse` con `totalDestinatarios`.
- `listar(page, pageSize)`: comunicados publicados, el más reciente primero, con `totalDestinatarios` = avisos del comunicado.
- Bitácora: código del comunicado, destinatarios, convocatoria y cantidad; sin el contenido. Un comunicado publicado no se edita ni se borra.
- Tests unitarios: cada tipo de destinatario, los rechazos y que se anota una sola vez.

Archivos esperados (crear): `ComunicadoService`, su consulta de destinatarios y sus tests.

## Fuera de alcance (no tocar)

- Editar o borrar comunicados, borrador y publicación programada.
- `ADJUDICADOS` (follow-up).
- Endpoint (UT-10).

## Resultado esperado

Un comunicado publicado queda en la bandeja de cada destinatario y en la bitácora.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
