# Caso de uso

| Id   | Nombre                  | Módulo                     | Prioridad | Estado      |
| ---- | ----------------------- | -------------------------- | --------- | ----------- |
| CU03 | Precalificar con IA     | P3. Preselección con IA    | Alta      | En progreso |

**Actor principal:** Sistema (se dispara cuando el Postulante envía su postulación, [CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md))
**Actores secundarios:** Módulo de IA
**Objetivo:** Que cada postulación enviada reciba automáticamente un puntaje de vulnerabilidad, para ordenar a los postulantes según su necesidad sin revisar uno por uno.
**Plataforma:** Backend y módulo de IA; el cálculo se dispara al enviar la postulación desde la web o la app, y el Administrador consulta el resultado en una pantalla web de solo lectura
**RF asociados:** RF-03 (lo extiende el RF-21, [CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md))

**Descripción:**
Cuando el Postulante envía su postulación ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)), el sistema le confirma el envío y, **en segundo plano**, pide el puntaje al módulo de IA: le manda las respuestas socioeconómicas de la postulación, el módulo las evalúa con las ponderaciones de la versión vigente del modelo y devuelve un puntaje de **0 a 70**. El sistema lo guarda con su fecha de cálculo y la versión del modelo usada, y la postulación pasa de «pendiente» a «calificada»: desde ahí entra a la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)), que usa el Administrador DUBSS para elegir a los finalistas ([CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md)). Los 70 puntos de la IA se suman a los hasta 30 que pone el auditor por rendimiento académico ([CU-07](CU07%20-%20Auditar%20expediente%20finalista.md)), para un total de 100.

El Postulante **nunca espera ni pierde su envío** por esto: si el módulo de IA no responde, la postulación queda «pendiente» y el sistema vuelve a intentarlo solo hasta lograrlo. Este CU también es el que usa el [CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md) para recalcular puntajes con otra versión del modelo, conservando los anteriores.

> **Pantalla:** la pantalla «Precalificación con IA» del mockup de grado existe como consulta de solo lectura: se abre desde el detalle de la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)) y muestra el puntaje, el aporte de cada variable y la traza. El puntaje de la IA y el total también se ven en el expediente del finalista ([CU-07](CU07%20-%20Auditar%20expediente%20finalista.md)) y en la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)); ver [la tabla de diferencias](../Implementacion/CU03%20-%20Precalificar%20con%20IA.md).

> El módulo de IA **no lee los documentos adjuntos** ni usa reconocimiento de texto: solo recibe las respuestas del formulario socioeconómico. Tampoco recibe el nombre, el CI ni el contacto del Postulante: solo el código de la postulación y las variables del modelo.

---

## Precondiciones

- La postulación fue enviada ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)), superó la validación de elegibilidad y duplicados ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)) y está en estado «pendiente».
- La postulación tiene completas las respuestas que exige el modelo: ingresos familiares, dependientes, procedencia, patrimonio, estado de vulnerabilidad y promedio académico (la carrera viaja pero no puntúa).
- El módulo de IA está disponible. Si todavía no hay ninguna versión del modelo guardada ([CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md)), el módulo usa sus ponderaciones por defecto (versión `v1.0`).

## Flujo principal

1. El Postulante envía su postulación y el sistema le confirma la recepción ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)).
2. En segundo plano, el sistema prepara las respuestas de la postulación y la versión vigente del modelo con sus ponderaciones.
3. El sistema pide el cálculo al módulo de IA (A2, A5).
4. El módulo de IA valida que los datos estén completos y sean coherentes (A1).
5. El módulo de IA aplica las ponderaciones y calcula el puntaje de vulnerabilidad, de 0 a 70, con el aporte de cada variable (A7).
6. El módulo de IA devuelve el puntaje, la versión del modelo que usó y el detalle por variable.
7. El sistema comprueba que la versión exista y que el puntaje esté en el rango (A6), y registra el puntaje como **vigente** con la fecha y hora de cálculo y la versión del modelo, conservando los anteriores si los hubiera (A3, A4).
8. El sistema cambia la postulación a «calificada» y queda disponible en la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)).

## Flujos alternativos y excepciones

- **A1 (paso 4):** los datos son incompletos o incoherentes (por ejemplo, un promedio fuera de 0 a 100) → el módulo de IA no calcula, el sistema registra el motivo y la postulación sigue «pendiente»; se reintenta. Tras 3 rechazos queda **marcada para revisión** (así la llama el diagrama de grado) y el reintento automático deja de tomarla; reenviar la postulación reinicia la cuenta. _Sigue «pendiente»: no hay un estado nuevo ni una pantalla para revisarlas, y queda por definir con la DUBSS._
- **A2 (paso 3):** el módulo de IA no responde, está caído o excede el tiempo de espera (5 segundos) → el sistema lo registra, la postulación sigue «pendiente» y se **reintenta automáticamente cada 5 minutos**, de las más antiguas a las más recientes. Si el módulo sigue caído, el reintento se detiene en la primera falla en lugar de insistir con cada postulación.
- **A3 (paso 7, reprocesamiento):** si la solicitud viene del [CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md), el sistema calcula el puntaje con la versión pedida, el nuevo pasa a ser el vigente, el anterior queda en el historial y la postulación sigue «calificada». El módulo debe responder con la misma versión que se le pidió.
- **A4 (pasos 3 a 7):** si llega una solicitud repetida para una postulación que ya está «calificada» (o para la que ya tiene puntaje vigente de esa versión), no se genera un segundo puntaje (idempotencia).
- **A5 (paso 3):** ante picos de envíos (por ejemplo, al cierre de la convocatoria, hasta 20.000 por gestión) las solicitudes se encolan y se procesan en orden sin frenar a quien envía. Si la cola se llena, la postulación sigue «pendiente» y el reintento de A2 la recoge: ninguna se pierde.
- **A6 (paso 7):** el módulo de IA responde con una versión de modelo que no está registrada o con un puntaje fuera de rango → el sistema no registra nada, avisa en el registro de errores y la postulación sigue «pendiente».
- **A7 (paso 5):** la postulación no trae algunos datos del formulario oficial de la DUBSS (se envió antes de que el formulario los pidiera) → esas variables no entran al cálculo y el módulo reparte el 100 % entre las que sí tienen dato; el detalle lo indica.

