# Caso de uso

| Id   | Nombre                       | Módulo                       | Prioridad | Estado    |
| ---- | ---------------------------- | ---------------------------- | --------- | --------- |
| CU07 | Auditar expediente finalista | P4. Auditoría y adjudicación | Alta      | Pendiente |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Postulante (recibe el aviso del resultado, a través del servicio de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md))
**Objetivo:** Que el Administrador DUBSS revise el expediente de un finalista, coteje su documentación física con la digital y los datos que declaró, y deje registrado el resultado de la auditoría (validado, observado o no válido), sus observaciones y el puntaje académico (la parte manual del puntaje), para decidir con respaldo si se le adjudica la beca.
**Plataforma:** Web
**RF asociados:** RF-04

**Descripción:**
El Administrador DUBSS elige un finalista de la lista de finalistas de una convocatoria (la pantalla del [CU-21](CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md), que gana una columna «Auditoría» y una acción «Auditar») y abre su expediente. El sistema muestra los datos del postulante, sus respuestas socioeconómicas, los documentos digitales que adjuntó (con su estado «Recibido» o «Falta» frente a lo que exige la convocatoria y la posibilidad de abrirlos), el puntaje de vulnerabilidad vigente calculado por la IA (hasta 70 puntos) con su desglose, el estado de la entrega de la documentación física y las auditorías anteriores, si las hay. Desde ahí puede notificar al finalista y registrar la recepción de su documentación física, que resuelve el CU-21 (incluido por este CU). Con la documentación física recibida, el Administrador la coteja con los documentos digitales y los datos declarados y registra el resultado de la auditoría —«Validado», «Observado» o «No válido»—, sus observaciones y el **puntaje académico** (hasta 30 puntos, manual). El sistema guarda la auditoría con su resultado, observaciones, puntaje académico, auditor y fecha, la anota en la bitácora y avisa al postulante; si el resultado es «Validado», el finalista queda habilitado para la decisión de adjudicación del [CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md). Una nueva auditoría del mismo finalista (por ejemplo, después de una devolución) no borra las anteriores. Si el expediente tiene defectos subsanables, el Administrador puede devolver la postulación ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)); si la documentación no es válida o el finalista no entregó en plazo, puede archivarla o rechazarla ([CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md)). Este CU **no cambia el estado de ninguna postulación**: devolver, archivar o rechazar son CU propios.

Es incluido por el CU-05, que elige a los finalistas a partir de la lista del [CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md), e incluye al CU-21. Lo extienden el CU-19 y el CU-20.

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- La postulación fue seleccionada como finalista (CU-05) y está en estado «calificada».
- Los documentos digitales de la postulación están almacenados en MinIO (CU-16); si alguno falta, el expediente lo muestra como «Falta».
- **Datos de apoyo (semilla):** el CU-02 y el CU-16 (postulación y documentos adjuntos) los implementa otro integrante del equipo, y el CU-05 (finalistas) se hará más adelante; este CU **no cuenta con ellos**. Para poder probarlo, sus unidades de trabajo crean la tabla `auditoria_expediente` tal como está en el diseño lógico y un cargador de datos de prueba (solo desarrollo, nunca en producción, como en el CU-21) con finalistas que tienen respuestas socioeconómicas y documentos adjuntos de ejemplo (con sus archivos en MinIO), con la entrega física registrada y sin registrar, y con auditorías previas. Si las tablas `tipo_documento`, `convocatoria_documento`, `documento_adjunto` o `respuesta_socioeconomica` todavía no existen cuando se implemente, las UT las crean exactamente como están en el diseño lógico; los CU-02 y CU-16 las **amplían** sin recrearlas.

## Flujo principal

1. El Administrador DUBSS entra a «Finalistas» en el menú (o llega desde la adjudicación, CU-05) y elige una convocatoria (A9).
2. El sistema muestra los finalistas de la convocatoria con su posición, nombre, puntaje, estado de la entrega física y resultado de su última auditoría («Sin auditar», «Validado», «Observado» o «No válido») en la columna «Auditoría» (A10).
3. El Administrador selecciona «Auditar» en la fila de un finalista y abre su expediente (A11).
4. El sistema muestra el expediente: datos del postulante (nombre, CI, carrera, posición), respuestas socioeconómicas, documentos digitales con su estado («Recibido» o «Falta») y acceso para abrir cada uno (A4), puntaje de vulnerabilidad vigente de la IA con su desglose (solo lectura), estado de la entrega de la documentación física y auditorías anteriores con su puntaje académico.
5. Si todavía no lo hizo, el Administrador notifica al finalista el plazo y los documentos físicos que debe presentar, y registra la recepción de la documentación cuando la entrega (flujos A y B del CU-21, incluido por este CU) (A1).
6. El Administrador coteja la documentación física entregada con los documentos digitales y los datos declarados.
7. El Administrador registra el resultado de la auditoría (**Validado**, **Observado** o **No válido**), sus observaciones y el **puntaje académico** de 0 a 30 (A2, A6, A7, A8).
8. El sistema guarda la auditoría con su resultado, observaciones, puntaje académico, auditor y fecha, y la anota en la bitácora (A5, A12).
9. El sistema avisa al postulante el resultado (bandeja y push, A16), muestra el expediente como auditado con el **puntaje total** (IA + académico) y, si el resultado es «Validado», deja al finalista habilitado para la decisión de adjudicación (CU-05).

