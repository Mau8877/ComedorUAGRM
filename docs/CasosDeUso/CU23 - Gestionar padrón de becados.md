# Caso de uso

| Id   | Nombre                      | Módulo                          | Prioridad | Estado      |
| ---- | --------------------------- | ------------------------------- | --------- | ----------- |
| CU23 | Gestionar padrón de becados | P5. Becados y asistencia        | Media     | Hecho       |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Mantener actualizado el padrón de becados vigentes de la gestión, registrando altas, bajas, suspensiones y reemplazos.
**Plataforma:** Web
**RF asociados:** RF-18

**Descripción:**
El Administrador DUBSS da de alta como becados a los adjudicados de una convocatoria, consulta el padrón y registra sobre él las bajas, suspensiones y reemplazos que ocurren durante la gestión. Cada cambio actualiza el estado de la beca y queda registrado como un movimiento con su motivo, responsable y fecha, y en la bitácora ([CU-25](CU25%20-%20Consultar%20bit%C3%A1cora%20de%20acciones.md)). En un reemplazo, el cupo liberado se asigna al siguiente postulante priorizado. Los cambios se reflejan en las planillas de asistencia que se generen después ([CU-09](CU09%20-%20Generar%20planilla%20de%20asistencia.md)), que consideran solo las becas vigentes.

Los becados son los adjudicados de la convocatoria ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)) una vez publicados los resultados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicaci%C3%B3n.md)). Este CU **no** decide quién se adjudica, **no** cambia el estado de las postulaciones y **no** registra la asistencia ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)).

---

## Precondiciones

- El Administrador está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Los resultados de adjudicación de la convocatoria fueron publicados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicaci%C3%B3n.md)).
- Para los flujos B y C, la beca ya fue dada de alta (flujo D).

## Flujo principal

**Flujo D: dar de alta a los adjudicados.**

1. El Administrador selecciona la opción «Padrón de becados» y la convocatoria.
2. El sistema indica cuántos adjudicados de la convocatoria todavía no figuran en el padrón y ofrece «Dar de alta».
3. El Administrador confirma.
4. El sistema crea una beca vigente por cada adjudicado que aún no la tiene y deja constancia en la bitácora.

**Flujo A: consultar el padrón.**

5. El Administrador selecciona la opción «Padrón de becados» y la convocatoria.
6. El sistema muestra las becas de la convocatoria con el nombre, CI y carrera del becado y el estado de la beca (vigente, suspendida o baja).

**Flujo B: dar de baja o suspender una beca.**

7. El Administrador selecciona una beca vigente y la opción «Dar de baja» o «Suspender».
8. El sistema solicita el motivo.
9. El Administrador registra el motivo y confirma.
10. El sistema actualiza el estado de la beca a «baja» o «suspendida», según corresponda.
11. El sistema registra el movimiento (tipo, motivo, usuario y fecha) y deja constancia en la bitácora.

**Flujo C: reemplazar un cupo liberado.**

12. El Administrador selecciona una beca dada de baja y la opción «Reemplazar».
13. El sistema muestra al siguiente postulante de la lista priorizada disponible para ocupar el cupo.
14. El Administrador confirma el reemplazo.
15. El sistema crea una nueva beca vigente para el postulante reemplazante.
16. El sistema registra el movimiento de reemplazo, vinculando la beca liberada con la beca reemplazante, y deja constancia en la bitácora.

## Flujos alternativos y excepciones

- **A1 (paso 9):** no se registra el motivo → el sistema no permite completar la operación (`400`).
- **A2 (paso 7):** la beca no está vigente → el sistema no permite darla de baja ni suspenderla (`409`).
- **A3 (paso 12):** la beca seleccionada no fue dada de baja → el sistema no permite el reemplazo (`409`).
- **A4 (paso 13):** no existe un postulante disponible para ocupar el cupo → el sistema lo informa y no realiza el reemplazo (`409`).
- **A5 (pasos 3, 9 y 14):** el Administrador cancela la operación → el padrón no se modifica.
- **A6 (paso 1):** el usuario no tiene el rol Administrador DUBSS → `403`.
- **A7 (paso 1):** los resultados de la convocatoria todavía no fueron publicados → el sistema lo informa y no permite dar de alta ni mostrar padrón (`409`).
- **A8 (paso 2):** no hay adjudicados sin alta → el sistema lo informa y no crea nada. Repetir el alta no duplica becas (idempotencia, RNF-03).

