# Caso de uso

| Id   | Nombre                              | Módulo                       | Prioridad | Estado    |
| ---- | ----------------------------------- | ---------------------------- | --------- | --------- |
| CU22 | Publicar resultados de adjudicación | P4. Auditoría y adjudicación | Alta      | Hecho |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Postulante (recibe el aviso de su resultado y consulta la lista de adjudicados, a través del servicio de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md))
**Objetivo:** Que el Administrador DUBSS dé a conocer de forma oficial y transparente los resultados de la adjudicación de una convocatoria, avise a cada finalista su resultado y cierre el proceso de selección.
**Plataforma:** Web (publicación); web y móvil (consulta del postulante)
**RF asociados:** RF-17

**Descripción:**
Cuando las decisiones de adjudicación de una convocatoria están tomadas ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)), el Administrador DUBSS entra a «Publicar resultados», revisa el resumen (adjudicados y no adjudicados con su puntaje final) y confirma. El sistema verifica que **ningún finalista quede en «calificada»**, registra la publicación (quién y cuándo), la anota en la bitácora y **avisa a cada finalista su resultado** por bandeja, push y correo. Desde ese momento los resultados son oficiales: la convocatoria queda «publicada», no se registran ni modifican decisiones ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md), [CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md), [CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md)) y el postulante empieza a ver su resultado en [CU-04](CU04%20-%20Consultar%20estado%20de%20postulación.md) y la **lista de adjudicados** de la convocatoria.

Hasta aquí el CU-05 no avisó a nadie: el aviso de «adjudicada» o «no adjudicada» se envía **solo al publicar**, para que nadie se entere antes de la publicación oficial. Este CU extiende el CU-05 y **no genera el acta** ([CU-08](CU08%20-%20Generar%20acta%20de%20adjudicación.md)) ni toca el padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- La convocatoria está **cerrada** y tiene finalistas.
- **Ningún finalista de la convocatoria está en «calificada»**: cada uno está adjudicado, no adjudicado, archivado o devuelto. *(Decisión 7 del CU-05.)*
- Los resultados de la convocatoria **no** se han publicado antes.
- **No se exige el acta** (CU-08): mientras el CU-08 no exista, publicar no depende de ella. *(Decisión de Mau.)*

## Flujo principal

1. El Administrador DUBSS entra a «Publicar resultados» y elige la convocatoria (A2).
2. El sistema muestra el resumen: adjudicados y no adjudicados con el puntaje final de cada finalista, y cuántas decisiones hay en cada caso.
3. El Administrador revisa y pulsa «Publicar» (A4).
4. El sistema muestra una ventana de confirmación que avisa que la publicación **no se puede deshacer** y que se avisará a los finalistas.
5. El Administrador confirma (A4).
6. El sistema valida que la convocatoria exista (A3), que no esté publicada (A2) y que no queden finalistas en «calificada» (A1).
7. El sistema registra la publicación (usuario y fecha) y la anota en la bitácora, en una misma operación (A8).
8. El sistema crea un aviso por cada finalista con decisión de adjudicación y se lo envía por bandeja, push y correo (A5).
9. El sistema confirma la publicación; la lista de adjudicados queda consultable por los postulantes (CU-04).

## Flujos alternativos y excepciones

- **A1 (paso 6):** Hay finalistas en «calificada» → `409` (`ERR_PUB_01`); no se publica y se indica **cuántos faltan**.
- **A2 (pasos 1 y 6):** Los resultados ya estaban publicados → `409` (`ERR_PUB_02`); se muestra la publicación existente (quién y cuándo) y no se registra otra. Un doble clic o dos administradores a la vez reciben este mismo `409`.
- **A3 (paso 6):** La convocatoria no existe → `404` (`ERR_PRI_01`); no está cerrada → `409` (`ERR_PUB_03`); se vuelve a la lista de convocatorias.
- **A4 (pasos 3 y 5):** El Administrador cancela → no se publica nada.
- **A5 (paso 8):** Falla el envío de un aviso → la publicación queda registrada, el aviso queda en la bandeja y CU-EXTRA-01 reintenta el push y el correo. Un fallo de envío nunca deshace la publicación.
- **A6 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A7 (cualquier paso):** El usuario no es Administrador DUBSS → `403`; no ve la opción ni puede usar el endpoint.
- **A8 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento; la publicación y la bitácora se guardan juntas o no se guarda ninguna.

