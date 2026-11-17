# Caso de uso

| Id   | Nombre                           | Módulo                         | Prioridad | Estado      |
| ---- | -------------------------------- | ------------------------------ | --------- | ----------- |
| CU17 | Guardar borrador de postulación | P2. Convocatoria y postulación | Baja      | En progreso |

**Actor principal:** Postulante
**Actores secundarios:** —
**Objetivo:** Guardar el avance del formulario socioeconómico, aunque esté incompleto, y retomarlo después sin perder lo ya ingresado.
**Plataforma:** Móvil y Web
**RF asociados:** RF-12 (guardado de borrador), con RF-02 ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)) y RF-11 (documentos, [CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md))

**Descripción:**
Mientras completa el formulario de la postulación ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)), el Postulante elige «Guardar borrador». El sistema comprueba que la convocatoria siga abierta, guarda lo que haya escrito **sin exigir los campos ni los documentos obligatorios**, registra la fecha y hora del último guardado y confirma «Borrador guardado · 14:32». Más tarde puede retomarlo desde «Nueva postulación» (que le devuelve el formulario con lo guardado) o ver su avance en «Mis postulaciones» (cuántos campos y documentos le faltan) y continuar. Un borrador **no es una postulación enviada**: no se procesa con el módulo de IA, no cuenta como postulación vigente ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)) y no aparece en el listado de enviadas ([CU-04](CU04%20-%20Consultar%20estado%20de%20postulaci%C3%B3n.md)). Al enviarlo, el borrador se convierte en la postulación (CU-02); al cerrarse la convocatoria queda bloqueado.

Este CU **extiende** al [CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md), que ya crea la postulación en estado `borrador` al iniciar y la devuelve con sus respuestas al retomarla; aquí se agrega el guardado parcial y la vista «Mi borrador». Depende de [CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md) (la convocatoria abierta).

> **Fuentes consultadas** (documentación de grado): `images/046` (CU), `059` (análisis), `129` (secuencia), `142` (mockup) y el detalle del CU-17 y el RF-12 en los capítulos 4, 5.4 y 7. Lo que el diagrama dibuja con otro nombre o forma se registra en [la tabla de diferencias](../Implementacion/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md).

---

## Precondiciones

- El Postulante tiene sesión iniciada ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con rol `POSTULANTE`.
- Existe una convocatoria `ABIERTA`, dentro de sus fechas ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md)).
- El Postulante abrió «Nueva postulación» ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)), de modo que su postulación existe en estado `borrador` (o `devuelta`).
- El Postulante no tiene una postulación vigente ya enviada en la misma gestión ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)).

## Flujo principal

### A: guardar borrador

1. Durante el llenado del formulario, el Postulante selecciona «Guardar borrador» (en el paso de datos socioeconómicos o en el de documentos).
2. El sistema verifica que la convocatoria continúe abierta y que el Postulante pueda seguir postulando ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)).
3. El sistema guarda los datos escritos hasta ese momento (los campos vacíos quedan vacíos); los documentos ya adjuntados ([CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)) siguen vinculados a la postulación.
4. El sistema registra la fecha y hora del último guardado.
5. El sistema confirma «Borrador guardado» con la hora, y el Postulante sigue en el mismo paso.

### B: retomar borrador

1. El Postulante abre «Nueva postulación» o elige «Continuar» en la tarjeta «Mi borrador» de «Mis postulaciones».
2. El sistema detecta el borrador y lo muestra con los datos guardados, la hora del último guardado y sus documentos.
3. El Postulante continúa: puede guardar de nuevo o enviar la postulación ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)).

### C: ver «Mi borrador»

1. En «Mis postulaciones», el sistema muestra la tarjeta del borrador de la convocatoria abierta: nombre de la convocatoria, última modificación, cuántos campos y documentos obligatorios faltan, y el aviso de que se conserva hasta el cierre.

## Flujos alternativos y excepciones

- **A1 (flujo A, paso 2):** si la convocatoria ya cerró → no se guarda ni se edita y se informa que el plazo finalizó (`409` `ERR_VAL_03`). Si el Postulante ya no es elegible o ya tiene una postulación vigente en la gestión → rechazo con el motivo (`403` `ERR_VAL_02`, `409` `ERR_VAL_01`).
- **A2 (flujo A, paso 3):** si algún campo escrito tiene un valor inválido (texto en un campo numérico, número fuera de rango u opción que no existe) → el sistema lo señala y **no guarda nada de esa petición** (`400` `ERR_ENV_05`); los campos vacíos no son error.
- **A3 (flujo A, paso 3):** si falla la conexión o el servidor → el cliente informa que no se guardó y permite reintentar sin perder lo escrito.
- **A4 (flujo B, paso 2):** si no existe un borrador, «Nueva postulación» crea una postulación nueva en blanco ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)); «Mi borrador» no muestra nada.
- **A5 (flujo B, paso 3):** al enviarse la postulación, el borrador deja de serlo: pasa a `pendiente` ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)) y desaparece de «Mi borrador».
- **A6 (general):** al cerrar la convocatoria los borradores sin enviar quedan **bloqueados**: no se pueden guardar ni enviar (`ERR_VAL_03`) y dejan de mostrarse en «Mi borrador» ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md)).
- **A7:** si la postulación no existe o es de otro usuario → `404` `ERR_ENV_02`. Si ya fue enviada o está en otro estado (por ejemplo, adjudicada) → `409` `ERR_ENV_03`.

