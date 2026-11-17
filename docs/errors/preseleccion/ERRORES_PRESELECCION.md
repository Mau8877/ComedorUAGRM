# Errores — Preselección (`MOD`)

Errores de la parametrización del modelo de preselección y del reprocesamiento de puntajes (CU26). Una convocatoria
inexistente reutiliza [`ERR_CV_01`](../convocatorias/ERRORES_CONVOCATORIAS.md#err_cv_01).

## `ERR_MOD_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Los parámetros del modelo no son válidos: falta una de las diez variables (las seis del mockup y las cuatro del formulario oficial de la DUBSS), hay una repetida o
  desconocida, una ponderación es nula, está fuera de 0–100 o tiene más de 2 decimales, o las ponderaciones no suman
  100. El mensaje lista todo lo que falla.
- **Dónde se lanza:** `ModeloPreseleccionService.validar()` (llamado desde `guardar()`)

## `ERR_MOD_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** No hay una versión vigente del modelo: todavía no se guardó ningún parámetro, así que no hay con qué
  reprocesar.
- **Dónde se lanza:** `ReprocesamientoService.reprocesar()`

## `ERR_MOD_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya hay un reprocesamiento en curso (`PENDIENTE` o `EN_PROCESO`) para esa convocatoria. También
  responde cuando dos pedidos llegan a la vez y el segundo viola el índice único parcial
  `uq_lote_reprocesamiento_en_curso`.
- **Dónde se lanza:** `ReprocesamientoService.reprocesar()`

## `ERR_MOD_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Los resultados de la convocatoria ya fueron publicados (CU22): no se reprocesan sus puntajes.
- **Dónde se lanza:** `ReprocesamientoService.reprocesar()`

## `ERR_MOD_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El lote de reprocesamiento consultado no existe (o fue eliminado).
- **Dónde se lanza:** `ReprocesamientoService.lote()`

## `ERR_MOD_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** No hay postulaciones `calificada` en la convocatoria sin puntaje vigente de la versión vigente: no
  hay nada por recalcular (todas ya se calcularon con esa versión, o no hay calificadas).
- **Dónde se lanza:** `ReprocesamientoService.reprocesar()`

## `ERR_MOD_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Dos guardados de parámetros llegaron a la vez y el segundo violó el índice único de la versión (o
  del modelo activo): otra versión se guardó primero y hay que volver a abrir los parámetros.
- **Dónde se lanza:** `ModeloPreseleccionService.guardar()`

## `ERR_MOD_08`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** Se pidió la precalificación (CU03) de una postulación que no existe o está eliminada.
- **Dónde se lanza:** `PrecalificacionDetalleService.obtener()`
