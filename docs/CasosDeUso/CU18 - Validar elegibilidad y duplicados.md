# Caso de uso

| Id   | Nombre                            | Módulo                         | Prioridad | Estado      |
| ---- | --------------------------------- | ------------------------------ | --------- | ----------- |
| CU18 | Validar elegibilidad y duplicados | P2. Convocatoria y postulación | Alta      | Hecho       |

**Actor principal:** Postulante (a través del [CU-02](CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md); la validación la ejecuta el sistema)
**Actores secundarios:** —
**Objetivo:** Comprobar, al recibir una postulación, que el postulante cumple las condiciones de elegibilidad y que no tiene otra postulación vigente en la misma gestión, para evitar registros duplicados o inválidos que distorsionen la priorización y la adjudicación.
**Plataforma:** Móvil y Web, siempre a través del CU-02 (el CU no tiene pantalla propia). Esta entrega es solo de backend.
**RF asociados:** RF-13

**Descripción:**
Cuando el Postulante envía su postulación ([CU-02](CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md)), el sistema ejecuta esta validación antes de registrarla. Comprueba que la convocatoria de destino acepta postulaciones, que el postulante cumple las condiciones de elegibilidad y que no existe otra postulación vigente suya en la misma gestión académica. Si todo está bien, devuelve «válida» y el CU-02 continúa con el registro. Si ya existe una postulación vigente devuelve «duplicada», y si no cumple alguna condición, «no elegible» con el motivo; en ambos casos el CU-02 rechaza el envío, no crea ningún registro e informa el motivo al postulante. El reenvío de una postulación devuelta ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)) no es un duplicado. Repetir la misma solicitud da el mismo resultado sin crear nada.

Este CU es incluido por el CU-02 y depende de [CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md) (la convocatoria y su gestión).

> **Fuentes consultadas** (documentación de grado): `images/047` (CU), `060` (análisis), `130` (secuencia), `143` (mockup) y el detalle del CU-18 y el RF-13 en los capítulos 4, 5.4 y 7. Lo marcado como _(propuesta)_ no figura en ninguna de ellas y debe confirmarse. Los puntos en que las fuentes no coinciden están en [Decisiones tomadas y puntos abiertos](#decisiones-tomadas-y-puntos-abiertos).

---

## Precondiciones

- El Postulante tiene sesión iniciada ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) y envió su postulación con los campos y documentos obligatorios completos (lo comprueba el CU-02 antes de llamar a esta validación).
- Existe una convocatoria `ABIERTA` a la que se asocia la postulación ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)).

## Flujo principal

1. El sistema recibe la solicitud de validación con la identidad del Postulante (la del token, nunca la de un campo del body) y la convocatoria de destino.
2. El sistema busca al postulante y la convocatoria.
3. El sistema consulta las postulaciones existentes del postulante en la misma gestión académica.
4. El sistema verifica que no exista otra postulación vigente suya en esa gestión.
5. El sistema verifica las condiciones de elegibilidad.
6. El sistema devuelve al CU-02 el resultado «válida».
7. El CU-02 continúa con el registro de la postulación.

## Flujos alternativos y excepciones

Los códigos A1 a A5 son los de las fuentes de grado; A6 en adelante son de este documento.

- **A1 (paso 4):** ya existe otra postulación vigente del postulante en la gestión → el sistema devuelve «duplicada» (`409`) y el CU-02 rechaza el envío e informa el motivo, con la fecha y el estado de la postulación existente.
- **A2 (paso 5):** el postulante no cumple alguna condición de elegibilidad → el sistema devuelve «no elegible» con el motivo específico y el CU-02 rechaza el envío.
- **A3 (paso 4):** si se trata del reenvío de una postulación devuelta (CU-19) en la misma convocatoria, no se considera duplicada: el sistema la reconoce como la postulación original, que se vuelve a enviar conservando su historial, sin crear otra.
- **A4 (paso 4):** si se recibe una solicitud repetida, el sistema devuelve el mismo resultado sin crear ni modificar registros (idempotencia, RNF-03).
- **A5 (paso 2):** si el servicio de validación no está disponible (falla la base de datos), el sistema no acepta la postulación e informa que se reintente (error genérico `500`).
- **A6 (paso 2):** la convocatoria no existe o está eliminada → `404`. Una cuenta que no es de un postulante (sin datos de postulante) no es elegible: se rechaza como A2.
- **A7 (paso 2):** la convocatoria no está `ABIERTA` o la fecha actual está fuera de su apertura y cierre → el sistema rechaza con el motivo _(propuesta de código y de HTTP en las UT)_.
- **A8:** sin sesión o con sesión vencida → `401`; usuario desactivado → `403`.

## Reglas de negocio

