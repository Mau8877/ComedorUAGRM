# Unidades de trabajo — CU21-BE

**Caso de uso:** Backend para notificar a los finalistas, registrar la entrega de su documentación física y publicar comunicados, más la bandeja de avisos del usuario.
**Fuente CU:** [`CU21 - Notificar finalistas y publicar comunicados`](../../../CasosDeUso/CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md)
**Diagramas de grado:** CU-21 en la documentación de grado: contexto (`048`), análisis (`061`), secuencia (`131`) y maqueta (`144`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** `/api/v1/finalistas`, `/api/v1/comunicados` y `/api/v1/notificaciones/me` (ver tabla).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

- **Tablas nuevas, tal como el diseño lógico:** `finalista` y `comunicado` (+ enum `destinatarios_comunicado`). Se les agregan `codigo_<tabla>` y las columnas comunes. **No** se crean `auditoria_expediente`, `adjudicacion` ni nada del CU-05/07: el CU-05 amplía `finalista` sin recrearla.
- **`notificacion` se amplía** con `postulacion_id` y `comunicado_id` (nullables; ya lo anticipaba la migración de CU-EXTRA-01). Su `tipo` sigue siendo `SUCCESS/INFO/WARNING/ERROR` (el del servicio hecho), **no** el enum `CAMBIO_ESTADO/FINALISTA/COMUNICADO/RESULTADO` del diseño lógico: contradicción por resolver (ver pendientes del CU).
- **Módulos:** `features/finalistas/` y `features/comunicados/`; los eventos nuevos van en `features/notificaciones/` (`EventoNotificacion`). Un módulo usa a otro solo a través de su `service` (regla de dependencia).
- **Eventos nuevos de notificación:** `FINALISTA_NOTIFICADO` (INFO, push + correo; marcadores `{nombres}`, `{plazo}`, `{lugar}`, `{documentos}`) y `COMUNICADO` (INFO, solo bandeja + push; marcadores `{titulo}`, `{contenido}`). `EventoNotificacion` gana un indicador de si el evento envía correo.
- **Envío masivo:** el comunicado crea las filas de `notificacion` por lotes y el despacho es el asíncrono de CU-EXTRA-01; la respuesta no espera la entrega.
- **Estado de la notificación del finalista** (`SIN_ENVIAR/ENVIADA/FALLIDA`) se deriva del último `notificacion` con ese `postulacion_id` (y sin `comunicado_id`): `enviada_en` nulo = fallida.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en los `service` de finalistas y comunicados. La bandeja exige sesión y devuelve solo las del usuario del token.
- **Bandeja del usuario (UT 09):** implementa el contrato ya escrito en [`NOTIFICACIONES_WEB.md`](../../../pendientes/NOTIFICACIONES_WEB.md); **no se redefine acá**. *(Decisión propuesta: entra en este CU porque sin ella el Postulante no puede consultar sus avisos; si se prefiere, se separa como CU-EXTRA-02.)*
- **Errores** (prefijos `FIN`, `COM` y `NOT`, documentar en `docs/errors/`): `ERR_FIN_01` finalista no existe (404), `ERR_FIN_02` convocatoria no existe (404), `ERR_FIN_03` fecha de recepción futura (400), `ERR_FIN_04` plazo no posterior a hoy (400), `ERR_FIN_05` `filter`/`sort` no soportado (400), `ERR_COM_01` destinatarios no soportados o sin destinatarios (400), `ERR_COM_02` `FINALISTAS` sin convocatoria (400), `ERR_NOT_01` aviso inexistente o ajeno (404), `ERR_NOT_02` `sort` de la bandeja no soportado (400). Los campos vacíos caen en `ERR_SYS_01`.
- **Decisión sobre el `tipo` del aviso (contradicción abierta del CU):** se mantiene `SUCCESS/INFO/WARNING/ERROR` (lo ya implementado en CU-EXTRA-01 y lo que consume la campanita); el enum `CAMBIO_ESTADO/FINALISTA/COMUNICADO/RESULTADO` del diseño lógico **no** se adopta en este CU. Los dos avisos nuevos son `INFO` y se distinguen por su contexto (`postulacion_id` / `comunicado_id`).
- **Decisión sobre la bandeja:** entra en este CU (UT 09), porque sin ella el Postulante no puede consultar sus avisos (flujo D). Sigue siendo reutilizable por cualquier otro CU.
- **Resultado de la notificación:** `enviados` son los avisos registrados en la bandeja y `fallidos` los que no se pudieron registrar; el despacho de push y correo es asíncrono y no se espera. Por eso el estado «Fallida» de un finalista se deriva del aviso sin `enviada_en` y, justo después de enviar, puede verse «Fallida» unos segundos hasta que el despacho termina (la pantalla vuelve a consultar).
- **Fechas:** `plazoEntrega` y `documentacionRecibidaEn` se exponen como fecha (`LocalDate`, día en `America/La_Paz`); `notificadoEn` como instante.
- **Envío masivo:** el comunicado guarda los avisos por lotes de 500 en la misma transacción; el despacho usa el ejecutor de CU-EXTRA-01 (cola acotada). Si la cola se llena, el push se descarta (el aviso queda en la bandeja): límite conocido de CU-EXTRA-01, no se cambia acá.
- **Datos de ejemplo:** UT 04 (finalistas) y UT 12 (avisos, comunicado y un postulante con sesión), solo con perfil `dev`, sobre la convocatoria `[PRUEBA]` y el administrador `admin@dubss.com` del CU06.
- **Bitácora:** `NOTIFICACION_FINALISTAS`, `ENTREGA_DOCUMENTACION_REGISTRADA` (con valor anterior en el detalle si es corrección) y `COMUNICADO_PUBLICADO`, con `@Auditar` donde el resultado sea `Auditable`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Query / cuerpo | Respuesta | Errores |
|--------|------|----------------|-----------|---------|
| `GET` | `/api/v1/finalistas` | `filter[convocatoriaId]` (oblig.), `search`, `page`, `pageSize` | `200` array de `FinalistaResponse` + `meta` | `400` `ERR_SYS_05`; `404` `ERR_FIN_02` |
| `POST` | `/api/v1/finalistas/notificaciones` | `{ finalistas: [codigo], plazoEntrega, lugarHorario, documentos }` | `200` `{ solicitados, enviados, fallidos }` | `400` `ERR_SYS_01`/`ERR_FIN_04`; `404` `ERR_FIN_01` |
| `PATCH` | `/api/v1/finalistas/{codigo}/entrega` | `{ fechaRecepcion?, observaciones? }` | `200` `FinalistaResponse` | `400` `ERR_FIN_03`; `404` `ERR_FIN_01` |
| `POST` | `/api/v1/comunicados` | `{ titulo, contenido, destinatarios, convocatoriaId? }` | `201` `ComunicadoResponse` (con `totalDestinatarios`) | `400` `ERR_SYS_01`/`ERR_COM_01`/`ERR_COM_02`; `404` `ERR_FIN_02` |
| `GET` | `/api/v1/comunicados` | `page`, `pageSize` | `200` array + `meta` (los ya publicados) | — |
| `*` | `/api/v1/notificaciones/me…` | según `NOTIFICACIONES_WEB.md` | según ese documento | `401` |

`FinalistaResponse` = `codigo`, `posicion`, `nombres`, `apellidos`, `ci`, `puntaje`, `estadoNotificacion`, `plazoEntrega`, `notificadoEn`, `documentacionRecibidaEn`. La convocatoria se elige con el endpoint existente `GET /api/v1/lista-priorizada/convocatorias` (CU-06). Todos exigen `ADMINISTRADOR_DUBSS`, salvo la bandeja. Todos los identificadores son códigos públicos (UUID).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| UT-CU21-BE-01 | Migración: `finalista`, `comunicado` y columnas de contexto en `notificacion` | — |
| UT-CU21-BE-02 | Models, enums y repositories de `finalista` y `comunicado` | 01 |
| UT-CU21-BE-03 | Códigos de error, acciones de bitácora, DTOs y eventos de notificación | — |
| UT-CU21-BE-04 | Finalistas de prueba en el cargador de desarrollo | 02 |
| UT-CU21-BE-05 | Listado de finalistas con estados | 02, 03 |
| UT-CU21-BE-06 | Notificar a los finalistas (con reenvío a los fallidos) | 02, 03 |
| UT-CU21-BE-07 | Registrar y corregir la entrega de documentación | 02, 03 |
| UT-CU21-BE-08 | Publicar comunicados y listarlos | 02, 03 |
| UT-CU21-BE-09 | Bandeja de avisos del usuario (contrato de `NOTIFICACIONES_WEB.md`) | — |
| UT-CU21-BE-10 | Endpoints de finalistas y comunicados | 05, 06, 07, 08 |
| UT-CU21-BE-11 | Pruebas de integración con base real | 04, 09, 10 |
| UT-CU21-BE-12 | Datos de ejemplo: avisos, comunicado y postulante con sesión | 04, 09 |

**Cadena:** `01 → 02 → 04`; `03` independiente; `02, 03 → 05, 06, 07, 08 → 10`; `09` independiente; `04, 09, 10 → 11`; `04, 09 → 12`.

## Alcance de cada UT

- **01:** una migración `V{timestamp}__crear_tablas_finalista_y_comunicado.sql`: `finalista` (`postulacion_id` UQ parcial, `posicion`, `notificado_en`, `plazo_entrega`, `documentacion_recibida_en`), `comunicado` (`convocatoria_id` nullable, `titulo`, `contenido`, `destinatarios`, `publicado_por`, `publicado_en`), el enum, y `postulacion_id`/`comunicado_id` en `notificacion` con sus índices.
- **02:** Models (`Finalista` con `@AttributeOverride` del código, `Comunicado`), el enum `DestinatariosComunicado`, repositories y la relación del aviso con su postulación/comunicado.
- **03:** `FinalistaErrorCodes`, `ComunicadoErrorCodes`, `docs/errors/finalistas/` y `docs/errors/comunicados/` con su índice, constantes en `BitacoraAcciones`, DTOs (`record`) y los dos eventos en `EventoNotificacion` con el indicador «envía correo».
- **04:** el cargador de CU-06 crea finalistas de prueba (los primeros puestos de la lista de la convocatoria `[PRUEBA]`), algunos ya notificados y con entrega recibida.
- **05:** consulta paginada de finalistas con búsqueda por nombre o CI y el estado de notificación derivado.
- **06:** `FinalistaService.notificar`: valida, fija `plazo_entrega` y `notificado_en`, llama a `NotificacionService.notificar` por cada finalista y anota la bitácora; un fallo de uno no frena a los demás.
- **07:** `FinalistaService.registrarEntrega`: fecha por defecto hoy, rechaza fechas futuras, conserva en la bitácora el valor anterior si corrige.
- **08:** `ComunicadoService.publicar`: resuelve destinatarios (`TODOS`, `FINALISTAS`; rechaza `ADJUDICADOS`), guarda el comunicado y crea los avisos por lotes; listado.
- **09:** endpoints de la bandeja del usuario según su documento contrato (listar, contador de no leídas, marcar leída, leer todo, eliminar todas).
- **10:** controllers delgados con `ApiResponse` y sus tests de contrato (`MockMvc`).
- **11:** pruebas con Postgres real: flujo A con un finalista sin teléfono, reenvío a fallidos, corrección de fecha, comunicado a un grupo, aislamiento de la bandeja, `401`/`403`/`404`, y que ninguna postulación cambia de estado.
- **12:** `DatosPruebaFinalistas` (dev): un postulante finalista con sesión, avisos de ejemplo (uno «Fallida»), un comunicado de prueba y su documentación en `docs/README.md`.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Pasos 2–3 | Convocatorias y listado de finalistas | 05, 10 (convocatorias: endpoint de CU-06) |
| Pasos 4–7, A1, A2, A6 | Notificar y reenviar | 03, 06, 10 |
| Pasos 8–11, A5, A7 | Entrega física | 07, 10 |
| Pasos 12–16, A8 | Comunicados | 08, 10 |
| Paso 17 | Bandeja del Postulante | 09 |
| A9, A10, A11, A12 | `404`, vacío, `401`, `403` | 05, 10, 11 |
| A13 | Fallo de Firebase o correo | 06, 08 (lo resuelve CU-EXTRA-01) |
| CA01–CA20 | — | 05–11 (datos para verlo: 04 y 12) |

## Follow-ups (no se implementan en estas UT)

- Adjudicados como destinatarios (`ADJUDICADOS`), cuando existan las adjudicaciones (CU-05 / CU-22).
- Borrador y publicación programada de la maqueta.
- Bandeja y push en la app móvil.
- Que el CU-05 cree finalistas reales y el CU-07 consuma el estado de la entrega.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- El mecanismo de envío (push, correo, reintentos): es CU-EXTRA-01.
- Seleccionar finalistas, auditar, devolver o archivar (CU-05, CU-07, CU-19, CU-20).
- Consultar la bitácora (CU-25).
