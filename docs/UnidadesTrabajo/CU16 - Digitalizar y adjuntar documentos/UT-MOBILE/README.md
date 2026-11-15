# Unidades de trabajo — CU16-MOB

**Caso de uso:** App móvil de los documentos de la postulación: ver los documentos que exige la convocatoria con su estado, capturar un documento con la cámara (o elegirlo de la galería o de los archivos), recortarlo o girarlo, revisarlo y adjuntarlo; reemplazar y eliminar mientras la postulación se pueda modificar.
**Fuente CU:** [`CU16 - Digitalizar y adjuntar documentos`](../../../CasosDeUso/CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)
**Diagramas de grado:** `images/058` (análisis), `128` (secuencia: `DocumentoView`) y `141` (mockup: «Capturar documento» y «Revisar y adjuntar»); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** consume `/api/v1/postulaciones/{codigo}/documentos` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md`, `TESTING_MOBILE.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`M/` = `projects/mobile/lib/`. Todo vive en `M/features/postulaciones/` (el mismo módulo del CU04).

- **Cámara del sistema, no una pantalla de cámara propia.** El mockup dibuja una pantalla «Capturar documento» con un marco guía, «Galería» y «Flash». Se usa `image_picker`, que abre la cámara del teléfono y se encarga del permiso: así no se mantiene una cámara propia (el paquete `camera`, el ciclo de vida, el flash). _Diferencia con el mockup._
- **Recorte y giro con `image_cropper`** (la interfaz nativa de recorte trae el giro): el botón «Recortar o girar» solo aparece con imágenes. «Repetir foto» vuelve a la misma fuente (cámara, galería o archivos).
- **Tres fuentes** (hoja inferior): «Tomar una foto», «Elegir de la galería» y «Elegir un archivo PDF» (`file_picker`). Sin permiso de la cámara (A1) se avisa y se vuelve a ofrecer la galería o los archivos.
- **Las fotos se reducen al elegirlas** (ancho máximo 2200 px, calidad JPEG 85) para que pesen menos que el máximo permitido. El tamaño y el formato se validan en la app con las `reglas` que manda el backend; el backend vuelve a validar por el contenido.
- **El tipo de documento ya viene elegido.** En el mockup «Revisar y adjuntar» tiene un selector de tipo; aquí se parte de la tarjeta del documento que la persona tocó (menos pasos y sin posibilidad de subir al tipo equivocado). Se muestra como título.
- **Dos servicios y no uno:** `DocumentosPostulacionService` (API) es nuevo y no se agregan métodos a `PostulacionesService` (CU04, solo lectura). `CapturaDocumentoService` envuelve los plugins para poder sustituirlos en los tests (no corren en `flutter test`).
- **Reintento de subidas.** Si la sesión vence a mitad de una subida, el `ApiClient` renueva y reintenta, pero un `FormData` ya enviado no se puede reutilizar: se guarda una copia sin enviar y el reintento la usa (`api_client.dart`). Sin esto la subida fallaba con «The FormData has already been finalized».
- **No se abre el archivo en la app** (el mockup no lo muestra): ver un archivo ya subido es de la web.
- **Rutas dentro de `GoRouter`:** `/mis-postulaciones/:codigo/documentos` (lista) y `/mis-postulaciones/:codigo/documentos/revisar` (recibe el archivo por `extra`; sin él vuelve a la lista). Se llega desde el botón «Gestionar documentos» del detalle de una postulación devuelta.
- **Permisos nativos.** Android: `UCropActivity` en el manifiesto. iOS: `NSCameraUsageDescription` y `NSPhotoLibraryUsageDescription`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU16-MOB-01](./UT-CU16-MOB-01.md) | Dependencias, permisos nativos y reintento de subidas en `ApiClient` | — |
| [UT-CU16-MOB-02](./UT-CU16-MOB-02.md) | Modelos, servicio y mapeo de errores de documentos | 01 |
| [UT-CU16-MOB-03](./UT-CU16-MOB-03.md) | Servicio de captura (cámara, galería, PDF y recorte) | 01 |
| [UT-CU16-MOB-04](./UT-CU16-MOB-04.md) | Proveedores y pantalla de documentos | 02, 03 |
| [UT-CU16-MOB-05](./UT-CU16-MOB-05.md) | Pantalla «Revisar y adjuntar», rutas y acceso desde el detalle | 04 |

**Cadena:** `01 → 02, 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 y 8 | Lista de documentos con «Adjuntado» / «Falta» / «Opcional» | 02, 04 |
| Flujo principal, pasos 3 a 7 | Capturar o elegir, validar, vista previa, confirmar y subir | 02, 03, 05 |
| A1 | Sin permiso de cámara: galería o archivos | 03, 04 |
| A2, CA03, CA04 | Formato o tamaño no permitido: motivo y no se sube | 02, 05 |
| A3, CA06, CA07, CA09, CA10 | Reemplazar y eliminar solo si `puedeEditar` | 04, 05 |
| A4 | Reintentar sin perder los demás documentos (incluida la sesión vencida) | 01, 02, 05 |
| A5, CA08 | Varios archivos por documento, con el máximo | 04 |
| A7, A8, CA11, CA12 | Postulación inexistente, ya enviada o convocatoria cerrada: aviso y vuelta a la lista | 02, 05 |
| CA01, CA02 | Listar y adjuntar | 02, 04, 05 |

## Follow-ups (no se implementan en estas UT)

- **Prueba en un teléfono real:** la cámara, la galería, el selector de PDF y el recortador son plugins nativos que no corren en `flutter test`; los tests usan una captura simulada. Hay que probar el flujo completo en Android e iOS.
- **Compilar la app Android/iOS** con las dependencias nuevas (`image_picker`, `image_cropper`, `file_picker`): aquí solo se corrió `flutter analyze` y `flutter test`.
- **CU02 (B8) y CU17 (B9):** integrar la pantalla de documentos en el formulario de postulación y bloquear el envío si faltan obligatorios (A6).
- Pantalla de cámara propia con marco guía y flash (mockup img 141, pantalla 1), si la DUBSS la quiere.

## Qué NO incluyen estas UT

- La web (ver `UT-FRONTEND`) y el backend (ver `UT-BACKEND`).
- El formulario de postulación (CU02) y el borrador (CU17).
- Abrir o descargar un archivo ya subido.
