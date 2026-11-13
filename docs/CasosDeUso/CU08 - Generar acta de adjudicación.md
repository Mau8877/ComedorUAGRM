# Caso de uso

| Id   | Nombre                         | Módulo                       | Prioridad | Estado    |
| ---- | ------------------------------ | ---------------------------- | --------- | --------- |
| CU08 | Generar acta de adjudicación   | P4. Auditoría y adjudicación | Media     | En progreso |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador DUBSS genere el acta oficial en PDF que deja constancia de los resultados de la adjudicación de una convocatoria, y pueda consultarla y descargarla después.
**Plataforma:** Web
**RF asociados:** RF-04

**Descripción:**
Cuando los resultados de una convocatoria ya fueron publicados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)), el Administrador DUBSS entra a «Actas», elige la convocatoria, puede ver una **vista previa** del documento y pulsa «Generar PDF». El sistema verifica que los resultados estén publicados y que la convocatoria no tenga acta, toma los adjudicados con su puntaje final ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)), asigna un **número de acta automático** (`NNN/AAAA`), genera el PDF, lo guarda en MinIO, registra el acta (número, ruta, quién y cuándo) y la anota en la bitácora, en una misma operación. Después el Administrador puede **consultar** las actas generadas y **descargar** cada una; cada descarga también queda en la bitácora.

Hay **una sola acta por convocatoria**: es definitiva, no se regenera ni se elimina. Este CU extiende el CU-05 y, a diferencia de lo que decía el taller, se genera **después de publicar** (CU-22), porque entonces las decisiones ya no pueden cambiar y el acta no queda desactualizada. Publicar sigue sin exigir el acta. Este CU **no avisa a nadie**, no emite comprobantes individuales por beneficiario ni toca el padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Los resultados de la convocatoria **están publicados** ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)); la convocatoria está en estado «publicada».
- Para **generar**: la convocatoria **no tiene acta** todavía. Para **consultar o descargar**: ya tiene una.
- **Datos de apoyo:** las adjudicaciones y la publicación ya existen (CU-05 y CU-22). Para probar este CU en desarrollo basta la convocatoria `[PRUEBA]` con resultados publicados que carga el cargador de datos de prueba del CU-22.

## Flujo principal

### A. Generar el acta

1. El Administrador DUBSS entra a «Actas». El sistema muestra la tabla de actas generadas (paginada) y el panel «Generar acta» con las convocatorias cuyos resultados están publicados y que aún no tienen acta (A1).
2. El Administrador elige una convocatoria.
3. Si lo desea, pulsa «Vista previa»: el sistema muestra el documento tal como quedaría, con el número «borrador», sin guardarlo ni anotarlo en la bitácora (A2, A3, A4).
4. El Administrador pulsa «Generar PDF» (A6).
5. El sistema muestra una ventana de confirmación que avisa que el acta es **definitiva**: no se puede regenerar ni eliminar.
6. El Administrador confirma (A6).
7. El sistema valida que la convocatoria exista (A2), que sus resultados estén publicados (A3) y que no tenga acta (A4).
8. El sistema obtiene los adjudicados de la convocatoria con nombre y apellido, CI, carrera y puntaje final, y asigna el número de acta (A7).
9. El sistema genera el PDF y lo guarda en MinIO (A8).
10. El sistema registra el acta (convocatoria, número, ruta del archivo, quién y cuándo) y la anota en la bitácora, en una misma operación (A10).
11. El sistema muestra el acta generada y permite descargarla e imprimirla.

### B. Consultar y descargar el acta

1. El Administrador DUBSS entra a «Actas» y ve la tabla de actas generadas (número, convocatoria, fecha, quién la generó).
2. Pulsa «Descargar» en el acta que quiere (o elige una convocatoria que ya tiene acta).
3. El sistema obtiene el acta registrada (A5) y comprueba que el archivo esté disponible (A9).
4. El sistema anota la descarga en la bitácora y entrega una dirección temporal y firmada para abrir el PDF (vigente 5 minutos).
5. El navegador descarga o abre el PDF para imprimirlo.

## Flujos alternativos y excepciones

