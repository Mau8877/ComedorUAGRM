# Caso de uso

| Id   | Nombre                          | Módulo                        | Prioridad | Estado    |
| ---- | ------------------------------- | ----------------------------- | --------- | --------- |
| CU04 | Consultar estado de postulación | P2. Convocatoria y postulación | Alta      | En progreso |

**Actor principal:** Postulante
**Actores secundarios:** —
**Objetivo:** Que el Postulante siga el estado de sus postulaciones, vea su historial de cambios y la información asociada a cada estado (observaciones, motivo, plazo de entrega, resultado) sin ir presencialmente a la DUBSS.
**Plataforma:** Móvil y Web
**RF asociados:** RF-06

**Descripción:**
El Postulante entra a «Mis postulaciones» y ve la lista de sus postulaciones enviadas con la gestión académica, la fecha de envío y el estado actual. Al elegir una, el sistema muestra el detalle: el estado visible, la **línea de tiempo** armada con el historial real de cambios de estado (fecha y hora) y la información que corresponde a ese estado: las **observaciones** de una devolución ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)), el **motivo** de un archivo o rechazo ([CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md)), el **plazo y la entrega de documentos** si es finalista ([CU-21](CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md)) y su **resultado** una vez publicados los resultados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)). Es una consulta de **solo lectura**: no modifica ningún dato. Si el aviso de un cambio llegó por push o por la bandeja, tocarlo abre directamente el detalle de esa postulación.

Este CU **lee** lo que generan otros CU y no cambia estados. El envío, la corrección y el reenvío son del [CU-02](CU02%20-%20Enviar%20postulación%20a%20Beca%20Comedor.md), que está a cargo de otra persona del equipo: ver «Puntos de enganche con el CU-02».

---

## Precondiciones

- El Postulante está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `POSTULANTE` y su cuenta está activa.
- Para ver una postulación, el Postulante ya la envió (CU-02). Sin postulaciones enviadas el CU igual se puede abrir (A1).

## Flujo principal

1. El Postulante entra a «Mis postulaciones» (A6 si llega desde un aviso).
2. El sistema muestra la lista de sus postulaciones enviadas, de la más reciente a la más antigua, con la gestión académica, la fecha de envío y el estado visible (A1, A2).
3. El Postulante selecciona una postulación.
4. El sistema muestra el detalle: convocatoria, fecha de envío, estado visible y la línea de tiempo con fecha y hora de cada cambio, más los pasos que aún faltan como pendientes.
5. Según el estado, el sistema agrega la información asociada (A3, A4, A5).
6. El Postulante revisa y, si quiere, vuelve a la lista.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No tiene postulaciones enviadas → mensaje informativo; si hay una convocatoria abierta, se invita a postular (lleva al CU-02).
- **A2 (paso 2):** Tiene postulaciones de varias gestiones → puede filtrarlas por gestión académica.
- **A3 (paso 5):** Estado «devuelta» → muestra las **observaciones** (el motivo de la devolución, CU-19) con su fecha y ofrece «Corregir y reenviar» (CU-02). El plazo para corregir está **por definir con la DUBSS**.
- **A4 (paso 5):** Estado «archivada» o «no adjudicada» por rechazo del CU-20 → muestra el **motivo** registrado. Si «no adjudicada» fue por decisión final (CU-05), **no** se muestra motivo.
- **A5 (paso 5):** Es finalista → muestra el bloque «Eres finalista» con el **plazo de entrega**, los **documentos físicos** a presentar (del último aviso de finalista, CU-21) y si la documentación ya fue recibida. Si los resultados están publicados y hay decisión → muestra su **resultado** y el botón «Ver resultados de la convocatoria» ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)).
- **A6 (paso 1):** Llega desde un aviso (push o bandeja) → se abre directamente el detalle de la postulación a la que apunta el aviso.
- **A7 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A8 (cualquier paso):** Rol distinto de Postulante → `403`.
- **A9 (paso 3):** La postulación no existe o es de otra persona → `404` (no se revela si existe).
- **A10 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento.

## Reglas de negocio