## Reglas de negocio

- **Solo el Administrador DUBSS** publica (RF-17). El Postulante solo recibe el aviso y consulta.
- **Qué se exige para publicar:** ningún finalista en «calificada». No se exige el acta (CU-08) ni que haya adjudicados mínimos. *(Decisión 7 del CU-05 y decisión de Mau.)*
- **Una publicación por convocatoria.** Es definitiva: **no existe despublicar** ni volver a publicar (decisión de Mau).
- **Sin adjudicados también se publica:** si ningún finalista resultó adjudicado, la publicación se permite igualmente y cada finalista recibe su aviso de «no adjudicada».
- **A quién se avisa:** a cada **finalista con decisión de adjudicación** (adjudicado o no adjudicado), cada uno con **su** resultado. Los archivados y rechazados (CU-19, CU-20) ya fueron avisados en su momento y no se les avisa de nuevo; los devueltos tampoco.
- **Canales del aviso:** bandeja, push y correo, siempre. Dos eventos nuevos del catálogo de CU-EXTRA-01, con `{nombres}` y `{convocatoria}`: `BECA_ADJUDICADA` (tipo `SUCCESS`) y `BECA_NO_ADJUDICADA` (tipo `INFO`).
- **Textos de los avisos:** título «Beca Comedor: resultado de tu postulación». Adjudicada: «{nombres}, ¡felicidades! Tu postulación en {convocatoria} fue adjudicada: obtuviste la Beca Comedor.» No adjudicada: «{nombres}, tu postulación en {convocatoria} no fue adjudicada. Gracias por participar.»
- **Envío por lotes, sin N+1:** los destinatarios se obtienen en una sola consulta, las notificaciones se guardan por lotes y los tokens de dispositivo se resuelven en bloque; ningún paso consulta dentro de un bucle.
- **Qué ve el postulante:** su propio resultado (CU-04) y la **lista de adjudicados** de la convocatoria. De cada adjudicado se muestra **nombre y apellido, nota, carrera y facultad**, donde **nota es el puntaje final** de la adjudicación. Todo lo demás es privado y no se comparte (CI, correo, ingresos y datos socioeconómicos). *(Decisión de Mau.)* Los no adjudicados **no** aparecen en la lista; cada uno ve solo su propio resultado.
- **Motivo de «no adjudicada»:** el motivo registrado en el CU-05 lo ve solo el Administrador; el postulante ve el resultado, no el motivo de la lista.
- **Puntaje en la pantalla del Administrador:** se usa el **puntaje final** guardado en la adjudicación (inmutable), no un cálculo nuevo.
- **Qué se guarda:** una fila en `publicacion_resultado` (una por convocatoria): `convocatoria_id`, `publicado_por`, `publicado_en`.
- **Después de publicar:** no se registran ni modifican decisiones (CU-05, CU-19, CU-20 responden `409`). La convocatoria queda «publicada» y nada más cambia de estado: las postulaciones **no** cambian de estado al publicar.
- **Bitácora:** la publicación queda anotada (quién, cuándo, código de convocatoria, cantidad de adjudicados y no adjudicados), **sin** nombres, CI, correos ni tokens.
- **Comunicados:** al publicar se habilita el destinatario `ADJUDICADOS` en los comunicados del CU-21. *(Pendiente del CU-05, decisión 10.)*
- **Acta (CU-08):** por ahora publicar no la exige; cuando el CU-08 exista se definirá si pasa a ser precondición.
- **Fuera de este CU:** acta de adjudicación (CU-08), padrón de becados (CU-23), apelación del postulante, lista de espera de suplentes y reportes.
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- La convocatoria queda «publicada» y la publicación registrada con su responsable y fecha.
- Cada finalista con decisión tiene su aviso en la bandeja (y el push y el correo, si pudieron enviarse).
- La lista de adjudicados es consultable por los postulantes y cada postulante ve su resultado.
- La acción queda en la bitácora; ninguna postulación cambia de estado.

