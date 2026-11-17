# Unidades de trabajo — CU24-BACKEND

**Caso de uso:** El becado consulta el estado de su beca y su historial de asistencia (solo lectura).
**Fuente CU:** [`CU24 - Consultar historial de asistencia y estado de beca.md`](../../../CasosDeUso/CU24%20-%20Consultar%20historial%20de%20asistencia%20y%20estado%20de%20beca.md)
**Diagramas de grado:** contexto img 049 · análisis img 062 · secuencia img 132 · mockup img 145 (diseño lógico img 091)
**Recurso HTTP / contrato:** `GET /api/v1/mi-beca`, `GET /api/v1/mi-beca/asistencias`

> `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend` y `T/` = `.../src/test/java/com/comedoruagrm/backend`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_BACKEND`, `CONVENCIONES_JAVA_BACKEND`, `ENDPOINTS_BACKEND`, `RESPONSES_BACKEND`, `EXCEPCIONES_BACKEND`, `GUIA_ERRORES_BACKEND`, `PERSISTENCIA_BD_BACKEND`, `LOGGING_BACKEND`, `SEGURIDAD_AUTH_BACKEND` y `TESTING_BACKEND`. Si hay conflicto, ganan las rules. Molde: `asistencias` (CU10) y `bitacora` (CU25).

## Decisiones de diseño

- **Módulo nuevo `features/mibeca/`**, prefijo de errores `MBE`. El diagrama de análisis lo llama `MiBecaController`. Lee `usuario`, `beca`, `convocatoria`, `gestion_academica`, `adjudicacion`, `movimiento_beca` y `asistencia` por JDBC (`MiBecaConsultaRepository`) sin usar repositories de otros módulos. **No hay migración**: las tablas ya existen (CU10 y CU23).
- **Solo la propia beca (RF-19, A4):** la beca se busca por el `codigo` del usuario del token (`@CurrentUserId`); ningún endpoint recibe el código de otra persona ni de una beca.
- **Dos endpoints** (estado y listado) en lugar de uno: el estado es un recurso único y el historial una colección paginada, con el contrato de `ENDPOINTS_BACKEND` (`page`, `pageSize`, `filter[desde]`, `filter[hasta]`, `sort`). La secuencia dibuja los dos pasos por separado (`consultarEstadoBeca` y `listarAsistencias`).
- **A1 = `404` `ERR_MBE_01`** (el diagrama dibuja `sinBeca()`); la pantalla lo traduce a un aviso con enlace a CU-04.
- **El motivo** es el del último movimiento `BAJA`/`SUSPENSION` de la beca (subconsulta); una beca vigente no lo lleva.
- **Sin faltas ni cumplimiento:** el modelo de datos solo guarda asistencias registradas y no tiene calendario de días de comedor; no se inventa. Se registra como diferencia con el mockup.
- **Solo lectura:** sin bitácora, sin log de consultas, sin escritura.
- Solo `POSTULANTE` (`@PreAuthorize` en el `service`).

## Contrato de datos / recurso HTTP

| Método y ruta | Qué hace | Respuesta |
| --- | --- | --- |
| `GET /api/v1/mi-beca` | Beca del usuario | `200`, `data`: `{ codigo, estado, convocatoria, gestion, fechaAdjudicacion?, fechaInicio, fechaFin?, motivo?, totalAsistencias }` |
| `GET /api/v1/mi-beca/asistencias?page&pageSize&filter[desde]&filter[hasta]&sort` | Historial paginado (`sort`: `fecha` o `-fecha`, por defecto `-fecha`) | `200`, `data`: `[{ fecha, horaRegistro }]` + `meta` |

Errores: `ERR_MBE_01` a `ERR_MBE_03` ([`docs/errors/mibeca/`](../../../errors/mibeca/ERRORES_MIBECA.md)), `ERR_SYS_01` (`page`/`pageSize`), `ERR_SYS_05` (filtro no soportado o fecha mal formada), `ERR_AUTH_07` (rol).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU24-BE-01](./UT-CU24-BE-01.md) | Consultas de la beca y del historial | — |
| [UT-CU24-BE-02](./UT-CU24-BE-02.md) | Servicio, controller y errores | BE-01 |
| [UT-CU24-BE-03](./UT-CU24-BE-03.md) | Pruebas | BE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 2 a 5 | Estado, motivo e historial con rango | 01, 02, 03 |
| A1, A2, A3 | Sin beca, historial vacío, beca no vigente | 02, 03 |
| A4 | Solo la propia información, rol | 02, 03 |
| A5 | Rango y `sort` inválidos | 02, 03 |
| CA01 a CA10 | Contrato HTTP | 01 a 03 |

## Follow-ups (no se implementan en estas UT)

- Faltas y porcentaje de cumplimiento del mockup: requieren un calendario de días de comedor que el modelo no tiene.
- «Posición en la lista» del mockup: el puntaje de la preselección no es un dato del becado en este CU.
- Turno (almuerzo): el diseño lógico no lo tiene.

## Qué NO incluyen estas UT

- Escrituras sobre beca o asistencia (CU-23, CU-10).
- La web y la app (ver `UT-FRONTEND` y `UT-MOBILE`).