- **Solo ve las suyas.** El Postulante consulta únicamente sus postulaciones; el identificador sale del token, nunca de la URL ni del body. Una postulación ajena responde `404`.
- **Qué se lista:** solo las enviadas (desde `pendiente` en adelante). Los `borrador` ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md)) no aparecen aquí.
- **Estado visible.** El sistema calcula el estado que ve el postulante:
  - Si la postulación fue adjudicada o no adjudicada por decisión final (CU-05) y los resultados de la convocatoria **aún no están publicados**, se muestra «calificada» y esa fila del historial se oculta. *(Decisión 3 del CU-05.)*
  - Tras la publicación (CU-22) se muestra «adjudicada» o «no adjudicada».
  - El rechazo (CU-20), el archivo y la devolución se ven **de inmediato**.
- **Línea de tiempo.** Se arma con `postulacion_historial` (estado anterior y nuevo, fecha y hora); no se inventan pasos sin un cambio de estado que los respalde. Los pasos futuros (auditoría, resultado) se muestran como pendientes según el estado. No se muestra quién hizo el cambio.
- **Motivo de «no adjudicada»:** solo el del rechazo (CU-20); el de la decisión final (CU-05) lo ve solo el Administrador. *(Coherente con el CU-22.)*
- **Finalista:** el plazo y la entrega salen de lo que ya guarda el CU-21 (`plazo_entrega`, documentación recibida); los documentos físicos, del texto del último aviso de finalista ligado a la postulación. Sin tablas nuevas.
- **Resultado y lista:** el detalle usa el resultado propio del CU-22 y enlaza a la pantalla de resultados de esa convocatoria; los no adjudicados ven solo su propio resultado.
- **Aviso → detalle (A6).** Los avisos de esta cadena de CU (devolución, archivo, rechazo, finalista, adjudicada, no adjudicada) ya guardan `postulacion_id`; tocarlos abre el detalle de esa postulación.
- **Sin efectos secundarios:** la consulta no marca avisos como leídos ni cambia estados ni escribe en la bitácora.
- **Listado:** paginado, ordenado por fecha de envío descendente, con filtro por gestión académica (`filter[gestionAcademica]`).
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- El Postulante visualiza el estado actualizado de su postulación.
- Ningún dato del sistema cambió.

## Criterios de aceptación

- **CA01:** Dado un postulante con postulaciones enviadas, el sistema debe listarlas con gestión académica, fecha de envío y estado visible, de la más reciente a la más antigua.
- **CA02:** Dado un postulante sin postulaciones enviadas, el sistema debe mostrar un mensaje informativo y, si hay convocatoria abierta, invitar a postular.
- **CA03:** Dado un postulante con postulaciones de varias gestiones, el sistema debe permitir filtrarlas por gestión académica.
- **CA04:** Dado el detalle de una postulación, el sistema debe mostrar el estado visible y el historial de cambios con fecha y hora, sin mostrar quién los hizo.
- **CA05:** Dada una postulación «devuelta», el sistema debe mostrar las observaciones y la opción «Corregir y reenviar».
- **CA06:** Dada una postulación «archivada» o rechazada (CU-20), el sistema debe mostrar el motivo registrado.
- **CA07:** Dada una decisión final del CU-05 con resultados sin publicar, el sistema debe mostrar «calificada» y ocultar esa fila del historial.
- **CA08:** Dados los resultados publicados, el sistema debe mostrar «adjudicada» o «no adjudicada» y el botón hacia los resultados de la convocatoria, sin mostrar el motivo de la decisión final.
- **CA09:** Dado un finalista, el sistema debe mostrar el plazo de entrega, los documentos a presentar y si la documentación ya fue recibida.
- **CA10:** Dado un aviso de cambio de estado, el sistema debe abrir al tocarlo el detalle de la postulación correspondiente.
- **CA11:** Dada una postulación de otra persona o inexistente, el sistema debe responder `404`; sin sesión válida, `401`; con otro rol, `403`.
- **CA12:** Dada una postulación en `borrador`, el sistema no debe incluirla en «Mis postulaciones».
- **CA13:** Dada cualquier consulta, el sistema no debe modificar estados, avisos ni bitácora.

## Puntos de enganche con el CU-02

El CU-02 lo desarrolla otra persona. Para que ambos CU encajen sin retrabajo:

