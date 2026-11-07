# Caso de uso

| Id   | Nombre                                                                              | Módulo                       | Prioridad | Estado    |
| ---- | ----------------------------------------------------------------------------------- | ---------------------------- | --------- | --------- |
| CU21 | Notificar finalistas, registrar entrega de documentación física y publicar comunicados | P4. Auditoría y adjudicación | Alta      | Hecho     |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Postulante (receptor de los avisos); Firebase Cloud Messaging y servicio de correo (entregan los avisos, a través del servicio de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md))
**Objetivo:** Que el Administrador DUBSS coordine la auditoría física de los finalistas —avisarles qué documentos entregar y hasta cuándo, dejar constancia de lo que entregan— y mantenga informados a los postulantes con comunicados.
**Plataforma:** Web (Administrador DUBSS). El Postulante consulta sus avisos en la bandeja de notificaciones de la web; el push al teléfono lo entrega el servicio de CU-EXTRA-01 y la bandeja en la app móvil queda fuera (ver Reglas de negocio).
**RF asociados:** RF-16 de la documentación de grado (en `requisitos_funcionales_propuestos.md` figura como RF-17)

**Descripción:**
El CU agrupa tres funciones que se ven en una sola pantalla («Finalistas: notificación y documentación física»). **A. Notificar a los finalistas:** el Administrador DUBSS elige una convocatoria, ve sus finalistas con el estado de su aviso y de su entrega física, selecciona a todos o a algunos, define el plazo, el lugar y horario de entrega y los documentos que deben presentar, y confirma. El sistema guarda el aviso en la bandeja de cada finalista, le envía push y correo, y lo deja anotado en la bitácora. Si el envío falla para alguno, lo marca y permite reintentar solo a esos. **B. Registrar la entrega física:** cuando el finalista lleva sus documentos, el Administrador marca la documentación como recibida con la fecha (por defecto, hoy) y observaciones opcionales; corregir una fecha ya registrada conserva la anterior en la bitácora. **C. Publicar comunicados:** el Administrador escribe un título y un contenido, elige a quién va dirigido (todos los postulantes o los finalistas), revisa una vista previa y confirma; el sistema publica el comunicado, lo envía como aviso a cada destinatario y lo anota en la bitácora. Este CU **no cambia el estado de ninguna postulación**.

Es incluido por el [CU-07: Auditar expediente finalista](CU07%20-%20Auditar%20expediente%20finalista.md) y parte de los finalistas que elige el [CU-05: Auditar y adjudicar becas](CU05%20-%20Auditar%20y%20adjudicar%20becas.md) a partir de la lista del [CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md). Usa el servicio de notificaciones de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md). Si la documentación llega incompleta se continúa en el CU-19 (devolver) y si vence el plazo, en el CU-20 (archivar o rechazar).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Para los flujos A y B: la convocatoria tiene finalistas definidos (CU-05).
- Para el flujo C: existe al menos un destinatario del grupo elegido.
- **Datos de apoyo mientras no existan los CU de origen:** los finalistas los crea el CU-05, que todavía no está implementado. Las unidades de trabajo de este CU crean la tabla `finalista` tal como está en el diseño lógico de la base de datos y amplían el cargador de datos de prueba del CU-06 (solo desarrollo) con finalistas de ejemplo. Cuando el CU-05 se implemente, **amplía** la tabla sin recrearla.

## Flujo principal

### Flujo A: notificar a los finalistas

1. El Administrador DUBSS entra a «Finalistas» en el menú (o llega desde la auditoría de expedientes, CU-07).
2. El sistema muestra las convocatorias y deja seleccionada la más reciente (A9).
3. El sistema muestra los finalistas de la convocatoria con su posición, nombre, puntaje, estado de la notificación («Sin enviar», «Enviada» o «Fallida») y estado de la documentación física («Pendiente» o «Recibida»); permite buscar por nombre o CI (A10).
4. El Administrador selecciona los finalistas a notificar (todos o un subconjunto) y define el **plazo de entrega**, el **lugar y horario** y los **documentos físicos** que deben presentar.
5. El Administrador confirma el envío (A6).
6. El sistema guarda el aviso en la bandeja de cada finalista seleccionado, le envía push y correo y marca al finalista como notificado (A1, A2).
7. El sistema anota el envío en la bitácora (destinatarios, fecha y hora) y muestra el resultado: cuántos avisos se enviaron y cuántos fallaron.

### Flujo B: registrar la entrega de documentación física

8. El Administrador busca al finalista por CI o nombre (paso 3) y lo selecciona.
9. El sistema muestra su estado de entrega («Pendiente» o «Recibida»).
10. El Administrador marca la documentación como recibida, con la fecha de recepción (por defecto, hoy) y observaciones opcionales (A3, A4, A5, A7).
11. El sistema guarda el registro, lo anota en la bitácora y habilita la continuación de la auditoría del expediente (CU-07).

