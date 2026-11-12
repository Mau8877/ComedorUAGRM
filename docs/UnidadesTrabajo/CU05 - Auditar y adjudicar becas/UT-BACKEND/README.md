# Unidades de trabajo — CU05-BE

**Caso de uso:** Backend para conducir la adjudicación de una convocatoria cerrada: mostrar el panel de cupos, seleccionar a los finalistas de la lista priorizada, y registrar para cada finalista auditado la decisión final de **adjudicar** o **no adjudicar** la beca, con su historial y su bitácora.
**Fuente CU:** [`CU05 - Auditar y adjudicar becas`](../../../CasosDeUso/CU05%20-%20Auditar%20y%20adjudicar%20becas.md)
**Diagramas de grado:** CU-05 en la documentación de grado (contexto, análisis, secuencia y maqueta; números de figura por confirmar). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** `GET /api/v1/convocatorias/{codigo}/adjudicacion`, `POST /api/v1/convocatorias/{codigo}/finalistas`, `POST /api/v1/finalistas/{codigo}/adjudicar` y `POST /api/v1/finalistas/{codigo}/no-adjudicar` *(rutas propuestas por el CU, por revisar)*.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

Las marcadas *(por revisar)* son propuestas del CU-05 que la DUBSS o Mau aún no confirmaron; si cambian, se ajusta la UT indicada.

- **Una sola migración nueva:** crea una tabla nueva (`adjudicacion`, con su enum de PostgreSQL `resultado_adjudicacion`) y una columna nueva en una tabla existente (`convocatoria.cantidad_finalistas`). El diseño lógico (`diseno_base_de_datos.md`, imagen 091 y `docs/DiagramaConceptual.mermaid`) se actualiza **antes** que la migración, como exige `PERSISTENCIA_BD_BACKEND.md`.
- **Dos módulos, según el dueño del dato.** *Seleccionar finalistas* crea filas de `finalista`, así que vive en `features/finalistas/` (dueño de esa tabla). *Adjudicar, no adjudicar y el panel* viven en `features/postulaciones/`, junto a la devolución (CU-19) y al cierre (CU-20), con el `Adjudicacion` como Model de ese módulo. Las consultas son JDBC propias; ningún módulo lee el repository del otro.
- **Todas las acciones sobre un finalista cuelgan de `/api/v1/finalistas/{codigo}`** (se llega desde el expediente, igual que el CU-19 y el CU-20). El panel y la selección cuelgan de la convocatoria.
- **Sin idempotencia por clave**, igual que el CU-19 y el CU-20 (decisión 2 del CU). Se bloquea la fila de la convocatoria y luego la de la postulación (`FOR UPDATE`, siempre en ese orden para evitar bloqueos cruzados); la segunda petición recibe `409` `ERR_ADJ_01`.
- **Por qué se bloquea la convocatoria:** el límite de cupos es un dato de la convocatoria, no de una postulación. Si solo se bloqueara la postulación, dos administradores adjudicando a dos finalistas distintos con un solo cupo libre podrían superarlo.
- **Cinco códigos de error propios**, prefijo `ADJ` *(propuesto, por revisar)*, todos `409`: `ERR_ADJ_01` (la postulación ya no está «calificada» o los resultados están publicados), `ERR_ADJ_02` (sin auditoría o última no «Validado»), `ERR_ADJ_03` (puntaje total menor al mínimo), `ERR_ADJ_04` (cupos agotados), `ERR_ADJ_05` (convocatoria no cerrada o sin cupos o puntaje mínimo). Convocatoria inexistente reutiliza `ERR_PRI_01`; finalista inexistente, `ERR_FIN_01`; cantidad o motivo inválido, `ERR_SYS_01`.
- **Puntaje total** = puntaje vigente de la IA + puntaje académico de la **última** auditoría (misma regla del CU-07); no se guarda en el expediente, se calcula al decidir y se copia a `adjudicacion.puntaje_final`, que queda inmutable.
- **`puntaje_final` puede ser nulo** *(por revisar)*: «No adjudicar» exige una auditoría de **cualquier** resultado, y una auditoría «Observado» o «No válido» puede no traer puntaje académico, así que el total no siempre se puede calcular. Al adjudicar nunca es nulo (la auditoría es «Validado» y trae puntaje académico).
- **Posición del finalista:** el lugar que ocupa en la lista priorizada **completa** de la convocatoria al momento de seleccionar (mismo orden del CU-06: puntaje vigente descendente, desempate por fecha de envío y luego por id), no en la página. La consulta repite ese orden en SQL propio y una prueba comprueba que coincide con el del CU-06.
- **Resultados publicados (CU-22):** el CU-22 aún no existe, así que **estas UT no crean la marca de publicación**. Se reutiliza el método `resultadosPublicados` que dejó el CU-20 (hoy devuelve siempre `false`, con `TODO` hacia el CU-22); la regla se prueba con el `service` simulado. Al implementar el CU-22 se conecta (ver Follow-ups).
- **Sin aviso al postulante** (decisión 3 del CU): ni bandeja, ni push, ni correo al decidir. El aviso es del CU-22. Por eso estas UT **no agregan eventos** a `EventoNotificacion` y una prueba comprueba que no se envía ninguno.
- **Motivo:** obligatorio solo al no adjudicar, máximo 500 caracteres, se valida en el DTO y se guarda en `postulacion_historial.motivo`.
- **Bitácora:** `SELECCION_FINALISTAS`, `ADJUDICACION_BECA` y `NO_ADJUDICACION_BECA`; códigos y estados, nunca el motivo, nombres, CI, rutas ni tokens *(decisión propia, por revisar)*.
- **`cantidad_finalistas` solo para el Administrador:** esta columna la escribirá el CU-13, que aún no está implementado. Estas UT solo la **leen**, y solo en los endpoints del Administrador (panel y selección); ningún endpoint de consulta del postulante ni público la devuelve (hoy ninguno devuelve la entidad `Convocatoria`; UT-09 lo fija con una prueba).
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en el `service`.
- **Datos de ejemplo:** UT-10, solo con perfil `dev`, sobre la convocatoria `[PRUEBA]`, más dos convocatorias de casos límite.

