# Caso de uso

| Id   | Nombre                               | Módulo                       | Prioridad | Estado    |
| ---- | ------------------------------------ | ---------------------------- | --------- | --------- |
| CU19 | Devolver postulación para corrección | P4. Auditoría y adjudicación | Alta      | Pendiente |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Postulante (recibe el aviso con las observaciones, a través del servicio de [CU-EXTRA-01](CU-EXTRA-01%20-%20Notificaciones%20push.md))
**Objetivo:** Que el Administrador DUBSS devuelva al Postulante una postulación con defectos subsanables, indicándole por escrito qué debe corregir, para que la complete y la reenvíe en lugar de descartarla.
**Plataforma:** Web
**RF asociados:** RF-14

**Descripción:**
Durante la auditoría del expediente de un finalista ([CU-07](CU07%20-%20Auditar%20expediente%20finalista.md)), el Administrador DUBSS, que ya registró la auditoría del expediente, detecta defectos que el Postulante puede subsanar (un documento ilegible, un certificado que falta, un dato mal declarado). Desde la pantalla del expediente elige «Devolver para corrección», escribe el **motivo** (las observaciones de lo que hay que corregir) y confirma. El sistema cambia la postulación del estado «calificada» a «devuelta», deja en el historial de la postulación el estado anterior, el estado nuevo, quién la devolvió, cuándo y el motivo, anota la acción en la bitácora y avisa al postulante por su bandeja, por push y por correo con las observaciones, para que sepa qué corregir. Una vez devuelta, la postulación deja de ser auditable y sale de la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)), porque esa lista solo muestra las «calificadas». El postulante corrige y reenvía con el [CU-02](CU02%20-%20Enviar%20postulación%20a%20Beca%20Comedor.md); ese reenvío y lo que sigue (volver a elegir al finalista, volver a registrar la entrega física, una auditoría nueva) **no son parte de este CU**.

Este CU **solo cambia el estado de la postulación a «devuelta»**: no borra auditorías anteriores, no toca el puntaje ni los documentos, y no archiva ni rechaza (eso es el [CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md)). Extiende el CU-07: se ejecuta desde el expediente del finalista, pero el CU-07 no depende de él.

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- El Administrador está viendo el expediente de un finalista (CU-07) y la postulación está en estado «calificada».
- El expediente tiene **al menos una auditoría registrada** (CU-07, cualquier resultado) posterior a la última devolución, si la hubo: siempre debe haber una auditoría antes de devolver.
- **Datos de apoyo (semilla):** el CU-02 (que envía y reenvía la postulación) lo implementa otro integrante del equipo y todavía no existe; este CU **no cuenta con él**. Para probarlo se reutilizan los finalistas de prueba que ya crea el cargador de datos del CU-07 (postulación «calificada», con y sin entrega física registrada) y las unidades de trabajo agregan, solo en desarrollo, un finalista cuya postulación ya está «devuelta» con su historial. La tabla `postulacion_historial` **no existe todavía en la base**: la crea este CU exactamente como está en el diseño lógico (más la columna que se indica en *Cambios al diseño lógico*); los CU-02, CU-20 y CU-05 la **amplían** sin recrearla.

## Flujo principal

1. El Administrador DUBSS abre el expediente de un finalista desde la lista de finalistas (CU-07) y ve la postulación en estado «calificada».
2. El Administrador selecciona «Devolver para corrección» (A2).
3. El sistema muestra una ventana de confirmación con el nombre del postulante y la convocatoria, y le pide el **motivo** de la devolución (obligatorio, hasta 500 caracteres).
4. El Administrador escribe el motivo y confirma (A2).
5. El sistema valida que el finalista exista (A3), que el motivo no esté vacío ni pase de 500 caracteres (A1) y que la postulación siga «calificada» (A4) y que el expediente tenga una auditoría registrada (A5).
6. El sistema cambia el estado de la postulación a «devuelta», registra el cambio en el historial de la postulación (estado anterior, estado nuevo, usuario, fecha y motivo) y lo anota en la bitácora, todo en una misma operación (A9).
7. El sistema avisa al postulante (bandeja, push y correo) con las observaciones (A6).
8. El sistema confirma la devolución al Administrador y actualiza el expediente, que pasa a mostrar la postulación como «Devuelta» y ya no permite registrar auditorías ni devolverla otra vez.

