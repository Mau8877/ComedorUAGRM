# Caso de uso

| Id   | Nombre                           | Módulo                       | Prioridad | Estado    |
| ---- | -------------------------------- | ---------------------------- | --------- | --------- |
| CU20 | Archivar o rechazar postulación  | P4. Auditoría y adjudicación | Alta      | Hecho     |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Postulante (recibe el aviso con el motivo, a través del servicio de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md))
**Objetivo:** Que el Administrador DUBSS cierre formalmente una postulación que no procede —archivándola o rechazándola— y deje constancia escrita del motivo, sin descartar el expediente ni perder su historia.
**Plataforma:** Web
**RF asociados:** RF-15

**Descripción:**
Durante la auditoría del expediente de un finalista ([CU-07](CU07%20-%20Auditar%20expediente%20finalista.md)), el Administrador DUBSS concluye que la postulación no puede continuar y que no tiene arreglo por el camino de la corrección ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)): la documentación no es válida, el finalista no entregó su documentación física en el plazo, o la postulación simplemente no procede. Desde la pantalla del expediente elige **«Archivar»** o **«Rechazar»**, escribe el **motivo** y confirma. Al **archivar**, la postulación pasa de «calificada» a «archivada»; al **rechazar**, pasa de «calificada» a «no adjudicada». En ambos casos el sistema deja en el historial de la postulación el estado anterior, el estado nuevo, quién decidió, cuándo y el motivo, anota la acción en la bitácora y avisa al postulante por su bandeja, por push y por correo. La postulación sale de la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)) porque esa lista solo muestra las «calificadas», pero **se conserva** para consulta histórica y reportes, y el postulante ve el estado y el motivo ([CU-04](CU04%20-%20Consultar%20estado%20de%20postulación.md)).

Este CU **solo cambia el estado de la postulación a «archivada» o «no adjudicada»**: no borra auditorías, documentos ni el puntaje, y no toca la fila de `finalista`. Extiende el CU-07: se ejecuta desde el expediente del finalista, pero el CU-07 no depende de él. La diferencia con el CU-19 es que aquí el cierre es definitivo para esa gestión: no hay reenvío.

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- El Administrador está viendo el expediente de un finalista (CU-07) y la postulación está en estado «calificada».
- Los resultados de la convocatoria **no** se han publicado ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)). *(Del detalle del CU-20 del documento de taller, A3.)*
- **Datos de apoyo (semilla):** se reutilizan los finalistas de prueba del cargador de datos del CU-07 (postulación «calificada») y, solo en desarrollo, un finalista cuya postulación ya está «archivada» y otro «no adjudicada» para probar los conflictos. `postulacion_historial` ya existe (la creó el CU-19); este CU no la recrea ni le agrega columnas.

## Flujo principal

**Flujo A: archivar**

1. El Administrador DUBSS abre el expediente de un finalista desde la lista de finalistas (CU-21) y ve la postulación «calificada».
2. Selecciona «Archivar» (A2).
3. El sistema muestra una ventana de confirmación con el nombre del postulante y la convocatoria y pide el **motivo** (obligatorio, hasta 500 caracteres).
4. El Administrador escribe el motivo y confirma (A2).
5. El sistema valida que el finalista exista (A3), que el motivo no esté vacío ni pase de 500 caracteres (A1), que la postulación siga «calificada» (A4) y que los resultados no estén publicados (A4).
6. El sistema cambia el estado a «archivada», registra el cambio en el historial (estado anterior, estado nuevo, usuario, fecha y motivo) y lo anota en la bitácora, todo en una misma operación (A8).
7. El sistema avisa al postulante (bandeja, push y correo) con el motivo (A5).
8. El sistema confirma la operación y el expediente pasa a mostrar la postulación como «Archivada», sin permitir más acciones.

**Flujo B: rechazar** — igual que el flujo A, pero en el paso 2 el Administrador elige «Rechazar», el estado nuevo es «no adjudicada» y el aviso usa el texto de rechazo.

## Flujos alternativos y excepciones

