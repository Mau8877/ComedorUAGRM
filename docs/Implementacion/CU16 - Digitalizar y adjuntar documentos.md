# CU16 — Digitalizar y adjuntar documentos

- **Caso de uso:** [CU16](../CasosDeUso/CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu16)
- **Diagramas de grado:** CU img 045 · análisis img 058 · secuencia img 128 · mockup img 141 (§5.4.16, §6.2.16, §7.4.16, §7.5.16)
- **Estado frente a los diagramas:** Difiere (ver tabla). **Solo está implementado el backend**; las pantallas web y móvil (cámara, recorte, vista previa) siguen pendientes, así que el mockup aún no se pudo contrastar. Revisadas las imágenes 128 y 141; **no se abrieron** la 045, la 058, la 090 ni la 091.

## Flujo real implementado (backend)

Todo exige sesión de Postulante (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol). El postulante sale del token; una postulación ajena responde como inexistente (`404` `ERR_DOC_04`).

**Listar los documentos** (`GET /api/v1/postulaciones/{codigo}/documentos`, `200`)

1. `DocumentosPostulacionController.listar` → `DocumentosPostulacionService.listar()`.
2. `DocumentosPostulacionConsultaRepository.cabecera()` busca la postulación del usuario con el estado de su convocatoria; `requeridos()` trae la lista de `convocatoria_documento` y `adjuntos()` los archivos ya subidos.
3. Devuelve cada requerido con `obligatorio`, `adjuntado` y sus archivos, `puedeEditar` (borrador o devuelta, convocatoria abierta y dentro de fechas) y las `reglas` (tamaño máximo, formatos, archivos por tipo) para que el cliente valide antes de subir.

**Adjuntar** (`POST .../documentos`, multipart `tipoDocumento` + `archivo`, `201`)

1. `DocumentosPostulacionController.adjuntar` lee el archivo (`ArchivoSubido.de`) y llama a `DocumentosPostulacionService.adjuntar()`.
2. Comprobaciones, en este orden: postulación del usuario (`404` `ERR_DOC_04`) → estado borrador o devuelta (`409` `ERR_DOC_06`) → convocatoria `ABIERTA` y sin vencer (`409` `ERR_DOC_07`) → el tipo figura en la lista de la convocatoria (`404` `ERR_DOC_05`) → `ReglasArchivoDocumento.validar()`: vacío (`400` `ERR_DOC_01`), tamaño (`400` `ERR_DOC_02`), formato por el **contenido** (`400` `ERR_DOC_03`).
3. `bloquearPostulacion()` (`FOR UPDATE`) y, si ya hay el máximo de archivos de ese tipo, `409` `ERR_DOC_08`.
4. `AlmacenamientoService.subir()` guarda el objeto en `postulaciones/{postulacion}/documentos/{uuid}.{ext}` y después `DocumentoAdjuntoRepository.saveAndFlush()` registra la fila. Si el registro falla, el archivo recién subido se retira.

**Reemplazar** (`PUT .../documentos/{documento}`, multipart `archivo`, `200`): mismas comprobaciones de estado y de archivo; el documento debe ser de esa postulación (`404` `ERR_DOC_09`). Sube el archivo nuevo, cambia la fila (`DocumentoAdjunto.reemplazarArchivo`, mismo `codigo`) y retira el archivo anterior **después** de confirmar la transacción.

**Eliminar** (`DELETE .../documentos/{documento}`, `200`, `data: null`): borrado lógico de la fila y retiro del archivo tras confirmar. Un tipo sin archivos vuelve a figurar como pendiente.

**Abrir** (`GET .../documentos/{documento}/acceso`, `200`): `AlmacenamientoService.urlTemporal()` firma una dirección de 5 minutos; no consulta el almacenamiento ni devuelve nunca la ruta.

**Almacenamiento:** MinIO en desarrollo y Amazon S3 en producción con el mismo código (`MinioConfig`: región y credenciales `static` o `aws-iam` por variables de entorno; ver [ALMACENAMIENTO_S3.md](../ALMACENAMIENTO_S3.md)). El bucket de desarrollo es privado.

