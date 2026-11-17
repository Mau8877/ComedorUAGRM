# Caso de uso

| Id   | Nombre                          | Módulo                         | Prioridad | Estado      |
| ---- | ------------------------------- | ------------------------------ | --------- | ----------- |
| CU16 | Digitalizar y adjuntar documentos | P2. Convocatoria y postulación | Alta      | En progreso |

**Actor principal:** Postulante
**Actores secundarios:** —
**Objetivo:** Capturar con la cámara del teléfono, o cargar desde un archivo en la web, cada documento que exige la convocatoria y asociarlo a su postulación como respaldo de los datos declarados, sin presentar carpetas físicas.
**Plataforma:** Móvil y Web
**RF asociados:** RF-11

**Descripción:**
El Postulante que completa una postulación (o un borrador) ve la lista de documentos que exige la convocatoria, con los que ya adjuntó y los que le faltan. Elige uno, lo captura con la cámara (móvil) o lo selecciona desde su equipo (web), el sistema valida su formato y tamaño, le muestra una vista previa y, al confirmar, lo guarda y lo asocia a la postulación. Puede reemplazar o eliminar un documento ya adjuntado mientras la postulación no se haya enviado o esté devuelta ([CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)), y adjuntar más de un archivo o página para un mismo documento. Si el formato o el tamaño no sirven, el sistema rechaza el archivo e informa el motivo; si la carga falla por la conexión, se puede reintentar sin perder los demás documentos.

Este CU es incluido por el [CU-02](CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md) y también se usa al completar un borrador ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md)). Depende de [CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md), que define la lista de documentos requeridos de cada convocatoria.

> **Fuentes consultadas** (documentación de grado): `images/045` (CU), `058` (análisis), `128` (secuencia), `141` (mockup) y el detalle del CU-16 y el RF-11 en los capítulos 4, 5.4 y 7. Lo marcado como _(propuesta)_ no figura en ninguna de ellas y debe confirmarse. Los puntos en que las fuentes no coinciden están en [Decisiones tomadas y puntos abiertos](#decisiones-tomadas-y-puntos-abiertos).

---

## Precondiciones

- El Postulante tiene sesión iniciada ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)).
- Tiene una postulación **en borrador o devuelta** a una convocatoria `ABIERTA` ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md), [CU-19](CU19%20-%20Devolver%20postulación%20para%20corrección.md)).
- La convocatoria tiene definida la lista de documentos requeridos ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)).
- En la app móvil, el Postulante otorgó el permiso de la cámara (si no, ver A1).

## Flujo principal

1. El sistema muestra la lista de documentos requeridos de la convocatoria, indicando cuáles están pendientes y cuáles ya se adjuntaron.
2. El Postulante selecciona el documento que quiere adjuntar.
3. En la app móvil captura el documento con la cámara; en la web selecciona un archivo de su equipo.
4. El sistema valida el formato y el tamaño del archivo.
5. El sistema muestra una vista previa del documento.
6. El Postulante confirma el documento, o lo reemplaza, lo vuelve a capturar o lo elimina.
7. El sistema guarda el archivo en el almacenamiento y registra el documento adjunto (tipo y archivo) asociado a la postulación.
8. El sistema actualiza la lista de documentos con el estado «adjuntado».

## Flujos alternativos y excepciones

Los códigos A1 a A6 son los de las fuentes de grado; A7 en adelante son de este documento.

- **A1 (paso 3):** si el Postulante no otorga el permiso de la cámara, el sistema le ofrece elegir el documento desde la galería o los archivos del dispositivo.
- **A2 (paso 4):** formato no permitido o tamaño excedido → el sistema rechaza el archivo e informa el motivo (`400`).
- **A3 (paso 6):** reemplazar o eliminar un documento ya adjuntado, solo si la postulación no fue enviada o está devuelta (si no, `409`).
- **A4 (paso 7):** si falla la carga por la conexión, el Postulante puede reintentar sin perder los demás documentos ya adjuntados.
- **A5 (paso 3):** un mismo documento admite más de un archivo o página, hasta un máximo por tipo de documento (`409` al pasarlo).
- **A6 (general):** si al enviar faltan documentos obligatorios, el CU-02 no permite el envío e indica los faltantes (lo comprueba el CU-02 con la lista que este CU expone).
- **A7:** la postulación o el tipo de documento no existe, no es del Postulante, o el tipo no figura en la lista de la convocatoria → `404`.
- **A8:** la convocatoria ya no está `ABIERTA` → `409`.
- **A9:** falla el almacenamiento (el servicio de archivos no responde) → error genérico `500`; no queda ningún documento registrado sin su archivo.
- **A10:** no es Postulante → `403`; sin sesión o con sesión vencida → `401`.

## Reglas de negocio

