# UT-CU13-BE-08 — Crear una convocatoria con sus documentos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-08` |
| **Nombre** | Crear una convocatoria con sus documentos |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-01`, `UT-CU13-BE-02`, `UT-CU13-BE-03`, `UT-CU13-BE-04`, `UT-CU13-BE-05`, `UT-CU13-BE-07` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

El Administrador prepara la convocatoria de la gestión: nombre, fechas, cupos y los documentos que el postulante deberá presentar (flujo principal, pasos 4 a 8).

## Objetivo

`ConvocatoriaService.crear(...)` guarda una convocatoria en `BORRADOR` con su lista de documentos requeridos y la anota en la bitácora.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `CREACION_CONVOCATORIA`.
- `B/features/convocatorias/service/DocumentosConvocatoriaService.java` (nuevo; lo reutiliza la UT 09), con dos métodos:
  - `validar(List<DocumentoRequeridoRequest>)`: devuelve el mapa `TipoDocumento → obligatorio`. Si un tipo se repite, `ValidationException` `ERR_CV_06`; si ninguno es obligatorio, `ValidationException` `ERR_CV_05`; si algún código no existe, `NotFoundException` `ERR_CV_03`.
  - `guardar(Convocatoria, mapa)`: para cada tipo, primero intenta `ConvocatoriaDocumentoRepository.reactivar(...)` y, si devuelve `0`, inserta un `ConvocatoriaDocumento` nuevo. Se agrega acá (y no se repite) por la clave compuesta que incluye filas eliminadas.
- `ConvocatoriaService.crear(UUID actorCodigo, ConvocatoriaRequest)`: `@Transactional`, `@PreAuthorize`, `@Auditar(accion = CREACION_CONVOCATORIA, entidad = "convocatoria")`. Orden: validar fechas (`fechaCierre` posterior a `fechaApertura`, si no `ValidationException` `ERR_CV_04`), buscar la gestión (si no, `NotFoundException` `ERR_CV_02`), validar los documentos, crear la `Convocatoria` en `BORRADOR` con `FechasConvocatoria` y `creadoPor` = id del actor, guardar los documentos. Devuelve `ResultadoAuditado<ConvocatoriaResponse>` con `entidadId` = la convocatoria y `detalle = {gestion: <codigo>, cantidadDocumentos: n}`.
- `INFO` al crear (`module=convocatorias`, `convocatoriaId` = código). Sin nombres ni textos libres en el log.
- No se comprueba «otra abierta» al crear: nace en `BORRADOR`; esa regla se aplica al abrir (`UT-CU13-BE-10`).
- Tests unitarios: crea en `BORRADOR` con `creadoPor` del actor; fechas iguales o invertidas → `ERR_CV_04`; gestión inexistente → `ERR_CV_02`; tipo inexistente → `ERR_CV_03`; tipo repetido → `ERR_CV_06`; ningún obligatorio → `ERR_CV_05`; con `cupos`, `puntajeMinimo` y `cantidadFinalistas` nulos se guarda igual; el `detalle` no lleva el nombre.

Archivos esperados (crear/tocar): `ConvocatoriaService.java`, `DocumentosConvocatoriaService.java`, `BitacoraAcciones.java` y sus tests.

## Fuera de alcance (no tocar)

- Editar, abrir, cerrar y eliminar (UT 09 a 11).
- El endpoint (`UT-CU13-BE-13`).
- Exigir cupos o puntaje mínimo (la DUBSS aún no los define).
- Que la fecha de cierre caiga dentro de la gestión académica: el CU no lo pide.

## Resultado esperado

El Administrador crea una convocatoria en borrador con su lista de documentos y queda anotado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
