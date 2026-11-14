# UT-CU06-BE-07 — Exportación de la lista a PDF con JasperReports

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-07` |
| **Nombre** | Exportación de la lista a PDF con JasperReports |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-04`, `UT-CU06-BE-06` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia de `@Auditar` con `ResultadoAuditado`: `PerfilService.actualizar`.

## Solicitud de negocio

El administrador necesita llevarse la lista, tal como la ve, en un PDF que pueda imprimir o archivar.

## Objetivo

El sistema genera un PDF con todas las postulaciones de la lista (no solo una página), aplica los filtros pedidos y anota la exportación en la bitácora.

## Alcance

- **Dependencia:** `net.sf.jasperreports:jasperreports`, `jasperreports-pdf`, `jasperreports-jdt` y `jasperreports-metadata` (este último es obligatorio para leer cualquier `.jrxml`), versión 7.1.0 fijada como property `jasperreports.version`. Comprobar que el `maven-compiler-plugin`/`annotationProcessorPaths` sigue compilando.
- `R/reports/lista_priorizada.jrxml` (formato JRXML de JasperReports 7: sin namespace, elementos con `kind`): columnas posición, nombre completo, CI, carrera, puntaje y fecha de cálculo; encabezado con el nombre de la convocatoria, la fecha y hora de generación, quién la generó y los filtros aplicados («Todas las carreras», la carrera elegida y el texto buscado); pie con número de página. Sin datos socioeconómicos, sin encabezado ni firma institucional. Español.
- `B/features/postulaciones/service/ListaPriorizadaPdfService.java` (o un método de `ListaPriorizadaService`, a criterio de quien implemente mientras el `service` no crezca de más):
  - `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, `@Transactional(readOnly = true)` y `@Auditar(accion = EXPORTACION_LISTA_PRIORIZADA, entidad = "convocatoria")`.
  - `ResultadoAuditado<PdfGenerado> exportar(UUID actorCodigo, ExportarListaRequest request)`, con `PdfGenerado(String nombreArchivo, byte[] contenido)`.
  - Convocatoria inexistente → `ERR_PRI_01`. **Lista vacía** con esos filtros → `ValidationException` `ERR_PRI_03`, sin generar nada ni auditar.
  - La plantilla se compila **una vez** (al arrancar o la primera vez) y se reutiliza.
  - **Lee por tramos** (por ejemplo, 1 000 filas por consulta, con la misma consulta de UT-06 y los mismos filtros) mediante un `JRDataSource` propio; **nunca** carga las 20 000 filas de una vez. Usa un `JRVirtualizer` para el documento.
  - Nombre del archivo: `lista-priorizada-{nombre-de-convocatoria-sin-espacios}-{yyyyMMdd}.pdf`.
  - `detalle` de la bitácora: convocatoria, `carreraId`, si hubo `search` (**no** el texto: podría ser un CI) y la cantidad de filas exportadas. Nunca nombres ni CI.
  - Un fallo al generar se loguea como `ERROR` (sin datos de postulantes) y sube como excepción no controlada (`500` `ERR_SYS_00`); no queda fila en la bitácora (se deshace con la transacción).
- **Fuentes en el contenedor:** probar la generación dentro de la imagen de `core`; si la JRE final no trae una fuente que la plantilla use, agregarla al `Dockerfile` (por ejemplo, instalar `fontconfig` y una fuente DejaVu) o usar la fuente por defecto de Jasper empaquetada como extensión. No depender de las fuentes del equipo de desarrollo.
- Tests: unitario del `JRDataSource` por tramos (cantidad de filas y de consultas); con base real, el PDF empieza con `%PDF`, tiene el tamaño esperado de páginas para un conjunto de prueba, respeta el filtro (otro conjunto de filas), la lista vacía da `ERR_PRI_03`, y queda **una** fila de bitácora con acción, actor, convocatoria y filtros **sin** nombres ni CI; una convocatoria inexistente no deja fila.

Archivos esperados (crear): `lista_priorizada.jrxml`, el `service` de exportación, `PdfGenerado` y su `JRDataSource`, y los tests. (Tocar): `pom.xml`, el `Dockerfile` si hace falta, `application.properties` si se configura el virtualizador.

## Fuera de alcance (no tocar)

- El endpoint (UT-08).
- Generar el PDF en segundo plano o enviarlo por correo.
- Otros formatos (Excel, CSV).
- Marca de agua, firma o logos institucionales.

## Resultado esperado

Dado un conjunto de filtros, el `service` devuelve el PDF completo de la lista, deja la exportación en la bitácora y rechaza la lista vacía.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Dentro del contenedor de `core`, un PDF generado desde un servicio de prueba se abre y muestra el texto con acentos y la letra «ñ».
4. La medición con 20 000 filas se hace en UT-09.