## Contrato de datos / recurso HTTP

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `GET` | `/api/v1/convocatorias/{codigo}/adjudicacion` | — | `200` `PanelAdjudicacionResponse` | `404` `ERR_PRI_01` |
| `POST` | `/api/v1/convocatorias/{codigo}/finalistas` | `{ cantidad? }` | `201` `SeleccionFinalistasResponse` | `400` `ERR_SYS_01`; `404` `ERR_PRI_01`; `409` `ERR_ADJ_01`, `ERR_ADJ_05` |
| `POST` | `/api/v1/finalistas/{codigo}/adjudicar` | — | `200` `DecisionAdjudicacionResponse` | `404` `ERR_FIN_01`; `409` `ERR_ADJ_01` a `ERR_ADJ_05` |
| `POST` | `/api/v1/finalistas/{codigo}/no-adjudicar` | `{ motivo }` | `200` `DecisionAdjudicacionResponse` | `400` `ERR_SYS_01`; `404` `ERR_FIN_01`; `409` `ERR_ADJ_01`, `ERR_ADJ_02`, `ERR_ADJ_05` |
| `GET` | `/api/v1/finalistas` (CU-21, ya ampliado por CU-19 y CU-20) | — | `estadoPostulacion` ahora también `adjudicada` | los del CU-21 |

- `PanelAdjudicacionResponse` = `convocatoria` (`codigo`, `nombre`, `estado`), `cupos`, `puntajeMinimo` (ambos pueden ser `null` si la DUBSS no los definió), `adjudicados`, `noAdjudicados`, `cuposDisponibles` (`null` si no hay cupos definidos), `cantidadFinalistas` (la configurada; `null` si no la hay), `totalFinalistas`, `calificadasSinSeleccionar`, `resultadosPublicados` y `decisionesHabilitadas` (verdadero solo si la convocatoria está cerrada, tiene cupos y puntaje mínimo y los resultados no están publicados).
- `SeleccionFinalistasResponse` = `convocatoria` (código), `seleccionados` (cantidad) y `finalistas` (lista de `codigo`, `postulacion` y `posicion`), en el orden de la lista priorizada.
- `DecisionAdjudicacionResponse` = `finalista`, `postulacion`, `resultado` (`ADJUDICADA` o `NO_ADJUDICADA`), `estado` (`adjudicada` o `no_adjudicada`), `puntajeFinal`, `fechaAdjudicacion`, `decididaPor` y `cuposDisponibles` (ya actualizados).

