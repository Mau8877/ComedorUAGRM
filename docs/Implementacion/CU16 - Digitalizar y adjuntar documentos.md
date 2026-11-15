# CU16 — Digitalizar y adjuntar documentos

- **Caso de uso:** [CU16](../CasosDeUso/CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu16)
- **Diagramas de grado:** CU img 045 · análisis img 058 · secuencia img 128 · mockup img 141 (§5.4.16, §6.2.16, §7.4.16, §7.5.16)
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementados el backend, la web y la app móvil. **Sin probar en un teléfono real ni contra el servidor y MinIO**: la web y la app se probaron con el cliente HTTP simulado y la cámara con una captura simulada. Revisadas las imágenes 128 y 141; **no se abrieron** la 045, la 058, la 090 ni la 091.

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

## Flujo real implementado (web)

La pantalla es `/mis-postulaciones/{codigo}/documentos` (`DocumentosPostulacionScreen`), que monta el panel reutilizable `DocumentosPostulacionPanel`. Se llega desde el botón «Gestionar documentos» del detalle de una postulación devuelta (`puedeCorregir`).

1. `useDocumentos` → `GET /postulaciones/{codigo}/documentos`: «Adjuntos (X de Y)» y una tarjeta por documento con `Adjuntado` / `Falta` / `Opcional`, sus archivos (nombre, tamaño, fecha) y las reglas.
2. «Adjuntar» o «Agregar otro archivo» (solo con `puedeEditar` y bajo el máximo) abre `SubirDocumentoModal`: el selector de archivos valida formato y tamaño con `crearArchivoDocumentoSchema(reglas)`, muestra una vista previa (imagen, o aviso para un PDF) y, al confirmar, `useAdjuntarDocumento` sube un `FormData` con `POST`.
3. Reemplazar (`PUT`) y eliminar (`DELETE`, con confirmación) usan los mismos hooks; abrir un archivo pide `GET .../acceso` y abre la dirección temporal en otra pestaña.
4. Cada cambio invalida `['postulaciones', 'documentos', codigo]`. Un `404`/`409` de estado (`ERR_DOC_04`/`06`/`07`/`09`) avisa y actualiza la lista; un error de conexión muestra el motivo dentro del modal y conserva el archivo elegido.

## Flujo real implementado (app móvil)

1. `DocumentosPostulacionScreen` (`/mis-postulaciones/:codigo/documentos`) lista los documentos como la web; con `puedeEditar`, «Adjuntar» abre una hoja con «Tomar una foto», «Elegir de la galería» y «Elegir un archivo PDF» (`CapturaDocumentoService`: `image_picker` y `file_picker`). Sin permiso de la cámara (A1) avisa y vuelve a ofrecer la galería o los archivos.
2. El archivo elegido viaja por `extra` a `RevisarDocumentoScreen` (`/mis-postulaciones/:codigo/documentos/revisar`): vista previa, «Recortar o girar» (`image_cropper`, solo imágenes), «Repetir foto» o «Elegir otro», y el motivo local si el archivo no cumple las reglas (botón deshabilitado).
3. «Adjuntar documento» (o «Reemplazar documento») llama a `DocumentosPostulacionAcciones` → `DocumentosPostulacionService` (`multipart/form-data`) y vuelve a la lista con un aviso; si el servidor rechaza el archivo o falla la conexión, se muestra el motivo con «Reintentar» y se conserva el archivo (A4).
4. Si la sesión vence a mitad de la subida, el `ApiClient` renueva y reintenta con una copia del `FormData` (sin ella fallaba con «already been finalized»).

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
| Mockup (141), pantalla 1 | «Capturar documento»: marco guía, «Galería» y «Flash» en una pantalla de cámara propia | La cámara del sistema (`image_picker`); «Galería» y «PDF» salen en una hoja inferior; sin marco guía ni flash propios | No se mantiene una cámara propia (el paquete `camera`, su ciclo de vida y el flash) |
| Mockup (141), pantalla 2 | «Recorte la imagen», «Girar», «Repetir foto» | «Recortar o girar» (un solo botón: el recortador nativo trae el giro) y «Repetir foto» (o «Elegir otro» si no vino de la cámara) | Se usa `image_cropper` |
| Mockup (141), pantalla 2 | Selector «Tipo de documento \*» antes de adjuntar | No hay selector: el tipo ya viene elegido de la tarjeta que se tocó y se muestra como título | Menos pasos y no se puede subir al tipo equivocado |
| Mockup (141), pantalla 2 | Lista «Adjuntos (3 de 4)» con tamaño en MB y estado «Adjuntado» / «Falta» | Igual, más la fecha, el estado «Opcional», y reemplazar y eliminar por archivo; en móvil no se abre el archivo | La lista trae `tamanoBytes`, `adjuntado` y `obligatorio` (CA01, A3) |
| Mockup (141) | Solo se dibuja la app móvil | La web tiene su propia pantalla (selector de archivos con vista previa en un modal, y abrir el archivo en otra pestaña) | El CU es de Móvil y Web |
| Texto de grado (CU-16) | Solo se menciona el «Postulante» y el flujo de captura | Se llega desde «Gestionar documentos» en el detalle de una postulación devuelta; el formulario de postulación (CU02) lo reutilizará | Todavía no existe el formulario del CU02 |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 128 | Sí | Ver especificación abajo | [ ] |
| Análisis | 058 | Sí | Sustituir `DocumentoController` por `DocumentosPostulacionController` y `DocumentosPostulacionService`; agregar `AlmacenamientoService` (MinIO/S3) y `Postulacion` | [ ] |
| Mockup | 141 | Sí | Ver especificación abajo | [ ] |
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

