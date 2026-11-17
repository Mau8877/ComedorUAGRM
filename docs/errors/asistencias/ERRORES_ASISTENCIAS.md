# Errores — Asistencia digital (`ASI`)

Errores del registro de asistencia del Personal del Comedor (CU10). Viven en el
módulo `asistencias`. Un CI vacío o demasiado largo cae en `ERR_SYS_01` (Bean
Validation) y un rol distinto de `PERSONAL_COMEDOR`, en `ERR_AUTH_07`.

## `ERR_ASI_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El CI buscado no corresponde a ningún becado (no tiene beca en el padrón).
- **Dónde se lanza:** `AsistenciaService.buscarBecado()`

## `ERR_ASI_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La beca está suspendida o dada de baja: no se puede registrar su asistencia.
- **Dónde se lanza:** `AsistenciaService.registrar()`

## `ERR_ASI_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La beca cuyo código llegó para registrar no existe o fue eliminada.
- **Dónde se lanza:** `AsistenciaService.registrar()`