## Criterios de aceptación

- **CA01:** Dado que no quedan finalistas en «calificada» y la convocatoria no está publicada, el sistema debe registrar la publicación con usuario y fecha.
- **CA02:** Dado que quedan finalistas en «calificada», el sistema debe responder `409` e indicar cuántos faltan, sin publicar.
- **CA03:** Dado que los resultados ya estaban publicados, el sistema debe responder `409` y mostrar la publicación existente.
- **CA04:** Dado que la misma petición llega dos veces o dos administradores actúan a la vez, el sistema debe registrar una sola publicación y un solo envío de avisos, y responder `409` a la otra.
- **CA05:** Dada una publicación registrada, el sistema debe avisar a cada finalista con decisión su resultado por bandeja, push y correo.
- **CA06:** Dado un finalista archivado, rechazado o devuelto, el sistema no debe volver a avisarle al publicar.
- **CA07:** Dado que falla un envío, el sistema debe conservar la publicación y dejar el aviso en la bandeja.
- **CA08:** Dado que el Administrador cancela en la confirmación, el sistema no debe publicar nada.
- **CA09:** Dados los resultados publicados, el sistema debe rechazar con `409` registrar o modificar decisiones (CU-05, CU-19, CU-20).
- **CA10:** Dados los resultados publicados, el postulante debe ver su resultado y la lista de adjudicados con nombre y apellido, nota, carrera y facultad, sin los no adjudicados.
- **CA11:** Dada una publicación registrada, el sistema debe anotarla en la bitácora sin nombres, CI, correos ni tokens.
- **CA12:** Dado que el acta (CU-08) no existe o no se generó, el sistema debe permitir publicar igualmente.
- **CA13:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`; con convocatoria inexistente, `404`.
- **CA14:** Dada una publicación registrada, el sistema no debe cambiar el estado de ninguna postulación ni crear el padrón de becados.
- **CA15:** Dado que ningún finalista resultó adjudicado, el sistema debe permitir publicar y avisar a cada finalista su resultado.
- **CA16:** Dada una publicación registrada, el sistema no debe permitir despublicarla ni publicarla de nuevo.
- **CA17:** Dada una convocatoria con muchos finalistas, el sistema debe avisar por lotes, con un número constante de consultas sin importar la cantidad de finalistas.
- **CA18:** Dada la lista pública, el sistema debe mostrar solo nombre y apellido, nota (puntaje final), carrera y facultad.

## Cambios al diseño lógico

- **Tabla ya prevista:** `publicacion_resultado` (`id`, `convocatoria_id` FK única, `publicado_por`, `publicado_en`). Es la marca que el CU-05 y el CU-20 consultan hoy con `resultadosPublicados` (devuelve siempre `false` con un `TODO` hacia este CU): al implementar, se conecta a la fila real.
- **Eventos de notificación:** `BECA_ADJUDICADA` y `BECA_NO_ADJUDICADA` (ver reglas de negocio).
- **Códigos de error** (prefijo `PUB`): `ERR_PUB_01` (`409`, quedan finalistas en «calificada»), `ERR_PUB_02` (`409`, ya publicada), `ERR_PUB_03` (`409`, convocatoria no cerrada) y `ERR_PUB_04` (`404`, los resultados aún no están publicados). Convocatoria inexistente reutiliza `ERR_PRI_01`. Documentarlos en `docs/errors/` y su índice al implementar.
- **Endpoints** (todos paginados donde devuelven colección):
  - Administrador: `GET /api/v1/convocatorias/{codigo}/resultados/resumen`, `GET /api/v1/convocatorias/{codigo}/resultados/decisiones` (con `filter[resultado]`) y `POST /api/v1/convocatorias/{codigo}/resultados/publicar`.
  - Postulante: `GET /api/v1/resultados/convocatorias` (convocatorias con resultados publicados), `GET /api/v1/convocatorias/{codigo}/resultados` (lista de adjudicados) y `GET /api/v1/convocatorias/{codigo}/resultados/mio` (su propio resultado).

## Cambios pendientes en el documento de taller

| Dónde | Hoy dice | Debe decir | Por qué |
| --- | --- | --- | --- |
| `04_detalle_casos_uso.tex`, CU-22, Descripción | «…notifica a cada postulante su resultado mediante notificación push… las adjudicaciones no se modifican sin dejar registro en la bitácora.» | «…avisa a cada finalista con decisión su resultado por bandeja, push y correo… tras publicar **no se registran ni modifican** decisiones.» | Alineado con CU-05 (decisiones 3 y 6) y con CU-19 y CU-20. |
| CU-22, Precondición 2 | «Todas las adjudicaciones de los finalistas… decididas (CU-05).» | «Ningún finalista queda en «calificada»: cada uno está adjudicado, no adjudicado, archivado o devuelto.» | Decisión 7 del CU-05. |
| CU-22, paso 4 | «…no existan adjudicaciones pendientes de decisión.» | «…no quede ningún finalista en «calificada».» | Igual. |
| CU-22, paso 6 y A3 | «…crea una notificación para cada postulante finalista…» | «…para cada finalista con decisión de adjudicación; archivados, rechazados y devueltos no se avisan de nuevo.» | Ya fueron avisados por CU-19 y CU-20. |
| CU-22, paso 7 | «…la lista de adjudicados queda consultable por los postulantes (CU-04).» | Agregar: «con nombre y apellido, nota, carrera y facultad; los no adjudicados no aparecen en la lista.» | Decisión de Mau. |
| CU-22, Postcondición 2 | «…los adjudicados conforman el padrón de becados (CU-23).» | Quitar la parte del padrón: «Cada finalista con decisión es notificado de su resultado.» El padrón es del CU-23. | Este CU solo hace lo que describe. |
| CU-22, Postcondición 3 | «…no se modifican sin dejar registro en la bitácora (CU-25).» | «Tras publicar no se registran ni modifican decisiones.» | CU-05 decisión 6. |
| CU-22, Excepciones | Menciona «Adjudicaciones pendientes de decisión». | «Finalistas en «calificada» pendientes de decisión». | Consistencia. |
| CU-22, Propósito/Precondición | No menciona el acta. | Aclarar que **no** exige el acta del CU-08. | Decisión de Mau (CU-08 aún en consulta). |
| `03_requisitos_especificos.tex`, RF-17, criterio 1 | «…solo cuando todas las adjudicaciones estén decididas.» | «…solo cuando ningún finalista esté en «calificada».» | Igual. |
| RF-17, criterios 2–3 | «La lista publicada incluye a los adjudicados… al publicar notifica a cada postulante.» | «…con nombre y apellido, nota, carrera y facultad; avisa a cada finalista con decisión por bandeja, push y correo.» | Decisiones de Mau. |
| RF-17, criterio 4 | «…no se modifica sin dejar registro en la bitácora (RF-20).» | «Una vez publicada, no se registran ni modifican decisiones.» | CU-05 decisión 6. |
| CU-21 (comunicados) | Destinatarios sin `ADJUDICADOS` habilitado. | Habilitar `ADJUDICADOS` al publicar. | Pendiente del CU-05. |
| Diagrama de análisis CU-22 / `02_analisis_casos_uso.tex` §6.2.22 | El push es «un mensaje del propio controlador (2.4)». | El aviso lo envía el servicio de notificaciones (CU-EXTRA-01), por bandeja, push y correo. | Mismo criterio que CU-19/CU-20. |
| `diseno_base_de_datos.md`, `notificacion` | Tipo `RESULTADO` sin evento. | Agregar `BECA_ADJUDICADA` y `BECA_NO_ADJUDICADA` al catálogo de eventos. | Eventos nuevos. |

## Decisiones confirmadas

1. **Nota** es el puntaje final de la adjudicación.
2. **Privacidad:** la lista pública muestra solo nombre y apellido, nota, carrera y facultad; CI, correo e ingresos no se comparten.
3. **Textos de los avisos:** los de las reglas de negocio.
4. **Despublicar** no existe.
5. **Sin adjudicados** se publica igualmente.
6. **Acta (CU-08):** no se exige por ahora; se retomará cuando el CU-08 exista.
7. **Avisos:** siempre por lotes, sin N+1.

## Desarrollador

Mauro Gutierrez
