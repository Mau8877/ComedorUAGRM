# Unidades de trabajo — CU26-BACKEND

**Caso de uso:** versiones del modelo de preselección (consultar, historial, guardar una nueva) y reprocesamiento en segundo plano de los puntajes de una convocatoria.
**Fuente CU:** [`CU26 - Parametrizar modelo de preselección y reprocesar puntajes.md`](../../../CasosDeUso/CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md)
**Diagramas de grado:** contexto img 050 · análisis img 063 · secuencia img 133 · mockup img 146
**Recurso HTTP / contrato:** `GET/POST /api/v1/modelos-preseleccion`, `GET /api/v1/modelos-preseleccion/vigente`, `GET /api/v1/convocatorias/{codigo}/resumen-reprocesamiento`, `POST /api/v1/convocatorias/{codigo}/reprocesar`, `GET /api/v1/lotes-reprocesamiento/{codigo}`

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md),
[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md),
[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md),
[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y
[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas no sustituyen esas reglas.

## Decisiones de diseño

- **Guardar crea una versión nueva y vigente de inmediato** (`v{M}.{n+1}`) y archiva la anterior; el «Borrador» del mockup es solo la edición en pantalla (A2: sin reprocesar, solo se aplica a lo que se precalifique desde entonces).
- **Seis variables con ponderación** (las del mockup): `ingresos_familiares`, `dependientes`, `procedencia`, `patrimonio`, `estado_vulnerabilidad`, `promedio_academico`; suman 100 (`ERR_MOD_01`). El puntaje mínimo para ser finalista es de la convocatoria (CU13): no se implementa aquí.
- **Actualización:** ahora son **diez variables**: se sumaron `dependencia_economica`, `ocupacion_sosten`, `discapacidad` e `hijos` (del formulario oficial de la DUBSS) y todas suman 100; una versión guardada con seis sigue sirviendo, pero para guardar una nueva hay que completar las diez. Ver el [CU03](../../CU03%20-%20Precalificar%20con%20IA/UT-BACKEND/README.md).
- **Reprocesar** toma las `calificada` de la convocatoria sin puntaje vigente de la versión vigente (idempotente: repetirlo reintenta solo las que fallaron). Bloqueos: A3 `ERR_MOD_04`, A4 `ERR_MOD_03`, sin versión `ERR_MOD_02`, nada por recalcular `ERR_MOD_06`.
- **Ejecución asíncrona** en el ejecutor `ejecutorPrecalificacion`, arrancada tras el `commit` por un evento transaccional; cada postulación en su transacción; IA caída = lote `FALLIDO`; una sola fallida se cuenta y se sigue (A5); los lotes huérfanos pasan a `FALLIDO` al arrancar.
- **La IA no guarda estado:** `SolicitudPuntaje` lleva `modelo` y `parametros`; para reprocesar, la respuesta debe repetir la versión pedida.
- **JDBC en vez de entidades** para `parametro_modelo` y `lote_reprocesamiento` (mismo criterio que `PrecalificacionConsultaRepository`).
- ✅ **Resuelto con la IA (B3):** las ponderaciones suman 100 y la IA escala el resultado a 0–70 de forma lineal (`× 0,7`); es un criterio adoptado que la DUBSS debe validar. Ver el [CU03](../../CU03%20-%20Precalificar%20con%20IA/UT-BACKEND/README.md).
- **Cancelar (A6)** es solo del cliente: antes de confirmar no existe nada en el servidor.

## Contrato de datos / recurso HTTP

| Método y ruta | Entrada | `data` | Estado / errores |
| --- | --- | --- | --- |
| `GET /api/v1/modelos-preseleccion/vigente` | — | `{codigo, version, descripcion, vigente, creadoEn, parametros[{clave, nombre, ponderacion}]}` o `null` | `200`; `403` `ERR_AUTH_07` |
| `GET /api/v1/modelos-preseleccion` | `page`, `pageSize` (sin `search`, `filter` ni `sort`) | `[{codigo, version, descripcion, vigente, creadoEn}]` + `meta` | `200`; `400` `ERR_SYS_01`, `ERR_SYS_05` |
| `POST /api/v1/modelos-preseleccion` | `{descripcion?, parametros[{clave, ponderacion}]}` | la versión nueva | `201`; `400` `ERR_MOD_01`; `409` `ERR_MOD_07` |
| `GET /api/v1/convocatorias/{codigo}/resumen-reprocesamiento` | — | `{convocatoria, nombre, gestion, modelo, calificadas, porReprocesar, resultadosPublicados, loteEnCurso}` | `200`; `404` `ERR_CV_01` |
| `POST /api/v1/convocatorias/{codigo}/reprocesar` | — | el lote `{codigo, convocatoria, modelo, estado, total, procesadas, fallidas, iniciadoEn, terminadoEn}` | `202`; `404` `ERR_CV_01`; `409` `ERR_MOD_02`, `ERR_MOD_03`, `ERR_MOD_04`, `ERR_MOD_06` |
| `GET /api/v1/lotes-reprocesamiento/{codigo}` | — | el lote | `200`; `404` `ERR_MOD_05` |

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU26-BE-01](./UT-CU26-BE-01.md) | Migración, repositorios y pruebas de base real | — |
| [UT-CU26-BE-02](./UT-CU26-BE-02.md) | Versiones del modelo: validar, guardar, consultar | BE-01 |
| [UT-CU26-BE-03](./UT-CU26-BE-03.md) | Recalcular un puntaje y contrato ampliado con la IA | BE-01 |
| [UT-CU26-BE-04](./UT-CU26-BE-04.md) | Reprocesamiento en lote y su avance | BE-01, BE-02, BE-03 |

**Cadena:** `01 → 02, 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Pasos 1–2 | Versión vigente e historial | 02 |
| Pasos 3–5 / A1 / A2 / A9 | Validar y guardar una versión | 02 |
| Pasos 6–8 / A6 | Resumen y confirmación (A6 es del cliente) | 04 |
| Pasos 9–11 / A5 | Lote en segundo plano, avance y fallidas | 03, 04 |
| A3, A4, A7, A8 | Bloqueos del reprocesamiento | 04 |
| A11 | Lotes huérfanos al reiniciar | 04 |
| CA01–CA05 | Versiones y reprocesamiento | 02, 03, 04 |
| CA10 | Solo Administrador | 02, 04 |
| CA11 | Bitácora | 02, 04 |

## Follow-ups (no se implementan en estas UT)

- **B3:** la API del modelo de IA (`POST /api/v1/puntaje`) con los `parametros` recibidos y el puntaje normalizado a 0–70.
- Regla de A3 (advertir o bloquear) y valores y rangos finales de las ponderaciones, con la DUBSS.
- Cancelar un lote en curso (el CU solo pide cancelar antes de confirmar).
- Limpieza de lotes viejos.

## Qué NO incluyen estas UT

- El módulo de IA en Python (B3) ni el cálculo del puntaje.
- El puntaje mínimo para ser finalista (CU13).
- Pantallas: ver [UT-FRONTEND](../UT-FRONTEND/README.md). No hay móvil en este CU.
