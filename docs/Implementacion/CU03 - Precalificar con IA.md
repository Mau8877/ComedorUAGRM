# CU03 — Precalificar con IA

- **Caso de uso:** [CU03](../CasosDeUso/CU03%20-%20Precalificar%20con%20IA.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu03) · **Unidades de trabajo:** [carpeta](../UnidadesTrabajo/CU03%20-%20Precalificar%20con%20IA/UT-BACKEND/README.md)
- **Diagramas de grado:** CU img 040 · análisis img 053 · secuencia img 123 · mockup img 136 (y sus secciones §5.4.3, §6.2.3, §7.4.3, §7.5.3). Revisadas las imágenes 053, 123 y 136; **no se abrieron** la 040, la 090 ni la 091 (ver «Para rehacer»).
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementado en el core y en el módulo de IA; con una **pantalla de consulta** para el Administrador (UT-CU03-BE-10 y UT-CU03-FE-01). Probado con Postgres real y con la IA real (el recorrido completo deja una postulación `calificada` con su puntaje vigente; con 20.000 postulantes, un puntaje tarda 14 ms de mediana y 103 ms como máximo), pero **sin prueba manual en la web ni en la app**.

## Flujo real implementado

El cálculo ocurre en segundo plano, entre el core y el módulo de IA, sin pantalla. Aparte, el Administrador DUBSS consulta el resultado con `GET /api/v1/postulaciones/{codigo}/precalificacion` (`PrecalificacionController` → `PrecalificacionDetalleService.obtener()`), que alimenta la pantalla `PrecalificacionScreen` (ruta `/precalificacion/$codigo`, enlazada desde el detalle de la lista priorizada).

1. `EnvioPostulacionService.enviar()` ([CU02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)) confirma el envío y, con la transacción ya confirmada (`afterCommit`), llama a `PrecalificacionService.solicitar(id)`. El Postulante ya tiene su comprobante.
2. `solicitar()` corre en el ejecutor `ejecutorPrecalificacion` (2 a 4 hilos, cola de 5.000; si se llena, la solicitud se descarta y la recoge el reintento) y llama a `precalificar(id)`. Nunca lanza una excepción.
3. `precalificar()`: `PrecalificacionConsultaRepository.datosPendiente(id)` lee las respuestas (y los datos del formulario oficial desde `datos_extra`); si la postulación ya no está `pendiente` o le faltan respuestas, devuelve `OMITIDA`. Lee la versión vigente y sus ponderaciones (`vigente()` + `parametros()`), que son `null` si todavía no hay ninguna; si la versión vigente **no tiene parámetros** (datos anteriores al CU26) tampoco se manda: con `parametros: {}` la IA rechazaría toda postulación, y así la IA usa su modelo por defecto `v1.0`.
4. `IaHttpPrecalificacionClient.calcular()` hace `POST {app.ia.base-url}/api/v1/puntaje` **sin transacción abierta** (2 s de conexión, 5 s de lectura). `404`, `5xx`, red caída o tiempo agotado → `IaNoDisponibleException` (`IA_NO_DISPONIBLE`); otro `4xx` → `IaDatosInvalidosException` (`NO_CALCULADA`).
5. En la IA, `main.puntaje()` → `scoring.calcular()`: traduce cada variable a un nivel de 0 a 1, aplica las ponderaciones recibidas (o las por defecto), reparte entre las variables con dato y escala a 0-70. Responde `{puntaje, modelo, detalle}`; sin versión pedida, `v1.0`.
6. El cliente comprueba que `puntaje` esté entre 0 y 70 y que venga la versión; si no, `IaDatosInvalidosException`.
7. `registrar()`, en una transacción corta con la postulación bloqueada (`FOR UPDATE`): vuelve a comprobar que siga `pendiente`, busca el modelo por la versión que informó la IA (si no está registrada, `NO_CALCULADA`), `retirarVigentes()` pasa el puntaje anterior a historial, `saveAndFlush(PuntajePostulacion)` guarda el nuevo con `detalle` y `fecha_calculo`, y `CalificacionPostulacionService.marcarCalificada()` pasa la postulación a `calificada` y deja una fila en `postulacion_historial` («Precalificación automática», sin usuario).
8. `PrecalificacionReintentoJob.reintentarPendientes()` (cron `0 */5 * * * *`, hora de La Paz) toma las 500 `pendiente` con respuestas más antiguas que no estén marcadas para revisión y llama a `precalificar()` por cada una; corta la pasada en la primera `IA_NO_DISPONIBLE`.

