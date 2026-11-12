# Caso de uso

| Id   | Nombre                    | Módulo                       | Prioridad | Estado    |
| ---- | ------------------------- | ---------------------------- | --------- | --------- |
| CU05 | Auditar y adjudicar becas | P4. Auditoría y adjudicación | Alta      | Pendiente |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** ninguno en este CU. El postulante **no** recibe aviso al decidir: se entera al publicar los resultados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md), decisión 3).
**Objetivo:** Que el Administrador DUBSS conduzca la selección de los beneficiarios de una convocatoria: elegir como finalistas a los postulantes mejor puntuados, auditar sus expedientes y registrar, para cada finalista, la decisión de **adjudicar** o **no adjudicar** la Beca Comedor, dentro de los cupos y del puntaje mínimo de la convocatoria.
**Plataforma:** Web
**RF asociados:** RF-04

**Descripción:**
Es el caso de uso que gobierna el proceso de adjudicación de una convocatoria **cerrada**. El Administrador DUBSS entra a «Auditoría y adjudicación», elige la convocatoria y el sistema muestra la lista priorizada por puntaje de vulnerabilidad ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md), que este CU incluye). De esa lista **selecciona a los finalistas** (los mejor puntuados) y el sistema los registra con la posición que ocupan. Luego **audita el expediente de cada finalista** ([CU-07](CU07%20-%20Auditar%20expediente%20finalista.md), incluido), que a su vez resuelve la notificación y la entrega de la documentación física ([CU-21](CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md)). Con el expediente auditado, el Administrador **registra la decisión final**: adjudicar o no adjudicar. El sistema guarda la adjudicación (resultado, puntaje final, quién decidió y cuándo), cambia la postulación de «calificada» a «adjudicada» o «no adjudicada», lo deja en el historial de la postulación y en la bitácora, y actualiza los cupos disponibles.

Para **adjudicar**, el sistema exige tres cosas: que la última auditoría del finalista sea «Validado», que su puntaje total alcance el puntaje mínimo de la convocatoria y que queden cupos. Para **no adjudicar** no pide ninguna de las tres condiciones de puntaje o cupo, pero sí un **motivo** y que el expediente haya sido auditado. Cuando los cupos se agotan, solo se admite «no adjudicar».

Lo extienden el [CU-08](CU08%20-%20Generar%20acta%20de%20adjudicación.md) (acta de adjudicación) y el [CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md) (publicar resultados), que se ejecutan cuando las decisiones están tomadas. Alimenta el [CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md): los adjudicados son el punto de partida del padrón de becados.

