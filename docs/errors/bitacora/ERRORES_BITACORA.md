# Errores — Bitácora (`BIT`)

Errores de la consulta de la bitácora de acciones (CU25).

## `ERR_BIT_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El campo de `sort` no está soportado por la consulta de la bitácora
  (solo `fecha`, `accion` y `entidad`).
- **Dónde se lanza:** `BitacoraConsultaService.listar()` (método privado `pagina()`)

## `ERR_BIT_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El rango de fechas es inválido: `filter[desde]` es posterior a `filter[hasta]`.
- **Dónde se lanza:** `BitacoraConsultaService.listar()`