**Reprocesamiento** ([CU26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md)): `PrecalificacionService.recalcular()` hace lo mismo sobre una `calificada` con una versión concreta, exige que la IA repita esa versión y deja el estado como está.

**Lo que el diagrama no mostraba:** nada se le avisa al Postulante cuando termina (no hay `precalificacionRegistrada()`); la postulación pasa a `calificada` sin que él lo vea hasta que consulta su estado ([CU-04](../CasosDeUso/CU04%20-%20Consultar%20estado%20de%20postulaci%C3%B3n.md)).

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (123) | `PostulacionView` llama a `precalificar(postulacionId)` en el mismo recorrido del envío | El envío se confirma primero y la precalificación corre **después**, en segundo plano (`solicitar()` asíncrono tras el `commit`) | Una IA lenta o caída no debe retener al Postulante ni deshacer el envío (A2) |
| Secuencia (123) / Análisis (053) | `PrecalificacionController` hace todo | No hay controlador: es `PrecalificacionService` (más `IaHttpPrecalificacionClient`, `CalificacionPostulacionService` y el job); no se expone por HTTP | Se dispara desde el CU02, no desde una pantalla |
| Secuencia (123) | `calcularPuntaje(postulacion, parametros)` como autollamada del controlador | El cálculo está en un módulo aparte, `comedor-uagrm-ia` (Python), al que el core llama por `POST /api/v1/puntaje` | El grado define la IA como un módulo consumido por API |
| Secuencia (123) | `find(postulacion)`, `find(modeloActivo)`, `list(parametros)` y `prepararVariables()` como cuatro mensajes | `datosPendiente()` (una consulta JDBC que une postulación, respuestas, postulante y carrera, y lee `datos_extra`) y `vigente()` + `parametros()` | Un solo `SELECT` evita cuatro viajes y no cruza repositorios de otros módulos |
| Secuencia (123) | `validarDatos(variables)` en el controlador | La validan el CU02 (al enviar, `ERR_ENV_04` / `ERR_ENV_05`) y la IA (`422`); el core solo omite una postulación sin respuestas completas | El servidor de cada capa no confía en la anterior |
| Secuencia (123) | `create(puntaje)` y `update(estado)` | `retirarVigentes()` + `saveAndFlush(PuntajePostulacion)` y `marcarCalificada()` (con una fila de historial), todo en una transacción | Un solo puntaje vigente por postulación y trazabilidad de CU04 / CU19 |
| Secuencia (123) | `precalificacionRegistrada()` hacia la vista | No existe | La vista no espera; el Postulante consulta su estado después |
| Secuencia (123), alt A1 | `update(marcadaRevision)` si los datos son incompletos o incoherentes | No hay un estado nuevo: cada rechazo de la IA se cuenta (`postulacion.intentos_precalificacion`) y al tercero se anota `precalificacion_revision_en`; sigue `pendiente` pero el reintento ya no la toma. Reenviarla la reinicia | Agregar un valor al enum `estado_postulacion` tocaría la máquina de estados de CU04, CU06, CU19 y el dashboard; falta decidir con la DUBSS si hace falta un estado y una pantalla |
| Secuencia (123), alt A2 | `registrarFallo()` y `programarReintento(postulacionId)` | Un `WARN` y el job que recorre las `pendiente` cada 5 minutos | No hace falta programar cada reintento: el job recoge todo lo `pendiente` |
| Secuencia (123), notas | A3 (reproceso), A4 (duplicado), A5 (picos) | A3: `recalcular()`; A4: solo se calcula una `pendiente`, bajo bloqueo; A5: ejecutor con cola de 5.000 y reintento | Se implementan tal cual, con otros nombres |
| Análisis (053) | `Postulacion`, `ModeloPreseleccion`, `ParametroModelo` y `PuntajePostulacion` como entidades del controlador | Se agregan el módulo de IA, `IaHttpPrecalificacionClient`, `RespuestaSocioeconomica`, el ejecutor y `PrecalificacionReintentoJob`; los parámetros viajan a la IA en cada solicitud | La IA no guarda estado |
| Mockup (136) | Pantalla «Precalificación con IA» del Administrador: puntaje «85.0 de 100», fecha, duración (2,8 s), versión del modelo, aporte de cada variable («26 / 30»…) y traza del proceso | **Existe** (`PrecalificacionScreen`): encabezado con el postulante y el estado, el anillo del puntaje, fecha, modelo, posición en la lista, aporte de cada variable, traza y la nota. Se llega desde el botón «Ver precalificación con IA» del detalle de la lista priorizada ([CU06](../CasosDeUso/CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)); el puntaje de la IA también se ve en el expediente ([CU07](../CasosDeUso/CU07%20-%20Auditar%20expediente%20finalista.md), `PuntajePanel.tsx`) | El mockup la dibuja como una pantalla del menú «Postulaciones»; aquí es una consulta que se abre desde la lista priorizada, porque no hay otro CU que liste postulaciones sueltas |
| Mockup (136) | «de 100» | El puntaje de la IA es **de 70**; los 30 restantes los pone el auditor (CU07) | Decisión de la documentación de grado (`diseno_base_de_datos.md`, Q3 cerrada): IA hasta 70 + rendimiento académico manual hasta 30 |
| Mockup (136) | Seis variables con su máximo (30, 20, 15, 15, 10, 10) | Diez variables con «puntos / máximo» cada una (el máximo es su ponderación sobre la suma de las que tienen dato, a escala 0-70); las que no tienen dato se marcan «Sin dato» y las de peso 0 «No pondera» | Se sumaron las del formulario oficial de la DUBSS; el promedio académico pesa 0 por defecto |
| Mockup (136) | «Duración» del cálculo y «Traza del proceso automático» | La **traza** sí existe, con lo que hay: los cambios de estado del historial de la postulación, la marca de revisión y la posición en la lista. La **duración** no se muestra: no se guarda | Guardar la duración exigiría una columna nueva en `puntaje_postulacion` |
| Análisis (053) / Secuencia (123) | Solo el cálculo ponderado | Además, el módulo de IA ofrece `segunda-opinion` (modelo aprendido) que ningún diagrama dibuja ni el core consume | La documentación de grado habla de un modelo de IA; es un contraste y no entra al flujo del CU |
| Diseño lógico (091) / Clases (090) | `postulacion` sin columnas de reintento | `intentos_precalificacion smallint` y `precalificacion_revision_en timestamptz` (migración `V20261122100000`) | Dejar de reintentar lo que la IA nunca puede calcular (A1); ya está en [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid) |
| Diseño lógico (091) | `puntaje_postulacion` con el puntaje | `puntaje_total` con `CHECK` de 0 a 70 (`V20261107110100`), `detalle` jsonb por variable, `fecha_calculo`, `vigente` y un índice único parcial de un solo vigente por postulación | RF-03 y RF-21: trazabilidad y reprocesamiento |