## Reglas de negocio

- **El puntaje de la IA va de 0 a 70.** El total de 100 se completa con los hasta 30 puntos de rendimiento académico que registra el auditor en el [CU-07](CU07%20-%20Auditar%20expediente%20finalista.md). Nunca se devuelve ni se guarda más de 70.
- **Solo hay un puntaje vigente por postulación.** Los anteriores se conservan como historial, con su fecha y versión, para poder auditarlos; nunca se borran.
- **El cálculo individual debe tardar como máximo 5 segundos** en condiciones normales, y el sistema debe procesar todas las postulaciones de una gestión (hasta 20.000) sin intervención manual.
- **Las ponderaciones las decide la DUBSS** en el [CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md): cada versión tiene una ponderación por variable y suman 100. El módulo de IA no guarda ponderaciones: aplica las de la versión que recibe en cada solicitud.
- **El puntaje mide necesidad económica y familiar**, no mérito. Las variables que puntúan son los ingresos familiares, los dependientes, la procedencia, el patrimonio (vivienda), el estado de vulnerabilidad, la dependencia económica, la ocupación de quien sostiene, la discapacidad, los hijos y, con peso 0 por defecto, el promedio académico (el auditor ya suma el rendimiento académico en el CU-07). Es un **criterio adoptado** (pesos, nivel de cada opción y escala lineal de 100 a 70) que debe validar la DUBSS: _por definir con la DUBSS_.
- **El puntaje es explicable:** además del total, se guarda cuántos puntos aportó cada variable.
- **Una opción que el modelo no conoce** (por ejemplo, si cambia el catálogo de una lista) no rechaza la postulación: se usa un valor neutro y se avisa en el detalle.
- **Nunca bloquea al Postulante:** un fallo de la IA no cambia ni deshace el envío.
- **Privacidad:** el módulo de IA no recibe nombre, CI ni contacto, y las respuestas del formulario no se escriben en los registros del sistema.
- **Estados de la postulación afectados:** «pendiente» → «calificada» (RF-03). Una postulación que cambia de estado después ([CU-19](CU19%20-%20Devolver%20postulaci%C3%B3n%20para%20correcci%C3%B3n.md), [CU-20](CU20%20-%20Archivar%20o%20rechazar%20postulaci%C3%B3n.md), adjudicación) conserva su puntaje.

## Postcondiciones

- Cada postulación procesada tiene un puntaje de vulnerabilidad vigente, con su fecha de cálculo y la versión del modelo.
- La postulación queda «calificada» y disponible en la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)).
- Las postulaciones que no se pudieron calcular siguen «pendientes» y se reintentan.

## Criterios de aceptación

- **CA01:** Dado que hay una postulación «pendiente» con sus respuestas completas y el módulo de IA disponible, el sistema debe registrarle un puntaje vigente de 0 a 70 con su fecha de cálculo y la versión del modelo, y pasarla a «calificada».
- **CA02:** Dado que el módulo de IA no responde o excede los 5 segundos, el sistema debe dejar la postulación «pendiente», no afectar el envío del Postulante y reintentar automáticamente cada 5 minutos.
- **CA03:** Dado que el módulo de IA vuelve a estar disponible, el sistema debe calificar las postulaciones «pendientes», las más antiguas primero, sin intervención manual.
- **CA04:** Dado que llega una solicitud repetida para una postulación ya «calificada», el sistema no debe generar un segundo puntaje.
- **CA05:** Dado un reprocesamiento ([CU-26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md)), el sistema debe dejar el puntaje nuevo como vigente, conservar el anterior en el historial y mantener la postulación «calificada».
- **CA06:** Dado que el módulo de IA responde con una versión de modelo no registrada o con un puntaje fuera de 0 a 70, el sistema no debe registrar el puntaje y la postulación debe seguir «pendiente».
- **CA07:** Dado que todavía no hay ninguna versión del modelo guardada, el sistema debe poder calificar igual con las ponderaciones por defecto y dejar registrada la versión `v1.0`.
- **CA08:** Dado que la postulación no trae algunos datos del formulario oficial, el sistema debe calificarla con las variables que sí tiene y el detalle debe indicar cuáles faltaron.
- **CA09:** Dado un pico de envíos al cierre de la convocatoria, el sistema no debe perder ninguna postulación: las que no entren en la cola deben ser recogidas por el reintento automático.
- **CA10:** Dado que una postulación quedó «calificada», el sistema debe mostrarla en la lista priorizada con su puntaje.
- **CA11:** Dado cualquier postulación, el puntaje de la IA nunca debe superar los 70 puntos.
- **CA12:** Dado un cálculo de puntaje, el módulo de IA no debe recibir el nombre, el CI ni el contacto del Postulante.
- **CA13:** Dado un puntaje calculado, el sistema debe conservar el aporte de cada variable para poder explicar cómo se llegó al total.

## Desarrollador

Leonel Barrios