## Flujos alternativos y excepciones

- **A1 (paso 5):** El finalista no entrega la documentación física dentro del plazo → este CU solo muestra que la entrega sigue «Pendiente» y no permite registrar la auditoría (A6); el Administrador puede archivar o rechazar la postulación (CU-20).
- **A2 (paso 7):** El expediente tiene defectos subsanables → el Administrador puede registrar el resultado «Observado» con sus observaciones y devolver la postulación para su corrección (CU-19). Este CU no la devuelve por sí mismo.
- **A3 (paso 7):** La documentación no es válida o la postulación no procede → el Administrador puede registrar «No válido» y archivar o rechazar la postulación (CU-20). Este CU no la archiva ni la rechaza.
- **A4 (paso 4):** Un documento digital no puede recuperarse del almacenamiento → el sistema informa el error con un mensaje claro y permite reintentar la descarga; el resto del expediente sigue disponible.
- **A5 (paso 8):** El finalista ya tenía una auditoría (por ejemplo, después de una devolución) → el sistema registra la nueva y conserva las anteriores; la que vale para la adjudicación es la última.
- **A6 (paso 7):** La documentación física todavía no está registrada como recibida → `409`; el sistema no registra la auditoría e indica que primero hay que registrar la entrega (CU-21).
- **A7 (paso 7):** Falta el resultado; o es «Observado» o «No válido» y las observaciones están vacías; o superan 500 caracteres; o es «Validado» y falta el puntaje académico; o el puntaje académico no está entre 0 y 30 → `400`; el sistema no guarda nada e indica los campos que faltan o son inválidos.
- **A8 (paso 7):** La postulación ya no está «calificada» (otro administrador la devolvió, archivó o adjudicó mientras tanto) → `409`; el sistema no registra la auditoría, informa el estado actual y actualiza el expediente.
- **A9 (paso 1):** No existe ninguna convocatoria, o la elegida ya no existe o fue eliminada → mensaje informativo, o `404` y vuelta a la selección.
- **A10 (paso 2):** La convocatoria no tiene finalistas → el sistema informa «Todavía no hay finalistas en esta convocatoria» y no muestra la tabla.
- **A11 (paso 3):** El finalista no existe o fue eliminado (por ejemplo, con un enlace guardado) → `404` y el sistema vuelve a la lista.
- **A12 (paso 8):** La misma petición llega duplicada (doble clic o reintento de red) → el sistema la procesa una sola vez; no queda una segunda auditoría idéntica (idempotencia, RNF-03).
- **A13 (cualquier paso):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario al inicio de sesión.
- **A14 (cualquier paso):** El usuario no tiene el rol Administrador DUBSS → `403`; no ve la pantalla ni puede usar los endpoints.
- **A15 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y posibilidad de reintentar, sin datos a medias.
- **A16 (paso 9):** Falla el envío del aviso al postulante → la auditoría queda registrada y el aviso en su bandeja; el servicio de CU-EXTRA-01 reintenta el envío y el Administrador no pierde lo que hizo.

## Reglas de negocio