## Pendientes

- **Segunda opinión con aprendizaje automático:** existe `POST /api/v1/segunda-opinion` (Random Forest entrenado con datos **sintéticos**, UT-CU03-BE-08). Es un contraste: el core no lo llama ni lo guarda y el puntaje oficial sigue siendo el ponderado. No se muestra al auditor mientras no se reentrene con datos reales.

- **«Marcada para revisión» (A1):** decidir si se agrega ese estado y una pantalla para revisar las postulaciones que la IA rechaza.
- **Revisión de las marcadas:** quedan marcadas (`precalificacion_revision_en`) pero no hay pantalla ni aviso para verlas; hoy solo el `WARN` del log. Desmarcarlas sin reenviar tampoco tiene camino.
- **Duración del cálculo:** el mockup la muestra y no se guarda.
- **Menú:** la pantalla no tiene ítem propio; se abre desde la lista priorizada.
- **Criterio de puntaje:** pesos, niveles por opción y escala son un criterio adoptado que la DUBSS debe validar.

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 123 | Sí | Ver especificación abajo | [ ] |
| Análisis | 053 | Sí | Ver especificación abajo | [ ] |
| Mockup | 136 | Sí | La pantalla ya existe: rehacerla con «de 70», diez variables con «puntos / máximo», posición en la lista, traza real del historial, el aviso de «marcada para revisión» y sin «Duración» (ver especificación) | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. `Postulacion` suma `intentosPrecalificacion` y `precalificacionRevisionEn`. `PuntajePostulacion` suma `detalle`, `fechaCalculo` y `vigente`; el puntaje va de 0 a 70 | [ ] |
| Diseño lógico BD | 091 | Por verificar | No se abrió la imagen. `postulacion` suma `intentos_precalificacion` y `precalificacion_revision_en` (`V20261122100000`). `puntaje_postulacion.puntaje_total` con `CHECK` de 0 a 70 (migración `V20261107110100`); coincide con [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid) | [ ] |
| Casos de uso | 040 | Por verificar | No se abrió la imagen. El actor es «Sistema» (incluido por el CU-02); el CU-26 lo extiende; el «Módulo de IA» es un actor secundario | [ ] |

