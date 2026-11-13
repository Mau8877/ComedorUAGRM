# Caso de uso

| Id   | Nombre                                 | Módulo                         | Prioridad | Estado    |
| ---- | -------------------------------------- | ------------------------------ | --------- | --------- |
| CU12 | Visualizar dashboard de indicadores BI | P6. Reportes y trazabilidad    | Alta      | En progreso |

**Actor principal:** Directivo
**Actores secundarios:** —
**Objetivo:** Que el Directivo consulte, en un dashboard interactivo, los indicadores clave del proceso de la Beca Comedor (postulaciones, finalistas, becas adjudicadas y asistencia) por gestión académica, convocatoria y mes, como apoyo a la toma de decisiones, y pueda descargarlos en PDF.
**Plataforma:** Web
**RF asociados:** RF-07

**Descripción:**
El Directivo entra a «Dashboard de indicadores» y el sistema le muestra, para la gestión académica vigente, cuatro indicadores (postulaciones recibidas, finalistas, becas adjudicadas y asistencia promedio) y dos gráficos por mes (postulaciones y tasa de asistencia), junto con la fecha de la última actualización. Puede cambiar la gestión académica y, opcionalmente, elegir una convocatoria; el dashboard se actualiza con esa selección. Si lo desea, descarga lo que está viendo como un PDF.

Los indicadores **no se calculan al consultar**: salen de una vista precalculada que se refresca por lote cada noche, de modo que la consulta no afecta a las operaciones transaccionales. Es una consulta de **solo lectura**: no modifica ningún dato.

El indicador de **asistencia** depende del registro de asistencia digital ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)), que todavía no existe: hasta entonces muestra «Sin datos». Cuando el CU-10 exista se completa ese indicador sin cambiar el resto del dashboard. Este CU **no** crea el registro de asistencia ni el padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)), no incluye proyecciones ni tendencias y no gestiona la bitácora ([CU-25](CU25%20-%20Consultar%20bitácora%20de%20acciones.md)).

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
6. Si lo desea, el Directivo pulsa «Descargar PDF» (A4): el sistema genera un documento con lo que está viendo y lo descarga.

## Flujos alternativos y excepciones

- **A1 (pasos 2 y 5):** No hay datos para la gestión o la convocatoria elegida → el sistema muestra un mensaje informativo y los indicadores en cero; no es un error.
- **A2 (paso 3):** El proceso por lote aún no se ejecutó → el sistema muestra los últimos indicadores disponibles con su fecha de actualización; si nunca se ejecutó, lo indica en vez de mostrar números inventados.
- **A3 (paso 4):** La gestión o la convocatoria elegida no existe, o la convocatoria no pertenece a la gestión → `404` (`ERR_DAS_01`); el dashboard conserva la última selección válida.
- **A4 (paso 6):** No hay datos que exportar (A1) → `400` (`ERR_DAS_02`); no se genera el PDF y se informa. Si falla la generación del documento → mensaje claro, no se descarga nada y se puede reintentar.
- **A5 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A6 (cualquier paso):** El usuario no tiene el rol Directivo → `403`; no ve la opción ni puede usar los endpoints.
- **A7 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento.

## Reglas de negocio

- **Solo el Directivo** consulta el dashboard (RF-07). El Administrador DUBSS y el Postulante no. *(Si la DUBSS pide que el Administrador también lo vea, se agrega al rol; hoy no.)*
- **Solo lectura.** Consultar o descargar no modifica datos y **no se anota en la bitácora**: los indicadores son agregados, sin datos personales de postulantes.
- **Datos precalculados por lote** (RNF «BI en lote»): una vista materializada `mv_indicadores_convocatoria`, refrescada por una tarea programada **cada noche (02:00)** y también al arrancar el sistema si está vacía. No hay botón de «actualizar ahora». La pantalla muestra siempre la fecha de la última actualización.
- **Qué cuenta cada indicador:**
  - **Postulaciones recibidas:** las postulaciones enviadas de la convocatoria; **no** cuentan los borradores.
  - **Finalistas:** los finalistas de la convocatoria ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)).
  - **Becas adjudicadas:** las adjudicaciones en «adjudicada» solo de convocatorias con **resultados publicados** ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)), para no adelantar lo que aún no es oficial.
  - **Asistencia promedio:** el promedio de asistencia de los becados. Se calcula con el registro de [CU-10](CU10%20-%20Registrar%20asistencia%20digital.md) cuando exista; la fórmula se define con la DUBSS. Mientras tanto muestra «Sin datos».
