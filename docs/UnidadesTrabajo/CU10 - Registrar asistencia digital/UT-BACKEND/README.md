# Unidades de trabajo — CU10-BACKEND

**Caso de uso:** Buscar un becado por CI y registrar su asistencia de hoy, de forma idempotente.
**Fuente CU:** [`CU10 - Registrar asistencia digital.md`](../../../CasosDeUso/CU10%20-%20Registrar%20asistencia%20digital.md)
**Diagramas de grado:** contexto img 069 · análisis img 082 · secuencia img 097 · mockup img 110 (diseño lógico img 091)
**Recurso HTTP / contrato:** `POST /api/v1/asistencias/buscar-becado`, `POST /api/v1/asistencias`

> `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend` y `T/` = `.../src/test/java/com/comedoruagrm/backend`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_BACKEND`, `CONVENCIONES_JAVA_BACKEND`, `ENDPOINTS_BACKEND`, `RESPONSES_BACKEND`, `EXCEPCIONES_BACKEND`, `GUIA_ERRORES_BACKEND`, `PERSISTENCIA_BD_BACKEND`, `LOGGING_BACKEND`, `SEGURIDAD_AUTH_BACKEND` y `TESTING_BACKEND`. Si hay conflicto, ganan las rules. Molde: el módulo `planillas` (CU09).

## Decisiones de diseño

- **Módulo nuevo `features/asistencias/`**, prefijo de errores `ASI`. Lee `beca`, `usuario`, `carrera` y `convocatoria` por JDBC (`AsistenciasConsultaRepository`) sin usar repositories de otros módulos.
- **Idempotencia en la base**: índice único parcial `(beca_id, fecha)` y `INSERT … ON CONFLICT DO NOTHING`. Dos peticiones simultáneas no duplican y no hay error que traducir. Si ya existía, responde `200` con `yaRegistrada = true` y la hora del primero; si la crea, `201`.
- **La fecha la fija el servidor** (`America/La_Paz`, `FechasConvocatoria`); el cliente no manda fecha.
- **Búsqueda por `POST` con el CI en el cuerpo**, no `GET ?ci=`: el CI es dato personal y no debe quedar en la URL ni en los registros del proxy.
- **La búsqueda no exige beca vigente**: devuelve también becas suspendidas o dadas de baja para que el personal vea el motivo (A2); `registrar` sí exige `VIGENTE`.
- **Sin bitácora**: la fila ya guarda quién y cuándo, y se escribiría un evento por comida de cada becado. Si la DUBSS lo pide, se agrega `@Auditar` en otra UT.
- Solo `PERSONAL_COMEDOR` (`@PreAuthorize` en el `service`).

## Contrato de datos / recurso HTTP

| Método y ruta | Qué hace | Respuesta |
| --- | --- | --- |
| `POST /api/v1/asistencias/buscar-becado` cuerpo `{ ci }` | Becado por CI | `200`, `data`: `{ becaCodigo, nombres, apellidos, ci, carrera, convocatoria, estadoBeca, fecha, horaRegistro? }` |
| `POST /api/v1/asistencias` cuerpo `{ becaCodigo }` | Registra la asistencia de hoy | `201` creada / `200` ya existía; `data`: `{ codigo, becaCodigo, fecha, horaRegistro, yaRegistrada }` |

Errores: `ERR_ASI_01` a `ERR_ASI_03` ([`docs/errors/asistencias/`](../../../errors/asistencias/ERRORES_ASISTENCIAS.md)), `ERR_SYS_01` (CI vacío o largo), `ERR_AUTH_07` (rol).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU10-BE-01](./UT-CU10-BE-01.md) | Migración y modelo de `asistencia` | — |
| [UT-CU10-BE-02](./UT-CU10-BE-02.md) | Consultas y registro idempotente | BE-01 |
| [UT-CU10-BE-03](./UT-CU10-BE-03.md) | Servicio, controller y errores | BE-02 |
| [UT-CU10-BE-04](./UT-CU10-BE-04.md) | Pruebas | BE-03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 3 a 9 | Buscar y registrar | 02, 03, 04 |
| A1, A2, A4 | CI inexistente, beca no vigente, beca inexistente | 03, 04 |
| A3 | Ya registrada | 02, 03, 04 |
| CA01 a CA10 | Contrato HTTP | 01 a 04 |

## Follow-ups (no se implementan en estas UT)

- Anotar el registro en la bitácora si la DUBSS lo pide.
- Turno (almuerzo) que dibuja el mockup: el diagrama de datos no lo tiene.

## Qué NO incluyen estas UT

- El cotejo (CU-11), el historial del becado (CU-24) y la lectura para BI (CU-12).
- La app móvil (ver `UT-MOBILE`).
