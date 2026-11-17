# Unidades de trabajo — CU16-BE

**Caso de uso:** Backend de los documentos de la postulación: listar los documentos que exige la convocatoria con su estado, adjuntar, reemplazar, eliminar y abrir un archivo, con el almacenamiento en MinIO (desarrollo) y Amazon S3 (producción).
**Fuente CU:** [`CU16 - Digitalizar y adjuntar documentos`](../../../CasosDeUso/CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)
**Diagramas de grado:** `images/045` (contexto), `058` (análisis), `128` (secuencia: `DocumentoView` → `DocumentoController` → `ConvocatoriaDocumento`, `DocumentoAdjunto`) y `141` (mockup, no usado en backend); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** `/api/v1/postulaciones/{codigo}/documentos`; exige el rol `POSTULANTE`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `TESTING_BACKEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend/`. Lo de documentos vive en `features/postulaciones/`; el almacenamiento es transversal (`common/storage/`, `config/`).

- **Sin migración.** `documento_adjunto` ya existe (CU07 la creó); este CU solo le agrega Model y repositorio.
- **MinIO y S3 con el mismo código.** El cliente de MinIO habla el protocolo S3, así que no se agrega otro almacenamiento: `MinioConfig` pasa a leer región y tipo de credenciales (`static` o `aws-iam`) de la configuración. Pasar a producción es solo cambiar variables (ver [`ALMACENAMIENTO_S3.md`](../../../ALMACENAMIENTO_S3.md)). El soporte de S3 no se probó contra una cuenta de AWS real.
- **Bucket privado.** Los documentos llevan datos personales: se abren solo con una URL temporal firmada (5 minutos). `create-buckets.sh` ya no deja el bucket de desarrollo con lectura anónima.
- **La postulación ya existe.** Se adjunta solo a una postulación en `borrador` o `devuelta` (con la convocatoria `ABIERTA` y dentro de fechas). Crear el borrador es del CU17 y el envío del CU02 (B9 y B8): para probar se crean borradores de prueba directo en la base.
- **La subida pasa por el backend** (`multipart/form-data`), no directo al almacenamiento: así se valida el contenido antes de guardar y el flujo es igual en MinIO y S3. Límite de Spring: 8 MB (`ERR_SYS_06`); el tamaño del documento lo comprueba el servicio.
- **El formato se decide por el contenido** (los primeros bytes: PDF, JPEG o PNG), no por la extensión ni por el tipo que declara el cliente. Formatos, tamaño (5 MB) y archivos por tipo (5) son valores iniciales **por definir con la DUBSS**, en `app.documentos.*`.
- **Consistencia archivo ↔ fila.** Se sube primero y se registra después; si el registro falla se retira el archivo. Un archivo reemplazado o eliminado se retira **después** de confirmar la transacción. Se bloquea la fila de la postulación (`FOR UPDATE`) para que dos subidas simultáneas no pasen el máximo por tipo.
- **Reemplazar** cambia el archivo en la misma fila (mismo `codigo`, ruta nueva). **Eliminar** es borrado lógico.
- **Ruta del objeto:** `postulaciones/{codigoPostulacion}/documentos/{uuid}.{ext}`; nunca con el nombre que trae el archivo. El nombre original se guarda solo para mostrarlo (sin carpetas ni caracteres de control).
- **La lista es un recurso, no una colección paginada:** `GET .../documentos` devuelve un objeto con los requeridos (los fija la convocatoria y son pocos), no un listado paginado.
- **Sin bitácora** (no es un cambio de estado). Los logs no llevan nombres de archivo ni rutas.

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md). `{codigo}` es el de la postulación.

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `GET` | `/api/v1/postulaciones/{codigo}/documentos` | — | `200` `DocumentosPostulacionResponse` (`puedeEditar`, `requeridos`, `reglas`) | `404` `ERR_DOC_04` |
| `POST` | `.../documentos` | multipart: `tipoDocumento` (código), `archivo` | `201` `DocumentoAdjuntoResponse` | `400` `ERR_DOC_01`/`02`/`03`/`ERR_SYS_05`/`ERR_SYS_06`; `404` `ERR_DOC_04`/`05`; `409` `ERR_DOC_06`/`07`/`08` |
| `PUT` | `.../documentos/{documento}` | multipart: `archivo` | `200` `DocumentoAdjuntoResponse` | los de `POST` y `404` `ERR_DOC_09` |
| `DELETE` | `.../documentos/{documento}` | — | `200` `data: null` | `404` `ERR_DOC_04`/`09`; `409` `ERR_DOC_06`/`07` |
| `GET` | `.../documentos/{documento}/acceso` | — | `200` `AccesoDocumentoResponse` (`url`, `expiraEn`) | `404` `ERR_DOC_04`/`09` |

