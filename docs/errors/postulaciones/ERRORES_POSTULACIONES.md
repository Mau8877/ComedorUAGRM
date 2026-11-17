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

## `ERR_VAL_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El postulante ya tiene una postulación vigente (enviada, es
  decir, no borrador) en alguna convocatoria de la misma gestión académica; no
  se acepta otra. Una `devuelta` de la misma convocatoria no cuenta: es el
  reenvío. El mensaje trae la gestión, el estado visible y la fecha de envío.
- **Dónde se lanza:** `ValidacionPostulacionService.validar()` (método privado
  `verificarSinDuplicados()`)

## `ERR_VAL_02`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El postulante no cumple las condiciones de elegibilidad: su
  cuenta está desactivada o no es de un postulante (no tiene datos de
  postulante). Las condiciones que defina la DUBSS se suman con este mismo
  código.
- **Dónde se lanza:** `ValidacionPostulacionService.validar()` (métodos
  privados `verificarCondiciones()` y `rechazoNoElegible()`)

## `ERR_VAL_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria de destino no está `ABIERTA` o la fecha
  actual queda fuera de su apertura y cierre; no recibe postulaciones.
- **Dónde se lanza:** `ValidacionPostulacionService.validar()` (método privado
  `verificarConvocatoriaAbierta()`)

## `ERR_DOC_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** No llegó ningún archivo en la subida de un documento, o el
  archivo está vacío.
- **Dónde se lanza:** `ReglasArchivoDocumento.validar()`

## `ERR_DOC_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El archivo supera el tamaño máximo permitido
  (`app.documentos.tamano-maximo-bytes`, por definir con la DUBSS).
- **Dónde se lanza:** `ReglasArchivoDocumento.validar()`

## `ERR_DOC_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El formato no está permitido (se decide por el contenido real
  del archivo, no por su extensión) o el contenido no corresponde al tipo que
  declaró el cliente.
- **Dónde se lanza:** `ReglasArchivoDocumento.validar()`

## `ERR_DOC_04`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La postulación de los documentos no existe, está eliminada o
  es de otra persona. Los casos responden igual para no revelar si existe.
- **Dónde se lanza:** `DocumentosPostulacionService.cabecera()` (desde
  `listar()`, `adjuntar()`, `reemplazar()`, `eliminar()` y `acceso()`)

## `ERR_DOC_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El tipo de documento no existe o no figura en la lista de
  documentos requeridos de la convocatoria de la postulación.
- **Dónde se lanza:** `DocumentosPostulacionService.adjuntar()`

## `ERR_DOC_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación ya fue enviada (no está en borrador ni
  devuelta): no se adjuntan, reemplazan ni eliminan documentos.
- **Dónde se lanza:** `DocumentosPostulacionService.adjuntar()`,
  `reemplazar()` y `eliminar()` (método privado `exigirEditable()`)

## `ERR_DOC_07`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La convocatoria de la postulación ya no está abierta, o su
  fecha de cierre ya pasó: no se modifican documentos.
- **Dónde se lanza:** `DocumentosPostulacionService.adjuntar()`,
  `reemplazar()` y `eliminar()` (método privado `exigirEditable()`)

## `ERR_DOC_08`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya hay el máximo de archivos de ese tipo de documento en la
  postulación (`app.documentos.maximo-archivos-por-tipo`, por definir con la
  DUBSS).
- **Dónde se lanza:** `DocumentosPostulacionService.adjuntar()`

## `ERR_DOC_09`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El documento adjunto no existe, está eliminado o no es de esa
  postulación.
- **Dónde se lanza:** `DocumentosPostulacionService.documento()` (desde
  `reemplazar()`, `eliminar()` y `acceso()`)

## `ERR_ENV_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** No hay una convocatoria `ABIERTA` dentro de sus fechas que
  reciba postulaciones, así que no se puede iniciar «Nueva postulación». El
  mensaje agrega la próxima convocatoria (abierta con apertura futura), si la
  hay.
- **Dónde se lanza:** `EnvioPostulacionService.iniciar()` (método privado
  `sinConvocatoria()`)

## `ERR_ENV_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La postulación que se quiere enviar o guardar como borrador
  no existe, está eliminada o no es del usuario del token (una ajena responde
  igual que una inexistente).
- **Dónde se lanza:** `EnvioPostulacionService.enviar()` y
  `BorradorPostulacionService.guardar()` (CU17)

## `ERR_ENV_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación ya no se puede enviar: no está en `borrador`
  ni `devuelta` y tampoco fue enviada hace poco (una `pendiente` o `calificada`
  recibe su comprobante de nuevo, no este error); por ejemplo, ya fue
  archivada, adjudicada o no adjudicada. Al **guardar un borrador** (CU17) es
  más estricto: cualquier estado distinto de `borrador` y `devuelta`, incluidas
  `pendiente` y `calificada`, ya no se puede editar.
- **Dónde se lanza:** `EnvioPostulacionService.enviar()` y
  `BorradorPostulacionService.guardar()` (CU17)

## `ERR_ENV_04`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Falta algún campo del formulario socioeconómico o algún
  documento marcado como obligatorio por la convocatoria. Los campos son los
  seis de columna y los obligatorios del formulario oficial de la DUBSS
  (de quién depende, ocupación, rango de ingresos, si tiene hijos y cuántos,
  tenencia de la vivienda, otro beneficio y discapacidad). El mensaje lista qué
  falta; no cambia el estado de la postulación. Un cliente que no manda los
  datos del formulario oficial (por ejemplo una app desactualizada) recibe este
  error.
- **Dónde se lanza:** `EnvioPostulacionService.enviar()` (métodos privados
  `validarRespuestas()` y `rechazoFaltantes()`; lo obligatorio del formulario
  oficial sale de `ReglasRespuestaSocioeconomica.faltantesComplementarios()`)

## `ERR_ENV_05`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Un dato del formulario está completo pero es inválido: monto
  o promedio fuera de rango o con más de 2 decimales, dependientes fuera de 0 a
  50, o una procedencia, patrimonio o estado de vulnerabilidad que no es una
  de las opciones vigentes; o, en el formulario oficial de la DUBSS, una opción
  de lista que no existe, más de 10 integrantes en el grupo familiar, una edad
  fuera de 0 a 120, un ambiente de la vivienda fuera de 0 a 20 o un texto más
  largo de lo permitido (comentario: 500 caracteres). El mensaje lista cada
  dato inválido. Al guardar un
  borrador (CU17) solo se revisan los campos que traen valor (un campo vacío
  no es inválido) y, si alguno no vale, no se guarda ninguno de la petición.
- **Dónde se lanza:** `EnvioPostulacionService.enviar()` (método privado
  `validarRespuestas()`) y `BorradorPostulacionService.guardar()` (CU17); las
  reglas de valor son de `ReglasRespuestaSocioeconomica.invalidos()` y, para el
  formulario oficial, `invalidosComplementarios()`

