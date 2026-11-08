# Cambios pendientes en el documento de taller (a raíz del CU19)

Al redactar [CU19 - Devolver postulación para corrección](CasosDeUso/CU19%20-%20Devolver%20postulación%20para%20corrección.md) quedaron decisiones que la documentación de grado (`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`) todavía no refleja. Esta lista dice **qué cambiar y dónde**. No se tocó nada de esa carpeta. Los cambios del CU07 (puntaje 70 + 30, MinIO, aviso al postulante, lista de finalistas con «Auditoría») están en el otro archivo de cambios pendientes y **no se repiten aquí**.

Decisiones que originan los cambios:

1. La devolución se hace **desde el expediente del finalista** (CU-07); no hay lista aparte de postulaciones ni opción «Postulaciones» en el menú.
2. El **motivo** es obligatorio y de **máximo 500 caracteres** (igual que las observaciones de la auditoría).
3. **Siempre se avisa** al postulante por **bandeja, push y correo**: no hay opción de no notificar.
4. Solo se devuelve una postulación **«calificada»** y con **al menos una auditoría registrada** después de su última devolución (siempre debe haber auditoría); no exige entrega física.
5. **Sin plazo de reenvío** y **sin límite de devoluciones**: el postulante reenvía cuando quiera y al reenviar sigue el flujo normal.
6. Una devolución **no toca** la fila de `finalista`, los documentos, el puntaje ni las auditorías anteriores.

## 1. Diseño lógico de la base de datos

| Dónde | Qué cambiar |
| --- | --- |
| `diseno_base_de_datos.md`, §P2 `postulacion_historial` (~línea 242) | Sin columnas nuevas; aclarar que `motivo` es `text` y su límite de 500 lo valida el servicio al devolver (el CU-20 y el CU-05 usan el mismo campo). Además, el CU-19 es el primero que crea la tabla en la base. |
| `diseno_base_de_datos.md`, §notificaciones (~línea 361) | Sin cambio de columnas; agregar el evento nuevo `POSTULACION_DEVUELTA` donde se listen los eventos de aviso. |

## 2. Requisitos (`secciones/05_capitulo4_ieee830/`)

| Dónde | Qué cambiar |
| --- | --- |
| `03_requisitos_especificos.tex`, RF-14 | Agregar los criterios: el motivo es obligatorio y de hasta 500 caracteres; solo se devuelve una postulación calificada y ya auditada; el postulante recibe el aviso por bandeja, push y correo con las observaciones; se elimina el criterio «dentro de un plazo» (no hay plazo ni límite de devoluciones); la devolución queda en el historial y en la bitácora. |

## 3. Detalle de casos de uso (`secciones/07_capitulo5_requisitos/04_detalle_casos_uso.tex`)

| Caso de uso | Qué cambiar |
| --- | --- |
| **CU-19 Devolver postulación para corrección** (§5.4.19, ~línea 1456) | **Descripción:** decir que se devuelve desde el expediente, que el motivo tiene máximo 500 caracteres y que el aviso sale por bandeja, push y correo; no hay plazo de reenvío. **Proceso:** el paso 4 valida motivo no vacío y de hasta 500, que la postulación siga «calificada» y que exista una auditoría posterior a la última devolución; la **precondición** pide esa auditoría; el «plazo establecido» de la descripción se elimina; el paso 6 registra historial y bitácora en una sola operación. **Flujos alternativos nuevos:** el finalista no existe (`404`), la postulación ya no está «calificada» (`409`, incluye dos administradores a la vez), sin auditoría previa (`409`), la petición repetida por doble clic (la segunda responde `409` por estado inválido; el botón se deshabilita al enviar). **A3** (falla del push): «la notificación queda en su bandeja y el servicio la reintenta»; hoy dice solo «registrada». **A4:** aclarar que el reenvío es del CU-02 y que sigue el mismo flujo del CU-07 (nueva entrega física si hace falta, nueva auditoría, las anteriores se conservan). **Excepciones:** agregar finalista inexistente y conflicto de estado. |
| **CU-04 Consultar estado** (~línea 320, A3) | Aclarar que las observaciones que ve el postulante son el `motivo` del historial de la devolución. |
| **CU-21 Notificar finalistas…** | La lista de finalistas muestra además el estado de la postulación («Devuelta») para que no se intente auditar. *(Decisión propuesta, por revisar.)* |

## 4. Análisis, diseño y maquetas

| Dónde | Qué cambiar |
| --- | --- |
| `images/100_Secuencia_CU19_Devolver_Postulacion_Correccion.jpg` y `images/085_Analisis_CU19_Devolver_Postulacion_Correccion.jpg` | El mensaje `devolverPostulacion(postulacionId, motivo)` pasa a `devolverPostulacion(finalista, motivo)` (se entra por el código del finalista, no de la postulación). El controlador debe leer el finalista y su postulación, y al crear el historial y el aviso agregar los mensajes de **bitácora** (entidad `Bitacora`). Mostrar que la validación de estado «calificada» ocurre antes del `update(estado=devuelta)`. |
| `secciones/08_capitulo6_analisis/02_analisis_casos_uso.tex`, 6.2.19 (~línea 318) | Agregar `Finalista` y `Bitacora` a las entidades que usa `DevolucionController`. |
| `secciones/09_capitulo7_diseno/04_disenar_caso_uso.tex`, 7.4.19 (~línea 299) | Sin texto propio; actualizar solo la figura (imagen 100). |
| `images/113_Mockup_CU19_Devolver_para_correccion.png` | (a) La pantalla de fondo es una lista «Postulaciones / Revisión de expedientes» con un botón «Devolver» por fila: no existe; el diálogo se abre desde el **expediente del finalista** (CU-07), con el botón «Devolver para corrección» que ya figura en la maqueta 107. (b) Quitar el cuadro «Notificar al postulante por push»: siempre se avisa, por bandeja, push y correo. (c) Agregar el contador de 500 caracteres al campo «Motivo». (d) Quitar «Postulaciones» del menú lateral. |
| `images/144` (maqueta del CU-21) | Mostrar la insignia «Devuelta» del estado de la postulación en la lista de finalistas. |
| `images/019_Diagrama_General_CU.jpg` | Sin cambio nuevo: el «extend» CU-19 → CU-07 ya figura entre las relaciones no dibujadas. |

## 5. Archivos de guía del taller

| Dónde | Qué cambiar |
| --- | --- |
| `plan_desarrollo_tareas.md`, tarea A10 (~línea 58) | Agregar: se devuelve desde el expediente del finalista tras una auditoría, el motivo tiene máximo 500 caracteres y siempre se avisa al postulante por bandeja, push y correo. |
| `casos_de_uso_filtrados.md` y `CLAUDE.md` del taller | Sin cambios de numeración ni de relaciones; revisar que la descripción de CU-19 coincida con el detalle nuevo. |

## 6. Reglas de este repositorio que también hay que alinear

| Dónde | Qué cambiar |
| --- | --- |
| `docs/DiagramaConceptual.mermaid` | Ver el punto 1. |
| `docs/errors/README.md` y `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` | Al implementar el CU, documentar `ERR_DEV_01` y `ERR_DEV_02` (sección «Devolución (`DEV`)» en el índice), como exige `GUIA_ERRORES_BACKEND.md`. |

## 7. Pendientes que no dependen de este CU, pero siguen abiertos

- Entrega física del finalista al reenviar y cómo se vuelve a elegir al finalista (CU-05 y CU-02, todavía sin redactar).
