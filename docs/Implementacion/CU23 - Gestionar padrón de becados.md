# CU23 — Gestionar padrón de becados

- **Caso de uso:** [CU23](../CasosDeUso/CU23%20-%20Gestionar%20padr%C3%B3n%20de%20becados.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu23)
- **Diagramas de grado:** CU img 075 · análisis img 088 · secuencia img 103 · mockup img 116 · diseño lógico img 091 (§5.4.23, §6.2.23, §7.4.23, §7.5.23)
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementados el backend y la web. **Hay 5 puntos que la documentación de grado no define** y se resolvieron con el valor mínimo (ver «Decisiones pendientes» del CU); conviene confirmarlos con la DUBSS.

## Flujo real implementado

Todo exige sesión de Administrador DUBSS (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol). El administrador sale del token; todas las rutas usan el `codigo` público.

**Entrar a la pantalla** (`/padron-becados`)

1. `PadronScreen` → `GET /api/v1/convocatorias?filter[estado]=PUBLICADA` (`200`): las convocatorias con resultados publicados (CU13).
2. `PadronScreen` → `GET /api/v1/convocatorias/{codigo}/padron/resumen` (`200`) → `PadronController.resumen()` → `PadronService.resumen()`: adjudicados, pendientes de alta, vigentes, suspendidas y bajas. Convocatoria inexistente: `404` `ERR_BEC_02`.
3. Con los resultados publicados, `PadronScreen` → `GET .../padron?page&pageSize&search&filter[estado]&sort` (`200`, `data` array + `meta`) → `PadronService.listar()` → `PadronConsultaRepository.listar()`. Sin publicar: `409` `ERR_BEC_03`; `sort` inválido: `400` `ERR_BEC_10`.

**Dar de alta a los adjudicados** (flujo D)

4. «Dar de alta (N)» abre `AltaPadronDialog` (N = `pendientesDeAlta`) → confirmar → `POST .../padron/alta` (`201`) → `PadronService.darDeAlta()`: bloquea la convocatoria, exige resultados publicados (`409` `ERR_BEC_03`) y adjudicados sin beca (`409` `ERR_BEC_04`), y `PadronConsultaRepository.crearBecasFaltantes()` inserta una beca `VIGENTE` por cada adjudicación `ADJUDICADA` sin beca (inicio = día de publicación en La Paz; `ON CONFLICT DO NOTHING`). Anota `ALTA_BECAS` en la bitácora (solo si creó becas).

**Dar de baja o suspender** (flujo B)

5. `MotivoBecaDialog` (motivo obligatorio, máx. 500) → `POST /api/v1/becas/{codigo}/dar-de-baja` o `/suspender` (`200`) → `BecaController` → `BecaEstadoService.cambiar()`: bloquea la beca, exige que esté `VIGENTE` (`409` `ERR_BEC_05`), cambia el estado, inserta el `MovimientoBeca` (tipo, motivo, usuario, fecha de La Paz) y anota `BAJA_BECA` o `SUSPENSION_BECA` (el `detalle` no lleva el motivo). Sin motivo: `400` `ERR_SYS_01`; beca inexistente: `404` `ERR_BEC_01`.

**Reemplazar un cupo** (flujo C)

6. `ReemplazoDialog` abre y pide `GET /api/v1/becas/{codigo}/reemplazo` (`200`) → `BecaEstadoService.candidatoDeReemplazo()`: exige beca en `BAJA` (`409` `ERR_BEC_06`) sin reemplazo previo (`409` `ERR_BEC_09`) y un candidato (`409` `ERR_BEC_07`). El candidato es el finalista de mejor posición cuya última auditoría es «Validado», con puntaje (IA + académico) ≥ mínimo, postulación «calificada» y sin beca.
7. Confirmar → `POST /api/v1/becas/{codigo}/reemplazar` `{postulanteCodigo}` (`200`) → `BecaEstadoService.reemplazar()`: bloquea la convocatoria y la beca, vuelve a calcular el candidato y exige que sea el confirmado (`409` `ERR_BEC_08`), crea la beca vigente del reemplazante (sin adjudicación, inicio = hoy en La Paz) y el `MovimientoBeca` `REEMPLAZO` (`beca_id` = liberada, `beca_reemplazante_id` = nueva, motivo fijo), y anota `REEMPLAZO_BECA`.

