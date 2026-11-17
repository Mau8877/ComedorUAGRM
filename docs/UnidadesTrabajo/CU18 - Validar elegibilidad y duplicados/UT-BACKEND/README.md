# Unidades de trabajo — CU18-BE

**Caso de uso:** Backend de la validación de elegibilidad y duplicados que el CU02 ejecuta al enviar una postulación: comprueba que la convocatoria recibe postulaciones, que el postulante es elegible y que no tiene otra postulación vigente en la misma gestión.
**Fuente CU:** [`CU18 - Validar elegibilidad y duplicados`](../../../CasosDeUso/CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md)
**Diagramas de grado:** `images/047` (contexto), `060` (análisis: `PostulacionView` → `ValidacionController` → `Postulante`, `Convocatoria`, `Postulacion`), `130` (secuencia) y `143` (mockup, no se implementa); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** ninguno. Es un servicio interno (`ValidacionPostulacionService.validar`) que usa el CU02 (B8) dentro de su transacción; no agrega rutas.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `LOGGING_BACKEND.md`, `TESTING_BACKEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend/`. Todo vive en `features/postulaciones/`.

- **Sin migración ni endpoint.** Las tablas ya existen (`postulacion` con el índice único parcial `uq_postulacion_postulante_convocatoria`). No hay `ValidacionController`: el actor es el sistema y lo incluye el CU02 (decisión 2 del CU).
- **Solo lectura.** La validación no escribe ni bloquea filas: repetirla da el mismo resultado (A4). La carrera entre dos envíos simultáneos la resuelve el índice único al insertar; traducir esa violación a `ERR_VAL_01` es del CU02.
- **JDBC y no el repository de otro módulo.** Igual que `DevolucionConsultaRepository`: la consulta cruza `usuario`, `postulante`, `convocatoria`, `gestion_academica` y `postulacion`.
- **Qué es duplicado.** Se leen todas las postulaciones del postulante en la gestión de la convocatoria: `borrador` se ignora (es la propia que se envía), `devuelta` de la **misma** convocatoria es el reenvío (`postulacionAReenviar`) y cualquier otra es `ERR_VAL_01`. El mensaje muestra el estado **visible** (`EstadoVisible`), así no se anticipa la decisión final del CU05.
- **Elegibilidad por definir.** Hoy solo cuenta activa y datos de postulante; las condiciones de la DUBSS se agregan en `verificarCondiciones()` con el mismo `ERR_VAL_02`.
- **Seguridad.** `@PreAuthorize("hasRole('POSTULANTE')")`; el usuario sale del token, nunca del body. Sin bitácora (es una consulta); el rechazo se loguea `WARN` sin datos personales.

## Contrato de datos

`ValidacionPostulacionService.validar(UUID usuarioCodigo, UUID convocatoriaCodigo)` devuelve `ResultadoValidacion(postulanteId, convocatoriaId, postulacionAReenviar?)` o lanza:

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_CV_01` | 404 | `NotFoundException` | La convocatoria no existe (código del CU13, reutilizado) |
| `ERR_VAL_01` | 409 | `ConflictException` | Ya hay una postulación vigente en la gestión |
| `ERR_VAL_02` | 403 | `ForbiddenException` | No es elegible (cuenta desactivada o que no es de un postulante) |
| `ERR_VAL_03` | 409 | `ConflictException` | La convocatoria no está `ABIERTA` o está fuera de fechas |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU18-BE-01](./UT-CU18-BE-01.md) | Códigos de error `ERR_VAL_01` a `ERR_VAL_03` y su catálogo | — |
| [UT-CU18-BE-02](./UT-CU18-BE-02.md) | Consultas de la validación (repositorio JDBC) | — |
| [UT-CU18-BE-03](./UT-CU18-BE-03.md) | `ValidacionPostulacionService` y sus pruebas | 01, 02 |

**Cadena:** `01, 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Buscar, comprobar duplicados y elegibilidad, devolver «válida» | 02, 03 |
| A1, CA02, CA05 | Duplicada en la gestión (`409`) | 01, 02, 03 |
| A2, CA06 | No elegible (`403`) | 01, 03 |
| A3, CA03, CA04 | Reenvío de una devuelta; el borrador no es duplicado | 02, 03 |
| A4, CA09 | Idempotencia (solo lectura) | 03 |
| A5, CA10 | Servicio no disponible: falla la base → `500` genérico (sin código nuevo) | 03 |
| A6, A7, CA07, CA08 | Convocatoria inexistente o que no recibe postulaciones | 01, 03 |
| A8, CA12 | `401`/`403` (sesión y cuenta desactivada) | 03 |
| CA01 | «Válida» | 03 |
| CA11 | Envíos simultáneos: lo garantiza el índice único (ver follow-ups) | — |

## Follow-ups (no se implementan en estas UT)

- **CU02 (B8):** llamar a `validar()` en el envío, reenviar la `postulacionAReenviar` en vez de crear otra, y traducir la violación del índice `uq_postulacion_postulante_convocatoria` (CA11) a `ERR_VAL_01`.
- Las condiciones de elegibilidad concretas de la DUBSS y, si se quiere, la pantalla web de resultados del mockup (exige guardar cada resultado; ver decisión 3 del CU).

## Qué NO incluyen estas UT

- Endpoints, pantallas web o móviles (el mensaje «Postulación duplicada» lo arma el CU02).
- Crear, enviar o reenviar postulaciones (CU02, CU17, CU19).
- Migraciones, bitácora y el panel de elegibles/duplicadas/inconsistentes del mockup.