## Reglas de negocio

- El padrón vigente es el conjunto de becas en estado «vigente»; las suspendidas y las dadas de baja no cuentan para las planillas de asistencia.
- El motivo es obligatorio en la baja y en la suspensión (A1).
- Solo una beca vigente se da de baja o se suspende (A2); solo una beca dada de baja se reemplaza (A3).
- Cada cambio de estado y cada reemplazo dejan un movimiento (tipo, motivo, usuario, fecha) y una fila en la bitácora. El movimiento de reemplazo vincula la beca liberada con la beca del reemplazante.
- Los movimientos y la bitácora no se editan ni se eliminan.
- Una beca dada de baja no se reactiva; la **reactivación** de una beca suspendida (tipo de movimiento `REACTIVACION` del modelo de datos) no figura en el flujo de este CU, ver «Decisiones pendientes».
- **Alta del padrón:** la lista de CU y el plan de tareas (B10) incluyen las «altas» en este CU. Se da de alta a los adjudicados de la convocatoria (resultado `ADJUDICADA`), una beca por adjudicación; el CU-22 no las crea (su CA14).

## Postcondiciones

- El padrón refleja el estado actualizado de cada beca, y cada cambio queda registrado como movimiento y en la bitácora.
- Las planillas de asistencia que se generen después ([CU-09](CU09%20-%20Generar%20planilla%20de%20asistencia.md)) consideran solo las becas vigentes.

## Decisiones pendientes (no definidas en la documentación de grado)

Estos puntos la documentación no los resuelve; se marcan aquí en vez de decidirlos en silencio y se confirman con la DUBSS o con el equipo:

1. **El flujo de alta no está desarrollado en el detalle del CU-23 de la documentación de grado** (que describe solo baja, suspensión y reemplazo), aunque la lista de CU lo incluye («Altas»). El flujo D de este CU es la lectura mínima: el Administrador da de alta a los adjudicados. Hay que agregarlo al detalle del CU-23 en la documentación de grado.
2. **Fechas de la beca.** `fecha_inicio` y `fecha_fin` existen en el modelo y ningún CU dice de dónde salen.
3. **El reemplazante y la adjudicación.** El texto dice «el siguiente postulante priorizado» y «crea una nueva beca vigente»; el diagrama de secuencia (img 103) llama `reemplazar(becaId, adjudicacionId)` y el modelo define `beca.adjudicacion_id` único. No se indica si el reemplazante recibe una adjudicación propia.
4. **Quién es «disponible» para reemplazar.** No se define el criterio (finalista validado, puntaje mínimo, sin decisión previa).
5. **Reactivar una suspensión.** El modelo de datos tiene el movimiento `REACTIVACION` y RF-18 no la nombra; el flujo del CU tampoco.

## Criterios de aceptación

- **CA00:** Dado que los resultados fueron publicados y hay adjudicados sin alta, el sistema debe crear una beca vigente por cada uno, sin duplicar si la operación se repite.
- **CA01:** Dado que los resultados de la convocatoria fueron publicados, el sistema debe mostrar el padrón con nombre, CI, carrera y estado de cada beca.
- **CA02:** Dado que se da de baja o se suspende una beca vigente con motivo, el sistema debe cambiar su estado y registrar el movimiento y la bitácora.
- **CA03:** Dado que no se indica el motivo, el sistema debe responder `400` y no cambiar la beca.
- **CA04:** Dado que la beca no está vigente, el sistema debe responder `409` y no dar de baja ni suspender.
- **CA05:** Dado que la beca fue dada de baja, el sistema debe permitir reemplazarla con el siguiente postulante priorizado disponible y crear su beca vigente, vinculada por un movimiento de reemplazo.
- **CA06:** Dado que la beca no fue dada de baja, el sistema debe responder `409` y no reemplazar.
- **CA07:** Dado que no hay postulante disponible, el sistema debe responder `409` y no reemplazar.
- **CA08:** Dado que los resultados no fueron publicados, el sistema debe responder `409` y no dar de alta ni mostrar padrón.
- **CA09:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`.
- **CA10:** Dado cualquier cambio del padrón, el sistema no debe permitir editar ni eliminar los movimientos ya registrados.

## Desarrollador

Leonel Barrios
