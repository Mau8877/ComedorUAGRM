# Unidades de trabajo — CU06-BE

**Caso de uso:** Backend de la lista priorizada de postulantes: las tablas de convocatoria, postulación y puntaje que el CU necesita, un cargador de datos de prueba, la consulta paginada (con búsqueda y filtro por carrera) ordenada por puntaje vigente, el resumen de la convocatoria y la exportación de la lista a PDF con JasperReports.
**Fuente CU:** [`CU06 - Visualizar lista priorizada de postulantes`](../../../CasosDeUso/CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)
**Diagramas de grado:** CU-06 en la documentación de grado: contexto (`065`), análisis (`078`) y secuencia (`093`); la maqueta `106` no se sigue (el equipo decidió no hacerlo). Diseño lógico de la base de datos: `images/091` (transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid)). Ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** `GET /api/v1/lista-priorizada/convocatorias`, `GET /api/v1/lista-priorizada/resumen`, `GET /api/v1/lista-priorizada` y `POST /api/v1/lista-priorizada/exportaciones` (todos exigen `ADMINISTRADOR_DUBSS`).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/` y `R/` = `.../src/main/resources/`.

- **Las tablas que no existen se crean ahora, tal como están en el diseño lógico.** `gestion_academica`, `convocatoria`, `postulacion`, `modelo_preseleccion` y `puntaje_postulacion` (y los enums `estado_convocatoria` y `estado_postulacion`) no están en la base hoy. Se crean con las columnas, tipos, índices y nombres del diagrama, **sin agregar ni quitar columnas**. Solo se crean estas cinco: el resto de las tablas de esos paquetes (`respuesta_socioeconomica`, `documento_adjunto`, `finalista`, etc.) las crean sus propios CU. Cuando el CU-13, el CU-02 y el CU-03 se implementen, **amplían** estas tablas (endpoints, reglas, columnas nuevas con migraciones nuevas) sin recrearlas.
- **Módulos nuevos de `features/`:** `convocatorias/` (gestión académica y convocatoria), `postulaciones/` (postulación; aquí vive también la lista priorizada, porque cada fila de la lista es una postulación) y `preseleccion/` (modelo y puntaje). Cada uno lleva solo sus carpetas `model/`, `repository/` y `enums/`; `postulaciones/` además `service/`, `controller/`, `dto/`, `exception/` y `constant/` de la lista. **Regla de dependencia:** la consulta de la lista lee los Models de `usuarios/` (nombre, CI, carrera), `convocatorias/` y `preseleccion/` en una sola consulta de solo lectura dentro de `PostulacionRepository`; no usa los repositories de esos módulos. Es el acoplamiento mínimo para una consulta con joins sobre 20 000 filas, y queda documentado para revisarlo en el code review.
- **Rutas.** El recurso es la lista, no la convocatoria: `/api/v1/lista-priorizada` (kebab-case, sin plural porque es una vista del CU y no una colección de entidades propias). La convocatoria viaja como `filter[convocatoriaId]` con su **código público** (UUID), nunca el `id` interno ([`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). La selección de convocatorias y el resumen son sub-recursos de lectura de este mismo CU: **no** se ocupan `/api/v1/convocatorias`, que son del CU-13.
- **Orden fijo.** La lista siempre va por puntaje vigente descendente; no admite `sort`. Un `sort` recibido es `400` `ERR_PRI_02` (no se ignora). Desempate: `fecha_envio` ascendente y, si coincide, `postulacion.id` ascendente.
- **La posición se calcula antes de filtrar.** Con `ROW_NUMBER() OVER (ORDER BY puntaje_total DESC, fecha_envio ASC, id ASC)` sobre **todas** las postulaciones calificadas de la convocatoria; la búsqueda y el filtro por carrera se aplican **después**, sobre ese resultado, y luego se pagina. Así un postulante filtrado conserva su posición real (A2) y es la misma que guardará el CU-05. **Implementado** con SQL (una CTE) en `ListaPriorizadaRepository` sobre `NamedParameterJdbcTemplate` y no como `@Query` de Spring Data: cruza cinco tablas de cuatro módulos y el tipado de la ventana y de los `timestamptz` es predecible; además sirve tal cual para leer el PDF por tramos.
- **Qué entra:** postulaciones no eliminadas de la convocatoria con `estado = 'calificada'` y con un puntaje `vigente` no eliminado. El resumen cuenta aparte las `pendiente`.
- **Búsqueda:** `search` busca por nombres y apellidos o por CI, sin distinguir mayúsculas, con el mismo tratamiento del patrón que `UsuarioPersonalService`. `filter[carreraId]` filtra por el código de la carrera. Cualquier otro `filter[...]` es `400` `ERR_PRI_02`, como en `UsuarioController`.
- **Roles.** `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en los métodos públicos de los `service`; ningún endpoint es público.
- **Exportación a PDF con JasperReports.** Se agregan `jasperreports`, `jasperreports-pdf` (exportador a PDF), `jasperreports-jdt` (compilador) y `jasperreports-metadata` (sin él no se puede leer ningún `.jrxml`: «Unable to load report»), todos en la versión **7.1.0** (fijada en la property `jasperreports.version`; existe en Maven Central). **Ojo con el formato:** JasperReports 7 lee el `.jrxml` **sin namespace XML** y con elementos `<element kind="textField" …>` (no el formato 6 con `<textField>`/`<band>`). La plantilla es `R/reports/lista_priorizada.jrxml` y se compila una vez al arrancar. Es `POST /api/v1/lista-priorizada/exportaciones` (no `GET`, porque deja una fila en la bitácora) y devuelve el PDF binario con `Content-Type: application/pdf` y `Content-Disposition: attachment`; **es la excepción al sobre de respuesta**: el éxito es el archivo y los errores siguen saliendo en el sobre estándar. Los datos se leen **por tramos** (por ejemplo 1 000 filas) con un `JRDataSource` propio, nunca los 20 000 en memoria de una vez, y se usa un `JRVirtualizer` para el documento. Genera el documento en la misma request (sin cola), porque el CU no pide proceso diferido. El tiempo máximo con 20 000 filas no está definido: se **mide y se registra** en la UT de pruebas.
- **Fuentes dentro del contenedor.** La imagen final de `core` es una JRE mínima; JasperReports necesita fuentes para renderizar texto (`DejaVu`, o la que use la plantilla). El `Dockerfile` de `core` instala `fontconfig` y `ttf-dejavu`, y `docker-compose.yml` hace lo mismo antes de `mvn spring-boot:run` en el contenedor de desarrollo (imagen Maven sobre alpine, que tampoco las trae). La plantilla usa la fuente lógica `SansSerif` y las fuentes estándar de PDF (`Helvetica`, sin incrustar).
- **Bitácora.** Solo la exportación se anota (`EXPORTACION_LISTA_PRIORIZADA`, con `@Auditar`; entidad `convocatoria`; `detalle` con la convocatoria, los filtros y la cantidad de filas, **sin** nombres ni CI de postulantes). Consultar en pantalla no se anota ni se loguea.
- **Un código de error por cada rechazo nuevo**, prefijo `PRI`, documentado en `docs/errors/`. La falla al generar el PDF no tiene código propio: cae en el `500` genérico `ERR_SYS_00` y se loguea como `ERROR` en el `service`.
- **Datos de prueba.** `UT-CU06-BE-03` agrega un cargador de **desarrollo** y `UT-CU06-BE-10` una cuenta inicial de Administrador DUBSS (`admin@dubss.com`, contraseña de prueba): los dos corren **solo con el perfil `dev`** (el de `docker-compose.yml`), por defecto activos y apagables por propiedad; jamás en producción. Los postulantes de prueba tienen `password_hash = '!'` (no pueden iniciar sesión).

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), excepto el PDF.

| Método | Ruta | Query / cuerpo | Respuesta | Errores |
|--------|------|----------------|-----------|---------|
| `GET` | `/api/v1/lista-priorizada/convocatorias` | — | `200` array de `ConvocatoriaPriorizableResponse` (la más reciente primero) | `401` `ERR_AUTH_01`; `403` `ERR_AUTH_07` |
| `GET` | `/api/v1/lista-priorizada/resumen` | `filter[convocatoriaId]` (obligatorio) | `200` `ResumenListaPriorizadaResponse` | `400` `ERR_SYS_05` (falta o mal formado); `404` `ERR_PRI_01` |
| `GET` | `/api/v1/lista-priorizada` | `filter[convocatoriaId]` (obligatorio), `filter[carreraId]`, `search`, `page`, `pageSize` | `200` array de `PostulacionPriorizadaResponse` + `meta` | `400` `ERR_SYS_05` / `ERR_SYS_01` / `ERR_PRI_02`; `404` `ERR_PRI_01` |
| `POST` | `/api/v1/lista-priorizada/exportaciones` | `{ convocatoriaId, carreraId?, search? }` | `200` `application/pdf` | `400` `ERR_SYS_01` / `ERR_PRI_03`; `404` `ERR_PRI_01` |

- `ConvocatoriaPriorizableResponse` = `codigo`, `nombre`, `gestion` (etiqueta de la gestión académica), `estado`, `fechaApertura`, `fechaCierre`.
- `ResumenListaPriorizadaResponse` = `totalCalificadas`, `totalPendientes`.
- `PostulacionPriorizadaResponse` = `codigo` (de la postulación), `posicion`, `nombres`, `apellidos`, `ci`, `carrera` {`codigo`, `nombre`}, `puntaje`, `fechaCalculo`, `estado`.

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_PRI_01` | 404 | `NotFoundException` | La convocatoria pedida no existe o está eliminada |
| `ERR_PRI_02` | 400 | `ValidationException` | Se mandó un `sort` o un `filter[...]` que la lista no soporta |
| `ERR_PRI_03` | 400 | `ValidationException` | La lista a exportar está vacía: no hay postulaciones que exportar |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU06-BE-01](./UT-CU06-BE-01.md) | Migración: gestión académica, convocatoria, postulación, modelo y puntaje | — |
| [UT-CU06-BE-02](./UT-CU06-BE-02.md) | Models, enums y repositories de las cinco tablas | 01 |
| [UT-CU06-BE-03](./UT-CU06-BE-03.md) | Cargador de datos de prueba (solo desarrollo) | 02 |
| [UT-CU06-BE-04](./UT-CU06-BE-04.md) | Códigos de error, acción de bitácora y DTOs | — |
| [UT-CU06-BE-05](./UT-CU06-BE-05.md) | Convocatorias disponibles y resumen de la lista | 02, 04 |
| [UT-CU06-BE-06](./UT-CU06-BE-06.md) | Consulta de la lista priorizada (orden, posición, búsqueda, filtro, paginación) | 02, 04 |
| [UT-CU06-BE-07](./UT-CU06-BE-07.md) | Exportación de la lista a PDF con JasperReports | 04, 06 |
| [UT-CU06-BE-08](./UT-CU06-BE-08.md) | Endpoints de la lista priorizada | 05, 06, 07 |
| [UT-CU06-BE-09](./UT-CU06-BE-09.md) | Pruebas de integración con base real y de rendimiento | 03, 08 |
| [UT-CU06-BE-10](./UT-CU06-BE-10.md) | Cuenta inicial de Administrador DUBSS para desarrollo | — |