- **A1 (paso 5):** El motivo falta, es solo espacios o supera 500 caracteres → `400` (`ERR_SYS_01`); no se cambia nada y se indica el campo inválido.
- **A2 (pasos 2 y 4):** El Administrador cancela la ventana → la postulación conserva su estado.
- **A3 (paso 5):** El finalista no existe o fue eliminado → `404` (`ERR_FIN_01`) y el sistema vuelve a la lista de finalistas.
- **A4 (paso 5):** La postulación ya no está «calificada» (ya devuelta, archivada, no adjudicada o adjudicada, incluye a dos administradores a la vez o un doble clic) o los resultados ya fueron publicados → `409` (`ERR_ARC_01`, **código propuesto**); no se cambia nada, se informa el estado actual y se actualiza el expediente. El botón se deshabilita mientras se envía; una segunda petición recibe este `409` y no registra una segunda decisión.
- **A5 (paso 7):** Falla el envío del aviso → la decisión queda registrada y el aviso queda en la bandeja; CU-EXTRA-01 reintenta el push y el correo.
- **A6 (cualquier paso):** Sesión expirada o inválida → `401`.
- **A7 (cualquier paso):** El usuario no es Administrador DUBSS → `403`; no ve las acciones ni puede usar el endpoint.
- **A8 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y reintento; el cambio de estado, el historial y la bitácora se guardan juntos o no se guarda ninguno.

## Reglas de negocio

- **Solo el Administrador DUBSS** ejecuta este CU (RF-15). El Postulante solo recibe el aviso y ve el estado y el motivo.
- **Solo se archiva o rechaza una postulación «calificada» de un finalista**, desde su expediente; no hay lista aparte de postulaciones. *(Igual que el CU-07 y el CU-19: la lista de finalistas del CU-21 es la única entrada.)*
- **Dos salidas, dos estados** (modelo de dominio): archivar → `archivada`; rechazar → `no_adjudicada`. Son estados finales: no se vuelve a «calificada».
- **Motivo:** obligatorio, máximo **500 caracteres** (mismo límite que las observaciones del CU-07 y el motivo del CU-19). Va en `postulacion_historial.motivo` (`text`; el límite lo valida el servicio).
- **Qué se guarda:** una fila nueva en `postulacion_historial` (append-only), con estado anterior «calificada», estado nuevo, usuario, fecha y motivo.
- **No se tocan** documentos, respuestas, puntaje de la IA, auditorías ni la fila de `finalista`. La postulación se conserva para consulta histórica y reportes (RF-15).
- **Aviso al postulante:** siempre (bandeja, push y correo) con el motivo; no hay opción de no avisar. Eventos nuevos del catálogo de CU-EXTRA-01: `POSTULACION_ARCHIVADA` y `POSTULACION_RECHAZADA` (tipos `INFO` y `ERROR`). *(Decisión propia, por revisar.)* Un fallo de envío nunca deshace la decisión.
- **Bitácora:** cada decisión queda anotada (quién, cuándo, códigos de postulación y finalista, estado anterior y nuevo, **sin** el motivo, nombres, CI, rutas ni tokens). *(Decisión propia, por revisar.)*
- **Lista de finalistas (CU-21):** el finalista sigue en la lista y muestra el estado de su postulación («Archivada» / «No adjudicada») para que no se intente auditar. *(Decisión propia, por revisar.)*
- **Lista priorizada (CU-06):** al dejar de estar «calificada» la postulación sale de la lista.
- **Resultados publicados:** tras publicar los resultados (CU-22) ya no se archiva ni rechaza.
- **Fuera de este CU:** devolver (CU-19), adjudicar o no adjudicar en la decisión final (CU-05), publicar resultados (CU-22), reapertura de una postulación archivada y apelación del postulante.
- Todos los mensajes están en español y no exponen detalles técnicos.

## Postcondiciones

- La postulación queda «archivada» o «no adjudicada» y el historial tiene una fila nueva con el motivo.
- El postulante tiene el aviso en su bandeja (y el push y el correo, si pudieron enviarse) y puede consultar el estado y el motivo (CU-04).
- La acción queda en la bitácora; auditorías, puntaje y documentos no cambian.

## Criterios de aceptación

