# Unidades de trabajo — CU17-BE

**Caso de uso:** Backend del borrador de la postulación: guardar el avance del formulario sin exigir lo obligatorio, devolverlo al retomarlo y resumir «Mi borrador».
**Fuente CU:** [`CU17 - Guardar borrador de postulación`](../../../CasosDeUso/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md)
**Diagramas de grado:** `images/046` (contexto), `059` (análisis), `129` (secuencia) y `142` (mockup); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** `PUT /api/v1/postulaciones/{codigo}/borrador` (guardar) y `GET /api/v1/postulaciones/mi-borrador` (resumen). Retomar es el `POST /api/v1/postulaciones` del CU02, que ahora devuelve también `guardadoEn`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `ENDPOINTS_BACKEND.md` (`PUT` vs `PATCH`), `RESPONSES_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `LOGGING_BACKEND.md`, `TESTING_BACKEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/features/postulaciones/`.

1. **Sin tabla ni migración nuevas.** `respuesta_socioeconomica` ya nació con las columnas de datos *nullable* para este CU (decisión 3 de [CU02-BE](../../CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor/UT-BACKEND/README.md)); el guardado reutiliza `EnvioPostulacionConsultaRepository.guardarRespuesta` (el mismo `UPSERT`).
2. **`PUT`, no `PATCH`.** El cliente manda el formulario completo y un campo omitido se guarda vacío (el caso que [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) reserva para `PUT`). Es un sub-recurso `…/borrador` del dueño de la postulación.
3. **Un valor inválido rechaza todo el guardado** (`ERR_ENV_05`) en vez de «no guardar ese valor» (A2 del diagrama): guardar a medias dejaría al cliente sin saber qué quedó. Un campo vacío nunca es error.
4. **Reglas de valor compartidas con el envío:** se extraen a `ReglasRespuestaSocioeconomica` (solo revisa los campos presentes); `EnvioPostulacionService.validarRespuestas` primero exige que no falte nada y luego usa la misma clase, para que borrador y envío no discrepen.
5. **Se guarda también una `devuelta`**, no solo un `borrador`: el postulante que corrige una devuelta puede guardar su avance. Cualquier otro estado es `ERR_ENV_03`.
6. **«Convocatoria cerrada» se valida con `ValidacionPostulacionService.validar` (CU18)**, igual que el envío: el bloqueo de borradores al cerrar (A6) no necesita un job, porque ni guardar ni enviar pasan con la convocatoria cerrada.
7. **Sin códigos de error nuevos:** se reutilizan `ERR_ENV_02` (ajena o inexistente), `ERR_ENV_03` (ya enviada), `ERR_ENV_05` (dato inválido) y `ERR_VAL_*` (CU18); se actualiza su documentación.
8. **Fecha del último guardado = `respuesta_socioeconomica.actualizado_en`** (nula si no hay fila); sin columna nueva.
9. **«Mi borrador» por SQL JDBC** (`borradorAbierto`): solo `borrador` de una convocatoria `ABIERTA` y vigente; `data: null` si no hay.

## Contrato de datos / recurso HTTP

| Método y ruta | Rol | Respuesta |
|---------------|-----|-----------|
| `PUT /api/v1/postulaciones/{codigo}/borrador` | `POSTULANTE` | `200` con `{postulacion, guardadoEn, camposPendientes}` |
| `GET /api/v1/postulaciones/mi-borrador` | `POSTULANTE` | `200` con `{postulacion, convocatoria, gestion, fechaCierre, guardadoEn, camposPendientes, documentosPendientes}`, o `data: null` si no hay borrador |
| `POST /api/v1/postulaciones` (CU02) | `POSTULANTE` | su `respuesta` suma `guardadoEn` |

Cuerpo del `PUT`: `{ingresosFamiliares, dependientes, procedencia, patrimonio, estadoVulnerabilidad, promedioAcademico}`, todos opcionales. Errores: `ERR_ENV_02`, `ERR_ENV_03`, `ERR_ENV_05`, `ERR_VAL_01` a `ERR_VAL_03` y `ERR_AUTH_07`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU17-BE-01](./UT-CU17-BE-01.md) | Reglas de valor compartidas, consultas del borrador y `BorradorPostulacionService` | — |
| [UT-CU17-BE-02](./UT-CU17-BE-02.md) | `BorradorPostulacionController` | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (guardar), A1, A2, A7 | Guardar con validaciones | 01, 02 |
| Flujo B (retomar), A4 | `guardadoEn` en el formulario de CU02 | 01 |
| Flujo C («Mi borrador») | Resumen del borrador | 01, 02 |
| A5, A6 | Enviar y cierre de convocatoria (ya cubiertos por CU02 y CU18) | 01 |
| CA01 a CA11 | Guardar, retomar, rechazos y resumen | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- **Eliminar el borrador** («Eliminar» del mockup, img 142): el CU no lo describe; no se construyó.
- **Limpieza de borradores de convocatorias cerradas:** hoy solo quedan bloqueados (no se borran).
- Los tests de base real (`EnvioPostulacionConsultaRepositoryTest`: `guardadoEn` y `borradorAbierto`) y una prueba manual con el stack.

## Qué NO incluyen estas UT

- Las pantallas web y móvil (carpetas `UT-FRONTEND` y `UT-MOBILE`).
- El envío, el comprobante y la precalificación ([CU02](../../CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor/UT-BACKEND/README.md)) ni los documentos (CU16).
