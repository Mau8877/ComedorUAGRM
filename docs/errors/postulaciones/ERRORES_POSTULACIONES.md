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
- **Dónde se lanza:** `ListaPriorizadaService.listarPriorizados()` (`sort`) y
  `ListaPriorizadaController.validarFiltros()` (`filter[...]`)

## `ERR_PRI_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La lista a exportar está vacía (la convocatoria no tiene
  postulaciones calificadas, o la búsqueda y el filtro no devuelven ninguna):
  no hay nada que exportar y no se genera el PDF.
- **Dónde se lanza:** `ListaPriorizadaPdfService.exportar()`

## `ERR_DEV_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación del finalista ya no está «calificada» (ya fue
  devuelta, archivada, rechazada o adjudicada, por ejemplo por otro administrador
  o por una segunda petición del mismo clic) o los resultados de la convocatoria ya
  fueron publicados; no se puede devolver.
- **Dónde se lanza:** `DevolucionService.devolver()`

## `ERR_DEV_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El finalista no tiene ninguna auditoría registrada después de
  su última devolución (nunca se auditó, o se devolvió, el postulante reenvió y
  todavía no se volvió a auditar); siempre debe haber una auditoría antes de devolver.
- **Dónde se lanza:** `DevolucionService.devolver()`

## `ERR_ARC_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación del finalista ya no está «calificada» (ya fue
  devuelta, archivada, no adjudicada o adjudicada, por ejemplo por otro
  administrador o por una segunda petición del mismo clic) o los resultados de
  la convocatoria ya fueron publicados; no se puede archivar ni rechazar. El
  mensaje trae el estado actual cuando es el primer caso.
- **Dónde se lanza:** `CierrePostulacionService.archivar()` y
  `CierrePostulacionService.rechazar()` (método privado `cerrar()`)

## `ERR_ADJ_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación del finalista ya no está «calificada» (ya fue
  devuelta, archivada, adjudicada o no adjudicada, por ejemplo por otro
  administrador o por una segunda petición del mismo clic) o los resultados de
  la convocatoria ya fueron publicados; no se puede adjudicar ni no adjudicar.
  El mensaje trae el estado actual cuando es el primer caso. También se lanza
  al seleccionar finalistas si los resultados ya fueron publicados.
- **Dónde se lanza:** `AdjudicacionService.adjudicar()` y
  `AdjudicacionService.noAdjudicar()` (método privado `decidir()`);
  `SeleccionFinalistasService.seleccionar()`

## `ERR_ADJ_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El expediente del finalista no tiene ninguna auditoría
  (aplica a adjudicar y a no adjudicar) o, al adjudicar, la última auditoría no
  es «Validado» (hay que auditar de nuevo o usar «No adjudicar»).
- **Dónde se lanza:** `AdjudicacionService.adjudicar()` y
  `AdjudicacionService.noAdjudicar()` (método privado `decidir()`)

## `ERR_ADJ_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Al adjudicar, el puntaje total del finalista (puntaje de la
  IA vigente más puntaje académico de la última auditoría) es menor que el
  puntaje mínimo de la convocatoria, o el puntaje de la IA no está disponible.
- **Dónde se lanza:** `AdjudicacionService.adjudicar()` (método privado
  `decidir()`)

## `ERR_ADJ_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Al adjudicar, los cupos de la convocatoria ya están agotados
  (las adjudicaciones igualan a los cupos).
- **Dónde se lanza:** `AdjudicacionService.adjudicar()` (método privado
  `decidir()`)

## `ERR_ADJ_05`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria no está cerrada, o no tiene cupos o puntaje
  mínimo definidos; no se pueden seleccionar finalistas ni decidir.
- **Dónde se lanza:** `SeleccionFinalistasService.seleccionar()`;
  `AdjudicacionService.adjudicar()` y `AdjudicacionService.noAdjudicar()`
  (método privado `decidir()`)

## `ERR_POS_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La postulación pedida en «Mis postulaciones» (CU04) no existe,
  es de otra persona o todavía es un borrador. Los tres casos responden igual
  para no revelar si la postulación existe.
- **Dónde se lanza:** `MisPostulacionesService.detalle()`
