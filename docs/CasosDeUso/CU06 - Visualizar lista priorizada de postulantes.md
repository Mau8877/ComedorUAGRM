# Caso de uso

| Id   | Nombre                                     | Módulo                         | Prioridad | Estado    |
| ---- | ------------------------------------------ | ------------------------------ | --------- | --------- |
| CU06 | Visualizar lista priorizada de postulantes | P4. Auditoría y adjudicación   | Alta      | Hecho       |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador DUBSS vea las postulaciones de una convocatoria ordenadas de mayor a menor puntaje de vulnerabilidad, para concentrar la auditoría solo en los postulantes mejor puntuados en vez de revisar expedientes al azar.
**Plataforma:** Web
**RF asociados:** RF-04 (y el requisito de rendimiento de la lista priorizada: respuesta en no más de 3 segundos con hasta 20 000 postulaciones por gestión)

**Descripción:**
El Administrador DUBSS entra a «Lista priorizada» (o llega a ella desde el [CU-05: Auditar y adjudicar becas](CU05%20-%20Auditar%20y%20adjudicar%20becas.md), que lo incluye) y elige una convocatoria. El sistema toma las postulaciones de esa convocatoria que están en estado «calificada» junto con su puntaje de vulnerabilidad **vigente** (el último que calculó el módulo de IA en el [CU-03: Precalificar con IA](CU03%20-%20Precalificar%20con%20IA.md)), las ordena de mayor a menor puntaje y las muestra paginadas, con la posición de cada una, los datos del postulante (nombre, CI, carrera), el puntaje y la fecha en que se calculó. La lista no se guarda: se arma con una consulta cada vez, así que siempre refleja el último puntaje calculado (por ejemplo, después de un reprocesamiento del [CU-26](CU26%20-%20Parametrizar%20y%20reprocesar%20puntajes.md)). El Administrador puede buscar por nombre o CI y filtrar por carrera sin perder la posición real de cada postulante. Desde la lista puede abrir una postulación para continuar con su auditoría en el CU-05. Consultar la lista no modifica ningún dato. Si la convocatoria no tiene postulaciones calificadas, el sistema lo informa y dice cuántas siguen pendientes de precalificación; si la sesión expiró o el usuario no es Administrador DUBSS, no muestra nada.

Depende del puntaje del CU-03 y de las convocatorias del [CU-13](CU13%20-%20Gestionar%20convocatoria.md). Es prerequisito del CU-05 (elegir finalistas) y, a través de él, del [CU-07: Auditar expediente finalista](CU07%20-%20Auditar%20expediente%20finalista.md).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Existe al menos una convocatoria registrada ([CU-13](CU13%20-%20Gestionar%20convocatoria.md)).
- Las postulaciones de la convocatoria fueron precalificadas por el módulo de IA ([CU-03](CU03%20-%20Precalificar%20con%20IA.md)); si ninguna lo fue, se aplica A1.
- **Datos de apoyo mientras no existan los CU de origen:** las convocatorias, postulaciones y puntajes los generan el CU-13, el CU-02 y el CU-03, que todavía no están implementados. Para poder probar este CU, sus unidades de trabajo crean las tablas `convocatoria`, `gestion_academica`, `postulacion` y `puntaje_postulacion` exactamente como están en el diseño lógico de la base de datos, y un cargador de datos de prueba (solo para desarrollo, nunca en producción) con unas pocas convocatorias y postulaciones calificadas, pendientes y de otros estados, con puntajes repetidos y con más de un puntaje por postulación. Cuando el CU-13, el CU-02 y el CU-03 se implementen, ellos amplían esas tablas sin recrearlas.

## Flujo principal

