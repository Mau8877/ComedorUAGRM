# UT-CU08-BE-06 — Plantilla y generador del PDF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-06` |
| **Nombre** | Plantilla y generador del PDF |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-04` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca nombres ni CI en los logs) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `ListaPriorizadaPdfService` y `R/reports/lista_priorizada.jrxml` (CU-06): compilación única de la plantilla, zona `America/La_Paz`, locale `es-BO`.

## Solicitud de negocio

El acta es un documento oficial: debe verse igual cada vez, con los datos correctos y sin información que no corresponde.

## Objetivo

Existe un generador que recibe los datos del acta y devuelve el PDF.

## Alcance

- `R/reports/acta_adjudicacion.jrxml`: hoja A4 vertical con
  - encabezado simple (UAGRM, DUBSS, título «Acta de adjudicación» y número del acta);
  - datos de la convocatoria: nombre, gestión, fechas de apertura y cierre, cupos y puntaje mínimo, y la fecha y hora de generación;
  - resumen «Adjudicados: N de M cupos»;
  - tabla de adjudicados con n.º de orden, nombre y apellido, CI, carrera y puntaje final (dos decimales);
  - «Generada por: {administrador}» y dos líneas de firma en blanco rotuladas «Administrador DUBSS» y «Autoridad DUBSS» *(rótulos por definir con la DUBSS)*;
  - número de página.
  Sin adjudicados, la tabla se reemplaza por la leyenda «Sin adjudicados».
- `B/features/actas/service/ActaPdfService.java` (`@Service`), con `record DatosActa` (convocatoria, gestión, fechas, cupos, mínimo, adjudicados, generada por, fecha de generación) y `byte[] generar(DatosActa datos, String numero)`: llena la plantilla (compilada una sola vez, `volatile`) con un `JRBeanCollectionDataSource` de la lista ya cargada y exporta con `JRPdfExporter`. El número «borrador» se imprime tal cual (si es borrador, el título lleva la marca «BORRADOR – sin valor oficial»).
- Un fallo de Jasper se loguea como `ERROR` (convocatoria y tipo de error, nunca nombres ni CI) y se relanza como `IllegalStateException`, que el handler global responde como `500` genérico.
- Tests: el PDF empieza por `%PDF`; con cinco adjudicados el texto extraído contiene el número, el nombre de la convocatoria y cada nombre y CI en el orden recibido; con cero adjudicados contiene «Sin adjudicados»; el borrador contiene «BORRADOR»; no aparece ningún dato fuera de lo previsto (por ejemplo, una columna de correo); con cientos de filas pagina sin fallar; un fallo simulado de la plantilla produce la excepción esperada sin datos personales en el log.

Archivos esperados (crear): `acta_adjudicacion.jrxml`, `ActaPdfService` y su test.

## Fuera de alcance (no tocar)

- Leer datos de la base, asignar el número o guardar en MinIO (UT-04, 05 y 07).
- El encabezado oficial definitivo, el escudo o las firmas reales: quedan por definir con la DUBSS.
- Firma digital del PDF.

## Resultado esperado

Con los datos del acta se obtiene un PDF correcto, también el borrador y el acta sin adjudicados.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Abrir el PDF generado por el test (se puede guardar en una carpeta temporal) y comprobar a ojo el diseño.