- **Una postulación vigente por postulante y gestión.** «Vigente» = una postulación no eliminada que el postulante ya envió (cualquier estado menos `borrador`) en cualquier convocatoria de esa gestión. La base ya lo respalda en parte con el índice único parcial `uq_postulacion_postulante_convocatoria` (`postulante_id`, `convocatoria_id`, sin eliminadas), que impide dos postulaciones del mismo postulante a la misma convocatoria; la validación lo anticipa con un motivo claro y lo amplía a toda la gestión _(ver decisión 1)_.
- **Un `borrador` no es duplicado:** es la misma postulación que el CU-02 está enviando ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md)).
- **Una `devuelta` de la misma convocatoria no es duplicado** (A3, RF-13): es la misma fila reenviada, con su historial. Una `devuelta` de **otra** convocatoria de la gestión sí cuenta como vigente.
- **Una postulación `archivada` o `no_adjudicada` sigue siendo vigente** en la gestión: es un resultado final, no una baja.
- **Condiciones de elegibilidad: por definir con la DUBSS.** No se inventan umbrales ni reglas. Hoy se comprueban solo las que se pueden verificar con los datos del sistema: que el postulante tenga cuenta activa con rol `POSTULANTE`, y que tenga carrera y registro universitario (los exige el registro, [CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)). Las condiciones nuevas que defina la DUBSS se agregan como reglas aparte, sin tocar el resto.
- **Solo lectura:** la validación no crea, modifica ni elimina nada (de ahí la idempotencia).
- **Quién es el postulante** sale siempre del token verificado ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-de-una-request-autenticada)), nunca del body.
- **Bitácora:** no se anota (es una consulta, no un cambio de estado); el rechazo se registra en el log como `WARN` ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#niveles)).
- **Cada rechazo tiene su código `ERR_*` catalogado** ([GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)).

## Postcondiciones

- El sistema determina si la postulación es válida; en caso afirmativo, el CU-02 puede registrarla.
- Si es rechazada, no se crea ningún registro y el postulante recibe el motivo.
- El estado de la base no cambia por ejecutar esta validación.

## Criterios de aceptación

- **CA01:** Dado que el postulante cumple las condiciones y no tiene otra postulación vigente en la gestión, el sistema debe devolver «válida».
- **CA02:** Dado que el postulante ya tiene una postulación vigente (enviada, calificada, archivada, adjudicada o no adjudicada) en la misma convocatoria o en otra de la misma gestión, el sistema debe devolver «duplicada» (`409`) con el motivo.
- **CA03:** Dado que la postulación existente está en `borrador`, el sistema no debe considerarla duplicada.
- **CA04:** Dado que la postulación existente de la misma convocatoria está `devuelta` ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)), el sistema debe devolver «válida» y reconocerla como la postulación a reenviar, sin crear otra.
- **CA05:** Dado que el postulante tiene una postulación vigente en otra gestión, el sistema no debe considerarla duplicada.
- **CA06:** Dado que el postulante no cumple una condición de elegibilidad, el sistema debe devolver «no elegible» con el motivo específico.
- **CA07:** Dado que la convocatoria no está `ABIERTA` o está fuera de sus fechas, el sistema debe rechazar con el motivo.
- **CA08:** Dado que la convocatoria no existe, el sistema debe responder `404`; dado que la cuenta no es de un postulante, debe rechazar como «no elegible» (`403`).
- **CA09:** Dado que se repite la misma solicitud, el sistema debe devolver el mismo resultado y no crear ni modificar registros.
- **CA10:** Dado que la base de datos no está disponible, el sistema no debe aceptar la postulación y debe responder con el error genérico `500`.
- **CA11:** Dado que dos envíos del mismo postulante a la misma convocatoria llegan a la vez, solo uno debe registrarse (lo garantiza el índice único; el segundo se informa como «duplicada»).
- **CA12:** Dado que el usuario está desactivado, el sistema debe rechazar con `403`; sin sesión, `401`.

## Decisiones tomadas y puntos abiertos

**Propuestas, pendientes de confirmar con el Desarrollador:**

1. **«Por gestión» o «por convocatoria».** El título, el flujo (paso 2), la nota de la secuencia y el mensaje del mockup dicen «por gestión»; el RF-13 y el paso 3 dicen «en la convocatoria». Se propone **por gestión** (es lo que dice el título y lo que ve el usuario). Como en una gestión solo hay una convocatoria `ABIERTA` a la vez, en la práctica solo cambia si el postulante envió a una convocatoria ya cerrada de la misma gestión.
2. **No es un endpoint propio, es un servicio interno.** El diagrama dibuja un `ValidacionController`, pero el actor es el Sistema y el CU-02 lo incluye: se implementa como servicio del módulo `postulaciones` que el CU-02 invoca en el mismo envío (misma transacción, así el control de duplicados y el registro no se separan). No agrega una ruta HTTP pública; el CU-02 (B8) lo usa. _Diferencia con el diagrama de análisis._
3. **La pantalla web del mockup (conteo de elegibles, duplicadas e inconsistentes) no se implementa.** Mostrarla exige **guardar** cada resultado de validación, y ni el diseño lógico (img 091) ni ninguna tabla lo contemplan; además la validación es de solo lectura. Si la DUBSS la quiere, se agrega primero la tabla a la documentación de grado y se hace en un CU aparte. _Diferencia con el mockup._
4. **El mensaje móvil «Postulación duplicada»** (con «Ver mi postulación») lo arma el CU-02 con los datos que esta validación devuelve en el rechazo (fecha de envío y estado de la postulación existente).
5. **«Datos inconsistentes» (mockup: «CI no coincide con el registro universitario») no se implementa:** el sistema no tiene un padrón de estudiantes contra el cual compararlo. Queda dentro de «condiciones de elegibilidad por definir».
6. **Convocatoria `ABIERTA` y dentro de fechas** se comprueba acá (A7) además de en el CU-02, porque la precondición de este CU lo dice y evita aceptar un envío contra una convocatoria cerrada si el CU-02 no lo hiciera.

**Abiertos, por definir con la DUBSS (no se inventan; se dejan parametrizados):**

- Las **condiciones de elegibilidad** concretas (más allá de cuenta activa, carrera y registro universitario).
- Si una postulación `archivada` debe seguir bloqueando una nueva en la misma gestión.

## Desarrollador

Leonel Barrios
