# Errores — Actas de adjudicación (`ACT`)

Errores de la generación, consulta y descarga del acta de adjudicación (CU08).
Viven en el módulo `actas`. Una convocatoria inexistente reutiliza
[`ERR_PRI_01`](../postulaciones/ERRORES_POSTULACIONES.md#err_pri_01) y un
`sort` o `filter` no soportado, `ERR_SYS_05`. Un fallo del PDF o de MinIO cae
en el `500` genérico (`ERR_SYS_00`).

## `ERR_ACT_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Los resultados de la convocatoria todavía no están
  publicados (CU22); no se genera el acta ni su vista previa.
- **Dónde se lanza:** `ActaAdjudicacionService.generar()` y
  `ActaAdjudicacionService.vistaPrevia()`

## `ERR_ACT_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria ya tiene acta (incluye el doble clic y a dos
  administradores a la vez: la segunda petición recibe este error). El acta es
  definitiva: no se regenera ni se elimina, y la vista previa tampoco se
  ofrece.
- **Dónde se lanza:** `ActaAdjudicacionService.generar()` y
  `ActaAdjudicacionService.vistaPrevia()`

## `ERR_ACT_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La convocatoria todavía no tiene acta; no se puede
  consultar ni descargar.
- **Dónde se lanza:** `ConsultaActasService.obtener()` y
  `ConsultaActasService.descargar()`

## `ERR_ACT_04`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El acta está registrada pero su archivo no está en el
  almacenamiento (MinIO); no se anota la descarga y hay que avisar a soporte.
- **Dónde se lanza:** `ConsultaActasService.descargar()`
