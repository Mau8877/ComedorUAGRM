# Caso de uso

| Id   | Nombre                             | Módulo                         | Prioridad | Estado      |
| ---- | ---------------------------------- | ------------------------------ | --------- | ----------- |
| CU02 | Enviar postulación a la Beca Comedor | P2. Convocatoria y postulación | Alta      | En progreso |

**Actor principal:** Postulante
**Actores secundarios:** Módulo de IA (lo consume el sistema para la precalificación, [CU-03](CU03%20-%20Precalificar%20con%20IA.md))
**Objetivo:** Completar el formulario socioeconómico, adjuntar la documentación digitalizada y enviar formalmente la postulación a la Beca Comedor de la convocatoria abierta.
**Plataforma:** Móvil y Web
**RF asociados:** RF-02 (envío), con RF-11 (documentos, [CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)), RF-13 ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)) y RF-03 (precalificación, [CU-03](CU03%20-%20Precalificar%20con%20IA.md))

**Descripción:**
El Postulante elige «Nueva postulación». El sistema comprueba que haya una convocatoria abierta y le muestra un asistente de tres pasos: sus datos de perfil (nombre, CI y carrera, precargados), el formulario socioeconómico (ingresos familiares, dependientes, procedencia, patrimonio, estado de vulnerabilidad y promedio académico) y los documentos de respaldo ([CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)) con el botón de envío. Al confirmar, el sistema verifica que no falte ningún campo ni documento obligatorio, valida la elegibilidad y que no haya duplicados ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)), registra la postulación en estado «pendiente» con su fecha y hora de envío y muestra un comprobante con su código. Después, y sin hacer esperar al postulante, pide la precalificación con el módulo de IA (CU-03): si responde, la postulación pasa a «calificada»; si no, queda «pendiente» y el cálculo se reintenta solo. Una postulación «devuelta» ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)) se abre con sus respuestas para corregirla y reenviarla; no cuenta como duplicada.

Este CU depende de [CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md) (la convocatoria abierta) e incluye el CU-16 y el CU-18. El [CU-17](CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md) (guardar borrador) lo extiende y se entrega aparte.

> **Fuentes consultadas** (documentación de grado): `images/039` (CU), `052` (análisis), `122` (secuencia), `135` (mockup) y el detalle del CU-02 y el RF-02 en los capítulos 4, 5.4 y 7. **Alcance de esta entrega:** la precalificación es el [CU-03](CU03%20-%20Precalificar%20con%20IA.md) (módulo de IA, tarea B3); este CU solo la pide en segundo plano tras confirmar el envío y, si la IA no responde, la postulación queda «pendiente» y se reintenta (flujo A7). Lo que el diagrama dibuja con otro nombre o forma se registra en [la tabla de diferencias](../Implementacion/CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md).

---

## Precondiciones

- El Postulante tiene sesión iniciada ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con rol `POSTULANTE`.
- Existe una convocatoria `ABIERTA`, dentro de sus fechas de apertura y cierre ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)).
- El Postulante no tiene otra postulación vigente (ya enviada) en la misma gestión, salvo una `devuelta` de esa convocatoria ([CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)).
- El Postulante dispone de los documentos obligatorios que exige la convocatoria.

## Flujo principal

1. El Postulante selecciona «Nueva postulación».
2. El sistema verifica que exista una convocatoria abierta, crea la postulación en estado `borrador` (o recupera la que ya tenía) y muestra el paso 1 con los datos del perfil precargados.
3. El Postulante pasa al paso 2 y completa las variables socioeconómicas: ingresos familiares, número de dependientes, procedencia, patrimonio, estado de vulnerabilidad y promedio académico, más las preguntas del formulario socioeconómico oficial de la DUBSS (FORM-DUBS-001): de quién depende y su ocupación, rango de ingresos, si tiene hijos, grupo familiar (hasta 10 integrantes), residencia, tenencia y ambientes de la vivienda, si tiene otro beneficio en la universidad, si tiene discapacidad y un comentario personal.
4. El Postulante pasa al paso 3 y digitaliza o adjunta los documentos de respaldo requeridos (se incluye el [CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)).
5. El Postulante selecciona «Enviar postulación» y confirma el aviso «Una vez enviada no podrá editarla».
6. El sistema verifica que no falte ningún campo ni documento obligatorio y que los datos tengan un formato válido.
7. El sistema valida la elegibilidad del Postulante y la inexistencia de duplicados (se incluye el [CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)).
8. El sistema guarda las respuestas, pasa la postulación a `pendiente` con su fecha y hora de envío, suma un envío y deja la fila en el historial de estados.
9. El sistema confirma la recepción y muestra el comprobante con el código de la postulación.
10. Ya confirmado el envío, el sistema solicita la precalificación ([CU-03](CU03%20-%20Precalificar%20con%20IA.md)) en segundo plano.

## Flujos alternativos y excepciones