- **A1 (paso 1):** Ninguna convocatoria tiene resultados publicados sin acta → el sistema lo informa («No hay convocatorias con resultados publicados pendientes de acta»); la tabla de actas generadas se sigue viendo.
- **A2 (pasos 3 y 7):** La convocatoria no existe o fue eliminada → `404` (`ERR_PRI_01`); se vuelve a la selección de convocatoria.
- **A3 (pasos 3 y 7):** Los resultados de la convocatoria **no están publicados** → `409` (`ERR_ACT_01`); no se genera nada y se indica que primero hay que publicarlos (CU-22).
- **A4 (pasos 3 y 7):** La convocatoria **ya tiene acta** → `409` (`ERR_ACT_02`); no se genera otra, la vista previa tampoco, y se muestra la existente (número, fecha y quién la generó). Un doble clic o dos administradores a la vez reciben este mismo `409`.
- **A5 (flujo B, paso 3):** La convocatoria todavía no tiene acta → `404` (`ERR_ACT_03`); el sistema lo informa y ofrece generarla si corresponde.
- **A6 (pasos 4 y 6):** El Administrador cancela → no se genera ni se guarda nada.
- **A7 (paso 8):** Dos actas se generan a la vez (de convocatorias distintas) → el sistema asigna a cada una un número distinto; nunca se repite un número.
- **A8 (paso 9):** Falla la generación del PDF o el guardado en MinIO → el sistema informa un error claro (`500` genérico, `ERR_SYS_00`), **no registra** el acta ni la bitácora y permite reintentar.
- **A9 (flujo B, paso 3):** El acta está registrada pero su archivo no está en el almacenamiento → `404` (`ERR_ACT_04`); no se anota la descarga y se informa que hay que avisar a soporte.
- **A10 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento; el acta y la bitácora se guardan juntas o no se guarda ninguna. Si el PDF ya estaba subido a MinIO, el archivo sobrante no es accesible porque no hay acta registrada que lo enlace.
- **A11 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A12 (cualquier paso):** El usuario no es Administrador DUBSS → `403`; no ve la opción ni puede usar los endpoints.

## Reglas de negocio

- **Solo el Administrador DUBSS** genera, consulta y descarga actas (RF-04). Ni el Directivo ni el Postulante las ven.
- **Qué se exige para generar:** que los resultados estén **publicados** (CU-22). Publicar **no** exige el acta; la dependencia va en un solo sentido. *(Decisión de Mau, 2026-11-13: se elige exigir la publicación para que el acta no quede desactualizada. El taller decía «todas las adjudicaciones decididas»; ver «Cambios pendientes en el documento de taller».)*
- **Una sola acta por convocatoria.** Es definitiva: no se regenera, no se reemplaza y no se elimina. Un segundo intento responde `409` (`ERR_ACT_02`).
- **Número de acta automático**, con formato `NNN/AAAA`: correlativo que **reinicia cada año** (`001/2026`, `002/2026`…), con al menos tres dígitos, donde `AAAA` es el año de la fecha de generación (zona horaria de La Paz). Es **único en todo el sistema** y no se reutiliza. El Administrador **no lo escribe** (el campo del mockup se elimina). *(Decisión de Mau.)*
- **Contenido del PDF** *(el formato oficial está por definir con la DUBSS; esta es la propuesta aceptada por Mau)*:
  - Encabezado simple con UAGRM y la DUBSS, el título «Acta de adjudicación» y el número del acta.
  - Datos de la convocatoria: nombre, gestión académica (etiqueta), fechas de apertura y cierre, cupos y puntaje mínimo, y la fecha y hora de generación.
  - Resumen: cantidad de adjudicados frente a los cupos.
  - Tabla de **adjudicados**: n.º de orden, nombre y apellido, **CI**, carrera y **puntaje final**, ordenados de mayor a menor puntaje final (a igual puntaje, por la posición del finalista en la lista priorizada).
  - Nombre del Administrador que la generó y líneas de firma en blanco para las autoridades de la DUBSS.
  - **No incluye** a los no adjudicados, motivos, correos ni datos socioeconómicos.
