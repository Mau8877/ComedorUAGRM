# Unidades de trabajo — CU09-BACKEND

**Caso de uso:** Generar la planilla de asistencia imprimible del padrón vigente de una convocatoria para un periodo, guardarla en MinIO, consultarla y descargarla.
**Fuente CU:** [`CU09 - Generar planilla de asistencia.md`](../../../CasosDeUso/CU09%20-%20Generar%20planilla%20de%20asistencia.md)
**Diagramas de grado:** contexto img 068 · análisis img 081 · secuencia img 096 · mockup img 109 (diseño lógico img 091)
**Recurso HTTP / contrato:** `GET/POST /api/v1/convocatorias/{codigo}/planillas`, `GET .../planillas/resumen`, `GET .../planillas/vista-previa`, `GET /api/v1/planillas/{codigo}/descarga`

> En estas fichas, `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

El molde más cercano es el CU08 (acta de adjudicación): mismo PDF con JasperReports, mismo almacenamiento en MinIO y misma descarga con dirección firmada.

## Decisiones de diseño

- **Un módulo nuevo `features/planillas/`** (paquete P5 «Becados y asistencia», prefijo de errores `PLA`). Lee la tabla `beca` por JDBC (`PlanillasConsultaRepository`) en vez de usar el repository del módulo `becas`, igual que el acta con los adjudicados ([ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia)).
- **Columna `total_becados` en `planilla_asistencia`.** El diagrama 091 no la dibuja, pero la lista de planillas del mockup 109 muestra «Becados» y el cotejo (CU-11) la compara con lo firmado. Se agrega la columna y se actualiza `DiagramaConceptual.mermaid` (queda anotado como diferencia con el diagrama).
- **Sin código «PL-0007».** El mockup muestra un código por planilla que el diagrama no tiene; la tabla identifica cada planilla por su periodo y su fecha de generación. No se agrega una columna `numero`.
- **Sin unicidad ni idempotencia por clave.** Una planilla es un registro de lo impreso: se pueden generar varias del mismo periodo (el padrón puede haber cambiado). Por eso no hay bloqueo de la convocatoria ni índice único, a diferencia del acta.
- **Límite de 31 días** por planilla (`ERR_PLA_03`): la hoja dibuja una columna de firma por día. Es un límite del formato, por definir con la DUBSS.
- **El periodo se valida en el `service`**, no solo con `@Valid`: la gestión académica de la convocatoria fija los límites (`ERR_PLA_02`). `@Valid` solo exige que ambas fechas vengan (`ERR_SYS_01`).
- **Una convocatoria cualquiera, no solo las publicadas.** El backend solo exige becas vigentes (`ERR_PLA_04`); la pantalla ofrece solo las convocatorias publicadas porque son las que tienen padrón.
- **Descargar se anota en la bitácora** (el PDF lleva CI), como el acta; listar, ver el resumen y la vista previa no.

## Contrato de datos / recurso HTTP

| Método y ruta | Qué hace | Respuesta |
| --- | --- | --- |
| `GET /api/v1/convocatorias/{codigo}/planillas/resumen` | Cuántos becados vigentes entrarían y los límites de la gestión | `200`, `data`: `{ becadosVigentes, gestion, gestionDesde, gestionHasta }` |
| `GET /api/v1/convocatorias/{codigo}/planillas` | Planillas de la convocatoria (`page`, `pageSize`, `sort`: `generadaEn` o `fechaDesde`; sin `filter` ni `search`) | `200`, `data` array de `PlanillaResponse` + `meta` |
| `POST /api/v1/convocatorias/{codigo}/planillas` | Genera la planilla. Cuerpo: `{ fechaDesde, fechaHasta }` | `201`, `data`: `PlanillaResponse` |
| `GET /api/v1/convocatorias/{codigo}/planillas/vista-previa?desde=&hasta=` | El PDF borrador, sin guardar | `200` `application/pdf` (los errores salen en el sobre) |
| `GET /api/v1/planillas/{codigo}/descarga` | Dirección temporal y firmada del PDF (5 min) | `200`, `data`: `{ url, expiraEn }` |

`PlanillaResponse`: `codigo`, `convocatoria` (código), `fechaDesde`, `fechaHasta`, `totalBecados`, `generadaEn`, `generadaPor` (nombre). Nunca lleva la ruta del archivo ni ids internos.

Errores: `ERR_PLA_01` a `ERR_PLA_06` (ver [`docs/errors/planillas/`](../../../errors/planillas/ERRORES_PLANILLAS.md)), `ERR_PRI_01` (convocatoria inexistente), `ERR_SYS_01` (fechas faltantes), `ERR_SYS_05` (`sort`/`filter` no soportado), `ERR_AUTH_07` (rol). Roles: solo `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU09-BE-01](./UT-CU09-BE-01.md) | Migración y modelo de `planilla_asistencia` | — |
| [UT-CU09-BE-02](./UT-CU09-BE-02.md) | Consultas de planillas y becados vigentes | BE-01 |
| [UT-CU09-BE-03](./UT-CU09-BE-03.md) | PDF de la planilla (JasperReports) | — |
| [UT-CU09-BE-04](./UT-CU09-BE-04.md) | Generar planilla, vista previa y resumen | BE-01, BE-02, BE-03 |
| [UT-CU09-BE-05](./UT-CU09-BE-05.md) | Listar y descargar planillas | BE-01, BE-02 |
| [UT-CU09-BE-06](./UT-CU09-BE-06.md) | Endpoints REST | BE-04, BE-05 |
| [UT-CU09-BE-07](./UT-CU09-BE-07.md) | Prueba de integración de punta a punta | BE-06 |

**Cadena:** `01 → 02 → 04, 05 → 06 → 07`, con `03` en paralelo antes de `04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (6 a 10) | Validar el periodo, leer vigentes, generar PDF, subir, registrar y anotar | 02, 03, 04, 06 |
| Flujo B | Listar y descargar | 05, 06 |
| A2 | Sin becas vigentes | 02, 04 |
| A3, A4, A5 | Periodo inválido | 04 |
| A6 | Convocatoria inexistente | 04, 05 |
| A7 | Falla del PDF o de MinIO | 03, 04 |
| A8, A9 | Planilla inexistente / archivo faltante | 05 |
| CA01 | Generar y registrar | 01, 04, 06, 07 |
| CA02 | Contenido del PDF | 03, 07 |
| CA03 a CA06 | Validaciones y padrón vacío | 04, 07 |
| CA07 | Solo becas vigentes | 02, 07 |
| CA08 | Atomicidad | 04 |
| CA09 | Listado paginado y ordenable | 02, 05, 06, 07 |
| CA10 | Descarga | 05, 07 |
| CA11 | Vista previa | 04, 06, 07 |
| CA12 | Rol | 04, 05, 07 |
| CA13 | Bitácora sin datos sensibles | 04, 05, 07 |
| CA14 | Convocatoria inexistente | 04, 07 |
| CA15 | Sin edición ni borrado | 06, 07 |

## Follow-ups (no se implementan en estas UT)

- Limpieza de archivos huérfanos en MinIO (si la base falla después de subir).
- Formato oficial de la planilla (encabezado, rótulos, días hábiles) y ajuste del límite de 31 días, cuando la DUBSS los defina.
- Que CU-11 (cotejo) lea `total_becados` y el periodo de cada planilla.

## Qué NO incluyen estas UT

- El registro de asistencia (CU-10), el cotejo (CU-11) y el historial del becado (CU-24).
- Cambiar el estado de las becas o el padrón (CU-23) ni avisar a nadie.
- La pantalla web (ver `UT-FRONTEND`).
