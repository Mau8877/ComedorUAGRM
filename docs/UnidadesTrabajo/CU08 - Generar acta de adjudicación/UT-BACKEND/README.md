# Unidades de trabajo — CU08-BE

**Caso de uso:** Backend para generar el acta de adjudicación en PDF de una convocatoria con resultados publicados: asignar un número automático, guardar el archivo en MinIO, registrar el acta, y dejar consultarla y descargarla.
**Fuente CU:** [`CU08 - Generar acta de adjudicación`](../../../CasosDeUso/CU08%20-%20Generar%20acta%20de%20adjudicación.md)
**Diagramas de grado:** CU-08 en la documentación de grado (contexto `067`, análisis `080`, secuencia `095` y maqueta `108`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** ver la tabla de abajo.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

Las decisiones de negocio están confirmadas por Mau (2026-11-13); las técnicas de abajo son la propuesta del equipo, por revisar.

- **Una sola tabla nueva:** `acta_adjudicacion`. Ya está en el diseño lógico de grado, con un cambio: `convocatoria_id` pasa a ser **único** entre las filas no eliminadas (una acta por convocatoria). Se actualiza el diseño **antes** que la migración (`PERSISTENCIA_BD_BACKEND.md`). No se tocan `adjudicacion`, `publicacion_resultado` ni `convocatoria`.
- **Módulo nuevo `features/actas/`**, dueño de `acta_adjudicacion`. Lee convocatorias, publicación, adjudicados y carreras con consultas JDBC propias (mismo estilo que `features/resultados/`); ningún módulo lee el repository del otro. Usa `AlmacenamientoService` (`common/storage/`) para MinIO y `UsuarioService` para el actor.
- **Se genera solo con los resultados publicados:** existe una fila en `publicacion_resultado` de la convocatoria. Publicar **no** exige el acta (el CU-22 no se modifica).
- **Una sola acta, definitiva:** no se regenera ni se elimina; no hay endpoints para eso. Sin idempotencia por clave: se bloquea la fila de la convocatoria (`FOR UPDATE`, igual que el CU-22) y la segunda petición recibe `409` `ERR_ACT_02`; el índice único parcial de `convocatoria_id` es la segunda red de seguridad.
- **Número automático `NNN/AAAA`:** correlativo que reinicia cada año (`AAAA` = año de la fecha de generación en `America/La_Paz`), al menos tres dígitos, único en todo el sistema (`UNIQUE (numero)`). Para que dos generaciones simultáneas de **convocatorias distintas** no repitan número, el correlativo se calcula bajo un bloqueo de transacción por año (`pg_advisory_xact_lock`); la unicidad de `numero` es la red de seguridad.
- **PDF con JasperReports**, igual que la lista priorizada del CU06 (plantilla `R/reports/acta_adjudicacion.jrxml` compilada una sola vez). Lee los adjudicados en una sola consulta (una acta tiene como mucho unos cientos de filas: cupos de la convocatoria). La vista previa es la misma plantilla con el número «borrador».
- **Orden de la tabla del acta:** `puntaje_final` de mayor a menor; a igual puntaje, la posición del finalista (`finalista.posicion`); el n.º de orden del PDF es ese orden. Incluye CI (el acta es un documento interno del Administrador).
- **Orden de las operaciones al generar:** valida → bloquea → número → PDF → **sube a MinIO** → guarda la fila y la bitácora, todo dentro de la misma transacción. Si MinIO falla, no se guarda nada. Si la base falla después de subir, el archivo queda huérfano pero inaccesible (nadie lo enlaza); se acepta, igual que el expediente del CU-07.
- **Ruta del archivo:** `actas/{codigo_convocatoria}/acta-{NNN}-{AAAA}.pdf` (la `/` del número se reemplaza). Nunca viaja a un response ni a la bitácora.
- **Descarga:** dirección temporal firmada de MinIO (`AlmacenamientoService.urlTemporal`, 5 minutos), como el expediente; antes se comprueba que el objeto exista (`ERR_ACT_04`).
- **Cuatro códigos de error propios**, prefijo `ACT`: `ERR_ACT_01` (`409`, resultados sin publicar), `ERR_ACT_02` (`409`, ya tiene acta), `ERR_ACT_03` (`404`, aún no tiene acta) y `ERR_ACT_04` (`404`, archivo ausente del almacenamiento). Convocatoria inexistente reutiliza `ERR_PRI_01`; `sort` o `filter` no soportado, `ERR_SYS_05`.
- **Bitácora:** `GENERACION_ACTA` (acta, convocatoria, número, cantidad de adjudicados) y `DESCARGA_ACTA` (acta y convocatoria). Sin nombres, CI, rutas ni direcciones firmadas. La vista previa y la consulta de la tabla no se anotan.
- **Sin cambios de código en CU-05, CU-22 ni CU-23** y ningún cambio de estado de postulaciones ni de la convocatoria. No se envía ningún aviso.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en el `service` para todo. Ningún endpoint del CU lo usa el postulante.
- **Datos de prueba:** no hace falta una UT: la convocatoria `[PRUEBA] Publicada` que deja el cargador del CU-22 (publicada y sin acta) alcanza para generar a mano; el `409` de «sin publicar» se prueba con `[PRUEBA]`.

## Contrato de datos / recurso HTTP

| Método | Ruta | Query / cuerpo | Respuesta | Errores |
|--------|------|----------------|-----------|---------|
| `GET` | `/api/v1/actas` | `search`, `sort`, `page`, `pageSize` | `200` array de `ActaResponse` + `meta` | `400` `ERR_SYS_05` |
| `GET` | `/api/v1/actas/pendientes` | `page`, `pageSize` | `200` array de `ConvocatoriaPendienteActaResponse` + `meta` | — |
| `GET` | `/api/v1/convocatorias/{codigo}/acta` | — | `200` `ActaResponse` | `404` `ERR_PRI_01`, `ERR_ACT_03` |
| `GET` | `/api/v1/convocatorias/{codigo}/acta/descarga` | — | `200` `AccesoActaResponse` | `404` `ERR_PRI_01`, `ERR_ACT_03`, `ERR_ACT_04` |
| `GET` | `/api/v1/convocatorias/{codigo}/acta/vista-previa` | — | `200` `application/pdf` (sin sobre) | `404` `ERR_PRI_01`; `409` `ERR_ACT_01`, `ERR_ACT_02` |
| `POST` | `/api/v1/convocatorias/{codigo}/acta` | — | `201` `ActaResponse` | `404` `ERR_PRI_01`; `409` `ERR_ACT_01`, `ERR_ACT_02` |

- `ActaResponse` = `codigo`, `numero`, `convocatoria` (`codigo`, `nombre`), `gestion` (etiqueta), `adjudicados` (cantidad), `generadaEn` y `generadaPor` (solo el nombre para mostrar). Nunca la ruta del archivo. Orden por defecto del listado: `-generadaEn`; `sort` admite `numero` y `generadaEn`; `search` busca en el número y en el nombre de la convocatoria.
- `ConvocatoriaPendienteActaResponse` = `codigo`, `nombre`, `publicadoEn`, `adjudicados`. Son las convocatorias con resultados publicados y sin acta. Orden `-publicadoEn`.
- `AccesoActaResponse` = `url` (temporal y firmada) y `expiraEn`.
- La vista previa devuelve el PDF directamente, como la exportación del CU06 (`Content-Type: application/pdf`, `Content-Disposition: inline`); los errores sí salen con el sobre estándar.

Todos los identificadores son códigos públicos (UUID).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU08-BE-01](./UT-CU08-BE-01.md) | Migración: tabla `acta_adjudicacion` | — |
| [UT-CU08-BE-02](./UT-CU08-BE-02.md) | Model y repository del acta | 01 |
| [UT-CU08-BE-03](./UT-CU08-BE-03.md) | Códigos de error, bitácora y DTOs | — |
| [UT-CU08-BE-04](./UT-CU08-BE-04.md) | Consultas del módulo de actas | 01 |
| [UT-CU08-BE-05](./UT-CU08-BE-05.md) | Número de acta automático | 04 |
| [UT-CU08-BE-06](./UT-CU08-BE-06.md) | Plantilla y generador del PDF | 04 |
| [UT-CU08-BE-07](./UT-CU08-BE-07.md) | Generar el acta y vista previa | 02, 03, 04, 05, 06 |
| [UT-CU08-BE-08](./UT-CU08-BE-08.md) | Listar, consultar y descargar actas | 02, 03, 04 |
| [UT-CU08-BE-09](./UT-CU08-BE-09.md) | Endpoints del Administrador | 07, 08 |
| [UT-CU08-BE-10](./UT-CU08-BE-10.md) | Pruebas de integración con base real | 09 |

