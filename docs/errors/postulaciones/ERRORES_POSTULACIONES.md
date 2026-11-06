# Errores — Lista priorizada (`PRI`)

Errores de la lista priorizada de postulantes (CU06): la consulta de la
lista y su exportación a PDF. Viven en el módulo `postulaciones`, que es
donde está la lista.

## `ERR_PRI_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La convocatoria pedida (`filter[convocatoriaId]` o
  `convocatoriaId` al exportar) no existe o está eliminada lógicamente.
- **Dónde se lanza:** `ListaPriorizadaService.exigirConvocatoria()` y
  `ListaPriorizadaPdfService.exportar()`

## `ERR_PRI_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Se mandó un `sort` o un `filter[...]` que la lista no
  soporta. La lista siempre va ordenada por puntaje vigente de mayor a menor
  y solo acepta `filter[convocatoriaId]` y `filter[carreraId]`.
- **Dónde se lanza:** `ListaPriorizadaService.listar()` (`sort`) y
  `ListaPriorizadaController.validarFiltros()` (`filter[...]`)

## `ERR_PRI_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La lista a exportar está vacía (la convocatoria no tiene
  postulaciones calificadas, o la búsqueda y el filtro no devuelven ninguna):
  no hay nada que exportar y no se genera el PDF.
- **Dónde se lanza:** `ListaPriorizadaPdfService.exportar()`