Todos los identificadores son códigos públicos (UUID). Todos exigen `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU05-BE-01](./UT-CU05-BE-01.md) | Migración: tabla `adjudicacion` y `convocatoria.cantidad_finalistas` | — |
| [UT-CU05-BE-02](./UT-CU05-BE-02.md) | Models: `Adjudicacion`, cantidad de finalistas y transiciones de la postulación | 01 |
| [UT-CU05-BE-03](./UT-CU05-BE-03.md) | Códigos de error, bitácora y DTOs | — |
| [UT-CU05-BE-04](./UT-CU05-BE-04.md) | Consulta de adjudicación | 01 |
| [UT-CU05-BE-05](./UT-CU05-BE-05.md) | Panel de la convocatoria | 03, 04 |
| [UT-CU05-BE-06](./UT-CU05-BE-06.md) | Seleccionar finalistas | 02, 03, 04 |
| [UT-CU05-BE-07](./UT-CU05-BE-07.md) | Adjudicar o no adjudicar | 02, 03, 04 |
| [UT-CU05-BE-08](./UT-CU05-BE-08.md) | Endpoints del panel, la selección y la decisión | 05, 06, 07 |
| [UT-CU05-BE-09](./UT-CU05-BE-09.md) | Las listas reflejan la decisión y la cantidad queda privada (solo pruebas) | — |
| [UT-CU05-BE-10](./UT-CU05-BE-10.md) | Datos de prueba de adjudicación | 01, 02 |
| [UT-CU05-BE-11](./UT-CU05-BE-11.md) | Pruebas de integración con base real | 08, 09, 10 |

**Cadena:** `01 → 02, 04`; `03` independiente; `03, 04 → 05`; `02, 03, 04 → 06, 07`; `05, 06, 07 → 08`; `01, 02 → 10`; `09` independiente; `08, 09, 10 → 11`.

## Criterios de aceptación cubiertos

| CA (CU05) | Descripción | UTs |
|---|---|---|
| CA01 | Lista priorizada y panel de cupos | 04, 05, 08, 11 |
| CA02 | Seleccionar finalistas en orden, con posición en la lista completa y sin repetir | 04, 06, 08, 11 |
| CA03 | Cantidad cero, negativa o mayor que las disponibles (`400`) | 03, 06, 08, 11 |
| CA04 | Adjudicar con puntaje final, usuario y fecha | 02, 07, 08, 11 |
| CA05 | No adjudicar con motivo válido | 02, 07, 08, 11 |
| CA06 | No adjudicar sin motivo, solo espacios o de más de 500 (`400`) | 03, 08, 11 |
| CA07 | Sin ninguna auditoría (`ERR_ADJ_02`) | 04, 07, 11 |
| CA08 | Última auditoría no «Validado»: no adjudica, sí puede no adjudicar | 07, 11 |
| CA09 | Puntaje menor al mínimo (`ERR_ADJ_03`) | 07, 11 |
| CA10 | Cupos agotados (`ERR_ADJ_04`) | 07, 11 |
| CA11 | Dos administradores adjudican con un solo cupo | 04, 07, 11 |
| CA12 | Ya no «calificada» o resultados publicados (`ERR_ADJ_01`) | 04, 06, 07, 11 |
| CA13 | Doble petición: una sola decisión y una sola anotación | 04, 07, 11 |
| CA14 | Convocatoria no cerrada o sin cupos o mínimo (`ERR_ADJ_05`) | 06, 07, 11 |
| CA15 | Cupos actualizados e historial completo sin editar | 02, 05, 07, 11 |
| CA16 | El puntaje final no cambia al reprocesar la IA | 01, 02, 07, 11 |
| CA17 | Bitácora sin datos sensibles | 03, 06, 07, 11 |
| CA18 | Cancelar la ventana | — (solo frontend) |
| CA19 | Sale de la lista priorizada y no se audita ni decide otra vez | 07, 09, 11 |
| CA20 | No se avisa al postulante al decidir | 07, 11 |
| CA21 | `403`/`401`/`404` | 06, 07, 08, 11 |
| CA22 | La cantidad configurada se propone, se ajusta sin cambiarla y no se expone | 04, 05, 09, 11 |

## Preguntas abiertas

- ¿`puntaje_final` nulo al no adjudicar una auditoría sin puntaje académico, o se exige otra cosa? (UT-01, UT-07).
- Si el puntaje de la IA vigente falta al adjudicar, ¿se trata como puntaje bajo (`ERR_ADJ_03`) o como expediente incompleto (`ERR_ADJ_02`)? Se propone `ERR_ADJ_03` con «no disponible» (UT-07).
- ¿La posición de un finalista se recalcula cuando otros salen de la lista priorizada (archivados, rechazados)? Se propone **no**: se guarda al seleccionar y no se toca (UT-06).
- Nombre del prefijo `ADJ` y de las rutas (`/adjudicar`, `/no-adjudicar`, `/convocatorias/{codigo}/adjudicacion`).

## Follow-ups (no se implementan en estas UT)

- Conectar la regla de «resultados publicados» con la marca real cuando se implemente el CU-22.
- CU-13: escribir `cantidad_finalistas` (formulario, validación positiva y, *propuesta*, no menor que los cupos).
- CU-22: avisar a los adjudicados al publicar, definir qué se exige para publicar (ningún finalista queda «calificada») y habilitar comunicados a `ADJUDICADOS`.
- CU-04: mostrar «adjudicada» o «no adjudicada» al postulante solo después de publicar.
- CU-08 (acta) y CU-23 (padrón de becados), que parten de las adjudicaciones.
- Distinguir en reportes (CU-12) «no adjudicada por decisión final» (con fila en `adjudicacion`) de «rechazada» (sin ella).

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Devolver (CU-19), archivar o rechazar (CU-20), acta (CU-08), publicar resultados y avisar (CU-22) y padrón (CU-23).
- Escribir `cantidad_finalistas` desde un endpoint (CU-13).
- Reapertura de una decisión, apelación, reparto de cupos por carrera y lista de espera de suplentes.
- La app móvil.
