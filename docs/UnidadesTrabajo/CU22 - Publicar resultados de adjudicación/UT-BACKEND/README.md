# Unidades de trabajo — CU22-BE

**Caso de uso:** Backend para publicar los resultados de adjudicación de una convocatoria cerrada: verificar que ningún finalista quede «calificada», registrar la publicación, avisar a cada finalista con decisión su resultado y dejar consultables los resultados para los postulantes.
**Fuente CU:** [`CU22 - Publicar resultados de adjudicación`](../../../CasosDeUso/CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)
**Diagramas de grado:** CU-22 en la documentación de grado (contexto `074`, análisis `087`, secuencia `102` y maqueta `115`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** ver la tabla de abajo.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

Todas las decisiones de abajo están confirmadas por Mau.

- **Una sola tabla nueva:** `publicacion_resultado` (una fila por convocatoria, `convocatoria_id` único). Ya está en el diseño lógico (`diseno_base_de_datos.md`); se actualiza el diseño **antes** que la migración, como exige `PERSISTENCIA_BD_BACKEND.md`. No se tocan `adjudicacion` ni `finalista`.
- **Módulo nuevo `features/resultados/`**, dueño de `publicacion_resultado`. Lee decisiones, finalistas, postulantes y convocatorias con consultas JDBC propias; ningún módulo lee el repository del otro. El aviso se envía a través del `service` de notificaciones.
- **Marca de publicación = la fila de `publicacion_resultado`.** Los CU-05, CU-19 y CU-20 hoy tienen un método `resultadosPublicados` que devuelve siempre `false` con un `TODO`: la UT-09 lo conecta a esta fila con el cambio mínimo. El estado de la convocatoria pasa a «publicada» *(solo ese cambio de estado, decisión 11 del CU; si el enum aún no tiene el valor, lo agrega la migración)*; ninguna postulación cambia de estado.
- **Precondición de publicar:** convocatoria cerrada y **ningún finalista «calificada»** (decisión 7 del CU-05). **No se exige el acta** (CU-08): el acta se genera después de publicar.
- **Sin idempotencia por clave**, igual que el CU-05, 19 y 20: se bloquea la fila de la convocatoria (`FOR UPDATE`); la segunda petición recibe `409` `ERR_PUB_02`. La unicidad de `convocatoria_id` es la segunda red de seguridad.
- **Cuatro códigos de error propios**, prefijo `PUB`: `ERR_PUB_01` (`409`, quedan finalistas en «calificada»), `ERR_PUB_02` (`409`, ya publicada), `ERR_PUB_03` (`409`, convocatoria no cerrada) y `ERR_PUB_04` (`404`, los resultados aún no están publicados). Convocatoria inexistente reutiliza `ERR_PRI_01`; `sort` o `filter` no soportado, `ERR_SYS_05`.
- **Aviso:** dos eventos nuevos, `BECA_ADJUDICADA` (`SUCCESS`) y `BECA_NO_ADJUDICADA` (`INFO`), con `{nombres}` y `{convocatoria}`, por **bandeja, push y correo**. Se avisa a cada finalista con adjudicación (adjudicado o no adjudicado); archivados, rechazados y devueltos no. Las filas de `notificacion` se crean por lotes de 500 **en la misma transacción** de la publicación y el despacho es el asíncrono de CU-EXTRA-01; un fallo de envío nunca deshace la publicación *(textos, por revisar)*.
- **Lista pública:** adjudicados con **nombre y apellido, nota, carrera y facultad** (decisión de Mau). Nunca CI, correo ni datos socioeconómicos. **«Nota» = `puntaje_final` de la adjudicación**. Los no adjudicados no aparecen; cada postulante consulta solo el suyo. La lista es paginada, con `search` sobre nombres, apellidos, carrera y facultad.
- **Motivo de «no adjudicada»:** nunca sale de los endpoints del Administrador hacia el postulante.
- **Bitácora:** `PUBLICACION_RESULTADOS` con código de convocatoria y cantidades; sin nombres, CI, correos ni tokens.
- **No existe despublicar** y **se publica aunque no haya adjudicados** (cada finalista recibe su «no adjudicada»).
- **Avisos por lotes, sin N+1:** destinatarios en una sola consulta, notificaciones guardadas por lotes y tokens resueltos en bloque; una prueba fija que el número de consultas no crece con la cantidad de finalistas.
- **Sin cambios de código en CU-08 ni CU-23**: no se genera acta ni se crea el padrón.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en el `service` para resumen, decisiones y publicar. Las consultas del postulante exigen sesión válida (`isAuthenticated()`) y no devuelven nada que no sea público o propio.
- **Datos de ejemplo:** UT-10, solo con perfil `dev`, sobre la convocatoria `[PRUEBA]`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Query / cuerpo | Respuesta | Errores |
|--------|------|----------------|-----------|---------|
| `GET` | `/api/v1/convocatorias/{codigo}/resultados/resumen` | — | `200` `ResumenPublicacionResponse` | `404` `ERR_PRI_01` |
| `GET` | `/api/v1/convocatorias/{codigo}/resultados/decisiones` | `filter[resultado]`, `search`, `sort`, `page`, `pageSize` | `200` array de `DecisionResultadoResponse` + `meta` | `404` `ERR_PRI_01`; `400` `ERR_SYS_05` |
| `POST` | `/api/v1/convocatorias/{codigo}/resultados/publicar` | — | `201` `PublicacionResultadoResponse` | `404` `ERR_PRI_01`; `409` `ERR_PUB_01`, `ERR_PUB_02`, `ERR_PUB_03` |
| `GET` | `/api/v1/resultados/convocatorias` | `page`, `pageSize` | `200` array de `ConvocatoriaPublicadaResponse` + `meta` | — |
| `GET` | `/api/v1/convocatorias/{codigo}/resultados` | `search`, `sort`, `page`, `pageSize` | `200` array de `AdjudicadoPublicoResponse` + `meta` | `404` `ERR_PRI_01`, `ERR_PUB_04`; `400` `ERR_SYS_05` |
| `GET` | `/api/v1/convocatorias/{codigo}/resultados/mio` | — | `200` `MiResultadoResponse` | `404` `ERR_PRI_01`, `ERR_PUB_04` |

- `ResumenPublicacionResponse` = `convocatoria` (`codigo`, `nombre`, `estado`), `adjudicados`, `noAdjudicados`, `calificadasPendientes`, `totalFinalistas`, `aAvisar` (cuántos finalistas recibirán aviso), `yaPublicada`, `publicadoEn`, `publicadoPor` (solo el nombre para mostrar) y `puedePublicar` (verdadero solo si está cerrada, no publicada y `calificadasPendientes = 0`).
- `DecisionResultadoResponse` = `finalista`, `nombres`, `apellidos`, `resultado` (`ADJUDICADA` o `NO_ADJUDICADA`), `puntajeFinal` (puede ser `null`, ver CU-05) y `fechaAdjudicacion`. Sin CI ni correo. Orden por defecto: `-puntajeFinal`. `sort` admite `puntajeFinal`, `apellidos` y `nombres`.
- `PublicacionResultadoResponse` = `convocatoria`, `publicadoEn`, `publicadoPor`, `adjudicados`, `noAdjudicados` y `avisados`.
- `ConvocatoriaPublicadaResponse` = `codigo`, `nombre`, `publicadoEn`. Orden `-publicadoEn`.
- `AdjudicadoPublicoResponse` = `nombres`, `apellidos`, `nota`, `carrera`, `facultad`. Orden por defecto `-nota`; `sort` admite `nota`, `apellidos` y `nombres`.
- `MiResultadoResponse` = `resultado` (`ADJUDICADA`, `NO_ADJUDICADA` o `null` si el usuario no es finalista con decisión en esa convocatoria), `nota` y `publicadoEn`.

Todos los identificadores son códigos públicos (UUID).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU22-BE-01](./UT-CU22-BE-01.md) | Migración: tabla `publicacion_resultado` | — |
| [UT-CU22-BE-02](./UT-CU22-BE-02.md) | Model y repository de la publicación | 01 |
| [UT-CU22-BE-03](./UT-CU22-BE-03.md) | Códigos de error, bitácora, DTOs y evento de aviso | — |
| [UT-CU22-BE-04](./UT-CU22-BE-04.md) | Consulta de resultados | 01 |
| [UT-CU22-BE-05](./UT-CU22-BE-05.md) | Resumen y decisiones para el Administrador | 03, 04 |
| [UT-CU22-BE-06](./UT-CU22-BE-06.md) | Publicar y avisar a los finalistas | 02, 03, 04 |
| [UT-CU22-BE-07](./UT-CU22-BE-07.md) | Consulta de resultados para el postulante | 03, 04 |
| [UT-CU22-BE-08](./UT-CU22-BE-08.md) | Endpoints del Administrador y del postulante | 05, 06, 07 |
| [UT-CU22-BE-09](./UT-CU22-BE-09.md) | Conectar la marca de publicación y habilitar comunicados a `ADJUDICADOS` | 02 |
| [UT-CU22-BE-10](./UT-CU22-BE-10.md) | Datos de prueba de publicación | 01, 02 |
| [UT-CU22-BE-11](./UT-CU22-BE-11.md) | Pruebas de integración con base real | 08, 09, 10 |

