# Errores — Padrón de becados (`BEC`)

Errores del padrón de becados: alta de los adjudicados, listado, baja, suspensión y reemplazo (CU23).

## `ERR_BEC_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La beca indicada no existe o fue eliminada.
- **Dónde se lanza:** `BecaEstadoService.darDeBaja()`, `BecaEstadoService.suspender()`,
  `BecaEstadoService.candidatoDeReemplazo()` y `BecaEstadoService.reemplazar()`

## `ERR_BEC_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La convocatoria indicada no existe o fue eliminada.
- **Dónde se lanza:** `PadronService.resumen()`, `PadronService.listar()` y `PadronService.darDeAlta()`

## `ERR_BEC_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Los resultados de la convocatoria todavía no fueron publicados (CU22), así que no hay padrón
  que mostrar ni adjudicados a quienes dar de alta.
- **Dónde se lanza:** `PadronService.listar()` y `PadronService.darDeAlta()`

## `ERR_BEC_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** No hay adjudicados pendientes de alta: todos los adjudicados de la convocatoria ya tienen su
  beca. Repetir el alta no duplica nada.
- **Dónde se lanza:** `PadronService.darDeAlta()`

## `ERR_BEC_05`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La beca no está vigente (está suspendida o dada de baja), así que no se puede dar de baja ni
  suspender.
- **Dónde se lanza:** `BecaEstadoService.darDeBaja()` y `BecaEstadoService.suspender()`

## `ERR_BEC_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La beca no fue dada de baja, así que no se puede reemplazar su cupo.
- **Dónde se lanza:** `BecaEstadoService.candidatoDeReemplazo()` y `BecaEstadoService.reemplazar()`

## `ERR_BEC_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** No hay un postulante disponible para ocupar el cupo: ningún finalista con última auditoría
  «Validado», puntaje total igual o mayor al mínimo, sin decisión y sin beca en la convocatoria.
- **Dónde se lanza:** `BecaEstadoService.candidatoDeReemplazo()` y `BecaEstadoService.reemplazar()`

## `ERR_BEC_08`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El postulante que el Administrador confirmó ya no es el siguiente disponible (otra operación
  ocupó su lugar entre la consulta y la confirmación). Hay que volver a consultar el candidato.
- **Dónde se lanza:** `BecaEstadoService.reemplazar()`

## `ERR_BEC_09`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La beca ya fue reemplazada: un cupo liberado se reemplaza una sola vez.
- **Dónde se lanza:** `BecaEstadoService.candidatoDeReemplazo()` y `BecaEstadoService.reemplazar()`

## `ERR_BEC_10`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El campo de `sort` no está soportado por el listado del padrón (solo `becado`, `ci`, `carrera`,
  `estado` y `fechaInicio`).
- **Dónde se lanza:** `PadronService.listar()`