1. El Administrador DUBSS selecciona «Lista priorizada» en el menú (o accede a ella desde el CU-05).
2. El sistema muestra las convocatorias disponibles y deja seleccionada la más reciente (A9).
3. El Administrador DUBSS selecciona una convocatoria.
4. El sistema obtiene las postulaciones de esa convocatoria en estado «calificada» y el puntaje de vulnerabilidad vigente de cada una (A1, A4).
5. El sistema las ordena de mayor a menor puntaje (A3) y muestra la primera página de la lista con: **posición**, **nombre del postulante**, **CI**, **carrera**, **puntaje** y **fecha de cálculo** del puntaje, junto con el total de postulaciones de la lista y cuántas siguen pendientes de precalificación.
6. El Administrador DUBSS recorre las páginas, cambia la cantidad de filas por página (10, 20 o 50) o aplica una búsqueda o un filtro (A2).
7. Si lo desea, el Administrador DUBSS exporta la lista a PDF (A12): el sistema genera un documento con la convocatoria, la fecha de generación, los filtros aplicados y todas las postulaciones de la lista (no solo la página en pantalla), con la misma información y el mismo orden que ve en pantalla, y lo descarga.
8. El Administrador DUBSS selecciona una postulación para ver su detalle y, si corresponde, continuar con su auditoría en el CU-05 (A5).

## Flujos alternativos y excepciones

- **A1 (paso 4):** La convocatoria no tiene postulaciones calificadas → el sistema muestra un mensaje informativo («No hay postulaciones calificadas en esta convocatoria»), indica cuántas siguen pendientes de precalificación y no muestra la tabla.
- **A2 (paso 6):** El Administrador busca por nombre o CI, o filtra por carrera → el sistema vuelve a consultar y muestra solo las postulaciones que cumplen el criterio, **conservando su posición en la lista completa** (por ejemplo, un postulante que es el 37.º de la convocatoria sigue mostrándose como 37.º aunque sea el primero del filtro) y vuelve a la primera página. Si ninguna cumple el criterio, muestra «Sin resultados» y permite limpiar la búsqueda.
- **A3 (paso 5):** Dos o más postulaciones tienen el mismo puntaje → el sistema aplica el criterio de desempate (ver Reglas de negocio): primero la postulación enviada antes y, si también coinciden, la creada antes, de modo que la paginación no repita ni omita postulaciones.
- **A4 (paso 4):** Las postulaciones en estado «borrador», «pendiente», «devuelta», «archivada», «adjudicada» o «no adjudicada» no forman parte de la lista priorizada activa → no se muestran; el sistema informa cuántas siguen «pendiente» de precalificación.
- **A5 (paso 8):** El CU-05 todavía no está disponible o la postulación ya no está «calificada» (otro administrador la archivó, la devolvió o la adjudicó mientras tanto) → el sistema muestra el detalle de solo lectura y, en el segundo caso, informa que la postulación ya no está en la lista activa y la actualiza.
- **A6 (cualquier paso):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario a la pantalla de inicio de sesión.
- **A7 (cualquier paso):** El usuario autenticado no tiene el rol Administrador DUBSS → `403` y el sistema no muestra la lista ni la opción del menú.
- **A8 (cualquier paso):** Falla la conexión con el servidor o con la base de datos, o la consulta excede el tiempo de respuesta → el sistema informa el error con un mensaje claro y permite reintentar; no muestra datos parciales.
- **A9 (paso 2):** No existe ninguna convocatoria → el sistema informa que todavía no hay convocatorias registradas.
- **A10 (paso 3):** El Administrador pide una convocatoria inexistente o eliminada (por ejemplo, con un enlace guardado) → `404` y el sistema vuelve a la selección de convocatoria.
- **A11 (paso 6):** Un reprocesamiento de puntajes (CU-26) termina mientras el Administrador está viendo la lista → al volver a consultar (cambiar de página, filtrar o recargar) la lista muestra los puntajes y posiciones nuevos.
- **A12 (paso 7):** La lista está vacía (A1 o un filtro sin resultados) → el sistema no genera el PDF e informa que no hay postulaciones para exportar. Si falla la generación del documento → informa el error con un mensaje claro, no descarga nada y permite reintentar.

## Reglas de negocio

