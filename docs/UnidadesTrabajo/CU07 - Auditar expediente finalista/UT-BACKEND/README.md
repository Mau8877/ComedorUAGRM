# Unidades de trabajo — CU07-BE

**Caso de uso:** Backend para consultar el expediente de un finalista, abrir sus documentos digitales, registrar la auditoría (resultado, observaciones y puntaje académico), avisar al postulante y mostrar el resultado en la lista de finalistas.
**Fuente CU:** [`CU07 - Auditar expediente finalista`](../../../CasosDeUso/CU07%20-%20Auditar%20expediente%20finalista.md)
**Diagramas de grado:** CU-07 en la documentación de grado: contexto (`066`), análisis (`079`), secuencia (`094`) y maqueta (`107`). Diseño lógico: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)).
**Recurso HTTP / contrato:** `/api/v1/finalistas/{codigo}/expediente`, `/auditorias` y `/documentos/{documentoCodigo}/acceso` (ver tabla).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

- **Módulo nuevo `features/expedientes/`.** La lista de finalistas sigue en `features/finalistas/` (CU-21); el módulo nuevo no lee el repository de otro módulo, solo consultas JDBC de lectura propias y el `service` de notificaciones.
- **Tablas:** `auditoria_expediente` (con la columna `puntaje_academico`, decisión de este CU que el diseño lógico todavía no tiene) y lo mínimo de `tipo_documento`, `convocatoria_documento`, `documento_adjunto` y `respuesta_socioeconomica` para poder probar mientras el CU-02 y el CU-16 (otro integrante) no estén. Se coordina con él para no duplicar migraciones (ver UT-01). El CU-05 (finalistas reales) se hará después y **amplía** `finalista` sin recrearla.
- **Puntaje:** la IA califica de 0 a 70 (UT-02) y el auditor escribe de 0 a 30. El **total no se guarda**: se calcula (IA vigente + puntaje académico de la última auditoría).
- **Auditorías append-only y la última vale.** No se edita ni se borra una auditoría; el Model no tiene setters.
- **Idempotencia (RNF-03):** el cliente manda un UUID por intento en el header opcional `Idempotency-Key`; repetir la petición con la misma clave devuelve la auditoría ya guardada (`200`, `creada = false`), la misma clave con otro contenido es `409` `ERR_EXP_06` y una clave que no es UUID, `400` `ERR_EXP_07`. Sin clave siempre se crea otra. Se guarda en `auditoria_expediente.clave_idempotencia` (índice único parcial por finalista) y la fila del finalista se bloquea durante el registro para que dos peticiones simultáneas no se pisen. *(Reemplaza la ventana de 60 segundos: una ventana de tiempo descarta correcciones legítimas y deja pasar duplicados tardíos.)*
- **Almacenamiento: MinIO.** Servicio transversal `common/storage/AlmacenamientoService`; los documentos se abren con una URL temporal de 5 minutos firmada con el host público (`MINIO_PUBLIC_URL`), no con rutas públicas.
- **Aviso al postulante:** tres eventos (`AUDITORIA_VALIDADA`, `AUDITORIA_OBSERVADA`, `AUDITORIA_NO_VALIDA`), bandeja y push; las observaciones viajan en el texto del aviso cuando el resultado no es «Validado». Un fallo de envío nunca deshace la auditoría.
- **Roles:** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en los `service`.
- **Errores** (prefijo `EXP`, documentar en `docs/errors/`): `ERR_EXP_01` entrega sin registrar (409), `ERR_EXP_02` postulación no «calificada» (409), `ERR_EXP_03` observaciones obligatorias (400), `ERR_EXP_04` puntaje académico obligatorio o fuera de 0–30 (400), `ERR_EXP_05` documento inexistente o ajeno (404). Finalista inexistente reutiliza `ERR_FIN_01`.
- **Bitácora:** `AUDITORIA_EXPEDIENTE` (resultado, puntaje académico y códigos; sin observaciones, nombres ni CI). Consultar el expediente o abrir un documento no se anota.
- **Datos de ejemplo:** UT-11, solo con perfil `dev`, sobre la convocatoria `[PRUEBA]` y sus 10 finalistas del CU-06 y CU-21.