## Flujos alternativos y excepciones

- **A1 (paso 5):** El motivo falta, es solo espacios, o supera 500 caracteres → `400` (`ERR_SYS_01`); el sistema no cambia nada e indica el campo inválido.
- **A2 (pasos 2 y 4):** El Administrador cancela la ventana → no se cambia nada; la postulación conserva su estado.
- **A3 (paso 5):** El finalista no existe o fue eliminado (por ejemplo, con un enlace guardado) → `404` (`ERR_FIN_01`) y el sistema vuelve a la lista de finalistas.
- **A4 (paso 5):** La postulación ya no está «calificada» (otro administrador la devolvió, archivó, rechazó o adjudicó mientras tanto, o se devolvió sin que esta petición lo supiera) → `409` (`ERR_DEV_01`); el sistema no devuelve nada, informa el estado actual y actualiza el expediente. Si dos administradores devuelven a la vez, el sistema atiende una petición después de la otra y la segunda recibe este `409`. Lo mismo pasa con un doble clic o un reintento de red que llegue después de la primera petición: el frontend deshabilita el botón mientras se envía, y si aun así llega una segunda petición, recibe este `409` (no se registra una segunda devolución).
- **A5 (paso 5):** El finalista no tiene ninguna auditoría registrada después de su última devolución (nunca se auditó, o ya se devolvió y el postulante reenvió pero todavía no se volvió a auditar) → `409` (`ERR_DEV_02`); el sistema no devuelve nada e indica que primero hay que registrar la auditoría del expediente (CU-07).
- **A6 (paso 7):** Falla el envío del aviso al postulante → la devolución queda registrada y el aviso queda en su bandeja; el servicio de CU-EXTRA-01 reintenta el push y el Administrador no pierde lo que hizo. El postulante ve las observaciones al consultar su postulación ([CU-04](CU04%20-%20Consultar%20estado%20de%20postulación.md)).
- **A7 (cualquier paso):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario al inicio de sesión.
- **A8 (cualquier paso):** El usuario no tiene el rol Administrador DUBSS → `403`; no ve la acción ni puede usar el endpoint.
- **A9 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y posibilidad de reintentar; sin datos a medias (el cambio de estado, el historial y la bitácora se guardan juntos o no se guarda ninguno).
- **A10 (posterior al paso 8, fuera de este CU):** El postulante corrige y reenvía con el CU-02: la postulación vuelve a «pendiente», aumenta su número de envíos y se conserva el historial de devoluciones y reenvíos. Se repite la misma revisión de siempre y sigue el flujo normal: se califica de nuevo, y si cumple, se audita (CU-07, con nueva entrega física si hace falta; las auditorías anteriores se conservan) y avanza a la adjudicación.

## Reglas de negocio

