# Unidades de trabajo — CU11-BACKEND

**Caso de uso:** Consultar los registros digitales de asistencia del periodo de una planilla y registrar el cotejo que el Administrador constató contra la planilla firmada.
**Fuente CU:** [`CU11 - Cotejar registro contra respaldo físico.md`](../../../CasosDeUso/CU11%20-%20Cotejar%20registro%20contra%20respaldo%20f%C3%ADsico.md)
**Diagramas de grado:** contexto img 070 · análisis img 083 · secuencia img 098 · mockup img 111 (diseño lógico img 091)
**Recurso HTTP / contrato:** `GET /api/v1/planillas/{codigo}/cotejos/datos`, `GET .../cotejos/registros`, `GET/POST .../cotejos`

> `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend` y `T/` = `.../src/test/java/com/comedoruagrm/backend`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_BACKEND`, `CONVENCIONES_JAVA_BACKEND`, `ENDPOINTS_BACKEND`, `RESPONSES_BACKEND`, `EXCEPCIONES_BACKEND`, `GUIA_ERRORES_BACKEND`, `PERSISTENCIA_BD_BACKEND`, `LOGGING_BACKEND`, `AUDITORIA_BITACORA_BACKEND`, `SEGURIDAD_AUTH_BACKEND` y `TESTING_BACKEND`. Si hay conflicto, ganan las rules. Molde: los módulos `planillas` (CU09) y `becas` (CU23).

## Decisiones de diseño

- **Módulo nuevo `features/cotejos/`**, prefijo de errores `COT`. Lee `planilla_asistencia`, `beca`, `asistencia`, `usuario` y `carrera` por JDBC (`CotejosConsultaRepository`) y escribe `cotejo_asistencia` con JPA.
- **El cotejo es manual.** El Administrador compara la planilla firmada a mano y declara total, coincidencias, discrepancias, resultado y observaciones; el sistema solo comprueba consistencia (`ERR_COT_02`, `03`, `04`). Sin lectura automática de la planilla (OCR): fuera de este CU.
- **Sin `cotejo_detalle`** (opcional en el diseño): no hay un detalle por becado y día que guardar mientras la planilla no se lea sola. Se agregará con esa lectura.
- **Registros digitales de una planilla** = asistencias del periodo de las becas de su convocatoria creadas antes de `generada_en` (la planilla no guarda su lista de becados). Aproximación documentada; si se quiere exactitud, un follow-up es guardar los becados de cada planilla.
- **`resultado` es `varchar` con `CHECK`**, como lo dibuja el diagrama (no un enum de PostgreSQL). Las reglas de consistencia están también como `CHECK` de la tabla.
- **Cotejos inmutables y múltiples** por planilla (A3): sin unicidad, sin `PUT`/`PATCH`/`DELETE`.
- **Anotación en la bitácora** (`COTEJO_ASISTENCIA`) con códigos, cantidades y resultado, sin observaciones.
- Rutas anidadas bajo la planilla (el cotejo no existe sin ella): `/api/v1/planillas/{codigo}/cotejos…`.

## Contrato de datos / recurso HTTP

| Método y ruta | Qué hace | Respuesta |
| --- | --- | --- |
| `GET /api/v1/planillas/{codigo}/cotejos/datos` | Planilla y conteo de registros digitales de su periodo | `200`, `data`: `{ planilla, convocatoria, convocatoriaNombre, fechaDesde, fechaHasta, totalBecados, generadaEn, registrosDigitales, becadosConAsistencia }` |
| `GET /api/v1/planillas/{codigo}/cotejos/registros` | Registros digitales del periodo (`page`, `pageSize`, `sort`: `fecha` o `apellidos`; sin `filter`) | `200`, `data` array `{ nombres, apellidos, carrera, fecha, horaRegistro }` + `meta` |
| `GET /api/v1/planillas/{codigo}/cotejos` | Cotejos registrados (`sort`: `creadoEn` o `fechaCotejo`; por defecto el más reciente) | `200`, `data` array de `CotejoResponse` + `meta` |
| `POST /api/v1/planillas/{codigo}/cotejos` cuerpo `{ totalRegistros, coincidencias, discrepancias, resultado, observaciones? }` | Registra el cotejo | `201`, `data`: `{ codigo, planilla, fechaCotejo, totalRegistros, coincidencias, discrepancias, resultado, observaciones, realizadoPor, creadoEn }` |

Errores: `ERR_COT_01` a `ERR_COT_04` ([`docs/errors/cotejos/`](../../../errors/cotejos/ERRORES_COTEJOS.md)), `ERR_SYS_01` (campos), `ERR_SYS_05` (`sort`/`filter`), `ERR_AUTH_07` (rol). Roles: solo `ADMINISTRADOR_DUBSS`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU11-BE-01](./UT-CU11-BE-01.md) | Migración y modelo de `cotejo_asistencia` | — |
| [UT-CU11-BE-02](./UT-CU11-BE-02.md) | Consultas de la planilla, los registros digitales y los cotejos | BE-01 |
| [UT-CU11-BE-03](./UT-CU11-BE-03.md) | Servicios, errores y bitácora | BE-02 |
| [UT-CU11-BE-04](./UT-CU11-BE-04.md) | Endpoints REST | BE-03 |
| [UT-CU11-BE-05](./UT-CU11-BE-05.md) | Pruebas | BE-04 |

**Cadena:** `01 → 02 → 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 3 | Datos y registros digitales | 02, 03, 04 |
| Flujo principal 7 y 8 | Validar y registrar | 01, 03, 04 |
| A1, A3 | Sin registros digitales; varios cotejos | 02, 03, 05 |
| A2, A7 | Reglas de consistencia | 01, 03, 05 |
| A6 | Planilla inexistente | 03, 05 |
| CA01 a CA12 | Contrato HTTP | 01 a 05 |

## Follow-ups (no se implementan en estas UT)

- Lectura automática (OCR) de la planilla firmada, que traería el detalle por becado y día (`cotejo_detalle`) y haría que el sistema calcule las coincidencias.
- Guardar los becados de cada planilla al generarla (para que «registros de la planilla» sea exacto).
- Confirmar con la DUBSS si se mantiene el CU-11 (el diseño lo marca «pendiente de consulta»).

## Qué NO incluyen estas UT

- La pantalla web (ver `UT-FRONTEND`), la generación de la planilla (CU-09), el registro de asistencia (CU-10) y el historial del becado (CU-24).
