# Cambios pendientes en el documento de taller (a raíz del CU07)

Al redactar [CU07 - Auditar expediente finalista](CasosDeUso/CU07%20-%20Auditar%20expediente%20finalista.md) quedaron decisiones del equipo que la documentación de grado (`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`) todavía no refleja. Esta lista dice **qué cambiar y dónde** para que todo quede acorde. No se tocó nada de esa carpeta.

Decisiones que originan los cambios:

1. **Puntaje:** la IA califica solo de **0 a 70** y el auditor escribe a mano de **0 a 30** en el CU-07, sin fórmula. Total máximo 100.
2. **Almacenamiento de archivos: MinIO** (no Firebase Storage). Firebase queda solo para push (FCM).
3. **Observaciones** de la auditoría: máximo **500 caracteres**.
4. **Aviso al postulante** en cada cambio, incluido el resultado de la auditoría.
5. **Postulación devuelta y reenviada** sigue el mismo flujo (entrega física, auditoría).
6. La **lista de finalistas del CU-21** gana la columna «Auditoría» y la acción «Auditar»; no hay lista aparte en el CU-07.
7. Solo se audita con la **entrega física registrada** y la postulación **«calificada»**.

## 1. Diseño lógico de la base de datos

| Dónde | Qué cambiar |
| --- | --- |
| `images/091_Diseno_Logico_Base_de_Datos.jpg` (manda) | `auditoria_expediente`: agregar `puntaje_academico numeric(5,2)` con `CHECK` entre 0 y 30, nula salvo auditoría «Validado»; `observaciones` pasa de `text` a `varchar(500)`. |
| `docs/DiagramaConceptual.mermaid` (este repo) | Mismos dos cambios, en el mismo cambio que la imagen 091. |
| `diseno_base_de_datos.md`, §P4 `auditoria_expediente` | Agregar la columna `puntaje_academico`, el límite de 500 y la nota de que el puntaje total (IA + académico) **no se guarda**, se calcula. |
| `diseno_base_de_datos.md`, §P3 `puntaje_postulacion` (nota sobre Q3, ~línea 327) | Reemplazar «si se recuperara la calificación manual…» por la decisión: la IA aporta hasta 70 y el 30 manual vive en `auditoria_expediente.puntaje_academico`. Cerrar **Q3** en la tabla de preguntas abiertas. |
| `diseno_base_de_datos.md`, `puntaje_postulacion.puntaje_total` (`CHECK` 0–100) | La IA califica solo de 0 a 70: el `CHECK` pasa a `BETWEEN 0 AND 70` (decidido; el compañero que hace el CU-03 lo aplica en su modelo). Ajustar en la imagen 091 y en `DiagramaConceptual.mermaid` también. |
| `diseno_base_de_datos.md`, `documento_adjunto.storage_path` («ruta en Firebase Storage») | Cambiar a «ruta en MinIO». |
| `diseno_base_de_datos.md`, §Archivos y tablas de integraciones | Reemplazar Firebase Storage por MinIO; Firebase solo para `dispositivo_fcm` y push. |

## 2. Requisitos (`secciones/05_capitulo4_ieee830/`)

| Dónde | Qué cambiar |
| --- | --- |
| `03_requisitos_especificos.tex`, RF-04 | Agregar el criterio: el Administrador registra el puntaje académico (0–30) al auditar y el sistema muestra el total con el puntaje de la IA. |
| `02_descripcion_general.tex`, línea ~82 («puntaje por revisión documental de 1 a 70 puntos») | Reescribir: 70 puntos los calcula la IA y 30 son de rendimiento académico registrados en la auditoría. |
| RF-16 y RF de documentos (RF-11) | Donde diga Firebase Storage, MinIO. |
| Requisitos no funcionales / arquitectura que citen Firebase Storage | Cambiar a MinIO. |

## 3. Detalle de casos de uso (`secciones/07_capitulo5_requisitos/04_detalle_casos_uso.tex`)

