# CU09 — Generar planilla de asistencia

- **Caso de uso:** [enlace](../CasosDeUso/CU09%20-%20Generar%20planilla%20de%20asistencia.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu09)
- **Diagramas de grado:** CU img 068 · análisis img 081 · secuencia img 096 · mockup img 109 (§5.4.9, §6.2.9, §7.4.9, §7.5.9 del documento de taller)
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

### A. Generar la planilla

1. `PlanillasScreen` (ruta `/planillas`, solo `ADMINISTRADOR_DUBSS`) → `GET /api/v1/convocatorias?estado=PUBLICADA…` (CU13) para el selector y `GET /api/v1/convocatorias/{codigo}/planillas/resumen` (`200`): `PlanillaAsistenciaService.resumen()` → `PlanillasConsultaRepository.convocatoria()` y `contarVigentes()`.
2. La pantalla muestra «Se incluirán N becados VIGENTES…» (sin vigentes bloquea los botones) y valida el periodo en el cliente con `crearPeriodoSchema` (sin fechas, final anterior, fuera de la gestión, más de 31 días).
3. Opcional: «Vista previa» → `GET /api/v1/convocatorias/{codigo}/planillas/vista-previa?desde=&hasta=` (`200`, PDF): `PlanillaController.vistaPrevia()` → `PlanillaAsistenciaService.vistaPrevia()` → mismas validaciones, `PlanillaPdfService.generar()` con `borrador = true`. No sube, no guarda y no anota.
4. «Generar planilla» → `POST /api/v1/convocatorias/{codigo}/planillas` con `{ fechaDesde, fechaHasta }` (`201`).
5. `PlanillaController.generar()` (`@Valid`: fechas obligatorias, `400` `ERR_SYS_01`) → `PlanillaAsistenciaService.generar()` (`@PreAuthorize`, `@Transactional`, `@Auditar(GENERACION_PLANILLA)`).
6. `generar()` lee el actor (`UsuarioService.obtenerPorCodigo`), la convocatoria con su gestión (`404` `ERR_PRI_01`) y valida el periodo: `ERR_PLA_01` (final anterior), `ERR_PLA_02` (fuera de la gestión), `ERR_PLA_03` (más de 31 días) (`400`).
7. `PlanillasConsultaRepository.becadosVigentes()` trae las becas `VIGENTE` con nombre, CI y carrera, por apellido y nombre; sin ninguna, `409` `ERR_PLA_04`.
8. `PlanillaPdfService.generar()` arma el PDF apaisado con JasperReports (`planilla_asistencia.jrxml`).
9. `AlmacenamientoService.subir()` guarda el PDF en MinIO en `planillas/{convocatoria}/planilla-{desde}-{hasta}-{aleatorio}.pdf`. Si falla el PDF o MinIO: `500` `ERR_SYS_00` y no se guarda nada.
10. `PlanillaAsistenciaRepository.saveAndFlush()` inserta la fila (`planilla_asistencia`, con `total_becados`); el aspecto de `@Auditar` anota `GENERACION_PLANILLA` en la misma transacción (detalle: códigos, periodo y cantidad).
11. La respuesta `PlanillaResponse` (sin ruta) vuelve a la pantalla: aviso con el periodo y los becados, botón «Descargar» y la tabla se refresca.

### B. Consultar y descargar

1. `PlanillasScreen` → `GET /api/v1/convocatorias/{codigo}/planillas?page=1&pageSize=20` (`200`, `data` array + `meta`): `PlanillaController.listar()` (rechaza `filter[...]`, `400` `ERR_SYS_05`) → `ConsultaPlanillasService.listar()` → `PlanillasConsultaRepository.planillas()` (`sort` solo por `generadaEn` o `fechaDesde`).
2. «Descargar» → `GET /api/v1/planillas/{codigo}/descarga` (`200`): `ConsultaPlanillasService.descargar()` (`@Transactional`, `@Auditar(DESCARGA_PLANILLA)`) busca la planilla (`404` `ERR_PLA_05`), comprueba que el archivo exista en MinIO (`404` `ERR_PLA_06`, sin anotar) y entrega `AccesoPlanillaResponse` con una URL firmada de 5 minutos. La pantalla la abre en otra pestaña.