- **«Por mes»:** las postulaciones se ubican en el mes de su envío; las adjudicaciones, en el mes de su decisión; la asistencia, en el mes del registro.
- **Filtros:** la **gestión académica** (por defecto la vigente, según sus fechas de inicio y fin) y, opcionalmente, una **convocatoria** de esa gestión («Todas» por defecto). Los indicadores de «Todas» son la suma de sus convocatorias.
- **PDF con Jasper:** el documento reproduce lo que ve el Directivo (los cuatro indicadores y los valores por mes de la selección), con la gestión y convocatoria elegidas, la fecha de la última actualización, y la fecha, hora y usuario que lo genera. Sin datos personales.
- **Fuera de este CU:** proyecciones y tendencias, otras exportaciones, el registro de asistencia (CU-10), el cotejo (CU-11), el padrón (CU-23) y la bitácora (CU-25).
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- El Directivo visualiza los indicadores de la selección con su fecha de actualización.
- Si descargó el PDF, tiene el documento; ningún dato del sistema cambió y no se anotó nada en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que el Directivo entra al dashboard, el sistema debe mostrar los indicadores de la gestión académica vigente con su fecha de última actualización.
- **CA02:** Dado que el Directivo elige una gestión, el sistema debe actualizar los cuatro indicadores y los gráficos solo con esa gestión.
- **CA03:** Dado que el Directivo elige una convocatoria de la gestión, el sistema debe mostrar solo los datos de esa convocatoria.
- **CA04:** Dado que una gestión o convocatoria no tiene datos, el sistema debe mostrar un mensaje informativo y los indicadores en cero, sin error.
- **CA05:** Dado que el proceso por lote aún no se ejecutó, el sistema debe mostrar los últimos indicadores disponibles con su fecha, o indicar que aún no hay.
- **CA06:** Dado que hay borradores, el sistema no debe contarlos en «postulaciones recibidas».
- **CA07:** Dada una convocatoria sin resultados publicados, el sistema no debe contar sus adjudicaciones en «becas adjudicadas».
- **CA08:** Dado que aún no existe el registro de asistencia, el sistema debe mostrar «Sin datos» en la asistencia y el resto del dashboard debe funcionar.
- **CA09:** Dada una consulta o una descarga, el sistema no debe modificar ningún dato ni anotar en la bitácora.
- **CA10:** Dado que el usuario no es Directivo, el sistema debe responder `403`; sin sesión válida, `401`; con gestión o convocatoria inexistente, `404`.
- **CA11:** Dado que el Directivo descarga el PDF, el sistema debe entregar un documento con los indicadores de la selección, la fecha de actualización y la fecha, hora y usuario de generación, sin datos personales.
- **CA12:** Dado que no hay datos que exportar, el sistema no debe generar el PDF e informar que no hay datos.
- **CA13:** Dada una gestión con 20 000 postulaciones, el sistema debe mostrar el dashboard en 3 segundos o menos, porque lee la vista precalculada.

## Cambios al diseño lógico

- **Sin tablas transaccionales nuevas.** Se agrega la vista materializada `mv_indicadores_convocatoria` (por convocatoria y mes: postulaciones recibidas, finalistas, adjudicadas y, cuando exista el CU-10, asistencia), alimentada por `postulacion`, `finalista`, `adjudicacion` y `publicacion_resultado`. Al crearse, el código que lee asistencia lleva un comentario: «cuando exista el CU-10 se agrega la asistencia».
- **Códigos de error** (prefijo `DAS`): `ERR_DAS_01` (`404`, gestión o convocatoria inexistente, o la convocatoria no es de la gestión) y `ERR_DAS_02` (`400`, no hay datos que exportar a PDF). Documentar en `docs/errors/` y su índice al implementar. Un filtro o `sort` no soportado reutiliza el patrón de cada módulo.
- **Endpoints** (solo `DIRECTIVO`): `GET /api/v1/dashboard/indicadores?filter[gestion]=…&filter[convocatoria]=…` (indicadores, series por mes y fecha de actualización) y `GET /api/v1/dashboard/indicadores/pdf` con los mismos filtros. `GET /api/v1/dashboard/filtros` entrega las gestiones y, de cada una, sus convocatorias para armar los filtros.
- **Rol y frontend:** el rol `DIRECTIVO` ya existe. Falta su layout y su ruta (`rutaInicioPorRol` hoy lo manda a `/panel`).

## Cambios pendientes en el documento de taller

| Dónde | Hoy dice | Debe decir | Por qué |
| --- | --- | --- | --- |
| `04_detalle_casos_uso.tex`, CU-12, Descripción y paso 3 | Indicadores «por convocatoria y por mes», filtro solo por gestión. | Agregar el filtro opcional por convocatoria (como el mockup 112). | Decisión de Mau. |
| CU-12, Proceso | No tiene descarga. | Agregar el paso 6 y el flujo alternativo A4: descargar el dashboard en PDF (Jasper). | Decisión de Mau (la exportación de BI pasa a este CU). |
| CU-12, Descripción | No aclara qué cuenta cada indicador. | Aclarar: recibidas sin borradores; adjudicadas solo con resultados publicados; asistencia «Sin datos» hasta el CU-10. | Decisiones de Mau. |
| `03_requisitos_especificos.tex`, RF-07 | Solo criterios de visualización y filtro por gestión. | Agregar: filtro por convocatoria y descarga en PDF. | Igual. |
| `CLAUDE.md` del taller, lista de descartados | «exportar reportes (ex CU-36)» como descartado. | Aclarar que el PDF del dashboard vive dentro del CU-12 y del CU-06; no es un CU aparte. | Mismo criterio que el CU-06. |
| Mockup 112 | Menú «Reportes» y gráfico de asistencia. | Quitar «Reportes» y agregar el botón «Descargar PDF». | Decisión de Mau. |

## Decisiones confirmadas

1. **Asistencia sin tablas:** el dashboard se hace completo y la asistencia muestra «Sin datos»; se deja un comentario en el código para completarla al existir el CU-10.
2. **Lote nocturno:** vista materializada refrescada a las 02:00 y al arrancar si está vacía; sin botón manual.
3. **Filtros:** gestión (vigente por defecto) y convocatoria opcional.
4. **Qué cuenta cada indicador:** como en las reglas de negocio.
5. **Asistencia:** es una asistencia normal; queda mejor cubierta cuando exista el CU-10.
6. **Alcance:** solo web, sin bitácora, sin proyecciones; el PDF se descarga con Jasper.

## Desarrollador

Mauro Gutierrez