No hay tareas en segundo plano. Las lecturas (resumen, listado y candidato) no escriben ni se anotan en la bitácora. El reemplazante **no** recibe adjudicación propia y su postulación **no** cambia de estado.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 103) | `PadronController` llama directo a `Beca` y `MovimientoBeca` | Entre ellos hay `PadronService`, `BecaEstadoService` y los repositorios (`PadronConsultaRepository`, `BecaRepository`, `MovimientoBecaRepository`) | Controllers delgados ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)) |
| Secuencia (img 103) | Solo consultar, baja, suspensión y reemplazo | Agrega el alta: `POST .../padron/alta` | La lista de CU incluye «Altas» en el CU-23; hay que darles de alta para tener becas |
| Secuencia (img 103) | `reemplazar(becaId, adjudicacionId)` → `reemplazarBeca(becaId, adjudicacionId)` | `GET /becas/{codigo}/reemplazo` (el candidato) y `POST /becas/{codigo}/reemplazar` con `postulanteCodigo`; no hay adjudicación | El CU dice «siguiente postulante priorizado» y «crea una nueva beca»; el reemplazante no recibe adjudicación (decisión pendiente) |
| Secuencia (img 103) | `listarBecas(convocatoriaId)` | Dos consultas: resumen y listado paginado; el listado exige resultados publicados | El resumen alimenta el botón de alta y los contadores |
| Secuencia (img 103) | Sin validaciones ni errores | Estado vigente/baja, motivo, candidato, reemplazo previo, bloqueo de convocatoria y beca | Reglas A1 a A8 del CU y concurrencia |
| Mockup (img 116) | Botones «Baja», «Suspender» y «Reemplazar» en la barra, sobre filas con casilla | Botones de ícono en cada fila, habilitados según el estado; sin casillas | Mismo patrón que el resto de los listados del panel |
| Mockup (img 116) | Botón «Historial» por fila | No implementado | El CU no lo describe; sería un `GET` de movimientos |
| Mockup (img 116) | «Buscar becado...», «Estado: Vigente ▾» y botón «Filtrar» | Búsqueda con retardo por nombre o CI; «Estado: todos» por defecto; sin «Filtrar»; agrega selector de convocatoria, resumen, «Dar de alta» y «Refrescar» | Mismo comportamiento que los demás listados; el selector y el resumen hacen falta |
| Mockup (img 116) | Subtítulo «Becas vigentes · Convocatoria 2026-II» | «Becas de la convocatoria …» con todas las becas (vigentes, suspendidas y bajas) | El padrón se administra con todos los estados |
| Diseño lógico (img 091) | `beca.adjudicacion_id` FK, UK | Nulable (nulo en una beca de reemplazo), único cuando existe | El reemplazante no tiene adjudicación propia |
| Casos de uso / detalle del CU-23 | Flujos B y C (baja, suspensión, reemplazo) y A (consultar) | Agrega el flujo D (alta) | La lista de CU lo incluye; el detalle del `.tex` no lo desarrolla |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 103 | Sí | Agregar el alta, la consulta del candidato y las capas de servicio; reemplazar por `postulanteCodigo` | [ ] |
| Análisis | 088 | Sí | Agregar `PadronService`, `BecaEstadoService` y `PadronConsultaRepository`; el reemplazo ya no usa `adjudicacionId` | [ ] |
| Mockup | 116 | Sí | Acciones por fila, selector de convocatoria, resumen, «Dar de alta», sin «Historial» ni «Filtrar» | [ ] |
| Clases / dominio | 090 | Sí | Agregar `Beca`, `MovimientoBeca`, `EstadoBeca`, `TipoMovimientoBeca` con sus atributos | [ ] |
| Diseño lógico BD | 091 | Sí | `beca.adjudicacion_id` nulable; restricciones nuevas (ver abajo) | [ ] |
| Casos de uso | 075 | Sí | Agregar el flujo de alta (o dejarlo en el texto del CU) | [ ] |

### Especificación de cada diagrama afectado