No hay tareas en segundo plano. No cambia el estado de ninguna beca ni se avisa a nadie. `PUT`, `PATCH` y `DELETE` sobre las rutas de planillas responden `405`.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 096) | `PlanillaController` consulta directo `Beca.list(becasVigentes)` y hace `create(planilla)` en `PlanillaAsistencia` | Hay `PlanillaAsistenciaService`, `PlanillasConsultaRepository` y `PlanillaAsistenciaRepository` entre el controller y las tablas | Controllers delgados, sin lógica ni acceso a repositorios ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)) |
| Secuencia (img 096) | `generarPdf(planilla)` como mensaje del propio controlador | `PlanillaPdfService.generar()` (JasperReports) y luego `AlmacenamientoService.subir()` a MinIO, antes de guardar la fila | El PDF es un componente aparte y se guarda en MinIO (el taller decía Firebase Storage, ver más abajo) |
| Secuencia (img 096) | `generarPlanilla(convocatoriaId, periodo)` sin validaciones | Valida convocatoria, periodo (`ERR_PLA_01`, `02`, `03`) y que haya becas vigentes (`ERR_PLA_04`) | Flujos alternativos A1, A2 y A4 del CU |
| Secuencia (img 096) | Sin bitácora | `@Auditar` anota `GENERACION_PLANILLA` y `DESCARGA_PLANILLA` | Auditoría de toda acción con datos personales ([AUDITORIA_BITACORA_BACKEND.md](../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)) |
| Secuencia (img 096) | Un solo bloque `opt` para consultar planillas | Además: `resumen`, `vista-previa` y `descarga` (dirección firmada) | La pantalla muestra cuántos becados entran, previsualiza y descarga; el diagrama no dibujaba el acceso al PDF |
| Análisis (img 081) | `PlanillaView`, `PlanillaController`, `Beca`, `PlanillaAsistencia` | En el código: `PlanillasScreen`, `PlanillaController`, `PlanillaAsistenciaService`, `ConsultaPlanillasService`, `PlanillaPdfService`, `PlanillasConsultaRepository`, `PlanillaAsistenciaRepository`, `AlmacenamientoService` (MinIO) y el aspecto de bitácora | Mismo motivo de capas; el PDF y MinIO son componentes del flujo |
| Mockup (img 109) | Tabla con «Planilla» (`PL-0007`), «Periodo», «Becados», «Generada», «PDF» | Columnas «Periodo», «Becados», «Generada», «Generada por» y una acción «Descargar» por fila; sin código de planilla | La tabla `planilla_asistencia` del diagrama 091 no tiene un número de planilla |
| Mockup (img 109) | Una vista previa del PDF siempre visible entre el aviso y la tabla | Un botón «Vista previa» que muestra el PDF en un `<iframe>` (antes de pedirla hay un texto guía) | La vista previa depende del periodo ingresado y del padrón de ese momento |
| Mockup (img 109) | Fechas «01/09/2026» y «30/09/2026» ya cargadas | Fechas vacías | No se inventa un periodo por defecto: lo decide el Administrador |
| Mockup (img 109) | Aviso «Se incluirán 880 becados VIGENTES…» | Igual, más la gestión a la que debe ajustarse el periodo; sin vigentes lo dice y bloquea los botones | Flujo alternativo A2 |
| Mockup (img 109) | Una tabla única con las planillas de «Convocatoria 2026-II» | Selector de convocatoria (las publicadas) y su propia tabla, paginada y ordenable | Una convocatoria cualquiera tiene su propia lista (RF y reglas de listados) |
| Clases (img 090) | `PlanillaAsistencia` con convocatoria, periodo, archivo, responsable y fecha | Agrega `totalBecados` | La lista muestra cuántos becados incluyó y el cotejo (CU-11) lo compara |
| Diseño lógico (img 091) | `planilla_asistencia` sin cantidad de becados | Columna `total_becados integer NOT NULL` (> 0), `CHECK (fecha_hasta >= fecha_desde)` e índices parciales | Ver la fila anterior; `DiagramaConceptual.mermaid` ya lo refleja |
| Texto de grado (§5.4.9) | El PDF se guarda en **Firebase Storage** | Se guarda en **MinIO** (compatible con S3) | Decisión de infraestructura del proyecto: Firebase solo para push (CLAUDE.md) |
| Texto de grado (§5.4.9) | Formato del PDF «por definir con la DUBSS» | Hoja A4 apaisada con N.º, apellidos y nombres, CI, carrera, recuadro de fotocopia de carnet y una columna de firma por día; **máximo 31 días** (`ERR_PLA_03`) | Valores propuestos para poder entregar el CU; el límite de 31 días es una restricción del diseño de la hoja que el taller no menciona |
| Texto de grado (§5.4.9) | Paso 4: validar que el periodo esté comprendido en la gestión académica | Igual (`ERR_PLA_02`) | — |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 096 | Sí | Ver especificación abajo | [ ] |
| Análisis | 081 | Sí | Ver especificación abajo | [ ] |
| Mockup | 109 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | Sí | Agregar `totalBecados: int` a `PlanillaAsistencia` | [ ] |
| Diseño lógico BD | 091 | Sí | Agregar `total_becados int NOT NULL` a `planilla_asistencia` | [ ] |
| Casos de uso | 068 | No | — | — |