- **Solo el Administrador DUBSS** ejecuta este CU (RF-14). El Postulante solo recibe el aviso y ve las observaciones.
- **Solo se devuelve una postulación «calificada» de un finalista.** Se llega desde su expediente; no hay una lista aparte de postulaciones para devolver. *(Decisión del equipo, igual que en el CU-07: la lista de finalistas del CU-21 es la única entrada.)*
- **Exige una auditoría previa; no exige entrega física.** Siempre debe haber una auditoría del expediente (CU-07, con cualquier resultado; lo normal es «Observado», A2) registrada **después de la última devolución**: tras un reenvío hay que auditar de nuevo antes de poder devolver otra vez. No se exige la entrega física registrada. *(Decisión de la DUBSS: nunca se devuelve sin auditoría.)*
- **Motivo:** obligatorio, máximo **500 caracteres** *(mismo límite que las observaciones de la auditoría; definido por el equipo)*. Es lo que el postulante lee, así que debe decir qué corregir. En la base va en `postulacion_historial.motivo`, que es `text`: el límite lo hace el DTO, no la columna, porque el CU-20 y el CU-05 usan el mismo campo.
- **Qué se guarda** (diseño lógico, `postulacion_historial`): la postulación, el estado anterior («calificada»), el estado nuevo («devuelta»), el usuario que la devolvió, la fecha y hora y el motivo. Es **append-only**: una fila no se edita ni se borra; el historial de devoluciones y reenvíos se conserva completo.
- **Una sola devolución a la vez, pero sin límite en total.** Mientras la postulación está «devuelta» no se puede devolver otra vez (`ERR_DEV_01`); cuando el postulante la reenvía y se califica y audita de nuevo puede devolverse otra vez, tantas veces como haga falta. *(Decisión de la DUBSS.)*
- **No se tocan** los documentos, las respuestas, el puntaje de la IA ni las auditorías anteriores del finalista, ni la fila de `finalista` (su entrega y su aviso se quedan como están). Al reenviar, el postulante pasa otra vez por la misma revisión y el flujo normal (CU-02, CU-03, CU-05, CU-07); cómo se vuelve a elegir al finalista y si se pide de nuevo la entrega física lo definen esos CU. *(Decisión de la DUBSS: el reenvío sigue el flujo normal.)*
- **Aviso al postulante:** siempre que se devuelve, el postulante recibe un aviso (**bandeja, push y correo**) con las observaciones. Usa el servicio de CU-EXTRA-01 con un evento nuevo de plantilla propia, que sí envía correo; un fallo de envío nunca deshace la devolución. Aquí **no hay opción de no avisar**. *(Decisión del equipo: cada cambio se avisa al postulante.)*
- **Bitácora:** cada devolución queda anotada (quién, cuándo, código de la postulación y del finalista, estado anterior y nuevo, sin el motivo). El motivo vive en el historial, no en la bitácora. Nunca nombres, CI, contraseñas, tokens ni rutas de almacenamiento. *(Decisión propuesta, por revisar con la DUBSS.)*
- **Sin plazo de reenvío:** el postulante reenvía cuando quiera; si no lo hace, es su responsabilidad. El sistema no guarda ni hace cumplir ningún plazo, el aviso no lo menciona y la postulación se queda «devuelta» hasta que el postulante la reenvíe. *(Decisión de la DUBSS; reemplaza el «dentro de un plazo» del RF-14.)*
- **Lista de finalistas (CU-21):** una vez devuelta, el finalista sigue en la lista, que muestra además el estado de su postulación («Devuelta») para que no se intente auditar. *(Decisión propuesta, por revisar.)*
- **Lista priorizada (CU-06):** al dejar de estar «calificada», la postulación sale de la lista; vuelve cuando el postulante reenvíe y se califique.
- **Datos personales:** solo los ve el Administrador DUBSS; ningún dato del postulante viaja en la bitácora ni en los errores.
- **Fuera de este CU:** archivar o rechazar (CU-20), el reenvío del postulante y el aumento de `numero_envios` (CU-02), recalcular el puntaje al reenviar (CU-03), elegir de nuevo al finalista y adjudicar (CU-05), consultar el estado y las observaciones desde la vista del postulante (CU-04) y exportar el historial.
- Todos los mensajes al usuario están en español y no exponen detalles técnicos.

## Postcondiciones

- La postulación queda en estado «devuelta» y el historial tiene una fila nueva con el estado anterior, el nuevo, el usuario, la fecha y el motivo.
- El postulante tiene en su bandeja el aviso con las observaciones (y el push, si pudo enviarse).
- La acción queda en la bitácora; las auditorías, el puntaje y los documentos del finalista no cambian.

## Criterios de aceptación