- **Solo el Administrador DUBSS** consulta la lista priorizada (RF-04). El Directivo no la ve: consume los indicadores agregados del CU-12.
- **Qué entra en la lista:** solo las postulaciones de la convocatoria elegida en estado «calificada» que tienen un puntaje **vigente** (solo hay uno por postulación; los anteriores quedan como historial, RF-21). Estados de `postulacion` que se leen: `calificada` (se muestra) y `pendiente` (solo se cuenta). Este CU **no cambia el estado** de ninguna postulación.
- **Orden:** de mayor a menor puntaje vigente. La **posición** es el lugar de la postulación en la lista completa de la convocatoria (no en la página ni en el resultado de un filtro), y es la que el CU-05 guarda al seleccionar a un finalista.
- **Desempate:** a igual puntaje, primero la postulación enviada antes (`fecha_envio`); si también coincide, la que se creó antes. Así el orden es siempre el mismo. *(Decisión del equipo: la documentación de grado deja el criterio «por definir con la DUBSS» y por ahora la DUBSS no define nada; si define otro, se reemplaza este.)*
- **La lista no se almacena:** se calcula en cada consulta, por eso siempre refleja el último puntaje (incluido el de un reprocesamiento del CU-26).
- **Paginación obligatoria** con 10, 20 o 50 filas por página (20 por defecto) — ver [ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md). Nunca se devuelve la lista completa de una vez.
- **Búsqueda y filtros:** búsqueda por nombre (nombres y apellidos) o CI del postulante, y filtro por carrera. No hay filtro por estado, porque la lista solo contiene postulaciones «calificada».
- **Exportación a PDF:** exporta la lista completa de la convocatoria con los filtros y la búsqueda aplicados (no solo la página visible), ordenada igual que en pantalla y con la posición real de cada postulante. Incluye las mismas columnas que la lista, el nombre de la convocatoria, la fecha y hora de generación y el nombre del administrador que la generó, sin datos socioeconómicos. No lleva encabezado ni firma institucional. *(Decisión del equipo: la documentación de grado no menciona la exportación; el formato PDF lo pidió el equipo.)*
- **Bitácora de la exportación:** como el PDF saca datos personales de los postulantes del sistema, la exportación sí se anota en la bitácora (quién, cuándo, convocatoria y filtros aplicados). *(Decisión propuesta, por revisar.)*
- **Rendimiento:** la lista se muestra en **no más de 3 segundos** con hasta **20 000 postulaciones** por gestión y al menos 50 administradores consultando a la vez (requisitos de rendimiento del capítulo 4).
- **Convocatorias consultables:** cualquier convocatoria no eliminada, abierta o cerrada; se preselecciona la más reciente. *(Decisión propuesta, por revisar: la documentación de grado solo exige «al menos una convocatoria registrada»; el CU-05 sí exige que esté cerrada para adjudicar.)*
- **Datos que se muestran:** solo los necesarios para priorizar (nombre, CI, carrera, puntaje, fecha de cálculo). Las respuestas socioeconómicas y los documentos adjuntos del expediente se ven en la auditoría (CU-07), no en la lista, por la política de protección de datos personales de la UAGRM.
- **Bitácora de la consulta:** consultar la lista en pantalla no se anota en la bitácora (es una lectura que no modifica nada). *(Decisión propuesta, por revisar con la DUBSS si quiere registrar quién consultó datos de postulantes.)*
- Los mensajes al usuario están en español y no exponen detalles técnicos.
- **Sin definición de la DUBSS por ahora:** el desempate, el registro en la bitácora de quién consulta la lista y el encabezado del PDF quedan como decisiones del equipo (arriba) y se ajustan si la DUBSS define algo distinto.

## Postcondiciones

- El Administrador DUBSS visualiza la lista priorizada de la convocatoria elegida, ordenada por el puntaje vigente; la consulta no modifica ningún dato del sistema.

## Criterios de aceptación