Arquitectura (img 118 a 120): sin cambios (MinIO y JasperReports ya estaban por el CU08). Texto de grado: en `04_detalle_casos_uso.tex` (CU-09) cambiar «Firebase Storage» por «MinIO» y agregar el límite de 31 días al paso 4.

### Especificación de cada diagrama afectado

#### Secuencia — img 096 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` PlanillasScreen · `Control` PlanillaController · `Control` PlanillaAsistenciaService · `Control` PlanillasConsultaRepository · `Control` PlanillaPdfService · `Control` AlmacenamientoService · `Entity` PlanillaAsistencia · `Entity` Beca

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador DUBSS → PlanillasScreen | `generarPlanilla(periodo)` | [igual] |
| 2 | PlanillasScreen → PlanillaController | `POST /api/v1/convocatorias/{codigo}/planillas` `{ fechaDesde, fechaHasta }` → `201` | [cambia] |
| 3 | PlanillaController → PlanillaAsistenciaService | `generar(actorCodigo, convocatoriaCodigo, desde, hasta)` | [cambia] |
| 4 | PlanillaAsistenciaService → PlanillasConsultaRepository | `convocatoria(codigo)` (con su gestión académica) | [nuevo] |
| 5 | PlanillaAsistenciaService → PlanillaAsistenciaService | `validarPeriodo(convocatoria, desde, hasta)` | [nuevo] |
| 6 | PlanillaAsistenciaService → PlanillasConsultaRepository | `becadosVigentes(convocatoriaId)` | [cambia] |
| 7 | PlanillasConsultaRepository → Beca | `list(becasVigentes)` | [igual] |
| 8 | PlanillaAsistenciaService → PlanillaPdfService | `generar(datosPlanilla)` → `byte[]` | [cambia] |
| 9 | PlanillaAsistenciaService → AlmacenamientoService | `subir(ruta, pdf, "application/pdf")` | [nuevo] |
| 10 | PlanillaAsistenciaService → PlanillaAsistencia | `create(planilla)` (`saveAndFlush`) | [igual] |
| 11 | PlanillaAsistenciaService → Bitácora | `registrar(GENERACION_PLANILLA)` (aspecto `@Auditar`) | [nuevo] |
| 12 | PlanillaAsistenciaService → PlanillaController → PlanillasScreen | `planilla()` → `mostrarPlanilla(planilla)` | [igual] |

Bloques `alt` de error: fecha final anterior → `400` `ERR_PLA_01`; fuera de la gestión → `400` `ERR_PLA_02`; más de 31 días → `400` `ERR_PLA_03`; sin becas vigentes → `409` `ERR_PLA_04`; convocatoria inexistente → `404` `ERR_PRI_01`; falla del PDF o de MinIO → `500` `ERR_SYS_00` sin registrar [todos nuevos].

Bloque `opt` «Vista previa»: `GET .../planillas/vista-previa?desde&hasta` → mensajes 3 a 8 con borrador, sin 9 a 11 → PDF [nuevo].

Bloque `opt` «El Administrador consulta las planillas generadas» [cambia]: `consultarPlanillas()` → `GET /api/v1/convocatorias/{codigo}/planillas` → `listar(convocatoriaCodigo, pageable)` en `ConsultaPlanillasService` → `planillas(convocatoriaCodigo, pageable)` → `list(planillas)` → `mostrarPlanillas(planillas)`.

Bloque `opt` «Descargar» [nuevo]: `GET /api/v1/planillas/{codigo}/descarga` → `descargar(actorCodigo, planillaCodigo)` → `existe(ruta)` y `urlTemporal(ruta, 5 min)` en `AlmacenamientoService` → Bitácora `DESCARGA_PLANILLA` → `AccesoPlanillaResponse(url, expiraEn)`; errores `404` `ERR_PLA_05` y `ERR_PLA_06`.

#### Análisis — img 081 (rehacer)

