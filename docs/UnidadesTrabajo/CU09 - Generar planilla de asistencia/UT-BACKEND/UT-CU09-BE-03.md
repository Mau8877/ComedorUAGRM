# UT-CU09-BE-03 — PDF de la planilla (JasperReports)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-03` |
| **Nombre** | PDF de la planilla (JasperReports) |
| **Caso de uso** | CU09-BE |
| **Depende de** | — |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear) (un fallo se loguea sin nombres ni CI) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `B/features/actas/service/ActaPdfService.java` y `resources/reports/acta_adjudicacion.jrxml` (formato JRXML de JasperReports 7, elementos con `kind`).

## Solicitud de negocio

Las auditorías exigen una planilla en papel donde cada becado firma y pega la fotocopia de su carnet.

## Objetivo

`PlanillaPdfService.generar(DatosPlanilla)` devuelve los bytes del PDF imprimible.

## Alcance

- `resources/reports/planilla_asistencia.jrxml`: hoja A4 **apaisada**; título, convocatoria, gestión, periodo, cantidad de becados, quién y cuándo; por becado una fila alta con N.º, apellidos y nombres, CI, carrera, un recuadro «Fotocopia de carnet» y **31 columnas de día** que se dibujan solo si el día existe en el periodo (parámetros `D1`..`D31`, vacío = no se dibuja); nota al pie; «Página N de M». Las medidas están pensadas para que entren los 31 días sin salirse de la hoja. Formato y rótulos por definir con la DUBSS.
- `B/features/planillas/service/PlanillaPdfService.java`: recibe los datos ya leídos (`DatosPlanilla`), no toca la base ni MinIO; compila la plantilla una sola vez; zona `America/La_Paz` y locale `es-BO`; `MAX_DIAS = 31`; la etiqueta de cada día es «dd» + salto de línea + inicial del día de la semana; en borrador el título lleva «BORRADOR». Un fallo se loguea con la convocatoria y el tipo de error (nunca nombres ni CI) y se relanza como `IllegalStateException`.
- Tests con JasperReports real: el PDF contiene el periodo, los becados, la convocatoria y quien genera; dibuja una columna por día del periodo y ninguna de más; con 31 días y 120 becados pagina y es apaisado; el borrador está marcado; una plantilla inexistente loguea sin datos personales y lanza `IllegalStateException`.

Archivos esperados (crear): `planilla_asistencia.jrxml`, `PlanillaPdfService.java` y `T/features/planillas/service/PlanillaPdfServiceTest.java`.

## Fuera de alcance (no tocar)

- Leer becas, subir a MinIO o registrar la planilla (BE-04).
- La plantilla del acta ni la lista priorizada.
- Una columna de puntaje, correo o datos socioeconómicos.

## Resultado esperado

Dado un periodo y una lista de becados, sale un PDF legible y listo para imprimir.

## Validación

1. `./mvnw test -Dtest=PlanillaPdfServiceTest` pasa (6 tests).
2. Con `-Dplanilla.pdf.dir=<carpeta>` el test deja los PDF para revisar el diseño a ojo: hoja apaisada, cabecera de días legible y 31 columnas dentro de la hoja.
