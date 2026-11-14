# Errores — Finalistas (`FIN`)

Errores de los finalistas (CU21): listado, notificación y registro de la entrega de la documentación física.

## `ERR_FIN_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El finalista (o alguno de los seleccionados para notificar) no existe o está eliminado lógicamente.
- **Dónde se lanza:** `FinalistaService.notificar()` y `FinalistaService.registrarEntrega()`

## `ERR_FIN_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La convocatoria pedida (`filter[convocatoriaId]` del listado o `convocatoriaId` de un comunicado) no existe o está eliminada.
- **Dónde se lanza:** `FinalistaService.listar()` y `ComunicadoService.publicar()`

## `ERR_FIN_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La fecha de recepción de la documentación es futura.
- **Dónde se lanza:** `FinalistaService.registrarEntrega()`

## `ERR_FIN_04`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El plazo de entrega no es posterior a hoy (día de Bolivia).
- **Dónde se lanza:** `FinalistaService.notificar()`

## `ERR_FIN_05`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Se mandó un `sort` o un `filter[...]` que el listado de finalistas no soporta (solo `filter[convocatoriaId]`; el orden es fijo por posición).
- **Dónde se lanza:** `FinalistaController.validarParametros()`