#### Secuencia — img 103 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` PadronScreen · `Control` PadronController · `Control` BecaController · `Control` PadronService · `Control` BecaEstadoService · `Entity` Beca · `Entity` MovimientoBeca

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador → PadronScreen | `listarPadron()` | [igual] |
| 2 | PadronScreen → PadronController | `GET /convocatorias/{codigo}/padron/resumen` → `200` | [nuevo] |
| 3 | PadronController → PadronService | `resumen(codigo)` | [nuevo] |
| 4 | PadronScreen → PadronController | `GET /convocatorias/{codigo}/padron` → `200` | [cambia] |
| 5 | PadronController → PadronService → Beca | `listar(codigo, page, pageSize, search, estado, sort)` → `list(becas)` | [cambia] |
| 6 | PadronScreen → Administrador | `mostrarPadron(becas)` | [igual] |

Bloque `alt` con cuatro ramas:

- **[alta]** `darDeAlta()` → `POST /convocatorias/{codigo}/padron/alta` → `201` → `PadronService.darDeAlta(actor, codigo)` → `Beca.create(becas)` (una por adjudicada sin beca) → `confirmacion()` → `mostrarConfirmacion()` [nuevo]
- **[baja]** `darDeBaja(becaCodigo, motivo)` → `POST /becas/{codigo}/dar-de-baja` → `200` → `BecaEstadoService.darDeBaja(actor, codigo, motivo)` → `Beca.update(estado=BAJA)` → `MovimientoBeca.create(BAJA)` → `confirmacion()` → `mostrarConfirmacion()` [cambia]
- **[suspensión]** igual con `suspender` y `estado=SUSPENDIDA`, `MovimientoBeca.create(SUSPENSION)` [cambia]
- **[reemplazo]** `reemplazar(becaCodigo)` → `GET /becas/{codigo}/reemplazo` → `BecaEstadoService.candidatoDeReemplazo(codigo)` → `candidato` → `mostrarCandidato()` → `confirmarReemplazo(postulanteCodigo)` → `POST /becas/{codigo}/reemplazar` → `BecaEstadoService.reemplazar(actor, codigo, postulanteCodigo)` → `Beca.create(becaReemplazante)` → `MovimientoBeca.create(REEMPLAZO)` → `confirmacion()` → `mostrarConfirmacion()` [cambia]

Bloque `alt` de error: resultados sin publicar `409` `ERR_BEC_03`; sin pendientes `409` `ERR_BEC_04`; beca no vigente `409` `ERR_BEC_05`; beca no dada de baja `409` `ERR_BEC_06`; sin candidato `409` `ERR_BEC_07`; candidato cambió `409` `ERR_BEC_08`; ya reemplazada `409` `ERR_BEC_09`; sin motivo `400` `ERR_SYS_01` [nuevo].

#### Análisis — img 088 (parchear)

Clases: `Administrador DUBSS` (actor), `PadronView` (`boundary`; en el código `PadronScreen`) [cambia de nombre], `PadronController` (`control`) [igual], `BecaController` (`control`) [nuevo], `PadronService` (`control`) [nuevo], `BecaEstadoService` (`control`) [nuevo], `Beca` (`entity`) [igual], `MovimientoBeca` (`entity`) [igual].
Enlaces: Administrador–PadronView, PadronView–PadronController, PadronView–BecaController [nuevo], PadronController–PadronService [nuevo], BecaController–BecaEstadoService [nuevo], PadronService–Beca [cambia], BecaEstadoService–Beca [cambia], BecaEstadoService–MovimientoBeca [cambia].
Mensajes numerados: 1 `listarPadron()`, 1.1 `listarBecas(convocatoriaCodigo)`, 1.2 `list(becas)`; 2 `darDeAlta()` (nuevo); 3 `darDeBaja(becaCodigo, motivo)`; 4 `suspender(becaCodigo, motivo)`; 5 `reemplazar(becaCodigo, postulanteCodigo)` (cambia: sin `adjudicacionId`).

#### Mockup — img 116 (parchear)