- **Solo el Administrador DUBSS** ejecuta este CU (RF-04). El Postulante no ve su propia auditoría.
- **Qué se guarda de una auditoría** (diseño lógico, `auditoria_expediente`): el finalista, el auditor (el usuario que la registra), la fecha y hora, el resultado (`VALIDADO`, `OBSERVADO` o `NO_VALIDO`), las observaciones y el **puntaje académico** (columna nueva, ver Cambios al diseño lógico). Es **append-only**: una auditoría no se edita ni se borra; corregir un resultado es registrar una nueva.
- **Una finalista puede tener varias auditorías.** La que vale es la **más reciente**. Solo un resultado «Validado» en la última auditoría habilita la adjudicación (CU-05, A2).
- **Observaciones:** máximo **500 caracteres** *(definido por el equipo)*; opcionales si el resultado es «Validado», obligatorias si es «Observado» o «No válido». *(Obligatoriedad: decisión propuesta, por revisar; el CU-19 sí exige observaciones al devolver.)*
- **Puntaje académico (manual):** el máximo de 100 puntos se reparte en **hasta 70 que calcula la IA** ([CU-03](CU03%20-%20Precalificar%20con%20IA.md), a cargo de otro integrante) y **hasta 30 de rendimiento académico que registra el auditor** en esta auditoría, con hasta dos decimales. Es obligatorio si el resultado es «Validado» (sin él no hay puntaje total para adjudicar) y opcional en los demás. **No hay fórmula**: el auditor escribe directamente el valor de 0 a 30, y es lo único numérico que ingresa. La IA califica solo de 0 a 70. *(Decisión del equipo.)*
- **Puntaje total:** es el puntaje vigente de la IA **más** el puntaje académico de la **última** auditoría; **no se guarda**, se calcula. Si un reprocesamiento ([CU-26](CU26%20-%20Parametrizar%20y%20reprocesar%20puntajes.md)) cambia el puntaje de la IA, el total se recalcula; el puntaje académico solo cambia con una nueva auditoría. El CU-05 toma este total para compararlo con el mínimo y copiarlo como puntaje final al adjudicar. El puntaje de la IA **no se modifica** en este CU.
- **Solo se audita con la entrega física registrada** (`finalista.documentacion_recibida_en` con valor): el cotejo es contra los documentos físicos. *(Decisión propuesta, por revisar: la documentación de grado ordena notificar y registrar la recepción antes del cotejo, pero no dice que el sistema lo exija.)*
- **Solo se audita una postulación «calificada».** El CU **no cambia su estado** ni escribe en `postulacion_historial`; el estado lo cambian el CU-19 (devuelta), el CU-20 (archivada o rechazada) y el CU-05 (adjudicada o no adjudicada).
- **Qué muestra el expediente:** datos del postulante, respuestas socioeconómicas, documentos adjuntos y su estado frente a lo que exige la convocatoria, puntaje vigente de la IA con su desglose (el `detalle` que guarda el módulo de IA; si no existe, solo el total), estado de la entrega física y auditorías anteriores con su puntaje académico y el total.
- **Documentos digitales:** viven en **MinIO**; el sistema solo guarda su ruta, tipo y tamaño (ver [PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)); el Administrador los abre con un acceso temporal, nunca con una ruta pública permanente.
- **Estado de cada documento digital:** «Recibido» si la postulación tiene un adjunto de ese tipo, «Falta» si la convocatoria lo exige (`convocatoria_documento`) y no hay adjunto. El estado de los documentos **físicos** no se guarda por documento: el resultado del cotejo es la auditoría. *(Decisión del equipo: el diseño lógico no tiene una tabla de calificación por documento.)*
- **Aviso al postulante:** cada vez que se registra una auditoría el postulante recibe un aviso (bandeja y push, sin correo) con el resultado y, si es «Observado» o «No válido», con las observaciones, para que sepa qué corregir. Usa el servicio de CU-EXTRA-01; un fallo de envío nunca deshace la auditoría. *(Decisión del equipo: cada cambio se avisa al postulante.)*
- **Postulación devuelta y reenviada:** si el postulante corrige y reenvía (CU-19, CU-02), el expediente sigue **el mismo flujo definido aquí**: se vuelve a notificar la entrega si hace falta, se registra la entrega y se registra una nueva auditoría, conservando las anteriores. *(Decisión del equipo; cómo se vuelve a elegir al finalista lo define el CU-05.)*
- **Bitácora:** cada auditoría registrada queda anotada (quién, cuándo, código del finalista y resultado, sin nombres ni CI). Las observaciones viven en la auditoría, no en el detalle de la bitácora. Nunca contraseñas, tokens ni rutas de almacenamiento. *(Decisión propuesta, por revisar con la DUBSS.)*
- **Consultar el expediente no se anota en la bitácora** (es una lectura). *(Decisión propuesta, por revisar con la DUBSS: el expediente incluye datos socioeconómicos, a diferencia de la lista priorizada del CU-06.)*
- **Lista de finalistas:** la pantalla del CU-21 gana la columna «Auditoría» y la acción «Auditar» que abre el expediente; no hay una lista aparte. *(Decisión del equipo.)* Esa lista sigue mostrando el puntaje de la IA, que es el que ordena la lista priorizada (CU-06).
- **Datos personales:** solo los ve el Administrador DUBSS, por la política de protección de datos de la UAGRM; ningún dato socioeconómico viaja a las demás pantallas.
- **Fuera de este CU:** devolver, archivar o rechazar (CU-19, CU-20), elegir finalistas y adjudicar (CU-05), calificar documento por documento, la fórmula del puntaje académico y exportar el expediente.
- Todos los mensajes al usuario están en español y no exponen detalles técnicos.