- **CA01:** Dado que el Administrador archiva la postulación calificada de un finalista con un motivo válido, el sistema debe cambiar su estado a «archivada» y registrar en el historial estado anterior, nuevo, usuario, fecha y motivo.
- **CA02:** Dado que la rechaza con un motivo válido, el sistema debe cambiarla a «no adjudicada» y registrar lo mismo en el historial.
- **CA03:** Dado un motivo vacío o solo espacios, el sistema debe responder `400` y no cambiar nada.
- **CA04:** Dado un motivo de más de 500 caracteres, el sistema debe responder `400` y no cambiar nada.
- **CA05:** Dado que la postulación ya no está «calificada», el sistema debe responder `409` e informar el estado actual.
- **CA06:** Dado que los resultados de la convocatoria ya fueron publicados, el sistema debe responder `409`.
- **CA07:** Dado que la misma petición llega dos veces o dos administradores actúan a la vez, el sistema debe registrar una sola decisión, un solo aviso y una sola anotación en la bitácora, y responder `409` a la otra.
- **CA08:** Dada una decisión registrada, el sistema debe avisar al postulante por bandeja, push y correo con el motivo, sin perder la decisión si el envío falla.
- **CA09:** Dada una decisión registrada, el sistema debe anotarla en la bitácora sin nombres, CI, motivo, rutas ni tokens.
- **CA10:** Dado que el Administrador cancela la ventana, el sistema no debe cambiar el estado.
- **CA11:** Dada una postulación archivada o no adjudicada, el sistema no debe mostrarla en la lista priorizada ni permitir auditarla, y debe conservar auditorías, puntaje y documentos para consulta histórica.
- **CA12:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`; con finalista inexistente, `404`.
- **CA13:** Dado el postulante, el sistema debe mostrarle el estado y el motivo registrados (CU-04).
- **CA14:** Dada una decisión registrada, el historial debe conservarse completo, sin editar ni borrar filas anteriores.

## Cambios al diseño lógico

- **Sin columnas ni tablas nuevas:** se reutiliza `postulacion_historial` (creada por el CU-19).
- **Eventos de notificación** (propuestos): `POSTULACION_ARCHIVADA` y `POSTULACION_RECHAZADA`, con `{nombres}` y `{motivo}`, bandeja, push y correo.
- **Códigos de error** (prefijo `ARC`, propuesto): `ERR_ARC_01` (`409`, la postulación ya no está «calificada» o los resultados están publicados). Finalista inexistente reutiliza `ERR_FIN_01`; el motivo inválido cae en `ERR_SYS_01`. Documentarlos en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` y su índice al implementar.
- **Endpoints** (propuestos, mismo estilo que el CU-19): `POST /api/v1/finalistas/{codigo}/archivar` y `POST /api/v1/finalistas/{codigo}/rechazar`, con `{ motivo }`.

## Preguntas abiertas

1. **¿Exige una auditoría previa?** El CU-19 la exige (decisión de la DUBSS). El documento de taller no lo dice para el CU-20, y su A1 del CU-07 prevé archivar a un finalista que **no entregó** en plazo, donde quizá no hay auditoría. Se propone **no exigirla**.
2. **¿Y la entrega física?** No se exige, por la misma razón.
3. **Plazo de entrega física:** el CU-07/CU-21 hablan de archivar al vencer el plazo, pero el plazo y qué pasa al vencer siguen sin definir con la DUBSS.
4. **¿Se puede reabrir?** Se propone que no (estado final). Falta confirmar con la DUBSS si existe reapertura o apelación del postulante.
5. **Nombre de estados:** «archivar» deja `archivada` y «rechazar» deja `no adjudicada`; confirmar con la DUBSS que «rechazar» antes de la adjudicación debe aparecer como «no adjudicada» para el postulante.
6. **¿Se avisa siempre?** Se propone sí (regla general); confirmar los textos con la DUBSS.
7. **Relación con el CU-05:** «no adjudicar» en la decisión final también deja `no_adjudicada`; confirmar cómo se distingue de «rechazar» en reportes (CU-12).

## Desarrollador

Mauro Gutierrez *(por confirmar)*
