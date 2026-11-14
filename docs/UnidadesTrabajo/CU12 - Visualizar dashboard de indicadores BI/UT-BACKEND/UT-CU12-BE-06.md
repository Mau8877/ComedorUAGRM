# UT-CU12-BE-06 — Descarga del dashboard en PDF con Jasper

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-06` |
| **Nombre** | Descarga del dashboard en PDF con Jasper |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-05` |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `UT-CU06-BE-07` y `ListaPriorizadaPdfService` (compilación única de la plantilla, fuentes del contenedor, `PdfGenerado`).

## Solicitud de negocio

El Directivo necesita llevarse el dashboard, tal como lo ve, en un PDF para reuniones y rendición de cuentas.

## Objetivo

El sistema genera un PDF con los indicadores de la selección vigente, con quién y cuándo lo generó.

## Alcance

- **Dependencias:** las de Jasper ya están en el `pom.xml` (CU-06); no se agregan nuevas.
- `R/reports/indicadores_dashboard.jrxml` (JasperReports 7: sin namespace, elementos con `kind`): título «Dashboard de indicadores — Beca Comedor», gestión y convocatoria elegidas («Todas las convocatorias» si no hay), fecha de la última actualización («Aún no calculado» si es nula), los cuatro totales (la asistencia como «Sin datos» mientras sea `null`), una tabla por mes (postulaciones, finalistas, adjudicadas y asistencia) y el pie con la fecha, hora y usuario que lo genera y el número de página. Sin datos personales. Español.
- `B/features/dashboard/service/IndicadoresPdfService.java`:
  - `@PreAuthorize("hasRole('DIRECTIVO')")` y `@Transactional(readOnly = true)`. Sin `@Auditar` (CA09).
  - `PdfGenerado generar(UUID actorCodigo, UUID gestion, UUID convocatoria)`, reutilizando `PdfGenerado(String nombreArchivo, byte[] contenido)` del módulo `postulaciones`; si no conviene importarlo entre módulos, se define uno equivalente en este módulo.
  - Obtiene los datos con la misma lógica de UT-05; gestión o convocatoria inexistente → `ERR_DAS_01`; **`sinDatos` verdadero → `ValidationException` `ERR_DAS_02`** sin generar nada.
  - La plantilla se compila una sola vez y se reutiliza.
  - Nombre del archivo: `indicadores-{etiqueta-de-gestion-sin-espacios}-{yyyyMMdd}.pdf`.
  - Un fallo al generar se loguea como `ERROR` (sin datos personales) y sube como excepción no controlada (`500` `ERR_SYS_00`).
  - El usuario que lo genera sale del `actorCodigo` (nombre para mostrar), nunca de un parámetro libre.
- Tests: el PDF empieza con `%PDF`; respeta la selección (otra gestión da otro contenido); sin datos → `ERR_DAS_02` y no se genera nada; gestión inexistente → `ERR_DAS_01`; un usuario sin rol Directivo recibe `403`; no queda fila en la bitácora; el texto con acentos y «ñ» se ve bien.

Archivos esperados (crear): `indicadores_dashboard.jrxml`, `IndicadoresPdfService` y sus tests.

## Fuera de alcance (no tocar)

- El endpoint (UT-07).
- Gráficos dentro del PDF *(propuesta: solo tablas y totales; los gráficos son del dashboard en pantalla)*.
- Otros formatos o envío por correo.
- El PDF de la lista priorizada (CU-06).

## Resultado esperado

Dada una selección, el `service` devuelve el PDF con sus indicadores y rechaza el caso sin datos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Dentro del contenedor de `core`, el PDF generado se abre y muestra bien los acentos y la «ñ».
