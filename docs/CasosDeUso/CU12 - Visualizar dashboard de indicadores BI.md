# Caso de uso

| Id   | Nombre                                 | Módulo                         | Prioridad | Estado    |
| ---- | -------------------------------------- | ------------------------------ | --------- | --------- |
| CU12 | Visualizar dashboard de indicadores BI | P6. Reportes y trazabilidad    | Alta      | Hecho |

**Actor principal:** Directivo
**Actores secundarios:** —
**Objetivo:** Que el Directivo consulte, en un dashboard interactivo, los indicadores clave del proceso de la Beca Comedor (postulaciones, finalistas, becas adjudicadas y asistencia) y un análisis BI (embudo del proceso, comparación entre facultades y convocatorias, distribución de puntajes de la IA, cobertura de cupos, asistencia y tendencia entre gestiones) por gestión académica, convocatoria y mes, como apoyo a la toma de decisiones, y pueda descargarlos en PDF y en Excel.
**Plataforma:** Web
**RF asociados:** RF-07

**Descripción:**
El Directivo entra a «Dashboard de indicadores» y el sistema le muestra, para la gestión académica vigente, cuatro indicadores (postulaciones recibidas, finalistas, becas adjudicadas y asistencia promedio) y dos gráficos por mes (postulaciones y tasa de asistencia), junto con la fecha de la última actualización. Puede cambiar la gestión académica y, opcionalmente, elegir una convocatoria; el dashboard se actualiza con esa selección. Si lo desea, descarga lo que está viendo como un PDF.

Los indicadores **no se calculan al consultar**: salen de una vista precalculada que se refresca por lote cada noche, de modo que la consulta no afecta a las operaciones transaccionales. Es una consulta de **solo lectura**: no modifica ningún dato.

Debajo de los indicadores, el **análisis BI** responde preguntas de gestión: cuántas postulaciones llegan a cada etapa del proceso (embudo), **qué facultad se postula más** y cuántas de sus postulaciones se adjudican (con las carreras de cada facultad), cómo se compara cada convocatoria de la gestión con la anterior, cómo se reparten los puntajes de la IA, qué parte de los cupos está cubierta por becas vigentes, cómo es la asistencia por día de la semana (y cuántas becas están en riesgo) y cómo evolucionan las gestiones. Elegir una convocatoria en la comparación filtra todo el dashboard por ella.

El indicador de **asistencia** sale del registro de asistencia digital ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)). Este CU **no** crea el registro de asistencia ni el padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)), no hace proyecciones a futuro (la tendencia compara gestiones ya registradas) y no gestiona la bitácora ([CU-25](CU25%20-%20Consultar%20bitácora%20de%20acciones.md)).

---

## Precondiciones

- El Directivo está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `DIRECTIVO` y su cuenta está activa.
- Existe al menos una gestión académica ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)). *(Si no hay datos que mostrar, se aplica A1.)*
- Los indicadores fueron calculados en el último proceso por lote. *(Si aún no se ejecutó, se aplica A2.)*

## Flujo principal

1. El Directivo selecciona la opción «Dashboard de indicadores».
2. El sistema consulta los indicadores precalculados de la gestión académica vigente, con todas sus convocatorias (A1, A2).
3. El sistema muestra los cuatro indicadores y los gráficos de postulaciones y de asistencia por mes, y la fecha de la última actualización.
4. El Directivo selecciona otra gestión académica o una convocatoria y pulsa «Aplicar» (A3).
5. El sistema consulta los indicadores de esa selección y actualiza el dashboard (A1).
6. El sistema muestra también el análisis BI de esa selección: embudo, facultades (con sus carreras), comparación de convocatorias, puntajes, cobertura de cupos, asistencia y tendencia.
7. El Directivo puede elegir una facultad para ver sus carreras, o una convocatoria de la comparación para filtrar todo el dashboard por ella.
8. Si lo desea, el Directivo pulsa «Descargar PDF» o «Descargar Excel» (A4): el sistema genera el documento con lo que está viendo y lo descarga.

## Flujos alternativos y excepciones