**Cadena:** `01 → 02, 04`; `03` independiente; `03, 04 → 05, 07`; `02, 03, 04 → 06`; `05, 06, 07 → 08`; `02 → 09`; `01, 02 → 10`; `08, 09, 10 → 11`.

## Criterios de aceptación cubiertos

| CA (CU22) | Descripción | UTs |
|---|---|---|
| CA01 | Publicar con todo decidido | 02, 05, 06, 08, 11 |
| CA02 | Quedan «calificadas» (`409`, cuántas faltan) | 04, 05, 06, 11 |
| CA03 | Ya publicada (`409`) | 04, 06, 11 |
| CA04 | Doble petición y concurrencia | 04, 06, 11 |
| CA05 | Aviso por bandeja, push y correo | 03, 06, 11 |
| CA06 | Archivados, rechazados y devueltos no se re-avisan | 04, 06, 11 |
| CA07 | Fallo de envío no deshace la publicación | 06, 11 |
| CA08 | Cancelar la confirmación | — (solo frontend) |
| CA09 | Tras publicar no se registran decisiones | 09, 11 |
| CA10 | El postulante ve su resultado y la lista de adjudicados | 07, 08, 11 |
| CA11 | Bitácora sin datos sensibles | 03, 06, 11 |
| CA12 | Publicar sin acta | 06, 11 |
| CA13 | `403`/`401`/`404` | 05, 06, 07, 08, 11 |
| CA14 | No cambia estados de postulación ni crea padrón | 06, 11 |
| CA15 | Se publica aunque no haya adjudicados | 05, 06, 11 |
| CA16 | No se despublica ni se vuelve a publicar | 06, 11 |
| CA17 | Avisos por lotes, sin N+1 | 04, 06, 11 |
| CA18 | La lista pública muestra solo los cinco campos | 04, 07, 11 |

## Follow-ups (no se implementan en estas UT)

- Que el CU-04 muestre al postulante «adjudicada» o «no adjudicada» solo después de publicar, leyendo este módulo.
- (Resuelto) El acta del CU-08 no es precondición de publicar: se genera después de publicar.
- Padrón de becados (CU-23) a partir de los adjudicados.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Adjudicar o no adjudicar (CU-05), devolver (CU-19), archivar o rechazar (CU-20), el acta (CU-08) y el padrón de becados (CU-23).
- La app móvil.
