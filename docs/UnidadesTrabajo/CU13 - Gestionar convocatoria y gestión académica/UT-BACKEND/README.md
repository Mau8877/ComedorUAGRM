# Unidades de trabajo — CU13-BE

**Caso de uso:** Backend de la gestión de convocatorias de la Beca Comedor y de las gestiones académicas: listar, consultar, crear, editar, abrir, cerrar (a mano y automático) y eliminar convocatorias con sus documentos requeridos, y listar, crear y editar gestiones académicas. Todo anotado en la bitácora y restringido al Administrador DUBSS.
**Fuente CU:** [`CU13 - Gestionar convocatoria y gestión académica`](../../../CasosDeUso/CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)
**Diagramas de grado:** `images/042` (contexto), `055` (análisis: `ConvocatoriaView` → `ConvocatoriaController` → `Convocatoria`, `GestionAcademica`, `ConvocatoriaDocumento`), `125` (secuencia) y `138` (mockup, no usado en backend); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** `/api/v1/convocatorias`, `/api/v1/gestiones-academicas` y `GET /api/v1/tipos-documento`; todos exigen el rol `ADMINISTRADOR_DUBSS`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md`, `TESTING_BACKEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/comedor-uagrm-core/src/test/java/com/comedoruagrm/backend/`. Todo vive en `features/convocatorias/` (ya existen `enums/`, `model/` y `repository/`).

- **Qué ya existe.** Las tablas `gestion_academica`, `convocatoria` (con `cantidad_finalistas`), `tipo_documento` y `convocatoria_documento` ya están creadas por migraciones anteriores: **este CU no agrega ninguna migración**. Los Models `Convocatoria` y `GestionAcademica` existen, pero son de solo lectura; `TipoDocumento` y `ConvocatoriaDocumento` no tienen Model. `EstadoConvocatoria` ya tiene un cuarto valor, `PUBLICADA` (lo agregó el CU22).
- **Estados.** Se crea siempre en `BORRADOR`. `abrir`: `BORRADOR → ABIERTA`. `cerrar`: `ABIERTA → CERRADA` (a mano o por el job). Se edita una `BORRADOR` o `ABIERTA`; una `CERRADA` o `PUBLICADA` no se modifica; solo se elimina una `BORRADOR`. Cualquier otro caso es `409` `ERR_CV_08`. El estado nunca viene en el body: solo cambia con las acciones.
- **Una abierta por gestión.** Al abrir se comprueba que no haya otra `ABIERTA` en la misma gestión y, además, se traduce la violación del índice único parcial `uq_convocatoria_abierta_por_gestion` (dos aperturas simultáneas) al mismo `409` `ERR_CV_07`.
- **Fechas.** El API recibe y devuelve fechas (`yyyy-MM-dd`) en hora de Bolivia. Se guardan como `timestamptz`: la apertura a las `00:00` y el cierre a las `23:59:59` de ese día en `America/La_Paz` (clase `FechasConvocatoria`). El cierre debe ser un día **posterior** a la apertura (`400` `ERR_CV_04`).
- **`PUT` y no `PATCH`.** La pantalla siempre manda la convocatoria completa (incluida la lista de documentos, que **reemplaza** a la anterior) y los campos opcionales (`cupos`, `puntajeMinimo`, `cantidadFinalistas`) tienen que poder vaciarse: omitirlos los vacía. Es el caso que [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#put-vs-patch) reserva para `PUT`. Lo mismo para las gestiones.
- **Documentos requeridos.** Cada convocatoria lleva su lista de tipos de documento con `obligatorio` (al menos uno `true`, sin repetir tipos). La tabla `convocatoria_documento` tiene clave primaria `(convocatoria_id, tipo_documento_id)` **aunque la fila esté eliminada lógicamente**: quitar y volver a agregar un tipo no puede insertar otra fila, sino **reactivar** la anterior (`UT-CU13-BE-03` y `UT-CU13-BE-04`).
- **Con postulaciones (A6, A7).** «Tiene postulaciones» = existe alguna postulación no eliminada de esa convocatoria. Con postulaciones no se cambia la fecha de apertura ni los documentos (`409` `ERR_CV_09`) y no se elimina (`409` `ERR_CV_10`). La consulta pasa por un servicio del módulo `postulaciones` (regla de dependencia entre módulos), no por su repositorio.
- **Cierre y borradores (A4).** Cerrar solo cambia el estado. Los borradores no enviados quedan bloqueados porque la postulación ([CU-02](../../../CasosDeUso/CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md), [CU-17](../../../CasosDeUso/CU17%20-%20Guardar%20borrador%20de%20postulación.md)) solo se envía con la convocatoria `ABIERTA`; esa comprobación es de esos CU, no de este.
- **Cierre automático (A5).** Un job (`@Scheduled`, cada 5 minutos y al arrancar, `America/La_Paz`) cierra las `ABIERTA` cuya `fecha_cierre` ya pasó, una por una, cada una en su propia transacción. La bitácora lo anota con **actor nulo** (`bitacora.usuario_id` admite `NULL`) y la acción `CIERRE_AUTOMATICO_CONVOCATORIA`. No hay apertura automática.
- **Seguridad en el `service`.** Cada método público lleva `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, **menos** el método que usa el job (nadie lo llama desde HTTP). El administrador que actúa sale del token (`@CurrentUserId`), nunca del body.
- **Bitácora con `@Auditar`.** Acciones nuevas en `BitacoraAcciones`: `CREACION_GESTION`, `EDICION_GESTION`, `CREACION_CONVOCATORIA`, `EDICION_CONVOCATORIA`, `APERTURA_CONVOCATORIA`, `CIERRE_CONVOCATORIA`, `CIERRE_AUTOMATICO_CONVOCATORIA` y `ELIMINACION_CONVOCATORIA`. El `detalle` lleva códigos, estados y los **nombres** de los campos modificados, nunca valores libres de texto.
- **`cantidadFinalistas` solo la ve el Administrador.** Se devuelve únicamente en los endpoints de este CU (todos de Administrador); ningún endpoint del postulante la devuelve (hoy solo la usan el panel del CU05 y la selección de finalistas, ambos de Administrador). Una prueba lo fija.
- **Listados.** Todos paginados (`page`, `pageSize`, `search`, `sort`; ver [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). Los catálogos (gestiones, tipos de documento) también, y la pantalla pide `pageSize` grande. Un `filter` o `sort` no soportado es `400`.
- **Un código `ERR_CV_*` por cada rechazo nuevo**, documentado en `docs/errors/convocatorias/`.

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md). Campos de las respuestas:

