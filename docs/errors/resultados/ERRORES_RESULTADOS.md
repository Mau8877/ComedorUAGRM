# Errores — Publicación de resultados (`PUB`)

Errores de la publicación de los resultados de adjudicación (CU22) y de su
consulta por el postulante. Viven en el módulo `resultados`. Una convocatoria
inexistente reutiliza [`ERR_PRI_01`](../postulaciones/ERRORES_POSTULACIONES.md#err_pri_01)
y un `sort` o `filter` no soportado, `ERR_SYS_05`.

## `ERR_PUB_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Quedan finalistas de la convocatoria en «calificada», es
  decir sin decisión de adjudicación; no se puede publicar. El mensaje indica
  cuántas decisiones faltan.
- **Dónde se lanza:** `PublicacionResultadosService.publicar()`

## `ERR_PUB_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Los resultados de la convocatoria ya estaban publicados
  (incluye el doble clic y a dos administradores a la vez: la segunda
  petición recibe este error). La publicación es definitiva: no se despublica
  ni se vuelve a publicar.
- **Dónde se lanza:** `PublicacionResultadosService.publicar()`

## `ERR_PUB_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria no está cerrada (sigue en borrador o
  abierta); solo se publican los resultados de una convocatoria cerrada.
- **Dónde se lanza:** `PublicacionResultadosService.publicar()`

## `ERR_PUB_04`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** Los resultados de la convocatoria todavía no se publicaron;
  el postulante no puede consultar la lista de adjudicados ni su resultado.
- **Dónde se lanza:** `ResultadosPostulanteService.adjudicados()` y
  `ResultadosPostulanteService.miResultado()`