**Cadena:** `01 → 02, 04`; `03` independiente; `04 → 05, 06`; `02, 03, 04, 05, 06 → 07`; `02, 03, 04 → 08`; `07, 08 → 09`; `09 → 10`.

## Criterios de aceptación cubiertos

| CA (CU08) | Descripción | UTs |
|---|---|---|
| CA01 | Generar con resultados publicados | 02, 06, 07, 09, 10 |
| CA02 | Sin publicar (`409` `ERR_ACT_01`) | 04, 07, 10 |
| CA03 | Ya tiene acta (`409` `ERR_ACT_02`) | 04, 07, 10 |
| CA04 | Doble petición y concurrencia | 01, 07, 10 |
| CA05 | Números distintos con generaciones simultáneas | 05, 10 |
| CA06 | Formato `NNN/AAAA`, correlativo y único | 01, 05, 10 |
| CA07 | Contenido del PDF | 04, 06 |
| CA08 | El PDF no lleva no adjudicados ni datos sensibles | 04, 06 |
| CA09 | Acta sin adjudicados | 06, 07, 10 |
| CA10 | Vista previa sin registrar ni consumir número | 05, 07, 09, 10 |
| CA11 | Cancelar la confirmación | — (solo frontend) |
| CA12 | Falla el PDF o MinIO: no se registra | 07, 10 |
| CA13 | Descarga con dirección temporal y bitácora | 03, 08, 10 |
| CA14 | Sin acta: `404` `ERR_ACT_03` | 08, 10 |
| CA15 | Archivo ausente: `404` `ERR_ACT_04` | 08, 10 |
| CA16 | Bitácora sin datos sensibles | 03, 07, 08, 10 |
| CA17 | Tabla paginada, búsqueda y orden | 04, 08, 09, 10 |
| CA18 | No cambia estados ni avisa | 07, 10 |
| CA19 | `403`/`401`/`404` | 07, 08, 09, 10 |
| CA20 | No se regenera ni se elimina | 07, 09 |

## Follow-ups (no se implementan en estas UT)

- Resolver con la DUBSS si el CU-08 se mantiene (Q5), el formato oficial del acta (encabezado, firmas, formato del número) y si hace falta un comprobante individual por beneficiario.
- Actualizar el documento de taller (ver la tabla del CU). El CU-05 y el CU-22 del repositorio ya quedaron alineados.
- Limpieza de PDF huérfanos en MinIO si algún día molestan.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Publicar resultados (CU-22), adjudicar (CU-05), padrón de becados (CU-23) y comprobantes individuales.
- Firma digital, regeneración o eliminación del acta.
- La app móvil.
