# UT-CU26-BE-04 — Reprocesamiento en lote y su avance

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-04` |
| **Nombre** | Reprocesamiento en lote y su avance |
| **Caso de uso** | CU26-BACKEND |
| **Depende de** | UT-CU26-BE-01, UT-CU26-BE-02, UT-CU26-BE-03 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md),
[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

Recalcular los puntajes de una convocatoria con la versión vigente sin bloquear al administrador, viendo el avance y pudiendo reintentar lo que falló.

## Objetivo

Los endpoints de resumen, arranque y avance del reprocesamiento y su ejecución en segundo plano.

## Alcance

- DTO: `ResumenReprocesamientoResponse`, `LoteReprocesamientoResponse`.
- `ReprocesamientoService`: `resumen()`, `reprocesar()` (`@Auditar` `REPROCESAMIENTO_PUNTAJES`; bloqueos `ERR_MOD_04`, `ERR_MOD_02`, `ERR_MOD_03`, `ERR_MOD_06`; publica `LoteReprocesamientoCreado`) y `lote()` (`ERR_MOD_05`).
- `ReprocesamientoEjecucionService` (`@Async` + `@TransactionalEventListener`, ejecutor `ejecutorPrecalificacion`): recorre las pendientes del lote, `recalcular()` por postulación, `sumarAvance()`, `COMPLETADO` o `FALLIDO` si la IA no está disponible.
- `ReprocesamientoArranque`: lotes en curso huérfanos → `FALLIDO` al arrancar.
- `ReprocesamientoController`: `GET /convocatorias/{codigo}/resumen-reprocesamiento`, `POST /convocatorias/{codigo}/reprocesar` (`202`), `GET /lotes-reprocesamiento/{codigo}`.
- `BitacoraAcciones.REPROCESAMIENTO_PUNTAJES`.
- Pruebas: `ReprocesamientoServiceTest`, `ReprocesamientoEjecucionServiceTest`.

Archivos esperados (crear/tocar): los de la lista, `BitacoraAcciones.java`.

## Fuera de alcance (no tocar)

- Cancelar un lote en curso, programar reprocesamientos o reprocesar una sola postulación.
- Cambiar el estado de las postulaciones o la lista priorizada (CU06): solo cambian los puntajes vigentes.
- Notificar a los postulantes.

## Resultado esperado

`POST .../reprocesar` responde `202` con el lote `PENDIENTE`; poco después `GET /lotes-reprocesamiento/{codigo}` muestra `procesadas` creciendo y termina `COMPLETADO` (o `FALLIDO`).

## Validación

1. `./mvnw -o -q test -Dtest='ReprocesamientoServiceTest,ReprocesamientoEjecucionServiceTest'` en verde.
2. Con el stack levantado, una convocatoria con postulaciones `calificada`, una versión guardada y la IA simulada: `POST .../reprocesar` → `202`; un segundo `POST` inmediato → `409` `ERR_MOD_03`; el lote termina y un nuevo `POST` → `409` `ERR_MOD_06`.
3. Con resultados publicados → `409` `ERR_MOD_04`. Con la IA apagada el lote termina `FALLIDO` y los puntajes ya recalculados se conservan. La bitácora tiene `REPROCESAMIENTO_PUNTAJES`.
