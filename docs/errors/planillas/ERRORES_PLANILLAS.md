# Errores — Planillas de asistencia (`PLA`)

Errores de la generación, consulta y descarga de la planilla de asistencia
imprimible (CU09). Viven en el módulo `planillas`. Una convocatoria
inexistente reutiliza
[`ERR_PRI_01`](../postulaciones/ERRORES_POSTULACIONES.md#err_pri_01) y un
`sort` o `filter` no soportado, `ERR_SYS_05`. Un fallo del PDF o de MinIO cae
en el `500` genérico (`ERR_SYS_00`); en ese caso la planilla no se registra.

## `ERR_PLA_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La fecha final del periodo es anterior a la inicial.
- **Dónde se lanza:** `PlanillaAsistenciaService.validarPeriodo()`, desde
  `generar()` y `vistaPrevia()`

## `ERR_PLA_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El periodo no está comprendido en la gestión académica de la
  convocatoria (empieza antes de su inicio o termina después de su fin).
- **Dónde se lanza:** `PlanillaAsistenciaService.validarPeriodo()`, desde
  `generar()` y `vistaPrevia()`

## `ERR_PLA_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El periodo supera los 31 días, que son las columnas de día
  que caben en la hoja de la planilla (límite de diseño del formato, por
  definir con la DUBSS).
- **Dónde se lanza:** `PlanillaAsistenciaService.validarPeriodo()`, desde
  `generar()` y `vistaPrevia()`

## `ERR_PLA_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria no tiene becas en estado vigente; no hay a
  quién incluir, así que no se genera la planilla ni su vista previa.
- **Dónde se lanza:** `PlanillaAsistenciaService.exigirBecados()`, desde
  `generar()` y `vistaPrevia()`

## `ERR_PLA_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La planilla que se quiere descargar no existe o fue
  eliminada.
- **Dónde se lanza:** `ConsultaPlanillasService.descargar()`

## `ERR_PLA_06`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La planilla está registrada pero su archivo no está en el
  almacenamiento (MinIO); la descarga no se anota en la bitácora.
- **Dónde se lanza:** `ConsultaPlanillasService.descargar()`
