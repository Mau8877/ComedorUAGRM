# Unidades de trabajo — CU16-FE

**Caso de uso:** Pantalla web de los documentos de la postulación: ver los documentos que exige la convocatoria con su estado, adjuntar un archivo (con validación y vista previa), reemplazarlo, eliminarlo y abrirlo.
**Fuente CU:** [`CU16 - Digitalizar y adjuntar documentos`](../../../CasosDeUso/CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md)
**Diagramas de grado:** `images/058` (análisis), `128` (secuencia: `DocumentoView`) y `141` (mockup: es de la app móvil; la web usa el mismo flujo con un selector de archivos en vez de la cámara); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** consume `/api/v1/postulaciones/{codigo}/documentos` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `TESTING_FRONTEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`F/` = `projects/frontend/comedoruagrm/src/`. Todo vive en `F/features/postulaciones/` (es el mismo módulo del backend y de CU04).

- **Dónde se entra.** La pantalla es `/mis-postulaciones/{codigo}/documentos` (hermana del detalle). Mientras no exista el formulario del CU02/CU17, el único acceso es el botón «Gestionar documentos» del detalle de una postulación **devuelta** (`puedeCorregir`). El panel (`DocumentosPostulacionPanel`) es reutilizable: el CU02 lo incrusta en su formulario sin cambios.
- **Flujo igual al de la app móvil**, con un selector de archivos en vez de la cámara: elegir el documento → elegir el archivo → validación inmediata → vista previa → confirmar. El recorte y el giro (mockup) son de la app móvil.
- **Validación en el cliente solo para dar feedback.** El esquema Zod (`crearArchivoDocumentoSchema`) se arma con las `reglas` que manda el backend (tamaño, formatos, archivos por documento), así los valores de la DUBSS viven en un solo lugar. El backend vuelve a validar por el **contenido** del archivo y es el que decide.
- **La lista manda.** `puedeEditar` lo calcula el backend (borrador o devuelta, convocatoria abierta): el cliente no recalcula el estado. Tras subir, reemplazar o eliminar se invalida `['postulaciones', 'documentos', codigo]`; un `404`/`409` de estado también refresca la lista.
- **El archivo no pierde lo elegido** si el servidor falla: el motivo se muestra dentro del modal y se puede reintentar sin perder los demás documentos (A4).
- **Abrir un archivo** pide la dirección temporal firmada y la abre en otra pestaña (`noopener`); la aplicación no guarda ni muestra rutas de almacenamiento.
- **Sin estado global:** todo es TanStack Query más estado local del modal.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU16-FE-01](./UT-CU16-FE-01.md) | Tipos, API, claves, hooks y mapeo de errores de documentos | — |
| [UT-CU16-FE-02](./UT-CU16-FE-02.md) | Esquema de validación del archivo | 01 |
| [UT-CU16-FE-03](./UT-CU16-FE-03.md) | Panel de documentos y modal de subida | 01, 02 |
| [UT-CU16-FE-04](./UT-CU16-FE-04.md) | Pantalla, ruta y acceso desde el detalle | 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 y 8 | Lista de documentos con «Adjuntado» / «Falta» / «Opcional» | 01, 03 |
| Flujo principal, pasos 3 a 7 | Elegir el archivo, validar, vista previa, confirmar y subir | 02, 03 |
| A2, CA03, CA04 | Formato o tamaño no permitido: mensaje y no se sube | 02, 03 |
| A3, CA06, CA07, CA09, CA10 | Reemplazar y eliminar solo si `puedeEditar` | 03 |
| A4 | Reintentar sin perder los demás documentos | 01, 03 |
| A5, CA08 | Varios archivos por documento, con el máximo | 03 |
| A7, A8, CA11, CA12 | Postulación inexistente, ya enviada o convocatoria cerrada: aviso y lista actualizada | 01, 03 |
| CA01, CA02 | Listar y adjuntar | 01, 03, 04 |
| CA15 | Abrir con dirección temporal | 01, 03 |
| Acceso | Botón «Gestionar documentos» en una postulación devuelta | 04 |

## Follow-ups (no se implementan en estas UT)

- **CU02 (B8) y CU17 (B9):** incrustar `DocumentosPostulacionPanel` en el formulario de postulación y bloquear el envío si faltan documentos obligatorios (A6).
- **Subida con barra de progreso** y recorte de imagen en la web: no están en el CU (el recorte es de la app móvil).
- **Prueba manual** contra el backend y MinIO: el panel y el modal se probaron con el cliente HTTP simulado, no contra el servidor real.

## Qué NO incluyen estas UT

- La app móvil (cámara, recorte): ver `UT-MOBILE`.
- El formulario de postulación (CU02) y el borrador (CU17).
- Los documentos del expediente que ve el administrador (CU07).
