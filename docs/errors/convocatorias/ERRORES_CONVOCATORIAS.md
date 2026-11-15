# Errores — Convocatorias (`CV`)

Errores de la gestión de convocatorias de la Beca Comedor, de las gestiones
académicas y de los documentos requeridos de cada convocatoria (CU13).

## `ERR_CV_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La convocatoria indicada no existe o fue eliminada.
- **Dónde se lanza:** `ConvocatoriaService.obtener()`, `ConvocatoriaService.editar()`,
  `ConvocatoriaService.eliminar()`, `ConvocatoriaEstadoService.abrir()`,
  `ConvocatoriaEstadoService.cerrar()`

## `ERR_CV_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La gestión académica indicada no existe o fue eliminada.
- **Dónde se lanza:** `ConvocatoriaService.crear()`, `ConvocatoriaService.editar()`,
  `GestionAcademicaService.editar()`

## `ERR_CV_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** Alguno de los tipos de documento de la lista de documentos
  requeridos no existe.
- **Dónde se lanza:** `DocumentosConvocatoriaService.validar()`

## `ERR_CV_04`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La fecha de cierre de la convocatoria no es posterior a la
  fecha de apertura (iguales o invertidas).
- **Dónde se lanza:** `ConvocatoriaService.crear()`, `ConvocatoriaService.editar()`

## `ERR_CV_05`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Ninguno de los documentos requeridos de la convocatoria está
  marcado como obligatorio; se exige al menos uno.
- **Dónde se lanza:** `DocumentosConvocatoriaService.validar()`

## `ERR_CV_06`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Un mismo tipo de documento aparece más de una vez en la lista
  de documentos requeridos.
- **Dónde se lanza:** `DocumentosConvocatoriaService.validar()`

## `ERR_CV_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se intenta abrir una convocatoria cuando ya hay otra abierta
  en la misma gestión académica (solo puede haber una por gestión). También
  cuando dos aperturas simultáneas chocan con el índice único.
- **Dónde se lanza:** `ConvocatoriaEstadoService.abrir()`

## `ERR_CV_08`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La operación no corresponde al estado actual de la
  convocatoria: abrir una que no está en borrador, cerrar una que no está
  abierta, editar una cerrada o publicada, cambiar la gestión de una que no
  está en borrador, o eliminar una que no está en borrador.
- **Dónde se lanza:** `ConvocatoriaEstadoService.abrir()`,
  `ConvocatoriaEstadoService.cerrar()`, `ConvocatoriaService.editar()`,
  `ConvocatoriaService.eliminar()`

## `ERR_CV_09`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria ya recibió postulaciones y se intenta cambiar
  su fecha de apertura o sus documentos requeridos, lo que las afectaría.
- **Dónde se lanza:** `ConvocatoriaService.editar()`

## `ERR_CV_10`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria tiene postulaciones asociadas y no se puede
  eliminar; solo se puede cerrar.
- **Dónde se lanza:** `ConvocatoriaService.eliminar()`

## `ERR_CV_11`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El campo de `sort` no está soportado por el listado
  (convocatorias, gestiones académicas o tipos de documento).
- **Dónde se lanza:** `ConvocatoriaService.listar()`, `GestionAcademicaService.listar()`,
  `TipoDocumentoService.listar()`

## `ERR_CV_12`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe una gestión académica con esa etiqueta (una
  eliminada lógicamente no cuenta).
- **Dónde se lanza:** `GestionAcademicaService.crear()`, `GestionAcademicaService.editar()`

## `ERR_CV_13`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La fecha de fin de la gestión académica no es posterior a la
  fecha de inicio.
- **Dónde se lanza:** `GestionAcademicaService.crear()`, `GestionAcademicaService.editar()`
