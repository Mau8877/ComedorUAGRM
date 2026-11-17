# CU11 — Cotejar registro contra respaldo físico

- **Caso de uso:** [enlace](../CasosDeUso/CU11%20-%20Cotejar%20registro%20contra%20respaldo%20f%C3%ADsico.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu11)
- **Diagramas de grado:** CU img 070 · análisis img 083 · secuencia img 098 · mockup img 111 (§5.4.11, §6.2.11, §7.4.11, §7.5.11 del documento de taller)
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

1. `CotejoScreen` (ruta `/cotejo-asistencia`, solo `ADMINISTRADOR_DUBSS`) → `useConvocatorias` (CU13, solo publicadas) para el selector de convocatoria y `usePlanillas` (CU09) para el de planilla; elige la primera de cada una.
2. Con la planilla elegida: `GET /api/v1/planillas/{codigo}/cotejos/datos` (`200`): `CotejoController.datos()` → `ConsultaCotejosService.datos()` (`@PreAuthorize`, solo lectura) → `CotejosConsultaRepository.planilla()` (`404` `ERR_COT_01`) y `resumenDigital()`. La pantalla muestra becados en la planilla, registros digitales y becados con asistencia; sin registros, avisa (A1).
3. A la vez, `GET /api/v1/planillas/{codigo}/cotejos/registros?page=1&pageSize=20` (`200`, `data` + `meta`): `ConsultaCotejosService.registros()` → `CotejosConsultaRepository.registros()` (asistencias del periodo de las becas de la convocatoria creadas antes de generar la planilla; `sort` solo por `fecha` o `apellidos`, `400` `ERR_SYS_05` si no). Sin CI.
4. `GET /api/v1/planillas/{codigo}/cotejos` (`200`, `data` + `meta`): el historial (`ConsultaCotejosService.cotejos()`; `sort` por `creadoEn` o `fechaCotejo`).
5. El Administrador compara a mano con la planilla física, ingresa total de registros y coincidencias; `calcularCotejo()` deduce discrepancias y resultado y `cotejoSchema` valida (observaciones obligatorias con discrepancias, coincidencias ≤ total).
6. «Guardar cotejo» → `POST /api/v1/planillas/{codigo}/cotejos` con `{ totalRegistros, coincidencias, discrepancias, resultado, observaciones? }` (`201`).
7. `CotejoController.registrar()` (`@Valid`: obligatorios y rangos, `400` `ERR_SYS_01`) → `CotejoAsistenciaService.registrar()` (`@PreAuthorize`, `@Transactional`, `@Auditar(COTEJO_ASISTENCIA)`): lee el actor, la planilla (`404` `ERR_COT_01`) y valida `ERR_COT_02` (suma), `ERR_COT_03` (resultado) y `ERR_COT_04` (observaciones) (`400`).
8. `CotejoAsistenciaRepository.saveAndFlush()` inserta la fila (con fecha de hoy en La Paz); el aspecto anota la bitácora en la misma transacción (códigos, cantidades y resultado, sin observaciones).
9. La pantalla muestra un aviso con el resultado, limpia el formulario y refresca el historial. Los errores del servidor se muestran traducidos, con «Reintentar» si son de red.