- **Un archivo por documento adjunto:** cada archivo cargado es una fila de `documento_adjunto` con su tipo de documento; un mismo tipo admite varios (A5).
- **Formatos y tamaño: por definir con la DUBSS.** _(Propuesta de valores iniciales, configurables: PDF, JPEG y PNG, hasta 5 MB por archivo y hasta 5 archivos por tipo de documento.)_ No se confía en la extensión ni en el tipo que declara el cliente: se comprueba el contenido real del archivo.
- **Solo documentos de la lista de la convocatoria:** el tipo de documento debe figurar en `convocatoria_documento` de la convocatoria de la postulación.
- **Quién y cuándo:** solo el dueño de la postulación (el postulante sale del token), solo con la postulación en `borrador` o `devuelta` y la convocatoria `ABIERTA`.
- **Almacenamiento:** los binarios no viven en la base: la tabla guarda solo la ruta, el tipo MIME y el tamaño ([PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)). **En desarrollo, MinIO; en producción, Amazon S3**, cambiando solo la configuración (ver decisión 1). La ruta se arma con códigos públicos, nunca con el nombre que trae el archivo.
- **Los documentos son privados:** un archivo se abre solo con una dirección temporal firmada, nunca con una ruta pública.
- **Borrado:** eliminar o reemplazar un documento es un borrado lógico de la fila ([PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)); el archivo del almacenamiento se retira solo si la fila se eliminó.
- **Sin datos personales en logs ni en la bitácora:** no se registran nombres de archivo ni rutas.

## Postcondiciones

- El documento queda guardado en el almacenamiento y vinculado a la postulación.
- La lista de documentos refleja cuáles están adjuntados y cuáles faltan.

## Criterios de aceptación

- **CA01:** Dado que tengo una postulación en borrador y la convocatoria está abierta, cuando consulto los documentos, el sistema debe mostrar los requeridos con su estado (pendiente o adjuntado) y cuántos archivos tiene cada uno.
- **CA02:** Dado que adjunto un archivo válido de un tipo de la lista, el sistema debe guardarlo, registrarlo asociado a mi postulación y mostrarlo como adjuntado.
- **CA03:** Dado que el formato no está permitido, o el contenido no corresponde al formato declarado, el sistema debe rechazar el archivo con `400` y no guardar nada.
- **CA04:** Dado que el archivo excede el tamaño máximo, el sistema debe rechazarlo con `400` y no guardar nada.
- **CA05:** Dado que el tipo de documento no está en la lista de la convocatoria, el sistema debe responder `404`.
- **CA06:** Dado que la postulación ya fue enviada (no está devuelta), cuando intento adjuntar, reemplazar o eliminar, el sistema debe responder `409`.
- **CA07:** Dado que la postulación está devuelta, el sistema debe permitir adjuntar, reemplazar y eliminar.
- **CA08:** Dado que ya hay el máximo de archivos de ese tipo, el sistema debe responder `409`.
- **CA09:** Dado que reemplazo un documento, el sistema debe dejar el nuevo archivo y retirar el anterior, sin tocar los demás documentos.
- **CA10:** Dado que elimino un documento, el sistema debe quitarlo de la lista (borrado lógico) y volver a mostrar el tipo como pendiente si no quedan archivos.
- **CA11:** Dado que la convocatoria ya no está abierta, el sistema debe responder `409`.
- **CA12:** Dado que la postulación es de otra persona, el sistema debe responder `404` (igual que una inexistente).
- **CA13:** Dado que falla el almacenamiento, el sistema debe responder `500` genérico y no dejar una fila sin archivo.
- **CA14:** Dado que no soy Postulante, el sistema debe responder `403`; sin sesión, `401`.
- **CA15:** Dado que quiero abrir un documento adjunto, el sistema debe darme una dirección temporal firmada y nunca una ruta pública permanente.

## Decisiones tomadas y puntos abiertos

Decidida con el Desarrollador:

1. **Almacenamiento: MinIO en desarrollo y Amazon S3 en producción.** Las fuentes de grado dicen «Firebase Storage»; el proyecto ya usa MinIO (CU07) y el Desarrollador pidió dejarlo listo para S3. _Diferencia con el CU de grado, que se registra en `docs/Implementacion/`._ Se resuelve con configuración (endpoint, región, credenciales, bucket), no con código distinto por entorno.

Propuestas, pendientes de confirmar con el Desarrollador:

2. **La postulación ya debe existir.** `documento_adjunto.postulacion_id` es obligatorio, así que este CU solo adjunta a una postulación en `borrador` o `devuelta`; crear el borrador es del CU-17 y el envío del CU-02 (B9 y B8, que vienen después). Para probar este CU se crean los borradores de prueba directo en la base.
3. **Subida a través del backend** (multipart), no directo del cliente al almacenamiento con una URL firmada: así el sistema valida el contenido real del archivo antes de guardarlo, y el mismo flujo sirve para MinIO y S3.
4. **El bucket de desarrollo pasa a ser privado.** Hoy `infrastructure/minio/create-buckets.sh` lo deja con lectura pública anónima; los documentos llevan datos personales (carnet, ingresos), así que se quita y se abre todo con URL temporal (como ya hace el CU07).
5. **Recorte, giro y repetir foto (mockup)** son de la app móvil, antes de subir: el backend solo recibe el archivo final.

Abiertos, por definir con la DUBSS (no se inventan; se dejan configurables):

- Los **formatos permitidos**, el **tamaño máximo** y la **cantidad máxima de archivos por documento** (los valores del apartado de reglas son una propuesta inicial).

## Desarrollador

Leonel Barrios