- **CA01:** Dado que el Administrador devuelve la postulación calificada de un finalista con un motivo válido, el sistema debe cambiar su estado a «devuelta» y registrar en el historial el estado anterior, el nuevo, el usuario, la fecha y el motivo.
- **CA02:** Dado que el motivo está vacío o es solo espacios, el sistema debe rechazar la devolución con `400` y no cambiar nada.
- **CA03:** Dado que el motivo supera 500 caracteres, el sistema debe rechazar la devolución con `400` y no cambiar nada.
- **CA04:** Dado que la postulación ya no está «calificada» (por ejemplo, ya está «devuelta»), el sistema debe rechazar la devolución con `409` e informar el estado actual.
- **CA05:** Dado que la misma petición llega dos veces (doble clic o reintento), el sistema debe registrar una sola devolución, un solo aviso y una sola anotación en la bitácora, y responder `409` a la segunda; el botón «Devolver» debe quedar deshabilitado mientras se envía.
- **CA06:** Dado que se registra una devolución, el sistema debe avisar al postulante, por bandeja, push y correo, con las observaciones, y no perder la devolución si el envío falla.
- **CA07:** Dado cualquier registro de devolución, el sistema debe anotarlo en la bitácora sin nombres, CI, motivo, rutas ni tokens.
- **CA08:** Dado que el Administrador cancela la ventana de confirmación, el sistema no debe cambiar el estado de la postulación.
- **CA09:** Dado una postulación devuelta, el sistema no debe permitir registrar una auditoría de su expediente (`409`, CU-07) ni mostrarla en la lista priorizada, y debe conservar sus auditorías anteriores y su puntaje.
- **CA10:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`.
- **CA11:** Dado un finalista inexistente o eliminado, el sistema debe responder `404`.
- **CA12:** Dado un finalista cuya postulación fue devuelta, la lista de finalistas debe mostrarla como «Devuelta».
- **CA13:** Dado que dos administradores devuelven a la vez la misma postulación, el sistema debe registrar una sola devolución y responder `409` a la otra.
- **CA14:** Dado que el finalista no tiene ninguna auditoría registrada después de su última devolución, el sistema debe rechazar la devolución con `409` e indicar que primero hay que auditar el expediente.
- **CA15:** Dado que la postulación se reenvió tras una devolución, el sistema debe permitir devolverla de nuevo una vez auditada, sin límite de devoluciones.
- **CA16:** Dado una devolución registrada, el sistema debe conservar el historial completo de devoluciones y reenvíos de la postulación, sin editar ni borrar filas anteriores.

## Cambios al diseño lógico

- **Tabla `postulacion_historial`:** el diseño la define (`id`, `postulacion_id` FK, `usuario_id` FK, `estado_anterior`, `estado_nuevo`, `motivo`, y las columnas de fecha y borrado lógico de toda tabla) pero **no existe en la base**; este CU la crea con la migración correspondiente, con el índice por `postulacion_id` que pide el historial, **sin ninguna columna extra respecto al diseño de grado** (no lleva clave de idempotencia). El CU-02, el CU-20 y el CU-05 la usarán sin recrearla.
- **Eventos de notificación:** `POSTULACION_DEVUELTA` (tipo `WARNING`, bandeja, push y correo, con `{nombres}` y `{observaciones}`) en el catálogo del servicio de CU-EXTRA-01.
- **Códigos de error** del módulo (prefijo `DEV`, a documentar en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` y su índice): `ERR_DEV_01` (`409`, la postulación ya no está «calificada») y `ERR_DEV_02` (`409`, el finalista no tiene una auditoría posterior a su última devolución). El finalista inexistente reutiliza `ERR_FIN_01`; el motivo vacío o de más de 500 caracteres cae en `ERR_SYS_01`.

## Preguntas abiertas

- **Entrega física al reenviar:** al repetirse el flujo normal, queda por confirmar en el CU-05/CU-02 si se vuelve a pedir la documentación física o vale la ya registrada.

## Desarrollador

Mauro Gutierrez