- **Sin adjudicados también se genera:** si ninguno resultó adjudicado, el acta se emite igual con la tabla vacía y la leyenda «Sin adjudicados» (mismo criterio que el CU-22).
- **«Puntaje final»** es el guardado en la adjudicación (inmutable), no un cálculo nuevo.
- **Vista previa:** genera el mismo documento con el número «borrador»; no guarda nada, no consume número y no se anota en la bitácora. Tiene las mismas precondiciones que generar.
- **Descarga:** el PDF vive en MinIO; la base solo guarda la ruta. Se abre con una **dirección temporal firmada** (5 minutos), nunca con una ruta permanente.
- **Bitácora:** se anotan la **generación** (acción `GENERACION_ACTA`: quién, cuándo, códigos de acta y convocatoria, número y cantidad de adjudicados) y **cada descarga** (`DESCARGA_ACTA`, porque el PDF lleva CI): quién, cuándo y códigos. **Sin** nombres, CI, rutas ni direcciones firmadas. Consultar la tabla no se anota. Vista previa tampoco. *(Decisión de Mau.)*
- **No cambia estados:** ni las postulaciones, ni la convocatoria, ni las adjudicaciones. No avisa a nadie.
- **Listado de actas:** paginado (10, 20 o 50; 20 por defecto), con `search` por número o nombre de convocatoria y orden por número o fecha (por defecto, la más reciente primero).
- **Fuera de este CU:** comprobante individual por beneficiario, firma digital, padrón de becados (CU-23), publicar resultados (CU-22) y reportes.
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- La convocatoria tiene su acta registrada con número, ruta del archivo, responsable y fecha, y el PDF está guardado en MinIO.
- La generación (y cada descarga) queda en la bitácora.
- Ninguna postulación, adjudicación ni estado de convocatoria cambió.

## Criterios de aceptación