- **Historial:** el CU-02 debe dejar una fila en `postulacion_historial` en cada envío y reenvío (estado anterior y nuevo, fecha y hora, sin motivo o con uno corto); de ahí sale el primer paso «Postulación enviada» y los reenvíos tras una devolución.
- **Estado y fecha:** `postulacion.estado`, `fecha_envio` y `numero_envios` (que aumenta en cada reenvío) son la fuente del listado.
- **Botón «Corregir y reenviar»:** apunta a la pantalla de edición del CU-02; mientras ese CU no exista se oculta o queda deshabilitado, sin romper la consulta.
- **Invitación a postular (A1):** apunta a la pantalla de postulación del CU-02.
- **Pasos intermedios** («Elegibilidad validada», «Precalificada con IA»): si el CU-02, el CU-18 o el CU-03 registran esos cambios en el historial, la línea de tiempo los muestra sin tocar este CU.
- **Avisos al enviar:** si el CU-02 crea avisos, deben guardar `postulacion_id` para que el aviso abra el detalle.

## Cambios al diseño lógico

- **Ninguna tabla ni columna nueva.** Se leen `postulacion`, `postulacion_historial`, `finalista`, `adjudicacion`, `publicacion_resultado` y `notificacion`.
- **Códigos de error** (prefijo propio, por definir al crear la UT, p. ej. `POS`): postulación inexistente o ajena, `404`. Documentarlos en `docs/errors/` y su índice al implementar.
- **Endpoints** (todos de solo lectura, paginado el listado):
  - `GET /api/v1/postulaciones/mias` con `filter[gestionAcademica]` y `sort` (por defecto `-fechaEnvio`).
  - `GET /api/v1/postulaciones/{codigo}` con el estado visible, la línea de tiempo y la información asociada.

## Cambios pendientes en el documento de taller

| Dónde | Hoy dice | Debe decir | Por qué |
| --- | --- | --- | --- |
| CU-04, A4 | «…si la postulación está «archivada» o «no adjudicada», el sistema muestra el motivo registrado (CU-20).» | «…muestra el motivo del archivo o rechazo (CU-20); el de la decisión final (CU-05) no se muestra.» | Alineado con el CU-22. |
| CU-04, paso 4 | Estados «…adjudicada o no adjudicada…» sin condición. | Aclarar que se muestran **solo tras publicar**; antes se ve «calificada». | CU-05, decisión 3. |
| CU-04, A5 | «…muestra la notificación con el plazo y los documentos físicos…» | «…muestra el bloque de finalista con el plazo, los documentos a presentar y si ya se recibió la documentación; con resultados publicados, el resultado y el enlace a la lista.» | Decisiones de Mau. |
| CU-04, descripción | «…el sistema notifica al Postulante mediante notificación push…» | «…avisa por bandeja, push y correo; el aviso abre el detalle.» | CU-EXTRA-01. |
| CU-04, A6 | Solo menciona push. | «Desde un aviso de la bandeja o un push.» | Igual. |
| `04_detalle_casos_uso.tex`, CU-04, precondición 2 | «ha enviado al menos una postulación.» | Con A1 ya cubre el caso sin postulaciones; aclarar que los borradores no se listan. | CU-17. |
| `03_requisitos_especificos.tex`, RF-06 | Estados «recibida, en evaluación, finalista, adjudicada, rechazada». | Los estados reales: pendiente, devuelta, archivada, calificada, adjudicada, no adjudicada (finalista es una marca, no un estado). | Diseño lógico. |
| Diagrama de secuencia y análisis CU-04 | Sin la parte de finalista, resultado ni estado visible. | Agregar `Finalista`, `Adjudicacion` y `PublicacionResultado` como lectura. | Decisiones de Mau. |

## Decisiones confirmadas

1. Solo se listan las postulaciones enviadas; los borradores no.
2. La línea de tiempo sale del historial real; los pasos futuros se muestran como pendientes.
3. Estado visible: «calificada» hasta publicar; el rechazo y la devolución se ven de inmediato.
4. El motivo se muestra solo en archivo y rechazo (CU-20), no en la decisión final.
5. El finalista ve plazo, documentos a presentar y estado de la entrega.
6. El detalle enlaza a los resultados del CU-22 y los avisos abren el detalle.
7. «Corregir y reenviar» depende del CU-02 (otro desarrollador); el plazo para corregir queda por definir.

## Desarrollador

Mauro Gutierrez