**Cadena:** `01 → 02 → 03`; `04` independiente; `02, 04 → 05` y `→ 06`; `04, 06 → 07`; `05, 06, 07 → 08`; `03, 08 → 09`; `10` independiente (la usa `03` para encontrar un administrador).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 a 3 | Elegir convocatoria | 05, 08 |
| Pasos 4 y 5 | Calificadas con puntaje vigente, orden, posición, resumen | 02, 05, 06, 08 |
| Paso 6 | Paginar, búsqueda y filtro por carrera | 06, 08 |
| Paso 7 | Exportar a PDF | 07, 08 |
| Paso 8 | Ver detalle (los datos ya vienen en la lista) | 06 |
| A1, A4, A9 | Lista vacía, otros estados, sin convocatorias | 05, 06 |
| A2 | Filtro con posición real | 06 |
| A3 | Desempate | 06 |
| A6, A7 | `401` y `403` | 08, 09 |
| A8 | Error del servidor | 09 (catch-all existente) |
| A10 | Convocatoria inexistente (`404`) | 04, 05, 06 |
| A11 | Puntajes nuevos tras reprocesar | 06 |
| A12 | PDF con lista vacía o con falla | 04, 07 |
| CA01–CA02 | Orden y puntaje vigente único | 06, 09 |
| CA03–CA04 | Otros estados fuera; lista vacía | 05, 06 |
| CA05–CA06 | Paginación y posición en la página 2 | 06 |
| CA07–CA08 | Búsqueda y filtro con posición real; desempate estable | 06 |
| CA09 | 20 000 postulaciones en menos de 3 s | 09 |
| CA10 | Puntajes nuevos tras reprocesar | 06 |
| CA11–CA13 | `403`, `401`, `404` | 05, 06, 08, 09 |
| CA14 | La consulta no modifica datos | 06, 09 |
| CA16–CA19 | PDF completo, con filtros, con cabecera y sin lista vacía | 07 |
| CA20 | Bitácora de la exportación | 07, 09 |
| CA21 | PDF de 20 000 postulaciones | 07, 09 |

## Follow-ups (no se implementan en estas UT)

- Endpoints, reglas y pantallas de convocatorias, postulaciones y puntajes (CU-13, CU-02, CU-03): estas UT solo dejan las tablas y los datos de prueba.
- Guardar la posición al seleccionar un finalista (CU-05).
- Generar el PDF en segundo plano si el tiempo medido con 20 000 filas resulta inaceptable.
- Limpiar el cargador de datos de prueba cuando existan los CU reales y datos semilla propios.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)); este CU no tiene versión móvil.
- Seleccionar finalistas, auditar o adjudicar (CU-05 y CU-07).
- Calcular o reprocesar puntajes (CU-03 y CU-26) ni la API de la IA.
- Crear o editar convocatorias o postulaciones (CU-13 y CU-02).
- Consultar la bitácora (CU-25).