- `GestionAcademicaResponse` = `codigo`, `etiqueta`, `fechaInicio`, `fechaFin`.
- `TipoDocumentoResponse` = `codigo`, `nombre`, `descripcion`.
- `ConvocatoriaResumenResponse` = `codigo`, `nombre`, `gestion` (`codigo`, `etiqueta`), `fechaApertura`, `fechaCierre`, `cupos`, `puntajeMinimo`, `cantidadFinalistas`, `estado`.
- `ConvocatoriaResponse` = lo anterior + `tienePostulaciones` + `documentos` (lista de `tipoDocumento` y `obligatorio`).
- Cuerpo de `POST`/`PUT` de convocatoria: `nombre`, `gestionCodigo`, `fechaApertura`, `fechaCierre`, `cupos?`, `puntajeMinimo?`, `cantidadFinalistas?`, `documentos` (lista de `tipoDocumentoCodigo` y `obligatorio`). Cuerpo de gestión: `etiqueta`, `fechaInicio`, `fechaFin`.

| Método | Ruta | Respuesta | Errores |
|--------|------|-----------|---------|
| `GET` | `/api/v1/convocatorias` (`page`, `pageSize`, `search`, `filter[estado]`, `filter[gestion]`, `sort`) | `200` array de `ConvocatoriaResumenResponse` + `meta` | `400` `ERR_SYS_05`/`ERR_CV_11` |
| `GET` | `/api/v1/convocatorias/{codigo}` | `200` `ConvocatoriaResponse` | `404` `ERR_CV_01` |
| `POST` | `/api/v1/convocatorias` | `201` `ConvocatoriaResponse` | `400` `ERR_SYS_01`/`ERR_CV_04`/`ERR_CV_05`/`ERR_CV_06`; `404` `ERR_CV_02`/`ERR_CV_03` |
| `PUT` | `/api/v1/convocatorias/{codigo}` | `200` `ConvocatoriaResponse` | los de `POST` y `404` `ERR_CV_01`; `409` `ERR_CV_08`/`ERR_CV_09` |
| `POST` | `/api/v1/convocatorias/{codigo}/abrir` | `200` `ConvocatoriaResponse` | `404` `ERR_CV_01`; `409` `ERR_CV_07`/`ERR_CV_08` |
| `POST` | `/api/v1/convocatorias/{codigo}/cerrar` | `200` `ConvocatoriaResponse` | `404` `ERR_CV_01`; `409` `ERR_CV_08` |
| `DELETE` | `/api/v1/convocatorias/{codigo}` | `200` `data: null` | `404` `ERR_CV_01`; `409` `ERR_CV_08`/`ERR_CV_10` |
| `GET` | `/api/v1/gestiones-academicas` (`page`, `pageSize`, `search`, `sort`) | `200` array de `GestionAcademicaResponse` + `meta` | `400` `ERR_SYS_05`/`ERR_CV_11` |
| `POST` | `/api/v1/gestiones-academicas` | `201` `GestionAcademicaResponse` | `400` `ERR_SYS_01`/`ERR_CV_13`; `409` `ERR_CV_12` |
| `PUT` | `/api/v1/gestiones-academicas/{codigo}` | `200` `GestionAcademicaResponse` | `400` `ERR_SYS_01`/`ERR_CV_13`; `404` `ERR_CV_02`; `409` `ERR_CV_12` |
| `GET` | `/api/v1/tipos-documento` (`page`, `pageSize`, `search`, `sort`) | `200` array de `TipoDocumentoResponse` + `meta` | `400` `ERR_SYS_05`/`ERR_CV_11` |