- **A1 (pasos 2 y 5):** No hay datos para la gestión o la convocatoria elegida → el sistema muestra un mensaje informativo y los indicadores en cero; no es un error.
- **A2 (paso 3):** El proceso por lote aún no se ejecutó → el sistema muestra los últimos indicadores disponibles con su fecha de actualización; si nunca se ejecutó, lo indica en vez de mostrar números inventados.
- **A3 (paso 4):** La gestión o la convocatoria elegida no existe, o la convocatoria no pertenece a la gestión → `404` (`ERR_DAS_01`); el dashboard conserva la última selección válida.
- **A4 (paso 8):** No hay datos que exportar (A1) → `400` (`ERR_DAS_02`); no se genera el PDF ni el Excel y se informa. Si falla la generación del documento → mensaje claro, no se descarga nada y se puede reintentar.
- **A5 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A6 (cualquier paso):** El usuario no tiene el rol Directivo → `403`; no ve la opción ni puede usar los endpoints.
- **A7 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento.
- **A8 (paso 6):** Falla solo la consulta del análisis BI → los indicadores y gráficos por mes se siguen mostrando, y el análisis ofrece «Reintentar».
- **A9 (paso 6):** Aún no hay registro de asistencia, puntajes o becas → la tarjeta correspondiente lo indica («Sin registros de asistencia», «Sin puntajes calculados») y las demás funcionan; los porcentajes sin denominador se muestran como «—».

## Reglas de negocio

- **Solo el Directivo** consulta el dashboard (RF-07). El Administrador DUBSS y el Postulante no. *(Si la DUBSS pide que el Administrador también lo vea, se agrega al rol; hoy no.)*
- **Solo lectura.** Consultar o descargar no modifica datos y **no se anota en la bitácora**: los indicadores son agregados, sin datos personales de postulantes.
- **Datos precalculados por lote** (RNF «BI en lote»): una vista materializada `mv_indicadores_convocatoria`, refrescada por una tarea programada **cada noche (02:00)** y también al arrancar el sistema si está vacía. No hay botón de «actualizar ahora». La pantalla muestra siempre la fecha de la última actualización.
- **Qué cuenta cada indicador:**
  - **Postulaciones recibidas:** las postulaciones enviadas de la convocatoria; **no** cuentan los borradores.
  - **Finalistas:** los finalistas de la convocatoria ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)).
  - **Becas adjudicadas:** las adjudicaciones en «adjudicada» solo de convocatorias con **resultados publicados** ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)), para no adelantar lo que aún no es oficial.
  - **Asistencia promedio:** asistencias registradas ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)) sobre las esperadas (becas vigentes por día de servicio con registro). ⚠️ Criterio adoptado, **a validar con la DUBSS**. Sin registros muestra «Sin datos».
- **Análisis BI** (mismos filtros que los indicadores; los porcentajes van de 0 a 100 con un decimal y son «—» si el denominador es cero):
  - **Embudo:** postulaciones → precalificadas por la IA → finalistas → adjudicadas (solo con resultados publicados) → becas vigentes.
  - **Facultades:** postulaciones y adjudicadas por facultad, su parte del total y su tasa de adjudicación, ordenadas de mayor a menor número de postulaciones; cada una trae el detalle por carrera.
  - **Comparación de convocatorias:** todas las de la gestión (el filtro de convocatoria solo marca la elegida), con postulaciones, finalistas, adjudicadas frente a cupos, tasa, puntaje promedio de la IA y variación de postulaciones frente a la convocatoria anterior.
  - **Puntajes de la IA:** histograma en 7 tramos de 10 puntos (0–70) y promedio.
  - **Cobertura de cupos:** becas vigentes sobre los cupos de las convocatorias, con suspendidas, dadas de baja y reemplazos.
  - **Asistencia:** tasa promedio, tasa por día de la semana (lunes a domingo) y **becas en riesgo**. ⚠️ Criterio adoptado, **a validar con la DUBSS**: becas vigentes con al menos 5 días de servicio y menos de 60 % de asistencia.
  - **Tendencia:** postulaciones y adjudicadas por gestión, con la variación de la gestión elegida frente a la anterior. Ignora el filtro de convocatoria.