### Flujo C: publicar un comunicado

12. El Administrador selecciona «Nuevo aviso o comunicado».
13. El Administrador ingresa el título y el contenido y elige a quién va dirigido: todos los postulantes o los finalistas (A6, A8).
14. El sistema muestra una vista previa y pide confirmación.
15. El Administrador confirma la publicación.
16. El sistema publica el comunicado, lo envía como aviso a cada destinatario (bandeja y push), lo anota en la bitácora y muestra a cuántos llegó.

### Flujo D: el postulante consulta sus avisos

17. El Postulante abre su bandeja de notificaciones en la web y ve el aviso del finalista o el comunicado, con su fecha, y lo marca como leído.

## Flujos alternativos y excepciones

- **A1 (paso 6):** Un finalista no tiene teléfono registrado para push → el aviso queda en su bandeja y se le envía por correo; no es un error (así lo resuelve el servicio de CU-EXTRA-01).
- **A2 (paso 6):** Falla el envío para algún finalista → el sistema lo marca como «Fallida» y el Administrador puede volver a notificar solo a esos (los selecciona y repite el flujo A). El resto no recibe el aviso dos veces.
- **A3 (paso 10):** El finalista entrega la documentación incompleta → el Administrador registra la observación y puede devolver la postulación (CU-19).
- **A4 (paso 10):** Venció el plazo y el finalista no entregó → el Administrador puede archivar o rechazar la postulación (CU-20). Este CU solo muestra que la entrega sigue «Pendiente».
- **A5 (paso 10):** El Administrador corrige una fecha de recepción ya registrada → el sistema guarda la nueva y deja el valor anterior en la bitácora.
- **A6 (pasos 5 y 13):** Faltan datos obligatorios (plazo, lugar y horario, documentos, título, contenido, destinatarios, o ningún finalista seleccionado) → el sistema no envía ni publica nada e indica los campos que faltan (`400`).
- **A7 (paso 10):** La fecha de recepción es futura → `400`; el sistema no la guarda.
- **A8 (paso 13):** El comunicado se dirige a un grupo sin destinatarios (por ejemplo, finalistas de una convocatoria que no tiene) → el sistema lo informa y no publica.
- **A9 (paso 2):** No existe ninguna convocatoria, o la elegida ya no existe o fue eliminada → mensaje informativo, o `404` y vuelta a la selección.
- **A10 (paso 3):** La convocatoria no tiene finalistas → el sistema informa «Todavía no hay finalistas en esta convocatoria» y no muestra la tabla; el flujo C sigue disponible.
- **A11 (cualquier paso):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario al inicio de sesión.
- **A12 (cualquier paso):** El usuario no tiene el rol Administrador DUBSS → `403`; no ve la pantalla ni puede usar los endpoints.
- **A13 (flujos A y C):** Firebase o el correo no están disponibles → los avisos quedan guardados en la bandeja y el servicio de CU-EXTRA-01 reintenta el envío; el flujo no se interrumpe ni se pierde el aviso.
- **A14 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y posibilidad de reintentar, sin datos a medias.

## Reglas de negocio

- **Solo el Administrador DUBSS** ejecuta este CU; el Postulante es solo receptor. El receptor ve **solo sus propios avisos**.
- **Los avisos usan el servicio de notificaciones de CU-EXTRA-01** (bandeja + push, y correo donde corresponda); este CU no implementa el envío, solo agrega dos eventos nuevos (aviso al finalista y comunicado) con su plantilla. Un fallo de envío nunca deshace el registro del aviso.
- **Lo que se guarda del finalista** (diseño lógico): `notificado_en`, `plazo_entrega` y `documentacion_recibida_en` de la tabla `finalista`. El **plazo es una fecha** y debe ser posterior a hoy.
- **Lugar, horario y lista de documentos** no tienen columna en el diseño lógico: viajan en el texto del aviso y en el detalle de la bitácora. *(Decisión del equipo: la documentación de grado los deja «por definir con la DUBSS» y no los modela; se ingresan como texto en cada envío.)*
- **Observaciones de la entrega** tampoco tienen columna: se guardan en el detalle de la bitácora. *(Decisión del equipo.)*
- **Estado de la notificación del finalista:** «Sin enviar» (nunca se notificó), «Enviada» (el último aviso se entregó por al menos una vía) o «Fallida» (el último aviso no se pudo entregar). Se deriva del último aviso ligado a esa postulación. *(Decisión del equipo.)*
- **Estado de la entrega física:** «Recibida» si `documentacion_recibida_en` tiene valor; «Pendiente» si no. Corregir la fecha no cambia el estado.
- **Destinatarios del comunicado:** el diseño lógico define `TODOS`, `FINALISTAS` y `ADJUDICADOS`. Hoy se aceptan **`TODOS` y `FINALISTAS`**; `ADJUDICADOS` se habilita cuando existan las adjudicaciones (CU-05/CU-22). `TODOS` son las cuentas activas de Postulante; si el comunicado está ligado a una convocatoria, solo quienes tienen una postulación enviada en ella. `FINALISTAS` exige convocatoria. *(Decisión del equipo.)*
- **Comunicados: push y bandeja, sin correo** (para no mandar correo masivo a miles de postulantes); el aviso al finalista sí va además por correo. *(Decisión del equipo; RF-16 solo exige push.)*
- **El envío masivo** (comunicado a todos) guarda los avisos y los despacha por lotes sin bloquear la respuesta al Administrador; el resultado del despacho no se espera.
- **Bitácora:** cada notificación a finalistas (cantidad y códigos de los destinatarios, sin nombres ni CI), cada registro o corrección de entrega (con el valor anterior) y cada comunicado publicado quedan anotados. Nunca contraseñas, tokens de Firebase ni correos en el detalle.
- **Un comunicado publicado no se edita ni se borra** desde este CU (es lo que ya recibieron los destinatarios). *(Decisión del equipo.)*
- **Fuera de este CU:** «Guardar borrador» y publicación programada que muestra la maqueta (`144`) —la documentación de texto no los pide—, el checklist de documentos recibidos de la maqueta, la bandeja en la app móvil, las preferencias por canal y la consulta de la bitácora (CU-25).
- Todos los mensajes al usuario están en español y no exponen detalles técnicos.

