# UT-CU16-MOB-02 — Modelos, servicio y mapeo de errores de documentos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-02` |
| **Nombre** | Modelos, servicio y mapeo de errores de documentos |
| **Caso de uso** | CU16-MOB |
| **Depende de** | `UT-CU16-MOB-01` |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) y [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita pedir, subir, reemplazar y eliminar documentos y explicar los errores con claridad.

## Objetivo

Existen los modelos, el servicio de la API de documentos, los formatos y la validación local, y el mapeo de errores.

## Alcance

- `M/features/postulaciones/models/archivo_local.dart`, `documentos_postulacion.dart` (`ArchivoAdjunto`, `DocumentoRequerido`, `ReglasArchivo`, `DocumentosPostulacion`) y `revision_documento.dart` (lo que viaja a «Revisar y adjuntar»).
- `M/features/postulaciones/services/documentos_postulacion_service.dart`: `listar`, `adjuntar` y `reemplazar` (`multipart/form-data`) y `eliminar`, con el `Dio` compartido.
- `M/features/postulaciones/formatos_documentos.dart`: `formatearTamano`, `nombresDeFormatos` y `validarArchivoLocal` (feedback inmediato; el backend decide por el contenido).
- `M/features/postulaciones/documentos_error_mapper.dart`: `ERR_DOC_04`/`09` → refrescar; `ERR_DOC_06`/`07` → no editable; conexión → reintentar sin perder los otros documentos; el resto deja pasar el mensaje en español del backend.
- Tests: `test/features/postulaciones/documentos_logica_test.dart` (modelos, formatos, errores y el servicio con un adaptador HTTP simulado).

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Pantallas y proveedores (`UT-CU16-MOB-04` y `05`).
- Agregar métodos a `PostulacionesService` del CU04.

## Resultado esperado

Un servicio listo para usar y errores traducidos a mensajes claros.

## Validación

1. `flutter test test/features/postulaciones/documentos_logica_test.dart` pasa.
2. `flutter analyze` sin problemas.
