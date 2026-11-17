# Unidades de trabajo — CU23-BACKEND

**Caso de uso:** dar de alta a los adjudicados como becados, consultar el padrón y registrar bajas, suspensiones y reemplazos.
**Fuente CU:** [`CU23 - Gestionar padrón de becados.md`](../../../CasosDeUso/CU23%20-%20Gestionar%20padr%C3%B3n%20de%20becados.md)
**Diagramas de grado:** contexto img 075 · análisis img 088 · secuencia img 103 · mockup img 116 · diseño lógico img 091
**Recurso HTTP / contrato:** ver la tabla de abajo

## Antes de implementar cualquier UT

Leer [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md),
[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y
[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md). Estas fichas no
sustituyen esas reglas.

## Decisiones de diseño

- **Módulo `features/becas/`**, dueño de `beca` y `movimiento_beca`. Lee adjudicaciones, finalistas y puntajes por
  JDBC (`PadronConsultaRepository`), igual que el CU05, sin usar los repositories de otros módulos.
- **Alta como acción explícita** (flujo D del CU): `POST .../padron/alta` crea una beca vigente por cada adjudicada sin
  beca. Es idempotente (índices únicos parciales + `ON CONFLICT DO NOTHING`). No toca el CU05 ni el CU22.
- **El reemplazante no recibe adjudicación propia**: su beca nace con `adjudicacion_id` nulo y su postulación no cambia
  de estado. *Pendiente de confirmar con la DUBSS* (el diagrama img 103 pasa `adjudicacionId`).
- **«Postulante disponible»** para reemplazar = el siguiente finalista (por `finalista.posicion`) cuya última auditoría
  es «Validado», con puntaje total (IA + académico) igual o mayor al mínimo, postulación «calificada» y sin beca.
- **Fecha de inicio** de la beca = fecha de publicación de los resultados (hora de La Paz); en un reemplazo, el día
  del reemplazo. `fecha_fin` queda nula (ningún CU la define).
- **Motivo del movimiento de reemplazo**: el CU no pide motivo en ese flujo y la columna es obligatoria; se guarda un
  texto fijo («Reemplazo del cupo liberado por la baja de otra beca»).
- **Sin reactivación** (el CU no la describe) ni `GET` del historial (el mockup tiene el botón «Historial»; queda como
  follow-up).
- **Códigos y verbos**: la ruta usa siempre el `codigo` público; las acciones van como sub-recurso verbo en infinitivo.

## Contrato de datos / recurso HTTP

Todos exigen `ADMINISTRADOR_DUBSS` (`403` `ERR_AUTH_07`).

| Método y ruta | Entrada | `data` | Errores |
| --- | --- | --- | --- |
| `GET /api/v1/convocatorias/{codigo}/padron/resumen` | — | `{convocatoria{codigo,nombre}, resultadosPublicados, adjudicados, pendientesDeAlta, vigentes, suspendidas, bajas}` | `404` `ERR_BEC_02` |
| `GET /api/v1/convocatorias/{codigo}/padron` | `page`, `pageSize`, `search` (nombre, apellidos o CI), `filter[estado]`, `sort` (`becado`, `ci`, `carrera`, `estado`, `fechaInicio`; defecto `becado`) | `[BecaResponse]` + `meta` | `404` `ERR_BEC_02`, `409` `ERR_BEC_03`, `400` `ERR_BEC_10`, `ERR_SYS_05` |
| `POST /api/v1/convocatorias/{codigo}/padron/alta` | — | `{creadas}` | `404` `ERR_BEC_02`, `409` `ERR_BEC_03`, `ERR_BEC_04` |
| `POST /api/v1/becas/{codigo}/dar-de-baja` | `{motivo}` | `BecaResponse` | `404` `ERR_BEC_01`, `400` `ERR_SYS_01`, `409` `ERR_BEC_05` |
| `POST /api/v1/becas/{codigo}/suspender` | `{motivo}` | `BecaResponse` | `404` `ERR_BEC_01`, `400` `ERR_SYS_01`, `409` `ERR_BEC_05` |
| `GET /api/v1/becas/{codigo}/reemplazo` | — | `CandidatoReemplazoResponse` | `404` `ERR_BEC_01`, `409` `ERR_BEC_06`, `ERR_BEC_07`, `ERR_BEC_09` |
| `POST /api/v1/becas/{codigo}/reemplazar` | `{postulanteCodigo}` | `{becaLiberada, becaReemplazante}` | `404` `ERR_BEC_01`, `409` `ERR_BEC_06`, `ERR_BEC_07`, `ERR_BEC_08`, `ERR_BEC_09` |

`BecaResponse`: `{codigo, becado{codigo, nombres, apellidos, ci, carrera}, estado, fechaInicio, fechaFin}`.
`CandidatoReemplazoResponse`: `{postulanteCodigo, nombres, apellidos, ci, carrera, posicion, puntajeTotal}`.
El `becado.codigo` es el `codigo_postulante`; nunca se expone un id interno.

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU23-BE-01](./UT-CU23-BE-01.md) | Tablas `beca` y `movimiento_beca` y sus modelos | — |
| [UT-CU23-BE-02](./UT-CU23-BE-02.md) | Consulta del padrón (resumen y listado) | BE-01 |
| [UT-CU23-BE-03](./UT-CU23-BE-03.md) | Alta de los adjudicados | BE-01, BE-02 |
| [UT-CU23-BE-04](./UT-CU23-BE-04.md) | Baja y suspensión | BE-01, BE-02 |
| [UT-CU23-BE-05](./UT-CU23-BE-05.md) | Reemplazo de un cupo liberado | BE-01, BE-04 |
| [UT-CU23-BE-06](./UT-CU23-BE-06.md) | Pruebas | BE-02 a BE-05 |

**Cadena:** `01 → 02 → 03, 04 → 05 → 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Flujo D | Dar de alta | 01, 03 |
| Flujo A | Consultar el padrón | 02 |
| Flujo B | Baja y suspensión | 04 |
| Flujo C | Reemplazo | 05 |
| A1 a A8 | Alternativas | 03, 04, 05 |
| CA00 a CA10 | Criterios de aceptación | 02 a 06 |

## Follow-ups (no se implementan en estas UT)

- `GET /becas/{codigo}/movimientos` (botón «Historial» del mockup).
- Reactivar una beca suspendida (`REACTIVACION`).
- Confirmar con la DUBSS los puntos de «Decisiones pendientes» del CU.
- Actualizar `DiagramaConceptual.mermaid` y la imagen 091 si cambia `beca.adjudicacion_id` a nulable (se hace en BE-01).

## Qué NO incluyen estas UT

- La planilla de asistencia (CU-09), la asistencia (CU-10) y el historial del becado (CU-24).
- Cambiar el estado de las postulaciones o crear adjudicaciones.
- Notificar al becado de su baja o suspensión.