## Postcondiciones

- El expediente del finalista tiene registrada una nueva auditoría con su resultado, observaciones, auditor y fecha.
- Si el resultado es «Validado», el finalista queda habilitado para la decisión de adjudicación (CU-05).
- La acción queda en la bitácora; ninguna postulación cambia de estado.

## Criterios de aceptación

- **CA01:** Dado que el Administrador abre el expediente de un finalista, el sistema debe mostrar los datos del postulante, sus respuestas socioeconómicas, sus documentos digitales con su estado, el puntaje vigente con su desglose, el estado de la entrega física y las auditorías anteriores.
- **CA02:** Dado que la convocatoria exige un documento que el postulante no adjuntó, el sistema debe mostrarlo como «Falta», y como «Recibido» y abrible el que sí adjuntó.
- **CA03:** Dado que el Administrador registra una auditoría «Validado» de un finalista con la entrega física recibida, el sistema debe guardarla con resultado, observaciones, auditor y fecha, y habilitarlo para la adjudicación.
- **CA04:** Dado que el resultado es «Observado» o «No válido» y no hay observaciones, el sistema debe rechazar el registro con `400` y no guardar nada.
- **CA05:** Dado que no se indica el resultado, el sistema debe rechazar el registro con `400`.
- **CA06:** Dado que el finalista ya tenía una auditoría, el sistema debe registrar la nueva, conservar las anteriores y tomar la más reciente como la vigente.
- **CA07:** Dado que la documentación física no está registrada como recibida, el sistema debe rechazar el registro con `409` e indicar que primero se registre la entrega.
- **CA08:** Dado que la postulación ya no está «calificada», el sistema debe rechazar el registro con `409`.
- **CA09:** Dado que la misma petición se envía dos veces, el sistema debe guardar una sola auditoría.
- **CA10:** Dado que un documento digital no puede recuperarse, el sistema debe informar el error, permitir reintentar y mostrar el resto del expediente.
- **CA11:** Dado cualquier registro de auditoría, el sistema debe anotarlo en la bitácora sin datos personales sensibles, rutas ni tokens.
- **CA12:** Dado cualquier consulta o registro, el sistema no debe modificar el estado de ninguna postulación ni su puntaje.
- **CA13:** Dado que el Administrador está en el expediente, el sistema debe permitirle notificar al finalista y registrar la entrega física con las mismas reglas del CU-21.
- **CA14:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`.
- **CA15:** Dado un finalista o una convocatoria inexistente o eliminada, el sistema debe responder `404`.
- **CA16:** Dado un finalista con auditorías, la lista de finalistas debe mostrar el resultado de la última («Validado», «Observado» o «No válido»), y «Sin auditar» si no tiene ninguna, con la acción «Auditar».
- **CA17:** Dado que se registra una auditoría, el sistema debe avisar al postulante el resultado (con las observaciones si es «Observado» o «No válido»), y no perder la auditoría si el envío falla.
- **CA18:** Dado que las observaciones superan 500 caracteres, el sistema debe rechazar el registro con `400`.
- **CA19:** Dado que el resultado es «Validado» y no se indica el puntaje académico, o este no está entre 0 y 30, el sistema debe rechazar el registro con `400` y no guardar nada.
- **CA20:** Dado que se registra una auditoría con puntaje académico, el sistema debe mostrar el puntaje total (IA + académico) en el expediente, sin modificar el puntaje de la IA.
- **CA21:** Dado que un reprocesamiento cambia el puntaje de la IA, el sistema debe mostrar el total recalculado con el mismo puntaje académico de la última auditoría.

## Cambios al diseño lógico

Para cumplir este CU, `auditoria_expediente` necesita una columna más que el diseño lógico vigente no tiene: `puntaje_academico numeric(5,2)` con `CHECK (puntaje_academico BETWEEN 0 AND 30)`, nula cuando la auditoría no es «Validado» o no se registró. Se agrega primero a la documentación de grado y a `docs/DiagramaConceptual.mermaid` (ver [PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)). Además, la IA califica solo de **0 a 70**: `puntaje_postulacion.puntaje_total` pasa a `CHECK (puntaje_total BETWEEN 0 AND 70)` (hoy 0–100), cambio que hace quien implementa el CU-03 y que las UT de este CU aplican en los datos de prueba.

## Preguntas abiertas

- **Plazo de entrega y qué pasa al vencer:** el CU-21 guarda el plazo y el CU-07 lo cita (A1); la DUBSS no definió cuántos días ni si el archivo es automático.

## Desarrollador

Mauro Gutierrez