Arquitectura (118–120): cambia, se suman el módulo de IA (FastAPI, consumido por HTTP), un ejecutor de tareas con cola y un job programado. Texto de grado: el CU-03 dice que el sistema marca la postulación «para revisión» ante datos incompletos (A1) y que la lista priorizada se arma al precalificar; hoy lo primero no existe y la lista se calcula al consultarla.

### Especificación de cada diagrama afectado

#### Secuencia — img 123 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Postulante · `Boundary` PostularScreen · `Control` EnvioPostulacionService · `Control` PrecalificacionService · `Control` IaHttpPrecalificacionClient · `Object` Módulo de IA · `Control` CalificacionPostulacionService · `Entity` Postulacion · `Entity` PuntajePostulacion · `Control` PrecalificacionReintentoJob

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | clic «Enviar postulación» (`enviarPostulacion()`) | [igual] |
| 2 | PostularScreen → EnvioPostulacionService | `POST /api/v1/postulaciones/{codigo}/enviar` → `200` con el comprobante | [cambia] |
| 3 | EnvioPostulacionService → PrecalificacionService | `solicitar(postulacionId)` asíncrono, tras el `commit` | [cambia] |
| 4 | PrecalificacionService → PrecalificacionService | `datosPendiente(postulacionId)` y `vigente()` + `parametros(modeloId)` | [cambia] |
| 5 | PrecalificacionService → IaHttpPrecalificacionClient | `calcular(solicitud)` | [nuevo] |
| 6 | IaHttpPrecalificacionClient → Módulo de IA | `POST /api/v1/puntaje` con las variables y las ponderaciones | [nuevo] |
| 7 | Módulo de IA → Módulo de IA | validar datos y `calcular()` (niveles, ponderaciones, escala 0-70) | [cambia] |
| 8 | Módulo de IA → IaHttpPrecalificacionClient | `200 {puntaje, modelo, detalle}` | [nuevo] |

**alt [respuesta válida]**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 9 | PrecalificacionService → PrecalificacionService | `registrar()`: bloquear la postulación, comprobar que siga `pendiente`, buscar la versión | [nuevo] |
| 10 | PrecalificacionService → PuntajePostulacion | `retirarVigentes()` y `saveAndFlush(puntaje)` | [cambia] |
| 11 | PrecalificacionService → CalificacionPostulacionService | `marcarCalificada(postulacionId)` | [cambia] |
| 12 | CalificacionPostulacionService → Postulacion | `calificar()` y fila de historial «Precalificación automática» | [cambia] |

**alt [la IA rechaza los datos, `422`, o responde fuera de rango o con una versión no registrada]:** 13 · PrecalificacionService → PrecalificacionService · `WARN` y resultado `NO_CALCULADA`; la postulación sigue `pendiente` · [cambia: sin `update(marcadaRevision)`]