**Cómo se distingue de «rechazar» (CU-20).** Ambos dejan la postulación en «no adjudicada», pero son decisiones distintas: **rechazar** ([CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md)) descarta un expediente que *no procede* (documentación no válida, no entregó en plazo) y **no crea** una adjudicación; **no adjudicar** (este CU) es la *decisión final* sobre un finalista cuyo expediente sí se evaluó (por ejemplo, validado pero sin cupo o sin puntaje suficiente) y **sí crea** una fila en `adjudicacion` con resultado `NO_ADJUDICADA`. Ver [Distinción con el CU-20](#distinción-con-el-cu-20-rechazar).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- La convocatoria está **cerrada** a la recepción de postulaciones ([CU-13](CU13%20-%20Gestionar%20convocatoria.md); estado `CERRADA`).
- La convocatoria tiene definidos el **número de cupos** y el **puntaje mínimo** (valores por definir con la DUBSS; en la base son opcionales, ver decisión 4). Opcionalmente tiene también la **cantidad de finalistas**, que el Administrador define al configurar la convocatoria (CU-13) y que solo él ve (decisión 1).
- Existen postulaciones en estado «calificada» con puntaje de vulnerabilidad vigente ([CU-03](CU03%20-%20Precalificar%20con%20IA.md)).
- **Datos de apoyo (semilla):** el CU-02, el CU-03 y el CU-13 los implementan otros integrantes del equipo y pueden no existir aún. Se reutiliza el cargador de datos de prueba que ya existe (solo desarrollo): convocatoria cerrada con cupos y puntaje mínimo, postulaciones «calificadas» con puntaje, finalistas con y sin auditoría (validada, observada, no válida), con entrega física registrada y sin registrar. Para probar los casos límite se agregan, solo en desarrollo, una convocatoria con cupos casi agotados y otra con resultados ya publicados.

## Flujo principal

1. El Administrador DUBSS selecciona «Auditoría y adjudicación» en el menú y elige la convocatoria a procesar (A1, A10).
2. El sistema muestra el panel de la convocatoria: cupos totales, adjudicados, no adjudicados, cupos disponibles y puntaje mínimo; y la lista priorizada de postulantes con su posición y puntaje (se incluye el CU-06) (A1).
3. El Administrador selecciona como finalistas a los postulantes mejor puntuados. El sistema propone la **cantidad de finalistas** configurada en la convocatoria y el Administrador puede ajustarla antes de confirmar (A2, A3).
4. El sistema registra a cada finalista con la posición que ocupa en la lista priorizada y lo agrega a la lista de finalistas (CU-21).
5. El Administrador audita el expediente de cada finalista, notificando la entrega física y registrando su recepción cuando corresponde (se incluye el CU-07, que incluye al CU-21).
6. El sistema muestra la lista de finalistas con el resultado de la última auditoría de cada uno, el estado de su postulación y los cupos disponibles.
7. El Administrador abre el expediente de un finalista auditado y selecciona «Adjudicar» o «No adjudicar» (A11).
8. El sistema muestra una ventana de confirmación con el nombre del postulante, su puntaje total y los cupos disponibles; si es «No adjudicar», pide el **motivo** (obligatorio, hasta 500 caracteres).
9. El Administrador confirma (A11).
10. El sistema valida que el finalista exista (A10), que la postulación siga «calificada» y los resultados no estén publicados (A6, A7), que el expediente esté auditado (A5) y, **solo si adjudica**, que la última auditoría sea «Validado» (A5), que el puntaje total alcance el mínimo (A4) y que queden cupos (A3).
11. El sistema registra la adjudicación (resultado, puntaje final, usuario que decide y fecha de adjudicación), cambia el estado de la postulación a «adjudicada» o «no adjudicada», lo anota en el historial de la postulación (estado anterior, estado nuevo, usuario, fecha y, si no adjudica, el motivo) y en la bitácora, y actualiza los cupos disponibles, todo en una misma operación (A9).
12. El sistema confirma la decisión y el expediente pasa a mostrar la postulación como «Adjudicada» o «No adjudicada», sin permitir más acciones.
13. Los pasos 7 a 12 se repiten hasta decidir sobre todos los finalistas.
14. Con las decisiones tomadas, el Administrador puede generar el acta (CU-08) y publicar los resultados (CU-22) (A8).

## Flujos alternativos y excepciones

- **A1 (paso 2):** La convocatoria no tiene postulaciones calificadas ni finalistas → el sistema informa que no hay postulaciones para auditar y no ofrece seleccionar finalistas.
- **A2 (paso 3):** El Administrador indica una cantidad inválida (cero, negativa o mayor que las postulaciones calificadas que aún no son finalistas) → `400` (`ERR_SYS_01`); no se selecciona a nadie. Si la convocatoria no tiene cantidad configurada, el Administrador debe escribirla.
- **A3 (pasos 3 y 10):** Al **seleccionar**, la cantidad pedida supera los cupos disponibles → se permite (decisión 1): los finalistas extra son suplentes y solo los cupos limitan las adjudicaciones. Al **adjudicar**, los cupos de la convocatoria están agotados → `409` (`ERR_ADJ_04`); el sistema no adjudica, informa que solo se admite «No adjudicar» y actualiza el expediente. Dos administradores adjudicando a la vez no pueden superar los cupos: el sistema atiende una petición tras otra y la que ya no tiene cupo recibe este `409`.
- **A4 (paso 10):** El puntaje total del finalista (puntaje de la IA vigente + puntaje académico de la última auditoría) no alcanza el puntaje mínimo → `409` (`ERR_ADJ_03`); no se adjudica y se informa el puntaje y el mínimo. Sí puede «No adjudicar».
- **A5 (paso 10):** El finalista no tiene ninguna auditoría registrada → `409` (`ERR_ADJ_02`) para cualquiera de las dos decisiones. Si tiene auditoría pero la última **no** es «Validado» → `409` (`ERR_ADJ_02`) solo al **adjudicar**; el sistema indica que hay que auditar de nuevo o usar «No adjudicar» (o rechazar, CU-20).
- **A6 (paso 10):** La postulación ya no está «calificada» (otro administrador la devolvió, archivó, rechazó, adjudicó o no adjudicó mientras tanto, o la petición está repetida por un doble clic o un reintento de red) → `409` (`ERR_ADJ_01`); no se cambia nada, se informa el estado actual y se actualiza el expediente. El botón se deshabilita mientras se envía; una segunda petición recibe este `409` y no registra una segunda decisión (ver decisión 2).
- **A7 (paso 10):** Los resultados de la convocatoria ya fueron publicados (CU-22) → `409` (`ERR_ADJ_01`); no se registran ni modifican decisiones.
- **A8 (paso 14):** Quedan finalistas sin decidir → el sistema avisa cuántos faltan; qué se exige exactamente para publicar es del CU-22 (decisión 7).
- **A9 (cualquier paso):** Falla la conexión con el servidor o la base de datos → mensaje claro y posibilidad de reintentar; sin datos a medias (la adjudicación, el cambio de estado, el historial y la bitácora se guardan juntos o no se guarda ninguno).
- **A10 (pasos 1 y 10):** La convocatoria o el finalista no existe o fue eliminado → `404` (`ERR_PRI_01` para la convocatoria, `ERR_FIN_01` para el finalista) y el sistema vuelve a la lista.
- **A11 (pasos 7 y 9):** El Administrador cancela la ventana → no se cambia nada.
- **A12 (paso 1):** La convocatoria **no está cerrada**, o no tiene cupos o puntaje mínimo definidos → `409` (`ERR_ADJ_05`); se puede consultar la lista priorizada (CU-06), pero no seleccionar finalistas ni decidir (decisión 4).
- **A13 (cualquier paso):** Sesión expirada o inválida → `401` y el sistema lleva al usuario al inicio de sesión.
- **A14 (cualquier paso):** El usuario no tiene el rol Administrador DUBSS → `403`; no ve la opción ni puede usar los endpoints.

## Reglas de negocio

- **Solo el Administrador DUBSS** ejecuta este CU (RF-04). El Directivo ve los resultados agregados en el CU-12; el Postulante no interviene.
- **Dos momentos, una entrada.** *Seleccionar finalistas* se hace a nivel de **convocatoria**; *adjudicar / no adjudicar* se hace a nivel de **finalista**, desde su expediente (CU-07), igual que devolver (CU-19) y archivar o rechazar (CU-20): la lista de finalistas es la única entrada para decidir. *(Decisión del equipo, para no tener tres pantallas distintas de acciones sobre el mismo expediente.)*
- **Cantidad de finalistas (decisión 1):** es un dato de la convocatoria que el Administrador define al crearla o configurarla ([CU-13](CU13%20-%20Gestionar%20convocatoria.md)) y que **solo el Administrador DUBSS ve**: ni el postulante ni el público lo reciben en las consultas de convocatorias. Al seleccionar, el sistema la propone y el Administrador **puede cambiarla** para esa selección (la cantidad configurada no se modifica). Debe ser un entero positivo, no mayor que las postulaciones calificadas que aún no son finalistas. Puede ser mayor que los cupos (suplentes). *(Detalle de la edición al seleccionar: propuesta del equipo, por revisar.)*
- **Selección de finalistas:** toma las postulaciones «calificadas» de la convocatoria **en orden de la lista priorizada** (CU-06: puntaje vigente, desempate por fecha de envío) que todavía no son finalistas, y guarda en `finalista.posicion` el lugar que ocupan en la lista **completa** de la convocatoria (no en la página). Puede repetirse para sumar finalistas (por ejemplo, para cubrir los que se archivaron) sin repetir a los ya seleccionados. Una postulación es finalista una sola vez (índice único existente); si fue devuelta y reenviada, **conserva su fila** y su posición (decisión 5).
- **Qué significa «auditado».** El finalista tiene al menos una auditoría registrada (CU-07). La que vale es siempre la **última**. Solo una última auditoría «Validado» habilita adjudicar.
- **Puntaje total** (CU-07): puntaje vigente de la IA (hasta 70) + puntaje académico de la última auditoría (hasta 30). No se guarda: se calcula al decidir y se copia a `adjudicacion.puntaje_final`, que queda **inmutable** aunque un reprocesamiento posterior (CU-26) cambie el puntaje.
- **Puntaje mínimo:** se compara contra el puntaje **total**. Es un valor de la convocatoria (`convocatoria.puntaje_minimo`) y no está definido: queda «por definir con la DUBSS». *(No se inventa un valor.)*
- **Cupos:** `convocatoria.cupos`. Cupos disponibles = cupos − adjudicaciones con resultado `ADJUDICADA` de esa convocatoria; **se calcula, no se guarda**. Solo cuentan las «adjudicadas», no las «no adjudicadas». Para que dos decisiones simultáneas no superen los cupos, la verificación y el registro se hacen bloqueando la convocatoria durante la operación.
- **Motivo:** obligatorio **solo** al no adjudicar, máximo **500 caracteres** (mismo límite que el CU-19 y el CU-20). Se guarda en `postulacion_historial.motivo`. Al adjudicar no hay motivo.
- **Qué se guarda:** una fila en `adjudicacion` (una por postulación, resultado `ADJUDICADA` o `NO_ADJUDICADA`, puntaje final, quién decidió y fecha de adjudicación) y una fila nueva en `postulacion_historial` (append-only) con estado anterior «calificada» y estado nuevo «adjudicada» o «no adjudicada».
- **Las decisiones son finales.** «Adjudicada» y «no adjudicada» son estados finales (ciclo de vida del diseño lógico): no se vuelve a «calificada» ni se cambia una decisión (decisión 6). Antes de la publicación el Administrador puede equivocarse, así que la ventana de confirmación muestra el nombre, el puntaje y el resultado.
- **No se tocan** documentos, respuestas, puntaje de la IA, auditorías ni la fila de `finalista`.
- **Resultados publicados:** tras publicar (CU-22) no se registran ni modifican decisiones.
- **Aviso al postulante:** este CU **no avisa** por sí mismo; la adjudicación se comunica al **publicar los resultados** (CU-22), de modo que nadie se entere antes de la publicación oficial (decisión 3). *(Decisión propuesta, por revisar con la DUBSS.)*
- **Bitácora:** cada decisión queda anotada (quién, cuándo, códigos de postulación, finalista y convocatoria, resultado y estados anterior y nuevo, **sin** el motivo, nombres, CI, rutas ni tokens). Seleccionar finalistas también se anota (quién, cuándo, convocatoria y cantidad). *(Decisión propuesta, por revisar.)*
- **Lista de finalistas (CU-21):** ya muestra el estado de la postulación; pasa a mostrar «Adjudicada» / «No adjudicada» para que no se intente auditar ni decidir otra vez.
- **Lista priorizada (CU-06):** al dejar de estar «calificada», la postulación sale de la lista.
- **Fuera de este CU:** acta de adjudicación (CU-08), publicar resultados y avisar a los postulantes (CU-22), padrón de becados (CU-23), bajas y reemplazos, apelación del postulante, reparto de cupos por carrera y lista de espera de suplentes.
- Todos los mensajes están en español y no exponen detalles técnicos.

### Distinción con el CU-20 (rechazar)

| | **Rechazar** (CU-20) | **No adjudicar** (CU-05) |
| --- | --- | --- |
| Cuándo | El expediente *no procede* (documentación no válida, no entregó en plazo) | El expediente se evaluó; la decisión final es no dar la beca (sin cupo, sin puntaje suficiente, otro motivo) |
| Estado resultante | `no_adjudicada` | `no_adjudicada` |
| Fila en `adjudicacion` | **No** | **Sí** (`NO_ADJUDICADA`, con `puntaje_final`) |
| Requiere auditoría | No | Sí, de cualquier resultado |
| Motivo | Obligatorio | Obligatorio |
| Aviso al postulante | Inmediato (bandeja, push, correo) | Al publicar resultados (CU-22) |
| Cuenta contra los cupos | No | No |

Para reportes (CU-12) la distinción **no necesita columna nueva**: «no adjudicada por decisión final» = postulación `no_adjudicada` **con** fila en `adjudicacion`; «rechazada» = `no_adjudicada` **sin** esa fila (ambas con su motivo en el historial). Ver decisión 8.

## Postcondiciones

- Cada finalista decidido tiene una adjudicación registrada con su resultado, puntaje final, responsable y fecha de adjudicación.
- La postulación queda «adjudicada» o «no adjudicada»; el historial tiene una fila nueva (con el motivo, si no se adjudicó) y la decisión está en la bitácora.
- Los cupos disponibles de la convocatoria reflejan las adjudicaciones; auditorías, documentos y puntaje de la IA no cambian.

## Criterios de aceptación

- **CA01:** Dado que la convocatoria está cerrada y tiene postulaciones calificadas, el sistema debe mostrar la lista priorizada y el panel de cupos, adjudicados y puntaje mínimo.
- **CA02:** Dado que el Administrador selecciona finalistas (con la cantidad configurada en la convocatoria o la que ajuste), el sistema debe registrarlos en el orden de la lista priorizada con la posición que ocupan en la lista completa, sin repetir a los ya seleccionados.
- **CA03:** Dado que la cantidad es cero, negativa o mayor que las postulaciones disponibles, el sistema debe responder `400` y no seleccionar a nadie.
- **CA04:** Dado un finalista cuya última auditoría es «Validado», con puntaje total igual o mayor al mínimo y con cupos disponibles, cuando el Administrador adjudica, el sistema debe registrar la adjudicación con puntaje final, usuario y fecha, y cambiar la postulación a «adjudicada».
- **CA05:** Dado un finalista auditado, cuando el Administrador no adjudica con un motivo válido, el sistema debe registrar la adjudicación con resultado `NO_ADJUDICADA` y cambiar la postulación a «no adjudicada».
- **CA06:** Dado que no adjudica sin motivo, solo con espacios o con más de 500 caracteres, el sistema debe responder `400` y no cambiar nada.
- **CA07:** Dado un finalista sin ninguna auditoría, el sistema debe rechazar cualquier decisión con `409` (`ERR_ADJ_02`).
- **CA08:** Dado un finalista cuya última auditoría no es «Validado», el sistema debe rechazar «Adjudicar» con `409` (`ERR_ADJ_02`) y permitir «No adjudicar».
- **CA09:** Dado un puntaje total menor al mínimo, el sistema debe rechazar «Adjudicar» con `409` (`ERR_ADJ_03`) y permitir «No adjudicar».
- **CA10:** Dado que los cupos están agotados, el sistema debe rechazar «Adjudicar» con `409` (`ERR_ADJ_04`) y permitir «No adjudicar».
- **CA11:** Dado que dos administradores adjudican a la vez cuando queda un solo cupo, el sistema debe adjudicar a uno solo y responder `409` al otro, sin superar los cupos.
- **CA12:** Dado que la postulación ya no está «calificada» o los resultados ya fueron publicados, el sistema debe responder `409` (`ERR_ADJ_01`) y no cambiar nada.
- **CA13:** Dado que la misma petición llega dos veces (doble clic o reintento), el sistema debe registrar una sola decisión y una sola anotación en la bitácora, y responder `409` a la segunda.
- **CA14:** Dado que la convocatoria no está cerrada o no tiene cupos o puntaje mínimo definidos, el sistema debe rechazar la selección y las decisiones con `409` (`ERR_ADJ_05`).
- **CA15:** Dada una decisión registrada, el sistema debe actualizar los cupos disponibles y conservar el historial completo, sin editar ni borrar filas anteriores.
- **CA16:** Dada una adjudicación registrada, el puntaje final debe conservar su valor aunque el puntaje de la IA se reprocese después.
- **CA17:** Dada una decisión o una selección de finalistas, el sistema debe anotarla en la bitácora sin nombres, CI, motivo, rutas ni tokens.
- **CA18:** Dado que el Administrador cancela la ventana de confirmación, el sistema no debe cambiar nada.
- **CA19:** Dada una postulación «adjudicada» o «no adjudicada», el sistema no debe mostrarla en la lista priorizada ni permitir auditarla ni decidirla otra vez.
- **CA20:** Dado que se registra una decisión, el sistema no debe avisar al postulante (se hará al publicar, CU-22).
- **CA22:** Dada una convocatoria con cantidad de finalistas configurada, el sistema debe proponerla al seleccionar, permitir ajustarla sin cambiar la configurada, y no exponerla en ninguna consulta de postulantes ni pública.
- **CA21:** Dado un usuario que no es Administrador DUBSS, el sistema debe responder `403`; sin sesión válida, `401`; con convocatoria o finalista inexistente, `404`.

## Cambios al diseño lógico

- **Tabla `adjudicacion`** (nueva; el diseño la define pero no existe en la base): `id`, `codigo_adjudicacion`, `postulacion_id` FK (único entre las no eliminadas), `resultado` (`ADJUDICADA` / `NO_ADJUDICADA`), `puntaje_final` `numeric(5,2)`, `decidido_por` FK→`usuario`, `fecha_adjudicacion` `timestamptz` y las columnas comunes de toda tabla. Índice por `postulacion_id`. Sin columnas extra respecto al diseño de grado (no lleva motivo ni clave de idempotencia).
- **Tabla `convocatoria` (columna nueva, decisión 1):** `cantidad_finalistas` `int` opcional, `CHECK (cantidad_finalistas IS NULL OR cantidad_finalistas > 0)`. **No está en el diseño de grado**: hay que agregarla primero al diseño lógico (`diseno_base_de_datos.md`, imagen 091 y `docs/DiagramaConceptual.mermaid`) y luego crear una migración nueva (la tabla ya existe). Solo se devuelve en los endpoints del Administrador; los de consulta del postulante o públicos no la incluyen.
- **Impacto en el CU-13:** el formulario de crear y editar convocatoria gana el campo «Cantidad de finalistas» (visible solo para el Administrador DUBSS), con las mismas reglas que los cupos; el CU-13 debe validar que sea positiva y, *(propuesta)*, que no sea menor que los cupos cuando ambos existan, porque con menos finalistas que cupos no se podrían cubrir. Se actualiza el CU-13 y el RF-08 del documento de taller.
- **Tabla `finalista`:** sin columnas nuevas; este CU solo **crea filas** (hoy las crea el cargador de datos de prueba) y escribe `posicion`.
- **Tabla `postulacion_historial`:** sin cambios; se reutiliza. Se aclara que `motivo` es obligatorio también al **no adjudicar** (en el diseño solo figura al devolver, archivar o rechazar).
- **Cupos disponibles:** se calculan, no hay columna.
- **Códigos de error** (prefijo `ADJ`, propuesto, a documentar en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` y su índice al implementar): `ERR_ADJ_01` (`409`, la postulación ya no está «calificada» o los resultados están publicados), `ERR_ADJ_02` (`409`, el expediente no está auditado o la última auditoría no es «Validado»), `ERR_ADJ_03` (`409`, puntaje total menor al mínimo), `ERR_ADJ_04` (`409`, cupos agotados), `ERR_ADJ_05` (`409`, convocatoria no cerrada o sin cupos o puntaje mínimo). Convocatoria inexistente reutiliza `ERR_PRI_01`; finalista inexistente, `ERR_FIN_01`; cantidad o motivo inválido, `ERR_SYS_01`.
- **Endpoints** (propuestos, mismo estilo que el CU-19 y el CU-20): `GET /api/v1/convocatorias/{codigo}/adjudicacion` (panel: cupos, adjudicados, disponibles, mínimo, cantidad de finalistas), `POST /api/v1/convocatorias/{codigo}/finalistas` con `{ cantidad }` opcional (si falta, se usa `cantidad_finalistas` de la convocatoria) (seleccionar), `POST /api/v1/finalistas/{codigo}/adjudicar` y `POST /api/v1/finalistas/{codigo}/no-adjudicar` con `{ motivo }` (solo la segunda).

## Decisiones confirmadas

Confirmadas por Mau (Administrador del proyecto) el 2026-11-09. Aún no se validaron con la DUBSS.

1. **Cantidad de finalistas.** Se define al iniciar o configurar la convocatoria (CU-13), la ve solo el Administrador, y se usa al seleccionar finalistas. Puede ser mayor que los cupos (suplentes). *(Reemplaza la propuesta de escribirla en cada selección; el documento de taller la deja «por definir».)*
2. **Idempotencia.** Una petición duplicada recibe `409` (`ERR_ADJ_01`), como en el CU-19 y el CU-20, sin clave de idempotencia. El texto de A7 del taller debe ajustarse.
3. **Aviso al postulante.** No se avisa al decidir; se avisa al publicar los resultados (CU-22). El CU-04 muestra «adjudicada» o «no adjudicada» (por decisión final) solo después de la publicación; antes sigue viendo «calificada». El rechazo del CU-20 se ve de inmediato.
4. **Cupos y puntaje mínimo.** Si falta alguno, o la convocatoria no está cerrada, no se puede seleccionar ni decidir (`ERR_ADJ_05`); se configuran en el CU-13.
5. **Postulación devuelta y reenviada.** Conserva su fila de `finalista` y su posición, y vuelve a auditarse (CU-07). Si debe volver a entregar la documentación física o vale la registrada queda pendiente (heredado del CU-19).
6. **Decisiones finales.** «Adjudicada» y «no adjudicada» no se reabren; A6 del taller se lee como «tras publicar no se registra nada».
7. **Todas decididas para publicar (CU-22).** Ningún finalista queda «calificada»: cada uno está adjudicado, no adjudicado, archivado o devuelto. Lo define el CU-22.
8. **Reportes de «no adjudicada».** Se distinguen por la fila en `adjudicacion`, sin columna nueva.
9. **«No adjudicar» exige una auditoría** de cualquier resultado; si no hay auditoría, la salida es el CU-20.
10. **Comunicados a `ADJUDICADOS`.** Se habilita en el CU-22, no aquí.
11. **Numeración.** `requisitos_funcionales_propuestos.md` del taller usa la numeración antigua; prevalece la vigente (CU-05).

## Pendiente de definir con la DUBSS

- Los **cupos** y el **puntaje mínimo** de cada convocatoria, y la cantidad de finalistas habitual.
- Si la DUBSS exige que la cantidad de finalistas sea al menos igual a los cupos (propuesta del CU-13, por revisar).

## Desarrollador

Mauro Gutierrez *(por confirmar)*
