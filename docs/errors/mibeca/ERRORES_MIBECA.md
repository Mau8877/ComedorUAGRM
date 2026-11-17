# Errores — Mi beca (`MBE`)

Errores de la consulta del becado sobre su beca y su historial de asistencia (CU24). Viven en el módulo
`mibeca`. Un `page`/`pageSize` no positivo cae en `ERR_SYS_01`, un `filter[...]` no soportado o una fecha mal
formada en `ERR_SYS_05` y un rol distinto de `POSTULANTE`, en `ERR_AUTH_07`.

## `ERR_MBE_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El usuario autenticado no tiene ninguna beca: no figura en el padrón de becados (flujo A1).
- **Dónde se lanza:** `MiBecaService.beca()` (desde `miBeca()` y `asistencias()`)

## `ERR_MBE_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El rango del historial es inválido: la fecha `filter[desde]` es posterior a `filter[hasta]`.
- **Dónde se lanza:** `MiBecaService.asistencias()`

## `ERR_MBE_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El `sort` pide ordenar por un campo que el historial no admite (solo `fecha` o `-fecha`).
- **Dónde se lanza:** `MiBecaService.asistencias()`
