# UT-CU19-BE-05 — Devolver la postulación y avisar al postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-05` |
| **Nombre** | Devolver la postulación y avisar al postulante |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-02`, `UT-CU19-BE-03`, `UT-CU19-BE-04` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- Como referencia: `ExpedienteService.registrarAuditoria` y su método `avisar` (CU-07).

## Solicitud de negocio

La DUBSS devuelve la postulación con su motivo y el postulante se entera de qué corregir.

## Objetivo

`DevolucionService.devolver` valida, cambia el estado, guarda el historial, anota la bitácora y avisa al postulante.

## Alcance

- `B/features/postulaciones/service/DevolucionService.java` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, `@Transactional` y `@Auditar(accion = DEVOLUCION_POSTULACION, entidad = "postulacion_historial")`; `devolver(UUID actorCodigo, UUID finalistaCodigo, DevolverPostulacionRequest request)` devuelve un `ResultadoAuditado<DevolucionResponse>`.
- Orden: finalista inexistente → `404` `ERR_FIN_01`; se bloquea la postulación (`bloquearPostulacion`); la postulación no está «calificada» → `409` `ERR_DEV_01` (también cubre la segunda de dos peticiones simultáneas o repetidas por doble clic); sin auditoría posterior a la última devolución → `409` `ERR_DEV_02`.
- Si todo es válido: `Postulacion.devolver()`, guarda una fila en `PostulacionHistorial` (estado anterior «calificada», nuevo «devuelta», el administrador del token como `usuarioId`, el motivo ya sin espacios sobrantes) y devuelve la respuesta. Una sola transacción: si algo falla no queda nada a medias. **No** hay idempotencia por clave: una segunda petición simplemente recibe `ERR_DEV_01`.
- Aviso: `NotificacionService.notificar` con `POSTULACION_DEVUELTA` (`nombres` y `observaciones` = el motivo) y la postulación como contexto; un fallo del aviso, del push o del correo **no** deshace la devolución (solo `WARN`, como en el CU-07).
- Bitácora: código del historial, de la postulación y del finalista, `estadoAnterior` y `estadoNuevo`; **sin** motivo, nombres ni CI. `LOG.info` con módulo y actor.
- **No toca** documentos, respuestas, puntaje, auditorías ni la fila de `finalista`.
- Tests unitarios: cada validación con su código y que no cambia nada, el camino feliz (estado, historial, evento y datos del aviso), aviso fallido sin deshacer, dos devoluciones seguidas (la segunda `ERR_DEV_01`), devolver de nuevo tras reenvío y nueva auditoría (permitido, sin límite) y que el detalle de la bitácora no lleva el motivo.

Archivos esperados (crear): `DevolucionService` y su test.

## Fuera de alcance (no tocar)

- Archivar o rechazar (CU-20).
- El reenvío del postulante, `numero_envios` y recalcular el puntaje (CU-02, CU-03).
- Elegir de nuevo al finalista o pedir otra entrega física (CU-05).
- Endpoint (UT-07).
- Cualquier plazo de reenvío: no existe.

## Resultado esperado

La postulación queda «devuelta» con su historial, su bitácora y su aviso; una repetición recibe `409`.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