Sin sesión, `401` (`ERR_AUTH_01`); sin el rol, `403` (`ERR_AUTH_07`). `sort` de convocatorias: `nombre`, `fechaApertura`, `fechaCierre`, `estado`, `creadoEn` (por defecto `-creadoEn`); de gestiones: `etiqueta`, `fechaInicio`, `fechaFin` (por defecto `-fechaInicio`); de tipos de documento: `nombre` (por defecto `nombre`). `search`: `nombre` en convocatorias, `etiqueta` en gestiones, `nombre` en tipos de documento.

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_CV_01` | 404 | `NotFoundException` | La convocatoria no existe o está eliminada |
| `ERR_CV_02` | 404 | `NotFoundException` | La gestión académica no existe o está eliminada |
| `ERR_CV_03` | 404 | `NotFoundException` | Un tipo de documento indicado no existe |
| `ERR_CV_04` | 400 | `ValidationException` | La fecha de cierre no es posterior a la de apertura |
| `ERR_CV_05` | 400 | `ValidationException` | Ningún documento de la lista es obligatorio |
| `ERR_CV_06` | 400 | `ValidationException` | Un tipo de documento aparece más de una vez |
| `ERR_CV_07` | 409 | `ConflictException` | Ya hay otra convocatoria abierta en la misma gestión |
| `ERR_CV_08` | 409 | `ConflictException` | La operación no corresponde al estado actual de la convocatoria |
| `ERR_CV_09` | 409 | `ConflictException` | La convocatoria ya tiene postulaciones: no se cambian sus documentos ni su fecha de apertura |
| `ERR_CV_10` | 409 | `ConflictException` | La convocatoria tiene postulaciones: no se elimina |
| `ERR_CV_11` | 400 | `ValidationException` | `sort` no soportado en el listado |
| `ERR_CV_12` | 409 | `ConflictException` | Ya existe una gestión con esa etiqueta |
| `ERR_CV_13` | 400 | `ValidationException` | La fecha de fin de la gestión no es posterior a la de inicio |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU13-BE-01](./UT-CU13-BE-01.md) | Códigos de error `ERR_CV_01` a `ERR_CV_13` y su catálogo | — |
| [UT-CU13-BE-02](./UT-CU13-BE-02.md) | `GestionAcademica` y `Convocatoria`: constructores y setters | — |
| [UT-CU13-BE-03](./UT-CU13-BE-03.md) | Models `TipoDocumento` y `ConvocatoriaDocumento` (clave compuesta) | 02 |
| [UT-CU13-BE-04](./UT-CU13-BE-04.md) | Consultas de repositorio y consulta de postulaciones por convocatoria | 02, 03 |
| [UT-CU13-BE-05](./UT-CU13-BE-05.md) | DTOs y `FechasConvocatoria` | 02, 03 |
| [UT-CU13-BE-06](./UT-CU13-BE-06.md) | Gestiones académicas: listar, crear y editar | 01, 02, 04, 05 |
| [UT-CU13-BE-07](./UT-CU13-BE-07.md) | Listar y consultar convocatorias; listar tipos de documento | 01, 03, 04, 05 |
| [UT-CU13-BE-08](./UT-CU13-BE-08.md) | Crear una convocatoria con sus documentos | 01, 02, 03, 04, 05, 07 |
| [UT-CU13-BE-09](./UT-CU13-BE-09.md) | Editar una convocatoria | 08 |
| [UT-CU13-BE-10](./UT-CU13-BE-10.md) | Abrir y cerrar una convocatoria | 08 |
| [UT-CU13-BE-11](./UT-CU13-BE-11.md) | Eliminar una convocatoria | 08 |
| [UT-CU13-BE-12](./UT-CU13-BE-12.md) | Cierre automático al llegar la fecha de cierre | 10 |
| [UT-CU13-BE-13](./UT-CU13-BE-13.md) | Controllers de convocatorias, gestiones y tipos de documento | 06, 07, 08, 09, 10, 11 |
| [UT-CU13-BE-14](./UT-CU13-BE-14.md) | Pruebas de integración con base real | 12, 13 |

**Cadena:** `02 → 03 → 04, 05`; `01, 04, 05 → 06`; `01, 04, 05 → 07 → 08 → 09, 10, 11`; `10 → 12`; `06, 07, 08, 09, 10, 11 → 13`; `12, 13 → 14`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1–2 | Lista de convocatorias con gestión, fechas, cupos y estado | 04, 05, 07, 13 |
| Flujo principal, pasos 3–8 | Crear y editar con validación, documentos y bitácora | 03, 05, 08, 09, 13 |
| Gestiones académicas | Listar, crear y editar | 02, 04, 06, 13 |
| A1 | Convocatoria vigente en la misma gestión (`409`) | 10 |
| A2 | Datos inválidos, fechas incoherentes, sin documento obligatorio (`400`) | 01, 05, 08, 09 |
| A3, A4 | Abrir y cerrar | 10 |
| A5 | Cierre automático | 12 |
| A6, A7 | Postulaciones: restringir edición, no eliminar | 04, 09, 11 |
| A8, A9, A10 | Inexistente (`404`), estado inválido (`409`), `401`/`403` | 01, 06–13 |
| CA01–CA04 | Crear, fechas, documentos, rangos | 05, 08 |
| CA05, CA06 | Una abierta por gestión, abrir | 10 |
| CA07, CA08 | Cerrar y cierre automático | 10, 12 |
| CA09, CA10 | Con postulaciones | 09, 11 |
| CA11 | Cerrada o publicada no se abre ni edita | 09, 10 |
| CA12, CA13 | `401`/`403` y `cantidadFinalistas` solo del Administrador | 13, 14 |
| CA14 | Bitácora | 06, 08, 09, 10, 11, 12, 14 |
| CA15, CA16 | Gestiones: alta, etiqueta única, fechas | 01, 06 |
| CA17 | Eliminar una en borrador sin postulaciones | 11 |

## Follow-ups (no se implementan en estas UT)

- Que el envío de una postulación compruebe estado `ABIERTA` y fechas (CU02 y CU17).
- Apertura automática en la fecha de apertura (el CU no la pide).
- Reabrir una convocatoria cerrada por error (no está en el CU).
- Cupos y puntaje mínimo: siguen opcionales hasta que la DUBSS los defina.

## Qué NO incluyen estas UT

- Las pantallas (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)). Este CU es solo web: no hay UT de mobile.
- Enviar o validar postulaciones (CU02, CU17, CU18) y publicar resultados (CU22).
- Migraciones nuevas, tipos de documento nuevos (el catálogo se administra en otro CU) y la eliminación de gestiones académicas.