- **Datos precalculados:** el análisis lee cuatro vistas materializadas más (`mv_bi_*`), refrescadas por el mismo lote nocturno.
- **Excel:** el archivo trae una hoja por bloque del análisis (resumen, por mes, facultad, carrera, convocatorias, puntajes, asistencia y tendencia) con la selección aplicada y la fecha de actualización; sin datos personales.
- **«Por mes»:** las postulaciones se ubican en el mes de su envío; las adjudicaciones, en el mes de su decisión; la asistencia, en el mes del registro.
- **Filtros:** la **gestión académica** (por defecto la vigente, según sus fechas de inicio y fin) y, opcionalmente, una **convocatoria** de esa gestión («Todas» por defecto). Los indicadores de «Todas» son la suma de sus convocatorias.
- **PDF con Jasper:** el documento reproduce lo que ve el Directivo (los cuatro indicadores y los valores por mes de la selección), con la gestión y convocatoria elegidas, la fecha de la última actualización, y la fecha, hora y usuario que lo genera. Sin datos personales.
- **Fuera de este CU:** proyecciones a futuro, otras exportaciones, el registro de asistencia (CU-10), el cotejo (CU-11), el padrón (CU-23) y la bitácora (CU-25).
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- El Directivo visualiza los indicadores de la selección con su fecha de actualización.
- Si descargó el PDF o el Excel, tiene el documento; ningún dato del sistema cambió y no se anotó nada en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que el Directivo entra al dashboard, el sistema debe mostrar los indicadores de la gestión académica vigente con su fecha de última actualización.
- **CA02:** Dado que el Directivo elige una gestión, el sistema debe actualizar los cuatro indicadores y los gráficos solo con esa gestión.
- **CA03:** Dado que el Directivo elige una convocatoria de la gestión, el sistema debe mostrar solo los datos de esa convocatoria.
- **CA04:** Dado que una gestión o convocatoria no tiene datos, el sistema debe mostrar un mensaje informativo y los indicadores en cero, sin error.
- **CA05:** Dado que el proceso por lote aún no se ejecutó, el sistema debe mostrar los últimos indicadores disponibles con su fecha, o indicar que aún no hay.
- **CA06:** Dado que hay borradores, el sistema no debe contarlos en «postulaciones recibidas».
- **CA07:** Dada una convocatoria sin resultados publicados, el sistema no debe contar sus adjudicaciones en «becas adjudicadas».
- **CA08:** Dado que aún no hay registros de asistencia, el sistema debe mostrar «Sin datos» en la asistencia y el resto del dashboard debe funcionar; con registros, debe mostrar la tasa real (asistieron sobre esperados).
- **CA09:** Dada una consulta o una descarga, el sistema no debe modificar ningún dato ni anotar en la bitácora.
- **CA10:** Dado que el usuario no es Directivo, el sistema debe responder `403`; sin sesión válida, `401`; con gestión o convocatoria inexistente, `404`.
- **CA11:** Dado que el Directivo descarga el PDF, el sistema debe entregar un documento con los indicadores de la selección, la fecha de actualización y la fecha, hora y usuario de generación, sin datos personales.
- **CA12:** Dado que no hay datos que exportar, el sistema no debe generar el PDF e informar que no hay datos.
- **CA14:** Dado el análisis BI de una selección, el sistema debe mostrar el embudo con sus cinco etapas y el porcentaje de conversión de cada una respecto de la anterior.
- **CA15:** Dado el análisis BI, el sistema debe mostrar el ranking de facultades por número de postulaciones, junto con sus adjudicadas, y al elegir una facultad, sus carreras.
- **CA16:** Dado el análisis BI, el sistema debe comparar todas las convocatorias de la gestión con la variación de postulaciones frente a la anterior, y al elegir una, filtrar todo el dashboard por ella.
- **CA17:** Dado el histograma de puntajes, el sistema debe repartir los puntajes de la IA en 7 tramos de 10 puntos (0–70) y mostrar el promedio.
- **CA18:** Dada la cobertura de cupos, el sistema debe mostrar el porcentaje de cupos cubiertos por becas vigentes y las becas suspendidas, dadas de baja y reemplazos.
- **CA19:** Dado el registro de asistencia, el sistema debe mostrar la tasa por día de la semana y el número de becas en riesgo (menos de 60 % con al menos 5 días de servicio).
- **CA20:** Dadas varias gestiones, el sistema debe mostrar la tendencia de postulaciones y adjudicadas y la variación de la gestión elegida, sin aplicar el filtro de convocatoria.
- **CA21:** Dado que el Directivo pulsa «Descargar Excel», el sistema debe entregar un `.xlsx` con las hojas del análisis de la selección y la fecha de actualización; sin datos, `400` `ERR_DAS_02`.
- **CA22:** Dado que el denominador de un porcentaje es cero, el sistema debe mostrar «—» y no un cero engañoso.
- **CA13:** Dada una gestión con 20 000 postulaciones, el sistema debe mostrar el dashboard en 3 segundos o menos, porque lee la vista precalculada.

## Cambios al diseño lógico

