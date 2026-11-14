# UT-CU05-BE-07 — Adjudicar o no adjudicar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-07` |
| **Nombre** | Adjudicar o no adjudicar |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-02`, `UT-CU05-BE-03`, `UT-CU05-BE-04` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- Como referencia: `CierrePostulacionService` (CU-20) y `DevolucionService` (CU-19), y cómo `ExpedienteService` calcula el puntaje total (IA + académico de la última auditoría).

## Solicitud de negocio

La DUBSS toma la decisión final sobre cada finalista auditado, dentro de los cupos y del puntaje mínimo.

## Objetivo

`AdjudicacionService.adjudicar(...)` y `noAdjudicar(...)` validan, registran la adjudicación, cambian el estado, guardan el historial y la bitácora, todo en una operación.

## Alcance

- `AdjudicacionService` (el de UT-05) gana dos métodos, `adjudicar(UUID actorCodigo, UUID finalistaCodigo)` y `noAdjudicar(UUID actorCodigo, UUID finalistaCodigo, NoAdjudicarRequest request)`, cada uno `@Transactional`, con su `@Auditar` (`ADJUDICACION_BECA` / `NO_ADJUDICACION_BECA`, entidad `adjudicacion`) y que devuelve un `ResultadoAuditado<DecisionAdjudicacionResponse>`. La lógica común va en un método privado parametrizado por el resultado (no se duplica).
- Orden de validaciones:
  1. finalista inexistente → `404` `ERR_FIN_01`;
  2. se bloquea la **convocatoria** y después la **postulación** (siempre en ese orden);
  3. convocatoria no `CERRADA`, o sin cupos o sin puntaje mínimo → `409` `ERR_ADJ_05`;
  4. postulación que ya no está «calificada» → `409` `ERR_ADJ_01` (cubre doble clic, reintento y dos administradores a la vez; el mensaje trae el estado actual); resultados publicados → `409` `ERR_ADJ_01`;
  5. el finalista **no tiene ninguna auditoría** → `409` `ERR_ADJ_02` (para las dos decisiones);
  6. **solo al adjudicar:** la última auditoría no es «Validado» → `409` `ERR_ADJ_02` (el mensaje indica auditar de nuevo o usar «No adjudicar»);
  7. **solo al adjudicar:** puntaje total (IA vigente + académico de la última auditoría) menor al mínimo → `409` `ERR_ADJ_03`, con el puntaje y el mínimo en el mensaje; si falta el puntaje de la IA vigente se trata igual, indicando «no disponible» *(por revisar, ver README)*;
  8. **solo al adjudicar:** cupos disponibles ≤ 0 → `409` `ERR_ADJ_04`.
- «No adjudicar» **no** exige validado, puntaje ni cupos (decisión 9); sí exige motivo (el DTO lo valida; el `service` lo recorta de espacios sobrantes).
- Si todo es válido: `Postulacion.adjudicar()` o `noAdjudicar()`; una fila `Adjudicacion` (resultado, `puntajeFinal` = total calculado al decidir, o `null` si no se puede calcular, `decididoPor` = administrador del token, `fechaAdjudicacion` = ahora); una fila `PostulacionHistorial` (estado anterior «calificada», nuevo «adjudicada» o «no adjudicada», el administrador como `usuarioId`, y `motivo` solo al no adjudicar); y la respuesta con los cupos disponibles ya actualizados. Una sola transacción (A9). **No** hay idempotencia por clave.
- **No** se avisa al postulante: este `service` no llama a `NotificacionService` (decisión 3).
- **No toca** documentos, respuestas, puntaje de la IA, auditorías ni la fila de `finalista`.
- Bitácora: códigos de la adjudicación, de la postulación, del finalista y de la convocatoria, `resultado`, `estadoAnterior` y `estadoNuevo`; **sin** motivo, nombres, CI, rutas ni tokens. `LOG.info` con módulo y actor.
- Tests unitarios: cada validación con su código y sin cambios; adjudicar feliz (estado, adjudicación con puntaje final, historial sin motivo, cupos); no adjudicar feliz con auditoría «Validado», «Observado» y «No válido» (historial con motivo); no adjudicar sin auditoría (`ERR_ADJ_02`); no adjudicar con puntaje bajo o sin cupos (permitido); adjudicar con última auditoría «Observado» (`ERR_ADJ_02`), con puntaje bajo (`ERR_ADJ_03`) y con cupos agotados (`ERR_ADJ_04`); puntaje igual al mínimo (permitido); dos decisiones seguidas (la segunda `ERR_ADJ_01`); dos adjudicaciones con un solo cupo, simuladas en serie (la segunda `ERR_ADJ_04`); postulación ya devuelta, archivada, rechazada, adjudicada o no adjudicada (`ERR_ADJ_01` con el estado actual); resultados publicados simulados; puntaje final que no cambia si después cambia el puntaje de la IA; que el detalle de la bitácora no lleva el motivo; que no se llama a `NotificacionService`.

Archivos esperados (tocar): `AdjudicacionService` y su test.

## Fuera de alcance (no tocar)

- Seleccionar finalistas (UT-06) y el panel (UT-05, solo se reutilizan sus cuentas).
- Archivar y rechazar (CU-20) y devolver (CU-19).
- Acta (CU-08), publicar resultados y avisar (CU-22) y padrón (CU-23).
- Reabrir o modificar una decisión.
- Endpoints (UT-08).

## Resultado esperado

La postulación queda «adjudicada» o «no adjudicada» con su adjudicación, historial y bitácora; una repetición recibe `409` y los cupos no se superan.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