Sin sesión, `401`; sin el rol, `403` (`ERR_AUTH_07`). Campos: `DocumentoAdjuntoResponse` = `codigo`, `nombreArchivo`, `mimeType`, `tamanoBytes`, `subidoEn`; `DocumentoRequeridoResponse` = `tipoDocumentoCodigo`, `nombre`, `descripcion`, `obligatorio`, `adjuntado`, `archivos`; `ReglasArchivoResponse` = `tamanoMaximoBytes`, `formatosPermitidos`, `maximoArchivosPorTipo`. Nada devuelve ids internos ni la ruta de almacenamiento.

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_DOC_01` | 400 | `ValidationException` | No llegó el archivo o está vacío |
| `ERR_DOC_02` | 400 | `ValidationException` | El archivo supera el tamaño máximo |
| `ERR_DOC_03` | 400 | `ValidationException` | Formato no permitido o contenido que no corresponde |
| `ERR_DOC_04` | 404 | `NotFoundException` | La postulación no existe o es de otra persona |
| `ERR_DOC_05` | 404 | `NotFoundException` | El tipo no figura en la lista de la convocatoria |
| `ERR_DOC_06` | 409 | `ConflictException` | La postulación ya fue enviada |
| `ERR_DOC_07` | 409 | `ConflictException` | La convocatoria ya no está abierta |
| `ERR_DOC_08` | 409 | `ConflictException` | Ya hay el máximo de archivos de ese tipo |
| `ERR_DOC_09` | 404 | `NotFoundException` | El documento no existe o no es de esa postulación |
| `ERR_SYS_06` | 400 | — | La subida supera el límite de Spring |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU16-BE-01](./UT-CU16-BE-01.md) | Almacenamiento listo para S3 (región, credenciales, `eliminar`, bucket privado) | — |
| [UT-CU16-BE-02](./UT-CU16-BE-02.md) | Códigos de error `ERR_DOC_*` y reglas de archivo | — |
| [UT-CU16-BE-03](./UT-CU16-BE-03.md) | Model `DocumentoAdjunto` y consultas | — |
| [UT-CU16-BE-04](./UT-CU16-BE-04.md) | `DocumentosPostulacionService` | 01, 02, 03 |
| [UT-CU16-BE-05](./UT-CU16-BE-05.md) | Controller de documentos | 04 |

**Cadena:** `01, 02, 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 y 8 | Lista de requeridos con su estado | 03, 04, 05 |
| Flujo principal, pasos 4 y 7 | Validar el archivo, guardarlo y registrar el adjunto | 01, 02, 03, 04, 05 |
| A2, CA03, CA04 | Formato o tamaño no permitido | 02, 04 |
| A3, CA06, CA07, CA09, CA10 | Reemplazar y eliminar solo en borrador o devuelta | 04, 05 |
| A4 | Reintentar sin perder los demás documentos | 04 (cada archivo es independiente) |
| A5, CA08 | Más de un archivo por documento, con máximo | 02, 03, 04 |
| A7, CA05, CA12 | Postulación, tipo o documento inexistente o ajeno | 03, 04 |
| A8, CA11 | Convocatoria no abierta | 04 |
| A9, CA13 | Falla el almacenamiento: `500` y sin fila huérfana | 01, 04 |
| A10, CA14 | `401`/`403` | 04, 05 |
| CA01, CA02 | Listar y adjuntar | 04, 05 |
| CA15 | Dirección temporal firmada, bucket privado | 01, 04, 05 |
| A1 | Sin permiso de cámara, elegir de la galería | (móvil) |
| A6 | Faltan obligatorios al enviar | CU-02: usa `GET .../documentos` |

## Follow-ups (no se implementan en estas UT)

- **Web y móvil** (selector de archivos, cámara, recorte, vista previa, lista de adjuntos): ver [`UT-FRONTEND`](../UT-FRONTEND/README.md) y [`UT-MOBILE`](../UT-MOBILE/README.md).
- **CU17 (B9) y CU02 (B8):** crear el borrador y enviar la postulación; el CU02 comprueba los documentos obligatorios con la lista de este CU (A6).
- **Limpieza de archivos huérfanos** si el servidor se cae entre subir el archivo y registrar la fila (hoy se retira al fallar el registro, pero no hay un barrido periódico).
- Configuración del modo `aws-iam` contra una cuenta real y CORS del bucket si algún día se sube directo desde el navegador.

## Qué NO incluyen estas UT

- Pantallas web y móviles, la cámara y el recorte (están en `UT-FRONTEND` y `UT-MOBILE`).
- Crear borradores, enviar postulaciones (CU17, CU02) y devolverlas (CU19).
- Los documentos del expediente que ve el administrador (CU07), que ya existen.
- Migraciones y bitácora.