- **CA01:** Dado que los resultados están publicados y la convocatoria no tiene acta, el sistema debe generar el PDF, guardarlo en MinIO y registrar el acta con número, ruta, usuario y fecha.
- **CA02:** Dado que los resultados no están publicados, el sistema debe responder `409` (`ERR_ACT_01`) y no generar nada.
- **CA03:** Dado que la convocatoria ya tiene acta, el sistema debe responder `409` (`ERR_ACT_02`) y mostrar la existente, sin generar otra.
- **CA04:** Dado que la misma petición llega dos veces o dos administradores generan a la vez, el sistema debe registrar una sola acta y responder `409` a la otra.
- **CA05:** Dado que se generan actas de dos convocatorias a la vez, el sistema debe asignar a cada una un número distinto.
- **CA06:** Dada una acta generada, su número debe tener el formato `NNN/AAAA`, correlativo dentro del año y único en el sistema.
- **CA07:** Dado el PDF generado, debe mostrar el número, la convocatoria y su gestión, la fecha, los cupos, el puntaje mínimo y la tabla de adjudicados con nombre y apellido, CI, carrera y puntaje final de mayor a menor, más las líneas de firma.
- **CA08:** Dado el PDF generado, no debe incluir no adjudicados, motivos, correos ni datos socioeconómicos.
- **CA09:** Dado que ningún finalista resultó adjudicado, el sistema debe generar el acta igualmente con la leyenda «Sin adjudicados».
- **CA10:** Dada la vista previa, el sistema debe mostrar el documento con el número «borrador», sin registrar nada, sin consumir número y sin anotar en la bitácora; con acta existente o sin publicar, debe responder `409`.
- **CA11:** Dado que el Administrador cancela la confirmación, el sistema no debe generar ni guardar nada.
- **CA12:** Dado que falla la generación del PDF o el guardado en MinIO, el sistema debe informar el error, no registrar el acta ni la bitácora y permitir reintentar.
- **CA13:** Dada una convocatoria con acta, el sistema debe dejar descargar el PDF con una dirección temporal firmada de 5 minutos y anotar la descarga en la bitácora.
- **CA14:** Dada una convocatoria sin acta, al consultarla o descargarla el sistema debe responder `404` (`ERR_ACT_03`).
- **CA15:** Dada un acta registrada cuyo archivo no está en el almacenamiento, el sistema debe responder `404` (`ERR_ACT_04`) y no anotar la descarga.
- **CA16:** Dada la generación o la descarga, el sistema debe anotarlas en la bitácora sin nombres, CI, rutas ni direcciones firmadas.
- **CA17:** Dada la tabla de actas, el sistema debe paginarla (10, 20 o 50; 20 por defecto), permitir buscar por número o convocatoria y ordenarla por número o fecha.
- **CA18:** Dada una acta generada, el sistema no debe cambiar el estado de ninguna postulación, adjudicación ni convocatoria, ni avisar a nadie.
- **CA19:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`; con convocatoria inexistente, `404` (`ERR_PRI_01`).
- **CA20:** Dado que el acta existe, el sistema no debe permitir regenerarla ni eliminarla.

## Cambios al diseño lógico

- **Tabla `acta_adjudicacion`** (ya prevista en el diseño de grado, pero no existe en la base ni en el código): `id`, `codigo_acta_adjudicacion`, `convocatoria_id` FK, `numero` `varchar(30)` único, `storage_path` `varchar(500)`, `generada_por` FK→`usuario`, `generada_en` `timestamptz` y las columnas comunes de toda tabla.
  - **Cambio nuevo respecto del diseño de grado:** `convocatoria_id` pasa a ser **único entre las filas no eliminadas** (índice único parcial), porque hay una sola acta por convocatoria. Hay que actualizarlo primero en `diseno_base_de_datos.md`, en la imagen 091 y en `docs/DiagramaConceptual.mermaid` (y, en el taller, en las tablas de mapeo y de volumen de `acta_adjudicacion`), y después crear la migración.
  - `numero` es único en todo el sistema, incluso entre filas eliminadas; su unicidad es la red de seguridad si dos generaciones simultáneas coinciden.
- **Sin cambios** en `adjudicacion`, `publicacion_resultado`, `convocatoria` (no cambia el estado) ni `postulacion`.
- **Bitácora:** dos acciones nuevas, `GENERACION_ACTA` y `DESCARGA_ACTA`.
- **Códigos de error** (prefijo `ACT`, a documentar en `docs/errors/` y su índice al implementar): `ERR_ACT_01` (`409`, los resultados aún no están publicados), `ERR_ACT_02` (`409`, la convocatoria ya tiene acta), `ERR_ACT_03` (`404`, la convocatoria todavía no tiene acta) y `ERR_ACT_04` (`404`, el archivo del acta no está en el almacenamiento). Convocatoria inexistente reutiliza `ERR_PRI_01`; `sort` o `filter` no soportado, `ERR_SYS_05`; un fallo de MinIO o del PDF cae en el `500` genérico.
- **Endpoints** (propuestos; las colecciones van paginadas):
  - `GET /api/v1/actas` (`search`, `sort`, `page`, `pageSize`): tabla de actas generadas.
  - `GET /api/v1/actas/pendientes` (`page`, `pageSize`): convocatorias con resultados publicados y sin acta (alimenta el selector del panel «Generar acta»).
  - `GET /api/v1/convocatorias/{codigo}/acta`: datos del acta de una convocatoria (sin la dirección de descarga).
  - `GET /api/v1/convocatorias/{codigo}/acta/descarga`: dirección temporal firmada del PDF; anota `DESCARGA_ACTA`.
  - `GET /api/v1/convocatorias/{codigo}/acta/vista-previa`: PDF borrador, sin guardar.
  - `POST /api/v1/convocatorias/{codigo}/acta`: genera el acta (`201`).
  - El contrato definitivo (campos de cada respuesta) está en las UT: [`UT-BACKEND`](../UnidadesTrabajo/CU08%20-%20Generar%20acta%20de%20adjudicación/UT-BACKEND/README.md).

## Cambios pendientes en el documento de taller

| Dónde | Hoy dice | Debe decir | Por qué |
| --- | --- | --- | --- |
| `04_detalle_casos_uso.tex`, CU-08, Descripción | «…genera, una vez decididas todas las adjudicaciones de una convocatoria, el acta… con la lista de beneficiarios aprobados. El acta se almacena en MinIO, se registra con un número único y puede consultarse posteriormente.» | «…genera, **una vez publicados los resultados (CU-22)**, el acta… con la lista de adjudicados, con número automático `NNN/AAAA`. Hay una sola acta por convocatoria. Se almacena en MinIO, se registra y puede consultarse y descargarse; cada descarga queda en la bitácora.» | Decisión de Mau (2026-11-13). |
| CU-08, Precondición 2 | «Todas las adjudicaciones de los finalistas de la convocatoria están decididas (CU-05).» | «Los resultados de la convocatoria están publicados (CU-22).» | Para que el acta no quede desactualizada. |
| CU-08, Flujo A, paso 2 | «…verifica que no existan finalistas con la adjudicación pendiente de decisión.» | «…verifica que los resultados estén publicados y que la convocatoria no tenga acta.» | Igual. |
| CU-08, Flujo A, pasos 3–4 | «…nombre, CI, carrera y puntaje final…; genera el acta… con un número único, la fecha, los datos de la convocatoria y de la gestión académica y la lista de adjudicados.» | Agregar: número automático `NNN/AAAA` por año, cupos, puntaje mínimo, líneas de firma, y que no incluye no adjudicados ni datos socioeconómicos. | Decisión de Mau. |
| CU-08, Flujo A | Sin vista previa ni confirmación. | Agregar «Vista previa» (borrador, sin guardar) y una ventana de confirmación porque el acta es definitiva. | Maqueta 108; propuesta del equipo. |
| CU-08, Flujo B | «…obtiene el acta registrada y la muestra para su descarga e impresión.» | «…entrega una dirección temporal firmada (5 minutos) y anota la descarga en la bitácora.» | El PDF lleva CI. |
| CU-08, A1 | «…si existen adjudicaciones pendientes de decisión, no genera el acta e indica cuántas faltan.» | «…si los resultados no están publicados, no genera el acta (`ERR_ACT_01`).» | Igual. |
| CU-08, A2 | «…muestra el acta existente en lugar de generar una nueva.» | Agregar que responde `409` (`ERR_ACT_02`) y que dos peticiones simultáneas no generan dos actas. | Una sola acta por convocatoria. |
| CU-08, Postcondiciones | «La generación del acta queda registrada en la bitácora.» | Agregar: «Cada descarga también queda en la bitácora.» | Igual. |
| CU-08, Excepciones | Sin el archivo ausente. | Agregar «El archivo del acta no está en el almacenamiento» (`ERR_ACT_04`). | Consistencia. |
| `03_requisitos_especificos.tex`, RF-04, «¿Por qué?» | «…generar las actas o comprobantes correspondientes a los beneficiarios aprobados.» | «…generar el acta de adjudicación de la convocatoria.» | Una acta por convocatoria, no comprobantes individuales. |
| RF-04, criterio 6 | «El sistema genera un acta o comprobante digital de adjudicación para cada beneficiario aprobado.» | «Una vez publicados los resultados, el sistema genera el acta de adjudicación de la convocatoria en PDF, con la lista de adjudicados y un número automático.» | Igual. |
| CU-05, flujo A5 (paso 10) | «una vez decididas todas las adjudicaciones, puede generar el acta (CU-08) y publicar los resultados (CU-22).» | «…puede publicar los resultados (CU-22) y, una vez publicados, generar el acta (CU-08).» | El acta se genera después de publicar. |
| `images/108_Mockup_CU08_…png` | Campo «Número de acta» editable. | Quitar el campo; el número lo asigna el sistema. | Número automático. |
| `images/095_Secuencia_CU08_…jpg` y `080_Analisis_CU08_…jpg` | `list(adjudicaciones)` sin verificar publicación ni guardar el archivo. | Verificar la publicación antes, listar solo adjudicados y mostrar el almacenamiento (MinIO) al crear el acta. | Consistencia con el flujo. |
| `diseno_base_de_datos.md`, `acta_adjudicacion`; `03a_mapeo.tex`; `03b_volumen.tex`; imagen 091 | `convocatoria_id` FK sin unicidad. | `convocatoria_id` único entre las filas no eliminadas. | Una sola acta por convocatoria. |
| `diseno_base_de_datos.md`, Q5 | «¿Se mantiene CU-08 (acta)…?» (abierta). | Dejar abierta hasta la consulta con la DUBSS; mientras tanto se implementa lo de este CU. | Sigue pendiente de consulta. |

## Cambios ya aplicados en otros casos de uso del repositorio

- **CU-22** (`CU22 - Publicar resultados de adjudicación.md` y `UT-BACKEND/README.md`): la frase «cuando el CU-08 exista se definirá si pasa a ser precondición» quedó resuelta: el acta **no** es precondición de publicar; al revés, el acta exige la publicación.
- **CU-05** (`CU05 - Auditar y adjudicar becas.md`, descripción y paso 14): el acta se genera después de publicar, no «con las decisiones tomadas».

## Decisiones confirmadas

Confirmadas por Mau (Administrador del proyecto) el 2026-11-13. Aún no se validaron con la DUBSS.

1. **Número de acta:** automático, correlativo por año (`NNN/AAAA`), sin campo manual.
2. **Una acta por convocatoria** (no comprobantes por beneficiario).
3. **Cuándo se genera:** solo con los resultados publicados (CU-22); publicar no exige el acta.
4. **Definitiva:** una sola acta, sin regenerar ni eliminar; el segundo intento responde `409`.
5. **Contenido del PDF:** el descrito en las reglas de negocio; encabezado y firmas por definir con la DUBSS.
6. **Vista previa:** incluida, sin guardar ni anotar.
7. **Descarga:** dirección temporal firmada de MinIO; generación y descargas en la bitácora.
8. **Pantalla:** ítem «Actas» en el menú del Administrador, con la tabla de actas y el panel para generar.

## Pendiente de definir con la DUBSS

- Si se mantiene este CU (Q5 de la documentación de grado).
- El **formato oficial** del acta: encabezado institucional, quién firma y el formato del número.
- Si hace falta un comprobante individual para cada beneficiario (hoy fuera de alcance).

## Desarrollador

Mauro Gutierrez
