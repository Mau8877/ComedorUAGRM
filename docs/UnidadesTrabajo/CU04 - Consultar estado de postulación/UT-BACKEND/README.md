# Unidades de trabajo — CU04-BE

**Caso de uso:** Backend de solo lectura para que el Postulante consulte sus postulaciones enviadas (listado) y el detalle de cada una: estado visible, línea de tiempo y la información asociada (observaciones, motivo, bloque de finalista, resultado).
**Fuente CU:** [`CU04 - Consultar estado de postulación`](../../../CasosDeUso/CU04%20-%20Consultar%20estado%20de%20postulación.md)
**Diagramas de grado:** CU-04 en la documentación de grado (diagrama del CU, análisis y secuencia de `images/051`–`063` y `121`–`133`, maqueta de `images/134`–`146`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)). El diagrama de secuencia y el de análisis del CU-04 deben ampliarse con `Finalista`, `Adjudicacion` y `PublicacionResultado` como lectura (ver «Cambios pendientes en el documento de taller» del CU).
**Recurso HTTP / contrato:** ver la tabla de abajo.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`.

- **Sin tablas ni columnas nuevas, sin migración.** Todo se lee de `postulacion`, `postulacion_historial`, `finalista`, `adjudicacion`, `publicacion_resultado`, `notificacion`, `convocatoria` y `gestion_academica`. Por eso no hay UT de migración ni cambio en `DiagramaConceptual.mermaid`.
- **Módulo:** el dueño del dato de `postulacion` es `features/postulaciones/`; el CU04 vive ahí (`MisPostulacionesController`, `MisPostulacionesService`, repository de consulta JDBC propio, igual que `DevolucionConsultaRepository`). Para saber si los resultados están publicados se consulta `publicacion_resultado` con SQL propio en ese repository de consulta: **no** se importa nada de `features/resultados/` ni de `features/notificaciones/` (regla de dependencia entre módulos).
- **Solo lectura:** ningún método escribe; no hay `@Auditar`, no se anota en la bitácora y no se marcan avisos como leídos (CA13). Los métodos van con `@Transactional(readOnly = true)`.
- **Identidad:** el postulante sale del token (`@CurrentUserId`), nunca de la URL ni del body. Una postulación inexistente **o ajena** responde el mismo `404` (no se revela si existe).
- **Roles:** `@PreAuthorize("hasRole('POSTULANTE')")` en el `service` (`403` `ERR_AUTH_07` para otro rol; `401` ya lo resuelve la seguridad).
- **Qué se lista:** solo estados distintos de `borrador` (de `pendiente` en adelante), orden por defecto `-fechaEnvio`, paginado, `filter[gestionAcademica]` (código público) y `sort` por `fechaEnvio`.
- **Estado visible** (función pura, probada aparte): si el estado real es `adjudicada` o `no_adjudicada` **y** la convocatoria no tiene fila en `publicacion_resultado`, se devuelve `calificada`; en cualquier otro caso, el estado real. Mientras no esté publicado se **oculta** la fila del historial cuyo estado nuevo es `adjudicada` o `no_adjudicada` (decisión 3 del CU-05). La devolución, el archivo y el rechazo se ven de inmediato.
- **Línea de tiempo:** salen de `postulacion_historial` (estado anterior y nuevo, fecha y hora, orden ascendente), sin `usuario_id` (no se muestra quién hizo el cambio). Los pasos futuros se calculan según el estado visible y se devuelven como `PENDIENTE`; no se inventa un paso pasado sin fila de historial.
- **Información asociada según el estado visible:**
  - `devuelta`: `observaciones` = `motivo` de la última fila del historial con estado nuevo `devuelta`, con su fecha, y `puedeCorregir` (ver «Enganche con el CU-02»).
  - `archivada` o `no_adjudicada` **por rechazo del CU-20:** `motivo` de la última fila de historial de ese cambio. Un `no_adjudicada` por decisión final (CU-05) **no** devuelve motivo.
  - Finalista (hay fila `finalista` de la postulación): `plazoEntrega`, `documentacionRecibida` (`documentacion_recibida_en` no nulo) y `documentosFisicos`, tomado del mensaje de la última `notificacion` de esa postulación creada por el aviso de finalista (CU-21).
  - Resultado publicado: `resultado` propio (`ADJUDICADA` o `NO_ADJUDICADA`) y el código de la convocatoria para el botón «Ver resultados». Nunca el motivo de la decisión final.
- **Enganche con el CU-02 (otra persona del equipo):** este CU **no** crea envíos ni correcciones. `puedeCorregir` es verdadero solo si el estado visible es `devuelta`; el frontend oculta el botón mientras no exista la pantalla del CU-02. Si el CU-02 aún no escribe filas en `postulacion_historial` al enviar, la línea de tiempo no tendrá el primer paso «Postulación enviada»: el CU-04 no lo fabrica (ver CU, «Puntos de enganche con el CU-02»).
- **Un código de error propio**, prefijo `POS` (a confirmar al crear la UT, no hay otro uso en `docs/errors/`): `ERR_POS_01` (`404`, la postulación no existe o es de otra persona). `sort` o `filter` no soportado reutiliza `ERR_SYS_05`.
- **Sin N+1:** el listado resuelve estado, gestión y publicación en una sola consulta; el detalle usa un número fijo de consultas (una por bloque), que una prueba fija.
- **Avisos que abren el detalle (A6):** las filas de `notificacion` ya guardan `postulacion_id`, pero `NotificacionResponse` no lo expone. La UT-05 agrega `postulacionCodigo` (nulable) al aviso de la bandeja, y el módulo de notificaciones lo resuelve con su propio SQL.
- **Datos de ejemplo:** UT-06, solo con perfil `dev`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Query | Respuesta | Errores |
|--------|------|-------|-----------|---------|
| `GET` | `/api/v1/postulaciones/mias` | `filter[gestionAcademica]`, `sort`, `page`, `pageSize` | `200` array de `MiPostulacionResumenResponse` + `meta` | `400` `ERR_SYS_05`; `401`; `403` `ERR_AUTH_07` |
| `GET` | `/api/v1/postulaciones/{codigo}` | — | `200` `MiPostulacionDetalleResponse` | `404` `ERR_POS_01`; `401`; `403` `ERR_AUTH_07` |

- `MiPostulacionResumenResponse` = `codigo`, `convocatoria` (`codigo`, `nombre`), `gestionAcademica` (`codigo`, `nombre`), `fechaEnvio`, `estado` (el visible), `numeroEnvios`.
- `MiPostulacionDetalleResponse` = el resumen más `lineaDeTiempo` (lista de `{ estado, fecha, situacion }`, con `situacion` = `COMPLETADO` o `PENDIENTE`), `observaciones` (`texto`, `fecha`; solo si `devuelta`), `motivo` (solo archivo o rechazo CU-20), `finalista` (`plazoEntrega`, `documentacionRecibida`, `documentosFisicos`; solo si es finalista), `resultado` (`ADJUDICADA`, `NO_ADJUDICADA` o `null`) y `puedeCorregir`.
- Todos los identificadores son códigos públicos (UUID); nunca CI, correo ni datos socioeconómicos.
- Un `GET /api/v1/postulaciones/{codigo}` con una ruta `mias` no choca: `mias` se declara como ruta literal antes del `{codigo}`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU04-BE-01](./UT-CU04-BE-01.md) | Código de error, DTOs y estado visible | — |
| [UT-CU04-BE-02](./UT-CU04-BE-02.md) | Listado «Mis postulaciones» | 01 |
| [UT-CU04-BE-03](./UT-CU04-BE-03.md) | Detalle: línea de tiempo e información asociada | 01 |
| [UT-CU04-BE-04](./UT-CU04-BE-04.md) | Endpoints del postulante | 02, 03 |
| [UT-CU04-BE-05](./UT-CU04-BE-05.md) | Código de postulación en los avisos de la bandeja | — |
| [UT-CU04-BE-06](./UT-CU04-BE-06.md) | Datos de prueba | — |
| [UT-CU04-BE-07](./UT-CU04-BE-07.md) | Pruebas de integración con base real | 04, 05, 06 |

**Cadena:** `01 → 02, 03 → 04`; `05` y `06` independientes; `04, 05, 06 → 07`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---|---|---|
| Pasos 1–2 | Listado ordenado | 02, 04 |
| Pasos 3–6 | Detalle con línea de tiempo | 03, 04 |
| A1 | Sin postulaciones (lista vacía) | 02, 07 |
| A2 | Filtro por gestión | 02, 04 |
| A3, A4, A5 | Observaciones, motivo, finalista y resultado | 03 |
| A6 | Aviso abre el detalle | 05 |
| A7, A8, A9 | `401`, `403`, `404` | 03, 04, 07 |
| CA01–CA03 | Listado, vacío y filtro | 02, 07 |
| CA04 | Estado visible e historial sin autor | 01, 03 |
| CA05–CA06 | Observaciones y motivo | 03 |
| CA07–CA08 | «Calificada» hasta publicar; resultado sin motivo de decisión final | 01, 03, 07 |
| CA09 | Finalista | 03 |
| CA10 | Aviso abre el detalle | 05 |
| CA11 | `404`/`401`/`403` | 03, 04, 07 |
| CA12 | Los borradores no se listan | 02, 07 |
| CA13 | Sin efectos secundarios | 02, 03, 07 |

## Follow-ups (no se implementan en estas UT)

- Pantalla de corrección y reenvío del CU-02 (otra persona del equipo); cuando exista, `puedeCorregir` ya la habilita.
- Que el CU-02 escriba filas en `postulacion_historial` al enviar y reenviar, y que sus avisos guarden `postulacion_id`.
- Plazo para corregir una devolución (por definir con la DUBSS).

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Enviar, corregir o reenviar una postulación (CU-02), guardar borrador (CU-17), devolver (CU-19), archivar o rechazar (CU-20), notificar finalistas (CU-21) y publicar resultados (CU-22): este CU solo los lee.
- Marcar avisos como leídos y la bandeja en sí (CU-EXTRA-01).
- La app móvil (ver [`UT-MOBILE`](../UT-MOBILE/README.md)).