## Contrato de datos / recurso HTTP

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `GET` | `/api/v1/finalistas/{codigo}/expediente` | — | `200` `ExpedienteResponse` | `404` `ERR_FIN_01` |
| `POST` | `/api/v1/finalistas/{codigo}/auditorias` | `{ resultado, observaciones?, puntajeAcademico? }` | `201` `AuditoriaResponse` (`200` si repite la última) | `400` `ERR_SYS_01`/`ERR_EXP_03`/`ERR_EXP_04`; `404` `ERR_FIN_01`; `409` `ERR_EXP_01`/`ERR_EXP_02` |
| `GET` | `/api/v1/finalistas/{codigo}/documentos/{documentoCodigo}/acceso` | — | `200` `{ url, expiraEn }` | `404` `ERR_FIN_01`/`ERR_EXP_05` |
| `GET` | `/api/v1/finalistas` (CU-21, ampliado) | — | `FinalistaResponse` + `resultadoAuditoria` y `puntajeTotal` | los del CU-21 |

`ExpedienteResponse` = `finalista` (código, posición, plazo, `entrega`: estado y fecha), `postulante` (nombres, apellidos, CI, carrera), `convocatoria`, `respuestaSocioeconomica` (con `promedioAcademico`), `documentos` (tipo, obligatorio, `RECIBIDO`/`FALTA`, nombre de archivo, MIME, código), `estadoPostulacion`, `puntajeIa` con `desglose`, `puntajeAcademico`, `puntajeTotal`, `auditorias` (resultado, observaciones, puntaje académico, fecha, auditor). Todos los identificadores son códigos públicos (UUID). Todos exigen `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| UT-CU07-BE-01 | Migración: auditoría de expediente y tablas de apoyo | — |
| UT-CU07-BE-02 | Puntaje de la IA de 0 a 70 | — |
| UT-CU07-BE-03 | Model, enum y repository de la auditoría | 01 |
| UT-CU07-BE-04 | Códigos de error, bitácora, DTOs y eventos de aviso | — |
| UT-CU07-BE-05 | Almacenamiento en MinIO y acceso temporal | — |
| UT-CU07-BE-06 | Consulta del expediente | 03, 04 |
| UT-CU07-BE-07 | Registrar la auditoría y avisar | 03, 04 |
| UT-CU07-BE-08 | Lista de finalistas con el resultado de la auditoría | 03 |
| UT-CU07-BE-09 | Acceso temporal a un documento | 03, 04, 05 |
| UT-CU07-BE-10 | Endpoints del expediente y la auditoría | 06, 07, 08, 09 |
| UT-CU07-BE-11 | Expedientes de prueba en el cargador de desarrollo | 01, 02, 03, 05 |
| UT-CU07-BE-12 | Pruebas de integración con base real | 10, 11 |

**Cadena:** `01 → 03`; `02`, `04` y `05` independientes; `03, 04 → 06, 07`; `03 → 08`; `03, 04, 05 → 09`; `06–09 → 10`; `01, 02, 03, 05 → 11`; `10, 11 → 12`.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Pasos 2–3 | Lista de finalistas con su auditoría | 08, 10 |
| Paso 4, A4 | Expediente y documentos | 05, 06, 09, 10 |
| Paso 5 | Notificar y registrar la entrega (CU-21 incluido) | — (ya existe) |
| Pasos 7–9, A5, A12, A16 | Registrar la auditoría y avisar | 04, 07, 10 |
| A6, A7, A8 | `409` y `400` | 04, 07 |
| A9, A11, A13, A14 | `404`, `401`, `403` | 06, 10, 12 |
| CA01–CA21 | — | 02–12 |

## Follow-ups (no se implementan en estas UT)

- Que el CU-05 cree finalistas reales y use el puntaje total para adjudicar.
- Devolver, archivar o rechazar desde el expediente (CU-19, CU-20).
- Exportar el expediente.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- El cálculo del puntaje de la IA (CU-03) ni el modelo de postulación y documentos (CU-02, CU-16).
- El mecanismo de envío de push (es CU-EXTRA-01).
- La app móvil.