No hay tareas en segundo plano. No cambia asistencias, planillas ni becas, y no se avisa a nadie. `PUT`, `PATCH` y `DELETE` sobre las rutas de cotejos responden `405`.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 098) | `CotejoController` consulta directo `PlanillaAsistencia.find` y `Asistencia.list` y hace `create(cotejo)` | Hay `ConsultaCotejosService`, `CotejoAsistenciaService`, `CotejosConsultaRepository` y `CotejoAsistenciaRepository` entre el controller y las tablas | Controllers delgados, sin acceso a repositorios ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)) |
| Secuencia (img 098) | Un solo mensaje `obtenerDatosCotejo(planillaId)` que devuelve `datos()` | Tres consultas: `datos` (planilla y conteos), `registros` (lista paginada) y el historial de cotejos | Los registros digitales pueden ser decenas de miles: se paginan y ordenan en el servidor |
| Secuencia (img 098) | `registrarCotejo(planillaId, resultado)` sin validaciones | Recibe total, coincidencias, discrepancias, resultado y observaciones y valida `ERR_COT_02`, `03` y `04` | Flujo alternativo A2 del CU y consistencia de lo declarado |
| Secuencia (img 098) | Sin bitácora | `@Auditar` anota `COTEJO_ASISTENCIA` (sin observaciones) | El CU pide dejar constancia en la bitácora (paso 6) |
| Secuencia / análisis | `Asistencia.list(asistencias)` trae las asistencias de la planilla | Las asistencias del periodo de las becas de su convocatoria creadas antes de generarla (`beca.creado_en <= planilla.generada_en`) | La planilla no guarda su lista de becados; es una aproximación (ver follow-up) |
| Análisis (img 083) | `CotejoView`, `CotejoController`, `PlanillaAsistencia`, `Asistencia`, `CotejoAsistencia` | En el código: `CotejoScreen`, `CotejoController`, `ConsultaCotejosService`, `CotejoAsistenciaService`, `CotejosConsultaRepository`, `CotejoAsistenciaRepository`, `CotejoAsistencia` y el aspecto de bitácora | Mismo motivo de capas |
| Mockup (img 111) | Selector «Planilla PL-0007», botón «Cotejar» y subtítulo «Planilla PL-0007 · 01–30/09/2026» | Selectores de convocatoria y de planilla (por periodo y becados, sin código); los datos se cargan al elegir, sin botón «Cotejar» | La planilla no tiene un código `PL-0007` (CU09); no hace falta un botón para cargar |
| Mockup (img 111) | Tarjetas «880 Registros en planilla», «861 Coincidencias», «19 Discrepancias» calculadas por el sistema | Tarjetas «Becados en la planilla», «Registros digitales en el periodo» y «Becados con asistencia digital»; las coincidencias y las discrepancias las declara el Administrador en el formulario | Sin lectura automática de la planilla firmada, el sistema no puede saber cuántas firmas coinciden |
| Mockup (img 111) | Tabla «Becado · Fecha · Digital · Planilla firmada · Resultado» con «Asistió / Sin registro», «Firmó / Sin firma» y «Conforme / Discrepancia» por fila | Tabla «Becado · Carrera · Fecha · Hora (digital)» con los registros digitales; sin «Planilla firmada» ni «Resultado» por fila | Capturar cada firma fila por fila no es viable a escala; queda para la lectura automática (OCR), por pedido del equipo |
| Mockup (img 111) | Campo «Observaciones» y botón «Guardar cotejo» | Formulario con total, coincidencias, discrepancias y resultado deducidos, observaciones (obligatorias con discrepancias) y «Guardar cotejo» | Paso 5 del CU: el cotejo registra total, coincidencias, discrepancias, resultado y observaciones |
| Mockup (img 111) | Sin historial | Tabla «Cotejos registrados» de la planilla | Flujo A3: los cotejos anteriores se conservan |
| Clases (img 090) | `CotejoAsistencia` con planilla, usuario, fecha, cantidades, resultado y observaciones | Igual (`planillaId`, `realizadoPor`, `fechaCotejo`, `totalRegistros`, `coincidencias`, `discrepancias`, `resultado`, `observaciones`); el resultado es el enum `ResultadoCotejo` | — |
| Diseño lógico (img 091) | `cotejo_asistencia` con `resultado varchar` | Igual, más `CHECK` de cantidades y de resultado, e índices parciales por planilla y por usuario; sin `cotejo_detalle` (opcional) | Última barrera de consistencia; sin detalle por becado mientras no haya lectura automática |
| Texto de grado (§5.4.11) | Paso 4: el Administrador compara con las firmas de la planilla física | Igual: la comparación es manual y el sistema no lee la planilla | Decisión: el OCR se aplicará más adelante |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 098 | Sí | Ver especificación abajo | [ ] |
| Análisis | 083 | Sí | Ver especificación abajo | [ ] |
| Mockup | 111 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | No | — (solo anotar el enum `ResultadoCotejo`) | — |
| Diseño lógico BD | 091 | No | — (los `CHECK` son detalle de implementación) | — |
| Casos de uso | 070 | No | — | — |

Arquitectura (img 118 a 120): sin cambios. Texto de grado: en `04_detalle_casos_uso.tex` (CU-11) aclarar que el sistema muestra los registros digitales y que el Administrador declara total y coincidencias (sin lectura automática por ahora).

### Especificación de cada diagrama afectado