- **Sin tablas transaccionales nuevas.** Se agregan la vista materializada `mv_indicadores_convocatoria` y, para el análisis BI, cuatro más: `mv_bi_asistencia_dia` (asistencia por beca y día), `mv_bi_convocatoria` (embudo, cupos, cobertura, puntaje promedio y becas en riesgo por convocatoria), `mv_bi_carrera_convocatoria` (postulaciones y adjudicadas por facultad y carrera) y `mv_bi_puntaje_convocatoria` (tramos de puntaje), todas con índice único para el refresco `CONCURRENTLY`. La primera (por convocatoria y mes: postulaciones recibidas, finalistas, adjudicadas y, cuando exista el CU-10, asistencia), alimentada por `postulacion`, `finalista`, `adjudicacion` y `publicacion_resultado`. La asistencia sale de `asistencia` y `beca` (CU-10 y CU-23).
- **Códigos de error** (prefijo `DAS`): `ERR_DAS_01` (`404`, gestión o convocatoria inexistente, o la convocatoria no es de la gestión) y `ERR_DAS_02` (`400`, no hay datos que exportar a PDF o Excel). Documentar en `docs/errors/` y su índice al implementar. Un filtro o `sort` no soportado reutiliza el patrón de cada módulo.
- **Endpoints** (solo `DIRECTIVO`): `GET /api/v1/dashboard/indicadores?filter[gestion]=…&filter[convocatoria]=…` (indicadores, series por mes y fecha de actualización) y `GET /api/v1/dashboard/indicadores/pdf` con los mismos filtros. `GET /api/v1/dashboard/bi` (mismos filtros) entrega el análisis BI y `GET /api/v1/dashboard/bi/excel` lo descarga en Excel. `GET /api/v1/dashboard/filtros` entrega las gestiones y, de cada una, sus convocatorias para armar los filtros.
- **Rol y frontend:** el rol `DIRECTIVO` ya existe. Falta su layout y su ruta (`rutaInicioPorRol` hoy lo manda a `/panel`).

## Cambios pendientes en el documento de taller

| Dónde | Hoy dice | Debe decir | Por qué |
| --- | --- | --- | --- |
| `04_detalle_casos_uso.tex`, CU-12, Descripción y paso 3 | Indicadores «por convocatoria y por mes», filtro solo por gestión. | Agregar el filtro opcional por convocatoria (como el mockup 112). | Decisión de Mau. |
| CU-12, Proceso | No tiene descarga. | Agregar el paso 6 y el flujo alternativo A4: descargar el dashboard en PDF (Jasper). | Decisión de Mau (la exportación de BI pasa a este CU). |
| CU-12, Descripción | No aclara qué cuenta cada indicador. | Aclarar: recibidas sin borradores; adjudicadas solo con resultados publicados; asistencia «Sin datos» hasta el CU-10. | Decisiones de Mau. |
| `03_requisitos_especificos.tex`, RF-07 | Solo criterios de visualización y filtro por gestión. | Agregar: filtro por convocatoria y descarga en PDF. | Igual. |
| `CLAUDE.md` del taller, lista de descartados | «exportar reportes (ex CU-36)» como descartado. | Aclarar que el PDF del dashboard vive dentro del CU-12 y del CU-06; no es un CU aparte. | Mismo criterio que el CU-06. |
| CU-12, Descripción y Proceso | Solo indicadores y gráficos por mes. | Agregar el análisis BI (embudo, facultades, convocatorias, puntajes, cobertura, asistencia, tendencia), la descarga en Excel y el filtro cruzado por convocatoria. | Decisión de Mau. |
| Mockup 112 | Menú «Reportes» y gráfico de asistencia. | Quitar «Reportes» y agregar el botón «Descargar PDF». | Decisión de Mau. |

## Decisiones confirmadas

1. **Asistencia sin tablas:** el dashboard se hace completo y la asistencia muestra «Sin datos»; se deja un comentario en el código para completarla al existir el CU-10.
2. **Lote nocturno:** vista materializada refrescada a las 02:00 y al arrancar si está vacía; sin botón manual.
3. **Filtros:** gestión (vigente por defecto) y convocatoria opcional.
4. **Qué cuenta cada indicador:** como en las reglas de negocio.
5. **Asistencia:** con el CU-10 hecho, el dashboard muestra la asistencia real (criterios de tasa y de «beca en riesgo» a validar con la DUBSS).
6. **Alcance:** solo web, sin bitácora, sin proyecciones a futuro; el PDF se descarga con Jasper y el Excel con Apache POI.
7. **Análisis BI (decidido con Mau):** embudo, facultad que más se postula (comparada con adjudicadas y con detalle por carrera), comparación de convocatorias con variación, histograma de puntajes, cobertura de cupos, asistencia real y tendencia entre gestiones; tablas precalculadas por lote; exportación a Excel.

## Desarrollador

Mauro Gutierrez