- **A1 (paso 1):** si no hay una convocatoria abierta → el sistema impide iniciar e informa las fechas de la próxima convocatoria, si existe una abierta con apertura futura (`409` `ERR_ENV_01`).
- **A2 (pasos 3 y 4):** si el Postulante quiere continuar más tarde, puede guardar su avance como borrador ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md)); los documentos ya subidos se conservan solos.
- **A3 (paso 6):** si faltan campos o documentos obligatorios → no se envía y el mensaje dice exactamente qué falta (`400` `ERR_ENV_04`). Si un dato tiene un formato, rango u opción inválida → `400` `ERR_ENV_05`.
- **A4 (paso 7):** si no es elegible o ya tiene una postulación vigente en la gestión → rechazo con el motivo (`403` `ERR_VAL_02`, `409` `ERR_VAL_01`).
- **A5 (paso 1):** si tiene una postulación `devuelta` de la convocatoria abierta → «Nueva postulación» la abre con sus respuestas; al reenviarla vuelve a `pendiente`, suma un envío y deja el motivo «Reenvío tras corregir» en el historial.
- **A6 (paso 5):** si se pulsa el botón varias veces o la petición llega repetida → se procesa una sola vez: la repetida devuelve el mismo comprobante, sin crear otra fila ni otro puntaje.
- **A7 (paso 10):** si el módulo de IA no responde (o aún no expone su API) → la postulación queda `pendiente` y la precalificación se reintenta automáticamente cada 5 minutos; el Postulante no pierde su envío.
- **A8 (pasos 2 a 5):** si la convocatoria se cierra mientras completa el formulario → no se acepta el envío y se informa que el plazo finalizó (`409` `ERR_VAL_03`).
- **A9:** si la postulación no existe o es de otro usuario → `404` `ERR_ENV_02`. Si ya no está en `borrador` ni `devuelta` ni fue enviada (por ejemplo, ya adjudicada) → `409` `ERR_ENV_03`.
- **A10:** error de conexión o del servidor → el Postulante puede reintentar sin perder lo que escribió.

## Reglas de negocio

- **Un solo envío por postulación:** el índice único `(postulante, convocatoria)` y el bloqueo de la fila hacen que dos envíos simultáneos se procesen en fila; el segundo recibe el comprobante del primero (RNF-03).
- **Estados:** `borrador → pendiente → calificada`; `devuelta → pendiente` al reenviar. La decisión final del [CU-05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md) no se revela aquí.
- **Los datos del perfil salen de la cuenta**, no del cuerpo de la petición: el postulante no puede falsearlos. La carrera que ve la IA es la de su perfil.
- **Son obligatorios al enviar** los seis campos socioeconómicos y, del formulario oficial, de quién depende, ocupación, rango de ingresos, si tiene hijos (y cuántos, si dijo que sí), tenencia de la vivienda, otro beneficio y discapacidad; el grupo familiar, la residencia, los ambientes de la vivienda y el comentario son opcionales _(cuáles son obligatorios está por definir con la DUBSS)_. Límites actuales _(por definir con la DUBSS)_: ingresos de 0 a 9 999 999 999 con 2 decimales, dependientes de 0 a 50, promedio de 0 a 100 con 2 decimales.
- **Las opciones de procedencia, patrimonio y estado de vulnerabilidad** (los desplegables del mockup) las define el backend y los clientes las leen de él; su catálogo definitivo está _por definir con la DUBSS_.
- **Los documentos obligatorios** son los que la convocatoria marca como tales ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)); los opcionales no bloquean el envío.
- **Precalificación fuera de la transacción del envío:** el envío se confirma primero; la llamada a la IA (hasta 5 s) no retiene la conexión ni retrasa al postulante. Un puntaje nuevo deja el anterior en el historial (RF-03).
- **Bitácora:** el envío no se anota en la bitácora; el cambio de estado queda en `postulacion_historial`. No se loguean las respuestas del formulario ni datos personales.

## Postcondiciones

- La postulación queda `pendiente` (o `calificada` si la IA ya respondió), con fecha de envío, asociada al Postulante y a la convocatoria y su gestión.
- Las respuestas socioeconómicas quedan guardadas y los documentos, vinculados a la postulación.
- El Postulante puede consultarla en «Mis postulaciones» ([CU-04](CU04%20-%20Consultar%20estado%20de%20postulación.md)).

## Criterios de aceptación

- **CA01:** Dado que hay una convocatoria abierta, el sistema debe crear (o recuperar) la postulación en `borrador` y mostrar el perfil precargado y las opciones de las listas.
- **CA02:** Dado que no hay convocatoria abierta, el sistema debe rechazar con `409` `ERR_ENV_01` e informar la próxima convocatoria si existe.
- **CA03:** Dado que el formulario está completo, los documentos obligatorios adjuntos y el postulante es elegible, el sistema debe pasar la postulación a `pendiente`, registrar la fecha de envío y devolver el comprobante con el código.
- **CA04:** Dado que falta algún campo o documento obligatorio, el sistema debe rechazar con `400` `ERR_ENV_04` indicando qué falta, sin cambiar el estado.
- **CA05:** Dado que un dato es inválido (rango, formato u opción), el sistema debe rechazar con `400` `ERR_ENV_05`.
- **CA06:** Dado que ya tiene una postulación vigente en la gestión o no es elegible, el sistema debe rechazar con `409` `ERR_VAL_01` o `403` `ERR_VAL_02`.
- **CA07:** Dado que la convocatoria cerró, el sistema debe rechazar con `409` `ERR_VAL_03`.
- **CA08:** Dado que la petición de envío se repite, el sistema debe devolver el mismo comprobante sin duplicar la postulación, el historial ni el puntaje.
- **CA09:** Dado que la postulación está `devuelta`, el sistema debe abrirla con sus respuestas y, al reenviarla, pasarla a `pendiente` sumando un envío y dejando «Reenvío tras corregir» en el historial.
- **CA10:** Dado que el módulo de IA no responde, el sistema debe conservar el envío en `pendiente` y reintentar la precalificación; cuando responde con un puntaje y una versión de modelo registrada, debe pasar la postulación a `calificada` y dejar un solo puntaje vigente.
- **CA11:** Dado que la postulación es de otro usuario o no existe, el sistema debe responder `404` `ERR_ENV_02`.
- **CA12:** Dado que quien llama no es Postulante, el sistema debe responder `403` `ERR_AUTH_07`; sin sesión, `401`.

## Desarrollador

Leonel Barrios