No se anota en la bitácora ni se loguean nombres de archivo ni rutas. Si el almacenamiento falla, el handler global responde `500` genérico y no queda fila sin archivo.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Texto de grado (§5.4.16, RF-11) | Los archivos se guardan en **Firebase Storage** | **MinIO** en desarrollo y **Amazon S3** en producción; la base guarda solo la ruta | El proyecto usa MinIO (CU07); Firebase queda solo para push (decisión 1 del CU) |
| Análisis (058) / Secuencia (128) | `DocumentoController` | `DocumentosPostulacionController` + `DocumentosPostulacionService` | Nombre por recurso y controller delgado |
| Secuencia (128) | `listarRequeridos(convocatoriaId)` | `GET /postulaciones/{codigo}/documentos`: se pide por la **postulación** y devuelve los requeridos **con** su estado | La postulación fija la convocatoria; el cliente necesita pendientes y adjuntados en una sola llamada |
| Secuencia (128) | `list(requeridos)` sobre `ConvocatoriaDocumento` y `list(adjuntos)` sobre `DocumentoAdjunto` | Tres consultas JDBC (`requeridos`, `adjuntos`, `cabecera`) en `DocumentosPostulacionConsultaRepository` | No se leen repositorios de otro módulo |
| Secuencia (128) | `validarFormato(archivo)` | `ReglasArchivoDocumento.validar()`: decide por el contenido (primeros bytes) y no por la extensión ni el tipo declarado; valida también el tamaño | Evitar un `.pdf` falso (CA03) |
| Secuencia (128) | `guardarArchivo(archivo)` y `create(documento)` | `AlmacenamientoService.subir()` y luego `DocumentoAdjuntoRepository.saveAndFlush()`, con retiro del archivo si falla el registro | Que no quede una fila sin archivo (CA13) |
| Secuencia (128) | Sin comprobaciones previas | Antes: postulación del usuario, estado borrador o devuelta, convocatoria abierta, tipo en la lista y máximo de archivos | Precondiciones y reglas del CU (A3, A5, A7, A8) |
| Secuencia (128) | Nota A3: `update(documento)` o `delete(documento)` sin dibujarlos | `reemplazar()` y `eliminar()`, cada uno con su endpoint | A3 del CU |
| Secuencia (128) | No dibuja abrir un documento | `acceso()`: URL temporal firmada de 5 minutos | CA15: los documentos son privados |
| Secuencia (128) | `errorArchivo(motivo)` | `400` con `ERR_DOC_01`/`02`/`03` (y `ERR_SYS_06` si pasa el límite de Spring) | Códigos catalogados |
| Mockup (141) | Captura con cámara, recorte, «Girar», «Repetir foto», selector de tipo y lista «Adjuntos (3 de 4)» | **Aún no implementado**: el backend solo recibe el archivo final; el recorte y el giro serán de la app móvil | Pendiente de las UT de móvil y web |
| Mockup (141) | Lista de adjuntos con tamaño en MB y estado «Adjuntado» / «Falta» | La lista trae `tamanoBytes`, `adjuntado` y `obligatorio` | Datos que necesita la pantalla |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 128 | Sí | Ver especificación abajo | [ ] |
| Análisis | 058 | Sí | Sustituir `DocumentoController` por `DocumentosPostulacionController` y `DocumentosPostulacionService`; agregar `AlmacenamientoService` (MinIO/S3) y `Postulacion` | [ ] |
| Mockup | 141 | Por verificar | Contrastar cuando existan las pantallas web y móvil | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. Revisar `DocumentoAdjunto` (`nombreArchivo`, `mimeType`, `storagePath`, `tamanoBytes`, `subidoEn`) | [ ] |
| Diseño lógico BD | 091 | Por verificar | No se abrió la imagen. Este CU **no agregó migraciones**: solo debe coincidir `documento_adjunto` | [ ] |
| Casos de uso | 045 | Por verificar | No se abrió la imagen. Ahora el CU incluye reemplazar, eliminar y abrir un documento | [ ] |