## Postcondiciones

- Los finalistas notificados tienen el aviso en su bandeja (y por push/correo) y quedan marcados como notificados con su plazo.
- La recepción de la documentación física queda registrada con su fecha.
- Los comunicados publicados quedan consultables por sus destinatarios en su bandeja.
- Las acciones quedan en la bitácora; ninguna postulación cambia de estado.

## Criterios de aceptación

- **CA01:** Dado que el Administrador elige una convocatoria con finalistas, el sistema debe listarlos con posición, nombre, puntaje, estado de la notificación y estado de la documentación física.
- **CA02:** Dado que selecciona finalistas y completa plazo, lugar y horario y documentos, el sistema debe guardar un aviso en la bandeja de cada uno, enviarles push y correo y marcarlos como notificados con el plazo.
- **CA03:** Dado que un finalista no tiene teléfono registrado, el sistema debe dejarle el aviso en la bandeja y enviarle el correo, sin error.
- **CA04:** Dado que el envío falla para algún finalista, el sistema debe mostrarlo como «Fallida» y permitir reenviar solo a esos.
- **CA05:** Dado que falta algún dato obligatorio o no hay finalistas seleccionados, el sistema debe rechazar el envío con `400` indicando los campos faltantes y no enviar nada.
- **CA06:** Dado que el plazo no es posterior a hoy, el sistema debe rechazarlo con `400`.
- **CA07:** Dado que el Administrador registra la entrega con una fecha válida, el sistema debe guardarla, mostrar la documentación como «Recibida» y anotarlo en la bitácora.
- **CA08:** Dado que no indica fecha, el sistema debe usar la fecha actual.
- **CA09:** Dado que la fecha de recepción es futura, el sistema debe rechazarla con `400`.
- **CA10:** Dado que corrige una fecha ya registrada, el sistema debe guardar la nueva y dejar la anterior en la bitácora.
- **CA11:** Dado que el Administrador publica un comunicado completo para `TODOS` o `FINALISTAS`, el sistema debe publicarlo y dejar un aviso en la bandeja de cada destinatario, con push.
- **CA12:** Dado que el título, el contenido o los destinatarios están vacíos, el sistema debe rechazar la publicación con `400` indicando los campos faltantes.
- **CA13:** Dado que se pide `ADJUDICADOS` o un grupo sin destinatarios, el sistema debe rechazar la publicación con un error claro y no publicar nada.
- **CA14:** Dado que el Postulante abre su bandeja, el sistema debe mostrarle solo sus propios avisos, y permitir marcarlos como leídos.
- **CA15:** Dado cualquiera de las tres acciones, el sistema debe anotarla en la bitácora sin datos personales sensibles ni tokens.
- **CA16:** Dado cualquiera de las tres acciones, el sistema no debe modificar el estado de ninguna postulación.
- **CA17:** Dado que Firebase o el correo fallan, el aviso debe quedar en la bandeja y el Administrador no debe perder lo que hizo.
- **CA18:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`.
- **CA19:** Dado una convocatoria inexistente o eliminada, el sistema debe responder `404`.
- **CA20:** Dado un comunicado a todos los postulantes de una convocatoria grande, el Administrador debe recibir la respuesta sin esperar a que terminen los envíos.

## Desarrollador

Mauro Gutierrez
