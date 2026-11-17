# Errores — Cotejo de asistencia (`COT`)

Errores del registro del cotejo entre la planilla física y los registros
digitales (CU11). Viven en el módulo `cotejos`. Un campo vacío, negativo o
fuera de rango cae en `ERR_SYS_01` (Bean Validation), un `sort` o `filter` no
soportado en `ERR_SYS_05` y un rol distinto de `ADMINISTRADOR_DUBSS`, en
`ERR_AUTH_07`.

## `ERR_COT_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La planilla que se quiere cotejar no existe o fue eliminada.
- **Dónde se lanza:** `CotejoAsistenciaService.registrar()` y
  `ConsultaCotejosService.exigirPlanilla()` (desde `datos()`, `registros()` y
  `cotejos()`)

## `ERR_COT_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Las coincidencias y las discrepancias declaradas no suman el
  total de registros.
- **Dónde se lanza:** `CotejoAsistenciaService.validar()`, desde `registrar()`

## `ERR_COT_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El resultado no corresponde a las discrepancias: «conforme»
  con discrepancias, o «con discrepancias» sin ninguna.
- **Dónde se lanza:** `CotejoAsistenciaService.validar()`, desde `registrar()`

## `ERR_COT_04`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El cotejo tiene discrepancias pero no trae observaciones
  (vacías o solo espacios).
- **Dónde se lanza:** `CotejoAsistenciaService.validar()`, desde `registrar()`