Arquitectura (118–120): **cambia** — el almacenamiento de archivos es MinIO/S3, no Firebase Storage. Texto de grado: §5.4.16 y el RF-11 deben decir MinIO/S3 en vez de Firebase Storage, y mencionar el máximo de archivos por documento.

### Especificación de cada diagrama afectado

#### Secuencia — img 128 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Postulante · `Boundary` DocumentoView · `Control` DocumentosPostulacionController · `Control` DocumentosPostulacionService · `Entity` ConvocatoriaDocumento · `Entity` DocumentoAdjunto · `Object` AlmacenamientoService (MinIO/S3)

**Bloque 1 — listar los documentos**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → DocumentoView | listarRequeridos() | [igual] |
| 2 | DocumentoView → DocumentosPostulacionController | `GET /api/v1/postulaciones/{codigo}/documentos` → `200` | [cambia] |
| 3 | DocumentosPostulacionController → DocumentosPostulacionService | listar(usuarioCodigo, postulacionCodigo) | [cambia] |
| 4 | DocumentosPostulacionService → ConvocatoriaDocumento | requeridos(convocatoriaId) / requeridos() | [cambia] |
| 5 | DocumentosPostulacionService → DocumentoAdjunto | adjuntos(postulacionId) / adjuntos() | [cambia] |
| 6 | DocumentosPostulacionController → DocumentoView | documentos() | [igual] |
| 7 | DocumentoView → Postulante | mostrarDocumentos(documentos) | [igual] |

**Bloque 2 — adjuntar (camino feliz)**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → DocumentoView | adjuntar(archivo) | [igual] |
| 2 | DocumentoView → DocumentosPostulacionController | `POST /api/v1/postulaciones/{codigo}/documentos` (multipart) → `201` | [cambia] |
| 3 | DocumentosPostulacionController → DocumentosPostulacionService | adjuntar(usuarioCodigo, postulacionCodigo, tipoDocumentoCodigo, archivo) | [cambia] |
| 4 | DocumentosPostulacionService → DocumentosPostulacionService | exigirEditable() y tipoRequerido() | [nuevo] |
| 5 | DocumentosPostulacionService → DocumentosPostulacionService | ReglasArchivoDocumento.validar(archivo) | [cambia: reemplaza validarFormato] |
| 6 | DocumentosPostulacionService → AlmacenamientoService | subir(ruta, contenido, mime) | [cambia: reemplaza guardarArchivo] |
| 7 | DocumentosPostulacionService → DocumentoAdjunto | create(documento) / documento() | [igual] |
| 8 | DocumentosPostulacionController → DocumentoView | documentoAdjuntado() | [igual] |
| 9 | DocumentoView → Postulante | mostrarDocumentoAdjuntado() | [igual] |

**Bloque `alt` — errores de adjuntar:** `[archivo vacío, grande o de formato no permitido]` → `400` `ERR_DOC_01`/`02`/`03` → `errorArchivo(motivo)` · `[postulación ya enviada]` → `409` `ERR_DOC_06` · `[convocatoria no abierta]` → `409` `ERR_DOC_07` · `[tipo fuera de la lista]` → `404` `ERR_DOC_05` · `[máximo de archivos]` → `409` `ERR_DOC_08` · `[falla el registro]` → `AlmacenamientoService.eliminar(ruta)` y error. Todos [nuevo] salvo el de formato [cambia].

**Bloques `opt`:** reemplazar `PUT .../documentos/{documento}` → `reemplazar()` → `subir()` del nuevo, `update(documento)` y `eliminar(rutaAnterior)` tras confirmar [nuevo, A3]; eliminar `DELETE .../documentos/{documento}` → `eliminar()` → `delete(documento)` y `eliminar(ruta)` [nuevo, A3]; abrir `GET .../documentos/{documento}/acceso` → `urlTemporal(ruta, 5 min)` [nuevo].

Notas del diagrama: A1 (sin cámara, elegir de la galería) es de la app móvil; A4 (reintentar sin perder los demás) se cumple porque cada archivo se sube por separado; A5 (varios archivos por documento) con máximo configurable.