#### Secuencia — img 098 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` CotejoScreen · `Control` CotejoController · `Control` ConsultaCotejosService · `Control` CotejoAsistenciaService · `Control` CotejosConsultaRepository · `Control` CotejoAsistenciaRepository · `Entity` PlanillaAsistencia · `Entity` Asistencia · `Entity` CotejoAsistencia

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador DUBSS → CotejoScreen | `consultarPlanilla(planilla)` | [igual] |
| 2 | CotejoScreen → CotejoController | `GET /api/v1/planillas/{codigo}/cotejos/datos` → `200` | [cambia] |
| 3 | CotejoController → ConsultaCotejosService | `datos(planillaCodigo)` | [cambia] |
| 4 | ConsultaCotejosService → CotejosConsultaRepository | `planilla(codigo)` → `DatosPlanilla` (`404` `ERR_COT_01`) | [nuevo] |
| 5 | CotejosConsultaRepository → PlanillaAsistencia | `find(planilla)` | [igual] |
| 6 | ConsultaCotejosService → CotejosConsultaRepository | `resumenDigital(planilla)` → registros y becados | [cambia] |
| 7 | CotejosConsultaRepository → Asistencia | `list(asistencias)` del periodo | [igual] |
| 8 | CotejoScreen → CotejoController | `GET …/cotejos/registros?page&pageSize&sort` → `200` (lista paginada) | [nuevo] |
| 9 | CotejoScreen → Administrador | `mostrarDatosCotejo(datos)` | [igual] |
| 10 | Administrador DUBSS → CotejoScreen | `registrarCotejo(total, coincidencias, observaciones)` (deduce discrepancias y resultado) | [cambia] |
| 11 | CotejoScreen → CotejoController | `POST /api/v1/planillas/{codigo}/cotejos` `{ totalRegistros, coincidencias, discrepancias, resultado, observaciones? }` → `201` | [cambia] |
| 12 | CotejoController → CotejoAsistenciaService | `registrar(actorCodigo, planillaCodigo, request)` | [cambia] |
| 13 | CotejoAsistenciaService → CotejosConsultaRepository | `planilla(codigo)` | [nuevo] |
| 14 | CotejoAsistenciaService → CotejoAsistenciaService | `validar(request, observaciones)`: `ERR_COT_02`, `03`, `04` (bloque `opt` [datos inválidos] → `400`) | [nuevo] |
| 15 | CotejoAsistenciaService → CotejoAsistenciaRepository | `saveAndFlush(cotejo)` | [cambia] |
| 16 | CotejoAsistenciaRepository → CotejoAsistencia | `create(cotejo)` | [igual] |
| 17 | CotejoAsistenciaService → CotejoAsistenciaService | anota `COTEJO_ASISTENCIA` en la bitácora (aspecto `@Auditar`) | [nuevo] |
| 18 | CotejoScreen → Administrador | `mostrarConfirmacion()` y se refresca el historial | [igual] |

#### Análisis (comunicación) — img 083 (rehacer completo)

**Clases:** `Actor` Administrador DUBSS · `boundary` CotejoScreen · `control` CotejoController · `control` ConsultaCotejosService · `control` CotejoAsistenciaService · `entity` PlanillaAsistencia · `entity` Asistencia · `entity` CotejoAsistencia

| Enlace | Mensajes numerados | Estado |
| --- | --- | --- |
| Administrador DUBSS ↔ CotejoScreen | `1: consultarPlanilla(planilla)` · `2: registrarCotejo(total, coincidencias, observaciones)` | [cambia] |
| CotejoScreen ↔ CotejoController | `1.1: obtenerDatosCotejo(planilla)` · `2.1: registrarCotejo(planilla, cantidades, resultado)` | [cambia] |
| CotejoController ↔ ConsultaCotejosService | `1.2: datos(planilla)` · `1.3: registros(planilla)` | [nuevo] |
| CotejoController ↔ CotejoAsistenciaService | `2.2: registrar(planilla, request)` | [nuevo] |
| ConsultaCotejosService ↔ PlanillaAsistencia | `1.4: find(planilla)` | [igual] |
| ConsultaCotejosService ↔ Asistencia | `1.5: list(asistencias)` | [igual] |
| CotejoAsistenciaService ↔ CotejoAsistencia | `2.3: create(cotejo)` | [igual] |

#### Mockup — img 111 (rehacer)

Pantalla «Cotejo de asistencia» (web, layout de administrador, menú «Cotejo de asistencia» activo):

- **Selectores:** «Convocatoria» (publicadas) y «Planilla» («01/09/2026 – 30/09/2026 · 880 becados») [cambia: sin «PL-0007» ni botón «Cotejar»].
- **Subtítulo:** «Planilla del periodo 01/09/2026 – 30/09/2026».
- **Tarjetas:** «Becados en la planilla», «Registros digitales en el periodo», «Becados con asistencia digital» [cambia: antes «Registros en planilla / Coincidencias / Discrepancias»].
- **Aviso** (sin registros digitales): «No hay registros digitales de asistencia en el periodo de esta planilla. Igual puedes registrar el cotejo.» [nuevo].
- **Tabla «Registros digitales del periodo»** (paginada, ordenable por Becado y Fecha): Becado · Carrera · Fecha · Hora (digital) [cambia: sin «Planilla firmada» ni «Resultado»].
- **Formulario «Registrar cotejo»:** «Total de registros» (número ≥ 0), «Coincidencias» (número entre 0 y el total), «Discrepancias» y «Resultado» (solo lectura, deducidos), «Observaciones» (texto, «(obligatorias)» si hay discrepancias, máx. 2000), botón «Guardar cotejo» [cambia].
- **Errores bajo cada campo** y aviso del servidor con «Reintentar» si es de conexión [nuevo].
- **Tabla «Cotejos registrados»** (paginada): Registrado · Resultado · Registros · Coincidencias · Discrepancias · Realizado por · Observaciones; vacía: «Esta planilla todavía no fue cotejada» [nuevo].