- **CA01:** Dado que el Administrador DUBSS elige una convocatoria con postulaciones calificadas, el sistema debe mostrarlas ordenadas de mayor a menor puntaje vigente, con posición, nombre, CI, carrera, puntaje y fecha de cálculo.
- **CA02:** Dado que una postulación tiene varios puntajes calculados (por ejemplo, tras un reprocesamiento), el sistema debe mostrarla una sola vez y con su puntaje vigente.
- **CA03:** Dado que la convocatoria tiene postulaciones en estado «borrador», «pendiente», «devuelta», «archivada», «adjudicada» o «no adjudicada», el sistema no debe incluirlas en la lista, y debe informar cuántas siguen «pendiente».
- **CA04:** Dado que la convocatoria no tiene postulaciones calificadas, el sistema debe mostrar un mensaje informativo y la cantidad de postulaciones pendientes de precalificación.
- **CA05:** Dado que la lista tiene más postulaciones que el tamaño de página, el sistema debe paginarla (10, 20 o 50 por página, 20 por defecto) e informar el total de postulaciones y de páginas.
- **CA06:** Dado que el Administrador pasa a la página 2 con 20 filas por página, la primera fila debe tener la posición 21.
- **CA07:** Dado que el Administrador busca por nombre o CI, o filtra por carrera, el sistema debe mostrar solo las postulaciones que cumplen el criterio, cada una con su posición en la lista completa de la convocatoria.
- **CA08:** Dado que dos postulaciones tienen el mismo puntaje, el sistema debe ordenarlas siempre de la misma forma, de modo que al recorrer todas las páginas ninguna postulación aparezca dos veces ni falte.
- **CA09:** Dado que una convocatoria tiene 20 000 postulaciones calificadas, el sistema debe mostrar cualquier página de la lista en no más de 3 segundos.
- **CA10:** Dado que se reprocesaron los puntajes (CU-26), el sistema debe mostrar los puntajes y posiciones nuevos en la siguiente consulta.
- **CA11:** Dado que el usuario está autenticado pero no tiene el rol Administrador DUBSS, el sistema debe responder `403` y no mostrar la lista.
- **CA12:** Dado que la sesión expiró o no es válida, el sistema debe responder `401` y llevar al usuario al inicio de sesión.
- **CA13:** Dado que se pide una convocatoria inexistente o eliminada, el sistema debe responder `404`.
- **CA14:** Dado cualquier consulta o exportación de la lista, el sistema no debe modificar el estado de ninguna postulación ni ningún otro dato (salvo la anotación de la exportación en la bitácora).
- **CA15:** Dado que el Administrador selecciona una postulación de la lista, el sistema debe mostrar su detalle y permitir continuar con su auditoría en el CU-05 cuando ese CU esté disponible.
- **CA16:** Dado que el Administrador exporta la lista a PDF, el sistema debe descargar un documento con todas las postulaciones de la lista (no solo la página visible), en el mismo orden, con la misma información y con la posición real de cada una.
- **CA17:** Dado que hay una búsqueda o un filtro aplicado, el PDF debe contener solo las postulaciones que cumplen ese criterio e indicar los filtros usados.
- **CA18:** Dado que el PDF se genera, debe mostrar la convocatoria, la fecha y hora de generación y quién lo generó, y no debe incluir datos socioeconómicos.
- **CA19:** Dado que la lista está vacía, el sistema no debe generar el PDF e informar que no hay postulaciones para exportar.
- **CA20:** Dado que el Administrador exporta la lista, el sistema debe anotarlo en la bitácora con el usuario, la fecha, la convocatoria y los filtros aplicados.
- **CA21:** Dado que se exporta una convocatoria con 20 000 postulaciones calificadas, el sistema debe entregar el PDF sin fallar ni dejar la pantalla bloqueada. *(El tiempo máximo aceptable se define con el equipo; el requisito de 3 segundos aplica a la consulta en pantalla.)*

## Desarrollador

Mauro Gutierrez
