# UT-CU20-BE-04 — Archivar o rechazar y avisar al postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-04` |
| **Nombre** | Archivar o rechazar y avisar al postulante |
| **Caso de uso** | CU20-BE |
| **Depende de** | `UT-CU20-BE-01`, `UT-CU20-BE-02`, `UT-CU20-BE-03` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- Como referencia: `DevolucionService` (CU-19).

## Solicitud de negocio

La DUBSS cierra la postulación con su motivo y el postulante se entera de la decisión.

## Objetivo

`CierrePostulacionService` valida, cambia el estado, guarda el historial, anota la bitácora y avisa al postulante.

## Alcance

- `B/features/postulaciones/service/CierrePostulacionService.java` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` y `@Transactional`. Dos métodos, `archivar(UUID actorCodigo, UUID finalistaCodigo, CierrePostulacionRequest request)` y `rechazar(...)`, cada uno con su `@Auditar` (`ARCHIVO_POSTULACION` / `RECHAZO_POSTULACION`, entidad `postulacion_historial`) y que devuelve un `ResultadoAuditado<CierrePostulacionResponse>`. La lógica común va en un método privado parametrizado por la acción (no se duplica).
- Orden: finalista inexistente → `404` `ERR_FIN_01`; se bloquea la postulación (`bloquearPostulacion`); la postulación no está «calificada» → `409` `ERR_ARC_01` (cubre doble clic y dos administradores a la vez; el mensaje trae el estado actual); resultados publicados → `409` `ERR_ARC_01`.
- **No se exige** auditoría previa ni entrega física *(por revisar, preguntas 1 y 2)*.
- Si todo es válido: `Postulacion.archivar()` o `rechazar()`, una fila en `PostulacionHistorial` (estado anterior «calificada», nuevo «archivada» o «no adjudicada», el administrador del token como `usuarioId`, el motivo sin espacios sobrantes) y la respuesta. Una sola transacción: estado, historial y bitácora se guardan juntos o ninguno (A8). **No** hay idempotencia por clave.
- Aviso: `NotificacionService.notificar` con `POSTULACION_ARCHIVADA` o `POSTULACION_RECHAZADA` (`nombres` y `motivo`) y la postulación como contexto; un fallo del aviso, del push o del correo **no** deshace la decisión (solo `WARN`, A5).
- Bitácora: códigos del historial, de la postulación y del finalista, `estadoAnterior` y `estadoNuevo`; **sin** motivo, nombres, CI, rutas ni tokens. `LOG.info` con módulo y actor.
- **No toca** documentos, respuestas, puntaje, auditorías ni la fila de `finalista`.
- Tests unitarios: cada validación con su código y sin cambios, camino feliz de archivar y de rechazar (estado, historial, evento y datos del aviso), aviso fallido sin deshacer, dos decisiones seguidas (la segunda `ERR_ARC_01`), postulación ya devuelta/archivada/no adjudicada/adjudicada (`ERR_ARC_01` con el estado actual), resultados publicados simulados (`ERR_ARC_01`) y que el detalle de la bitácora no lleva el motivo.

Archivos esperados (crear): `CierrePostulacionService` y su test.

## Fuera de alcance (no tocar)

- Devolver (CU-19) y la decisión final de adjudicar o no adjudicar (CU-05).
- Publicar resultados (CU-22) y reabrir o apelar.
- Eliminar o modificar auditorías, documentos o puntaje.
- Endpoints (UT-06).

## Resultado esperado

La postulación queda «archivada» o «no adjudicada» con su historial, su bitácora y su aviso; una repetición recibe `409`.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
