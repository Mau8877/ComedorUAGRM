# Errores — Dashboard de indicadores BI (`DAS`)

Errores de la consulta y la descarga en PDF del dashboard del Directivo
(CU12). Viven en el módulo `dashboard`. Un valor de `filter[gestion]` o
`filter[convocatoria]` que no es un UUID responde `ERR_SYS_05`. Un fallo del
PDF cae en el `500` genérico (`ERR_SYS_00`).

## `ERR_DAS_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La gestión o la convocatoria pedida no existe, o la
  convocatoria no pertenece a la gestión indicada.
- **Dónde se lanza:** `IndicadoresService.consultar()` e
  `IndicadoresPdfService.generar()`

## `ERR_DAS_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** No hay datos que exportar: todos los indicadores del
  periodo consultado son cero, así que no se genera el PDF.
- **Dónde se lanza:** `IndicadoresPdfService.generar()`