| Caso de uso | Qué cambiar |
| --- | --- |
| **CU-07 Auditar expediente finalista** | Propósito y descripción: agregar el puntaje académico y el aviso al postulante. Precondición: documentos en MinIO, no en Firebase Storage. Proceso: el finalista se elige desde la lista de finalistas (columna «Auditoría»); el paso 5 registra resultado, observaciones y puntaje académico; el paso 7 avisa al postulante. Flujos alternativos nuevos: entrega física no registrada (`409`), postulación que ya no está «calificada» (`409`), datos inválidos (observaciones > 500, falta el puntaje en «Validado», puntaje fuera de 0–30), petición duplicada (idempotencia). A4: «Firebase Storage» → «almacenamiento». Postcondiciones: puntaje académico guardado y aviso enviado. Excepciones: error al recuperar documentos de MinIO. |
| **CU-05 Auditar y adjudicar becas** | El puntaje que se compara con el mínimo (A3) y que se copia como `puntaje_final` es el **total** (IA + académico de la última auditoría), no solo el de la IA. Aclarar que una postulación devuelta y reenviada vuelve a recorrer finalista → entrega → auditoría. |
| **CU-19 Devolver para corrección** y **CU-02 Enviar postulación** | Aclarar que una postulación devuelta y reenviada sigue el mismo flujo del CU-07 (nueva entrega física si hace falta, nueva auditoría, las anteriores se conservan). |
| **CU-21 Notificar finalistas…** | La pantalla de finalistas muestra además la última auditoría y la acción «Auditar»; el aviso se envía por el servicio de notificaciones. |
| **CU-20, CU-22, CU-05 y todo CU que cambie un estado** | Confirmar que cada uno avisa al postulante (la regla es general). |
| **CU-16 Digitalizar y adjuntar documentos** | Archivos en MinIO. |

## 4. Análisis, diseño y maquetas

| Dónde | Qué cambiar |
| --- | --- |
| `images/094_Secuencia_CU07_Auditar_Expediente_Finalista.jpg` y `images/079_Analisis_CU07_Auditar_Expediente_Finalista.jpg` | `registrarAuditoria(resultado, observaciones)` pasa a incluir `puntajeAcademico`. Agregar los mensajes de bitácora y de aviso al postulante (entidades `Bitacora` y `Notificacion`), y mostrar que el controlador también lee el puntaje vigente de la IA. |
| `secciones/08_capitulo6_analisis/02_analisis_casos_uso.tex`, 6.2.7 | Actualizar el texto con las entidades y mensajes nuevos del diagrama de análisis. |
| `images/107_Mockup_CU07_Auditar_expediente_finalista.png` | Agregar el campo «Puntaje académico (0–30)» y mostrar el puntaje de la IA y el total en solo lectura. Quitar el segundo cuadro «Observaciones…» (está duplicado) y poner contador de 500 caracteres. Las filas de documentos con «Recibido» y «Falta» se mantienen. Revisar que el botón «Devolver para corrección» sea el del CU-19. |
| `images/144` (maqueta del CU-21) | Agregar la columna «Auditoría» y el botón «Auditar» en la lista de finalistas. |
| `images/118`–`120` (despliegue y arquitectura) | Si muestran Firebase Storage, cambiar por MinIO. |

## 5. Archivos de guía del taller

| Dónde | Qué cambiar |
| --- | --- |
| `CLAUDE.md` del taller, §«Puntaje» (línea ~50) | «Revisión documental 1–70 + rendimiento 30» pasa a «IA hasta 70 + rendimiento académico manual hasta 30 en la auditoría». |
| `CLAUDE.md` del taller, §6 («Pendiente: notificación push… dentro de CU-21 y CU-22») | Resolver: todo cambio de estado avisa al postulante por el servicio de notificaciones (CU-EXTRA-01 en este repo). |
| `plan_desarrollo_tareas.md`, tarea A9 (línea ~57: «checklist, puntaje documental 1–70 y académico hasta 30») | Reescribir: checklist de documentos digitales, puntaje académico manual (hasta 30), puntaje de IA solo lectura. |
| `casos_de_uso_filtrados.md` | Sin cambios de numeración; revisar que la descripción de CU-07 coincida con el detalle nuevo. |

## 6. Reglas de este repositorio que también hay que alinear

| Dónde | Qué cambiar |
| --- | --- |
| `.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md` («Archivos: los binarios viven en Firebase Storage») | Cambiar a MinIO. |
| `.claude/rules/ARQUITECTURA_BACKEND.md` (integraciones: «Firebase (FCM para push y Storage para archivos)») | Cambiar a «Firebase solo para FCM; archivos en MinIO». |
| `.claude/rules/backend/LOGGING_BACKEND.md` y `CASOS_DE_USO.md` | Revisar menciones de Firebase Storage. |
| `docs/DiagramaConceptual.mermaid` | Ver el punto 1. |

## 7. Pendientes que no dependen de este CU, pero siguen abiertos

- Relaciones «include» y «extend» del diagrama general (019) que el diagrama no dibuja (CU-07→CU-21, CU-19 y CU-20→CU-07, y otras): ya figuran en el `CLAUDE.md` del taller.
- Pregunta para la DUBSS: plazo de entrega física y qué pasa al vencer. (El puntaje ya está cerrado: IA 0–70, auditor 0–30 escrito a mano, sin fórmula.)
