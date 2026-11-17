# UT-CU13-BE-05 — DTOs y `FechasConvocatoria`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-05` |
| **Nombre** | DTOs y `FechasConvocatoria` |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-02`, `UT-CU13-BE-03` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`, sin exponer Models)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- `B/features/usuarios/dto/CrearUsuarioPersonalRequest.java` y `UsuarioPersonalResponse.java` (referencia de estilo)

## Solicitud de negocio

El cliente manda y recibe fechas «de calendario» (sin hora), pero la base guarda un instante. Ese paso se resuelve en un solo lugar para que apertura y cierre siempre signifiquen lo mismo.

## Objetivo

Existen los DTOs del contrato HTTP del CU y la clase que convierte entre la fecha de Bolivia y el instante guardado.

## Alcance

- `B/features/convocatorias/service/FechasConvocatoria.java` (clase `final`, métodos estáticos, zona `America/La_Paz`): `Instant aperturaDesde(LocalDate)` (`00:00:00`), `Instant cierreHasta(LocalDate)` (`23:59:59`) y `LocalDate aFecha(Instant)` (la fecha en La Paz).
- `B/features/convocatorias/dto/` (todos `record`, con Bean Validation y mensajes en español):
  - `GestionAcademicaRequest`: `etiqueta` (`@NotBlank`, `@Size(max = 50)`), `fechaInicio` y `fechaFin` (`@NotNull LocalDate`).
  - `GestionAcademicaResponse` (con `de(GestionAcademica)`), `GestionResumenResponse` (`codigo`, `etiqueta`) y `TipoDocumentoResponse` (`de(TipoDocumento)`).
  - `DocumentoRequeridoRequest`: `tipoDocumentoCodigo` (`@NotNull UUID`) y `obligatorio` (`@NotNull Boolean`).
  - `ConvocatoriaRequest`: `nombre` (`@NotBlank`, `@Size(max = 150)`), `gestionCodigo` (`@NotNull UUID`), `fechaApertura` y `fechaCierre` (`@NotNull LocalDate`), `cupos` (`@Positive Integer`, opcional), `puntajeMinimo` (`@DecimalMin("0")`, `@DecimalMax("100")`, `@Digits(integer = 3, fraction = 2)`, opcional), `cantidadFinalistas` (`@Positive Integer`, opcional) y `documentos` (`@NotEmpty @Valid List<DocumentoRequeridoRequest>`).
  - `DocumentoRequeridoResponse` (`tipoDocumento`, `obligatorio`), `ConvocatoriaResumenResponse` (con `de(Convocatoria)`) y `ConvocatoriaResponse` (con `de(Convocatoria, List<ConvocatoriaDocumento>, boolean tienePostulaciones)`). Los campos son los del [contrato del README](./README.md#contrato-de-datos--recurso-http); las fechas salen como `LocalDate` (`FechasConvocatoria.aFecha`).
- Tests: `FechasConvocatoria` (apertura `00:00` y cierre `23:59:59` en La Paz; ida y vuelta; una fecha cerca de medianoche UTC no se corre de día) y validaciones de los `Request` con `spring-boot-starter-validation-test` (etiqueta vacía, rangos de `puntajeMinimo`, `cupos = 0`, lista de documentos vacía).

Archivos esperados (crear/tocar): `FechasConvocatoria.java`, los 9 DTOs y sus tests.

## Fuera de alcance (no tocar)

- Reglas que dependen de la base o del estado (fechas coherentes entre sí, al menos un documento obligatorio, tipos repetidos): las comprueba cada servicio.
- Exponer `cantidadFinalistas` en ningún DTO que no sea de este CU.
- Controllers y servicios.

## Resultado esperado

El contrato HTTP del CU está fijado en código y las fechas se convierten de una sola forma.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