**alt [la IA no está disponible (`404`, `5xx`, red) o excede 5 s]:** 14 · PrecalificacionService → PrecalificacionService · `WARN` y resultado `IA_NO_DISPONIBLE`; sigue `pendiente` · [cambia: sin `registrarFallo()` ni `programarReintento()`]

**loop [cada 5 minutos]:** 15 · PrecalificacionReintentoJob → PrecalificacionService · `precalificar(id)` por cada una de las 500 `pendiente` más antiguas sin marca de revisión; corta en la primera `IA_NO_DISPONIBLE` · [nuevo]

*Se elimina `precalificacionRegistrada()` hacia la vista.*

#### Análisis (comunicación) — img 053 (rehacer completo)

**Elementos:** `actor` Postulante · `boundary` PostularScreen · `control` EnvioPostulacionService · `control` PrecalificacionService · `control` IaHttpPrecalificacionClient · `control` CalificacionPostulacionService · `control` PrecalificacionReintentoJob · `entity` Postulacion · `entity` RespuestaSocioeconomica · `entity` ModeloPreseleccion · `entity` ParametroModelo · `entity` PuntajePostulacion · `externo` Módulo de IA.

**Enlaces y mensajes:** Postulante – PostularScreen (`1: enviarPostulacion()`) · PostularScreen – EnvioPostulacionService (`1.1: enviar()`) · EnvioPostulacionService – PrecalificacionService (`1.2: solicitar(postulacionId)`) · PrecalificacionService – RespuestaSocioeconomica y Postulacion (`1.3: datosPendiente()`) · PrecalificacionService – ModeloPreseleccion y ParametroModelo (`1.4: vigente()`, `1.5: parametros()`) · PrecalificacionService – IaHttpPrecalificacionClient (`1.6: calcular(solicitud)`) · IaHttpPrecalificacionClient – Módulo de IA (`1.7: POST /api/v1/puntaje`) · PrecalificacionService – PuntajePostulacion (`1.8: retirarVigentes()`, `1.9: guardar(puntaje)`) · PrecalificacionService – CalificacionPostulacionService (`1.10: marcarCalificada()`) · CalificacionPostulacionService – Postulacion (`1.11: calificar()`) · PrecalificacionReintentoJob – PrecalificacionService (`2: precalificar(id)` cada 5 minutos).

#### Mockup — img 136 (rehacer o quitar)

La pantalla «Precalificación con IA» existe (`PrecalificacionScreen`). Contenido final:

- **Encabezado:** nombre, CI, convocatoria y estado de la postulación (`calificada`). [igual]
- **Puntaje de vulnerabilidad:** número grande y «de 70» (no «de 100»). [cambia]
- **Calculado el:** `fecha_calculo` (existe). [igual]
- **Modelo:** la versión usada, por ejemplo «v1.0» (existe en `modelo_preseleccion`). [igual]
- **Duración:** [no se guarda; se quita o se agrega]
- **Aporte de cada variable:** diez filas con «puntos / ponderación»: Ingresos familiares, Dependientes, Procedencia, Patrimonio, Estado de vulnerabilidad, Promedio académico, Dependencia económica, Ocupación de quien sostiene, Discapacidad e Hijos; las variables sin dato se muestran «sin dato». [cambia]
- **Posición en la lista:** número del lugar en la lista priorizada de la convocatoria; «—» si no está calificada. [nuevo]
- **Traza del proceso automático:** tabla Paso / Resultado / Fecha con los cambios de estado del historial (por ejemplo «Postulación enviada», «Precalificación automática»), «Marcada para revisión» si la IA rechazó los datos 3 veces y «Ubicación en la lista priorizada: Posición N». [cambia]
- **Aviso:** si está marcada para revisión, un recuadro con los intentos y que ya no se reintenta sola. [nuevo]
- **Nota:** «El puntaje solo ordena los expedientes para auditoría; la adjudicación depende de la revisión de la DUBSS.» [igual]