#### Mockup — img 141 (rehacer)

**Pantalla 1 · «Capturar documento» (móvil)** [cambia]: es la cámara del sistema; el marco guía, el botón «Flash» y la pantalla propia **no existen**. En su lugar, al tocar «Adjuntar» sobre un documento sale una **hoja inferior** con tres opciones: «Tomar una foto», «Elegir de la galería» y «Elegir un archivo PDF» [nuevo].

**Pantalla 2 · «Revisar y adjuntar» (móvil)** [cambia]:
- Barra superior con «Revisar y adjuntar» [igual]; título con el nombre del documento (en lugar del selector «Tipo de documento \*») [cambia].
- Vista previa de la imagen (o un icono de PDF con «Vista previa no disponible para PDF») [cambia]; nombre del archivo y tamaño debajo [nuevo].
- Botones «Recortar o girar» (solo imágenes) y «Repetir foto» o «Elegir otro» [cambia: antes «Girar» y «Repetir foto»].
- Aviso de error rojo cuando el archivo no cumple las reglas (formato, tamaño, vacío) o cuando el servidor lo rechaza, con «Reintentar» si fue la conexión [nuevo]; botón «Adjuntar documento» (o «Reemplazar documento») [igual].

**Lista «Documentos» (móvil)** [nuevo]: «Adjuntos (X de Y)» y la nota «Hasta N archivos por documento · formatos, hasta N MB»; una tarjeta por documento con su estado («Adjuntado», «Falta» en rojo, «Opcional»), sus archivos (nombre, tamaño y fecha) con «Reemplazar» y «Eliminar» (confirmación «Eliminar documento»), y «Adjuntar» o «Agregar otro archivo»; si la postulación ya no se puede modificar, el aviso «Esta postulación ya no se puede modificar: solo puedes ver tus documentos» y sin acciones.

**Pantalla web «Documentos de la postulación»** [nuevo]: «Volver a la postulación», «Adjuntos (X de Y)», las mismas tarjetas con «Abrir», «Reemplazar» y «Eliminar» por archivo, y un modal «Adjuntar {documento}» con «Archivo» (selector), vista previa, el mensaje de error y los botones «Cancelar» / «Adjuntar documento».

**Detalle de una postulación devuelta (web y móvil)** [nuevo]: sección «Tus documentos» con el botón «Gestionar documentos».
