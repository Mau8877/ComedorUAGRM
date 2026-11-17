# Unidades de trabajo — CU02-BE

**Caso de uso:** Backend del envío de la postulación: iniciar «Nueva postulación», enviar con todas las validaciones del CU02, guardar las respuestas socioeconómicas y pedir la precalificación a la IA.
**Fuente CU:** [`CU02 - Enviar postulación a la Beca Comedor`](../../../CasosDeUso/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)
**Diagramas de grado:** `images/039` (contexto), `052` (análisis), `122` (secuencia) y `135` (mockup); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** `POST /api/v1/postulaciones` (iniciar) y `POST /api/v1/postulaciones/{codigo}/enviar` (enviar); más la llamada saliente `POST {IA}/api/v1/puntaje`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `RESPONSES_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `LOGGING_BACKEND.md`, `TESTING_BACKEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`. Todo vive en `features/postulaciones/` salvo la precalificación, en `features/preseleccion/`.

1. **«Nueva postulación» crea el `borrador`.** Los documentos del CU16 solo se adjuntan a una postulación que ya existe (`borrador` o `devuelta`), así que el CU02 la crea al iniciar y el envío la pasa a `pendiente`. Es idempotente: repetir el inicio devuelve la misma. Un borrador vacío puede quedar sin enviar; no cuenta como duplicado (CU18).
2. **Envío idempotente.** Con la fila bloqueada (`FOR UPDATE`), una postulación ya `pendiente` o `calificada` devuelve su comprobante en vez de fallar (A6).
3. **`respuesta_socioeconomica` con `UPSERT` JDBC**, no un Model JPA: es una tabla PK = FK y el CU17 solo necesitará el mismo `UPSERT` con campos parciales. Las columnas son nullables a propósito.
4. **Los campos obligatorios se validan en el servicio**, no con Bean Validation, para devolver *qué* falta (`ERR_ENV_04`) y no el `ERR_SYS_01` genérico (A3).
5. **Sin bitácora.** El cambio de estado queda en `postulacion_historial` (igual que el CU19/CU20 lo hacen además con `@Auditar`; aquí el envío es una acción del propio dueño y no se audita).
6. **La IA se llama después del commit y fuera de transacción**, en un ejecutor con cola (A5 del CU03); si la cola se llena o la IA falla, el job de reintento recoge las `pendiente`.
7. **El contrato con la IA lo fija el core** (ver `docs/Implementacion/CU02`); B3 lo implementa del lado de Python.

## Contrato de datos / recurso HTTP

| Método y ruta | Rol | Respuesta |
|---------------|-----|-----------|
| `POST /api/v1/postulaciones` | `POSTULANTE` | `201` (borrador nuevo) o `200` (el existente o la devuelta) con el formulario: postulación, convocatoria, perfil, opciones y respuestas guardadas |
| `POST /api/v1/postulaciones/{codigo}/enviar` | `POSTULANTE` | `200` con el comprobante `{codigo, estado, fechaEnvio, numeroEnvios, reenvio, convocatoria, gestion}` |

Cuerpo de `enviar`: `{ingresosFamiliares, dependientes, procedencia, patrimonio, estadoVulnerabilidad, promedioAcademico}`. Errores: `ERR_ENV_01` a `ERR_ENV_05` (catálogo en `docs/errors/postulaciones/`) y `ERR_VAL_01` a `ERR_VAL_03` (CU18).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU02-BE-01](./UT-CU02-BE-01.md) | Tabla, errores y consultas del envío | — |
| [UT-CU02-BE-02](./UT-CU02-BE-02.md) | `EnvioPostulacionService` y `EnvioPostulacionController` | 01 |
| [UT-CU02-BE-03](./UT-CU02-BE-03.md) | Precalificación con la IA y reintento automático | 01 |

**Cadena:** `01 → 02`, `01 → 03` (la 02 pide la precalificación que la 03 implementa; en el código la 02 depende de la interfaz de la 03).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 2, A1, A5 | Iniciar la postulación | 01, 02 |
| Flujo principal 6 a 9, A3, A4, A6, A8 | Enviar con validaciones e idempotencia | 01, 02 |
| Flujo principal 10, A7 | Precalificación y reintento | 03 |
| CA01 a CA09, CA11, CA12 | Iniciar, enviar y rechazos | 01, 02 |
| CA10 | IA caída, puntaje y calificada | 03 |

## Follow-ups (no se implementan en estas UT)

- El borrador parcial y recuperarlo: **hecho en el [CU17](../../CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n/UT-BACKEND/README.md)** (`PUT …/borrador`).
- La API de IA y su modelo (B2, B3) y el reprocesamiento (CU26, B4).
- Marcar «para revisión» una postulación que la IA rechaza (CU03 A1).

## Qué NO incluyen estas UT

- Las pantallas web y móvil (carpetas `UT-FRONTEND` y `UT-MOBILE`).
- El CU16 (documentos) y el CU18 (validación): ya existen y solo se usan.
- Elegibilidad extra definida por la DUBSS (`ValidacionPostulacionService.verificarCondiciones`).