## Reglas de negocio

- **Representación completa:** el guardado envía todos los campos del formulario; uno omitido o vacío se guarda como vacío. Por eso es una operación de reemplazo (`PUT`), no de edición parcial ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md#put-vs-patch)).
- **Sin exigencias de envío:** no se piden los seis campos ni los documentos obligatorios; solo se valida el valor de los campos que traen algo, con las mismas reglas que al enviar ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md): ingresos 0 a 9 999 999 999 con 2 decimales, dependientes 0 a 50, promedio 0 a 100 con 2 decimales, y las opciones de las listas _por definir con la DUBSS_).
- **No cambia el estado:** guardar no mueve la postulación de `borrador` (ni de `devuelta`), no deja fila en el historial de estados, no la manda a la IA y no cuenta como vigente.
- **Una `devuelta` también se puede guardar** mientras se corrige, sin perder la devolución; el reenvío sigue exigiendo todo ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md), A5).
- **Fecha del último guardado:** es la de la última escritura de las respuestas; es nula mientras no se haya guardado nada.
- **Un guardado a la vez:** la fila de la postulación se bloquea durante el guardado, de modo que un guardado simultáneo con el envío se atiende en fila (si el envío llegó primero, el guardado recibe `ERR_ENV_03`).
- **Los documentos** ya se conservan solos al adjuntarlos ([CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)); el borrador no los vuelve a guardar.
- **Bitácora y logs:** el guardado no se anota en la bitácora; no se loguean las respuestas ni datos personales.
- **«Mi borrador»** solo muestra el borrador (no una `devuelta`, que ya figura en «Mis postulaciones») de una convocatoria abierta y vigente.

## Postcondiciones

- Las respuestas escritas quedan guardadas con su fecha y hora de último guardado y pueden retomarse mientras la convocatoria esté abierta.
- La postulación sigue en `borrador` (o `devuelta`); no fue enviada, ni procesada por la IA, ni cuenta como vigente.

## Criterios de aceptación

- **CA01:** Dado que hay una postulación en `borrador` y la convocatoria está abierta, el sistema debe guardar las respuestas escritas aunque falten campos y documentos obligatorios y devolver la fecha del guardado y cuántos campos faltan.
- **CA02:** Dado que se guarda con el formulario completamente vacío, el sistema debe aceptarlo y dejar todos los campos vacíos.
- **CA03:** Dado que un campo trae un valor inválido (rango, formato u opción), el sistema debe rechazar con `400` `ERR_ENV_05` sin guardar ningún campo de esa petición.
- **CA04:** Dado que la convocatoria cerró, el sistema debe rechazar con `409` `ERR_VAL_03` y no modificar nada.
- **CA05:** Dado que la postulación ya fue enviada (`pendiente` o posterior), el sistema debe rechazar con `409` `ERR_ENV_03`.
- **CA06:** Dado que la postulación es de otro usuario o no existe, el sistema debe responder `404` `ERR_ENV_02`.
- **CA07:** Dado que quien llama no es Postulante, el sistema debe responder `403` `ERR_AUTH_07`; sin sesión, `401`.
- **CA08:** Dado que el Postulante vuelve a abrir «Nueva postulación», el sistema debe devolver la misma postulación con las respuestas guardadas y la fecha del último guardado.
- **CA09:** Dado que hay un borrador en una convocatoria abierta, «Mi borrador» debe mostrar su convocatoria, la última modificación, los campos y documentos obligatorios que faltan y la fecha de cierre; y no debe mostrarlo si no hay borrador o la convocatoria cerró.
- **CA10:** Dado que se guardó un borrador, el sistema no debe cambiar su estado, ni crear una fila en el historial, ni pedir un puntaje a la IA, ni listarlo en «Mis postulaciones» entre las enviadas.
- **CA11:** Dado que se guarda y luego se envía la postulación completa, el sistema debe enviarla con las respuestas guardadas más lo que se haya editado ([CU-02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)).

## Desarrollador

Leonel Barrios