Pantalla «Padrón de becados» · subtítulo «Becas de la convocatoria …» [cambia].
Controles: desplegable «Convocatoria» (publicadas) [nuevo] · botón «Dar de alta (N)» (solo con pendientes) [nuevo] · resumen «Adjudicados · Vigentes · Suspendidas · Bajas» [nuevo] · campo «Buscar becado por nombre o CI...» [cambia] · desplegable «Estado: todos» [cambia] · botón «Refrescar» [nuevo] · botones «Filtrar», «Baja», «Suspender» y «Reemplazar» de la barra y casillas de selección [se quitan] · «Limpiar filtros» (con algún filtro) [nuevo].
Columnas: «Becado» (ordenable) · «CI» (ordenable) · «Carrera» (ordenable) · «Estado» (ordenable; Vigente, Suspendida o Baja) · «Inicio» (ordenable) · «Acciones» con tres íconos: «Suspender», «Dar de baja» y «Reemplazar» [cambia]; «Historial» [se quita].
Diálogos: «Dar de baja» y «Suspender beca» (campo «Motivo» obligatorio, aviso de baja definitiva) [nuevo]; «Reemplazar cupo» (muestra al candidato con CI, carrera, puesto y puntaje; «Reemplazar» y «Cancelar») [nuevo]; «Dar de alta a los adjudicados» [nuevo].
Estados: «Sin resultados», «Todavía no hay convocatorias con resultados publicados», «Los resultados de esta convocatoria todavía no fueron publicados», error con «Reintentar».

#### Clases / dominio — img 090 (parchear)

- `Beca`: `convocatoriaId`, `postulanteId`, `adjudicacionId` (opcional), `estado: EstadoBeca`, `fechaInicio: date`, `fechaFin: date` (opcional); operaciones `darDeBaja()` y `suspender()` [nuevo].
- `MovimientoBeca`: `becaId`, `becaReemplazanteId` (opcional), `registradoPor`, `tipo: TipoMovimientoBeca`, `fecha: date`, `motivo` [nuevo].
- `EstadoBeca` {`VIGENTE`, `SUSPENDIDA`, `BAJA`} y `TipoMovimientoBeca` {`BAJA`, `SUSPENSION`, `REACTIVACION`, `REEMPLAZO`} [nuevo; `REACTIVACION` sin uso todavía].
- Relaciones: `Adjudicacion 1 — 0..1 Beca` ("origina"; la beca de un reemplazo no tiene adjudicación); `Convocatoria 1 — * Beca` ("otorga"); `Postulante 1 — * Beca`; `Beca 1 — * MovimientoBeca` ("registra"); `MovimientoBeca * — 0..1 Beca` ("reemplazante").

#### Diseño lógico BD — img 091 (parchear)

Migración [`V20261115100000__crear_beca_y_movimiento_beca.sql`](../../projects/backend/comedor-uagrm-core/src/main/resources/db/migration/V20261115100000__crear_beca_y_movimiento_beca.sql); [`DiagramaConceptual.mermaid`](../DiagramaConceptual.mermaid) ya está al día.
- `beca.adjudicacion_id`: nulable [cambia]; único parcial `WHERE adjudicacion_id IS NOT NULL AND eliminado_en IS NULL` [nuevo].
- `beca`: único parcial `(postulante_id, convocatoria_id) WHERE eliminado_en IS NULL`; `CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)` [nuevo].
- `movimiento_beca`: `CHECK ((tipo = 'REEMPLAZO') = (beca_reemplazante_id IS NOT NULL))`; único parcial `(beca_id) WHERE tipo = 'REEMPLAZO' AND eliminado_en IS NULL` [nuevo].

#### Casos de uso — img 075 (parchear)

Sin cambios de actores ni de relaciones `include`/`extend`. Si se quiere mostrar el alta, agregarla como parte del mismo CU-23 (no como un CU nuevo).

#### Texto de grado

- `04_detalle_casos_uso.tex`, CU-23: agregar el flujo D (dar de alta a los adjudicados) y sus alternativas A7 y A8, que la lista de CU ya incluye como «Altas».
- Resolver con la DUBSS los puntos de «Decisiones pendientes» del CU (fechas de la beca, reemplazante y adjudicación, criterio de «disponible», reactivación).
