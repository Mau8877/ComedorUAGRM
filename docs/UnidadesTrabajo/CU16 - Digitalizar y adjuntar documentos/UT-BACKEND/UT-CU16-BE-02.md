# UT-CU16-BE-02 — Códigos de error `ERR_DOC_*` y reglas de archivo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-02` |
| **Nombre** | Códigos de error `ERR_DOC_*` y reglas de archivo |
| **Caso de uso** | CU16-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Un archivo se acepta o se rechaza con un motivo claro, y el formato no se puede falsear cambiando la extensión.

## Objetivo

Existen `ERR_DOC_01` a `ERR_DOC_09`, documentados, y `ReglasArchivoDocumento` valida tamaño y formato por el contenido.

## Alcance

- `B/features/postulaciones/exception/DocumentoPostulacionErrorCodes.java`: las nueve constantes.
- `B/features/postulaciones/service/ArchivoSubido.java`: `record(nombre, contentType, contenido)` con `de(MultipartFile)`.
- `B/features/postulaciones/service/ReglasArchivoDocumento.java`: `validar(ArchivoSubido)` → `Formato(mime, extension)`; vacío `ERR_DOC_01`, grande `ERR_DOC_02`, formato desconocido o no permitido o contenido que no coincide con el tipo declarado `ERR_DOC_03`. Reconoce PDF, JPEG y PNG por sus primeros bytes. Valores de `app.documentos.*`.
- `docs/errors/`: las nueve secciones, `ERR_SYS_06` y los enlaces del índice.
- Tests: `T/features/postulaciones/service/ReglasArchivoDocumentoTest.java`.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Guardar el archivo o tocar la base (`UT-CU16-BE-04`).
- Definir los formatos y el tamaño finales: son de la DUBSS, aquí solo valores iniciales configurables.

## Resultado esperado

Un `.pdf` que no es un PDF se rechaza; un archivo bueno devuelve su formato real.

## Validación

1. `./mvnw test -Dtest=ReglasArchivoDocumentoTest` pasa.
2. Cada código del `.java` tiene su sección y su enlace en `docs/errors/README.md`.
3. `./mvnw checkstyle:check` sin errores.