Clases: `Administrador DUBSS` (actor) [igual], `PlanillaView` (`boundary`, en el código `PlanillasScreen`) [cambia de nombre], `PlanillaController` (`control`) [igual], `PlanillaAsistenciaService` (`control`) [nuevo], `ConsultaPlanillasService` (`control`) [nuevo], `PlanillaPdfService` (`control`) [nuevo], `PlanillasConsultaRepository` (`control`) [nuevo], `AlmacenamientoService` (`control`, MinIO) [nuevo], `Beca` (`entity`) [igual], `PlanillaAsistencia` (`entity`) [igual].
Enlaces y mensajes: Administrador–PlanillaView (`1: generarPlanilla(periodo)`, `2: consultarPlanillas()`) · PlanillaView–PlanillaController (`1.1: generarPlanilla(convocatoriaId, periodo)`, `2.1: listarPlanillas(convocatoriaId)`) · PlanillaController–PlanillaAsistenciaService (`1.2: generar(...)`) [nuevo] · PlanillaAsistenciaService–PlanillasConsultaRepository (`1.3: becadosVigentes(convocatoriaId)`) [nuevo] · PlanillasConsultaRepository–Beca (`1.4: list(becasVigentes)`) · PlanillaAsistenciaService–PlanillaPdfService (`1.5: generar(datos)`) [nuevo] · PlanillaAsistenciaService–AlmacenamientoService (`1.6: subir(ruta, pdf)`) [nuevo] · PlanillaAsistenciaService–PlanillaAsistencia (`1.7: create(planilla)`) · PlanillaController–ConsultaPlanillasService (`2.2: listar(...)`) [nuevo] · ConsultaPlanillasService–PlanillasConsultaRepository (`2.3: planillas(...)`) [nuevo] · PlanillasConsultaRepository–PlanillaAsistencia (`2.4: list(planillas)`).

#### Mockup — img 109 (parchear)

Pantalla «Planilla de asistencia» · subtítulo «Se genera a partir del padrón vigente» [igual]. Menú lateral con «Planillas» activo [igual].
Controles: desplegable «Convocatoria» (las publicadas) [cambia] · fechas «Desde» y «Hasta» (tipo fecha, vacías, acotadas a la gestión; error bajo el campo) [cambia] · botón «Vista previa» [nuevo] · botón «Generar planilla» [igual].
Aviso (amarillo): «Se incluirán 880 becados VIGENTES, con espacio para firma y fotocopia de carnet. El periodo debe estar dentro de la gestión 2026-II.» [cambia]. Sin vigentes: «Esta convocatoria no tiene becas vigentes: no hay a quién incluir en la planilla.» y los dos botones bloqueados [nuevo].
Tras generar (verde): «Planilla generada para 01/09/2026 – 30/09/2026 con 880 becados.» con botón «Descargar» [nuevo].
Área de vista previa: texto guía «Elige el periodo y pulsa «Vista previa»…» hasta que se pide; luego el PDF en un recuadro [cambia].
Tabla «Planillas generadas» con botón «Refrescar»: columnas «Periodo» (ordenable) · «Becados» · «Generada» (ordenable, fecha y hora) · «Generada por» y la acción «Descargar» por fila (sin columna «Planilla» `PL-0007`); paginación; estado vacío «Todavía no hay planillas generadas para esta convocatoria»; error con «Reintentar» [cambia].

#### Clases — img 090 (parchear)

`PlanillaAsistencia`: `convocatoriaId: Long`, `generadaPor: Long`, `fechaDesde: LocalDate`, `fechaHasta: LocalDate`, `totalBecados: Integer` [nuevo], `storagePath: String`, `generadaEn: Instant`. Relaciones: `Convocatoria 1 — 0..* PlanillaAsistencia` [igual]; `Usuario 1 — 0..* PlanillaAsistencia` (generó) [igual].

#### Diseño lógico de la BD — img 091 (parchear)

`planilla_asistencia`: `id` PK · `codigo_planilla_asistencia` uuid UQ · `convocatoria_id` FK · `generada_por` FK → `usuario` · `fecha_desde` date · `fecha_hasta` date · `total_becados` integer NOT NULL [nuevo] · `storage_path` varchar(500) · `generada_en` timestamptz · `creado_en`, `actualizado_en`, `eliminado_en`. `CHECK (fecha_hasta >= fecha_desde)` y `CHECK (total_becados > 0)` [nuevos]; índices parciales `(convocatoria_id, generada_en DESC)` y `(generada_por)` [nuevos]. Sin unicidad por periodo. Migración: [`V20261116100000__crear_planilla_asistencia.sql`](../../projects/backend/comedor-uagrm-core/src/main/resources/db/migration/V20261116100000__crear_planilla_asistencia.sql); [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid) ya coincide.
