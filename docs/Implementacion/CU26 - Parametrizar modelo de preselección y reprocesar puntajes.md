# CU26 — Parametrizar modelo de preselección y reprocesar puntajes

- **Caso de uso:** [CU26](../CasosDeUso/CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu26)
- **Diagramas de grado:** CU img 050 · análisis img 063 · secuencia img 133 · mockup img 146 (y el CU-26 del §5.4.26, RF-21). Revisadas las imágenes 050, 063, 133 y 146; **no se abrieron** la 090 ni la 091 (ver «Para rehacer»).
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementados el backend y la web (no hay móvil en este CU). **Sin probar contra Postgres real, ni contra el stack levantado, ni contra el módulo de IA** (tarea B3, sin hacer): el backend se probó con Mockito, la web con el cliente HTTP simulado. `ModeloPreseleccionConsultaRepositoryTest`, `ReprocesamientoConsultaRepositoryTest` y `PrecalificacionConsultaRepositoryTest` están escritos pero no se ejecutaron (Docker apagado).

## Flujo real implementado

Todo exige sesión de `ADMINISTRADOR_DUBSS` (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol, comprobado en el `service` con `@PreAuthorize`). El administrador sale del token, nunca del cuerpo.

**Abrir la pantalla** — `ParametrosModeloScreen` (ruta `/parametros-modelo`):

1. `GET /api/v1/modelos-preseleccion/vigente` (`200`, `data` es la versión con sus parámetros o `null` si todavía no hay ninguna) → `ModeloPreseleccionService.vigente()` → `ModeloPreseleccionConsultaRepository.vigente()` y `parametros()`.
2. `GET /api/v1/modelos-preseleccion?page=1&pageSize=10` (`200`, `data` array + `meta`) → `ModeloPreseleccionService.listar()`: el historial, la versión más reciente primero (no admite `search`, `filter` ni `sort`).
3. El panel de reprocesar pide las convocatorias (`useConvocatorias` del CU13) y, con la elegida, `GET /api/v1/convocatorias/{codigo}/resumen-reprocesamiento` → `ReprocesamientoService.resumen()`: cuenta las `calificada` (`calificadas`) y las que no tienen puntaje vigente de la versión vigente (`porReprocesar`), si hay resultados publicados y el lote en curso, si lo hay.

**Guardar una versión** — `POST /api/v1/modelos-preseleccion` (`201`):

1. `FormularioParametros` valida con Zod (`parametrosSchema`: cada ponderación 0–100 con hasta 2 decimales) y calcula la suma; «Guardar versión» queda bloqueado si no da 100.
2. `ModeloPreseleccionController.guardar` → `ModeloPreseleccionService.guardar()` (`@Transactional`, `@Auditar` `NUEVA_VERSION_MODELO`): `validar()` (`400` `ERR_MOD_01` con todo lo que falla: variables faltantes, repetidas o desconocidas, ponderación nula o fuera de rango, más de 2 decimales, suma distinta de 100).
3. `VersionModelo.siguiente(versiones())` calcula `v{M}.{n+1}`; `archivarVigente()` (la vigente pasa a `activo = false`), `crear()` (la nueva queda `activo = true`) e `insertarParametro()` por cada variable. Si dos guardados llegan a la vez, el índice único (versión o modelo activo) hace fallar al segundo y este recibe `409` `ERR_MOD_07`.
4. La respuesta es la versión nueva con sus parámetros; la web vuelve a pedir la vigente, el historial y los resúmenes, y monta de nuevo el formulario con la versión nueva. Los puntajes existentes **no** cambian (A2).

**Reprocesar** — `POST /api/v1/convocatorias/{codigo}/reprocesar` (`202`, `data` es el lote):

1. `ReprocesarPanel` (botón «Reprocesar puntajes», bloqueado si no hay versión vigente, hay cambios sin guardar, hay resultados publicados, hay un lote en curso o no hay nada por recalcular) → `ReprocesarDialog` pide la confirmación (cancelar no hace ninguna llamada).
2. `ReprocesamientoController.reprocesar` → `ReprocesamientoService.reprocesar()` (`@Transactional`, `@Auditar` `REPROCESAMIENTO_PUNTAJES`): convocatoria inexistente (`404` `ERR_CV_01`), resultados publicados (`409` `ERR_MOD_04`), sin versión vigente (`409` `ERR_MOD_02`), lote en curso (`409` `ERR_MOD_03`), nada por recalcular (`409` `ERR_MOD_06`).
3. `ReprocesamientoConsultaRepository.crearLote()` inserta el lote `PENDIENTE` con el total (el índice único parcial `uq_lote_reprocesamiento_en_curso` da `ERR_MOD_03` si dos pedidos llegan juntos) y el servicio publica `LoteReprocesamientoCreado`.
4. Al **confirmar** la transacción, `ReprocesamientoEjecucionService.alCrearseElLote()` (`@TransactionalEventListener` + `@Async` en el ejecutor `ejecutorPrecalificacion`) llama a `ejecutar()`: `marcarEnProceso()` con el total real, y por cada postulación `PrecalificacionService.recalcular()` en su propia transacción.
5. `PrecalificacionService.recalcular()`: lee las variables de la postulación `calificada`, llama a la IA **sin transacción** con la solicitud ampliada (`modelo` y `parametros`), y en una transacción corta bloquea la fila, verifica que siga `calificada` y que no tenga ya un puntaje vigente de esa versión, retira el vigente (queda como historial), inserta el nuevo y **no cambia el estado**. Una respuesta de la IA con otra versión de modelo no sirve (`NO_CALCULADA`).
6. Cada postulación se cuenta con `sumarAvance()` (`procesadas`, y `fallidas` si la IA la rechazó o falló); si la IA no está disponible se corta el lote como `FALLIDO` (A5); al terminar, `COMPLETADO`. Repetir el reprocesamiento toma solo las que siguen sin puntaje de la versión.
7. La web consulta `GET /api/v1/lotes-reprocesamiento/{codigo}` cada 2 s mientras el lote está en curso (`useLoteReprocesamiento`) y muestra el avance, las fallidas y, al terminar, el enlace a la lista priorizada. Al volver a abrir la pantalla con un lote en curso, el resumen trae `loteEnCurso` y la web sigue su avance.
8. Al arrancar el backend, `ReprocesamientoArranque` marca como `FALLIDO` los lotes que quedaron en curso con el servidor apagado.

## Contrato con el módulo de IA (lo que B3 debe cumplir)

El contrato de [CU02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md) se amplía: la solicitud lleva la versión y las ponderaciones, para que la IA no guarde estado.

```json
// Solicitud POST {IA_BASE_URL}/api/v1/puntaje
{ "postulacion": "uuid", "ingresosFamiliares": 1800.00, "dependientes": 4, "procedencia": "Provincia",
  "carrera": "Ing. Informática", "patrimonio": "Sin vivienda propia",
  "estadoVulnerabilidad": "Hogar monoparental", "promedioAcademico": 72.5,
  "modelo": "v1.4",
  "parametros": { "ingresos_familiares": 28, "dependientes": 20, "procedencia": 15,
                  "patrimonio": 15, "estado_vulnerabilidad": 10, "promedio_academico": 12 } }
// Respuesta 200
{ "puntaje": 55.1, "modelo": "v1.4", "detalle": { "…": "opcional" } }
```

- `modelo` y `parametros` son `null` solo si todavía no se guardó ninguna versión (la precalificación del CU02 sigue funcionando: la IA usa su modelo por defecto y responde con la versión que usó, que debe estar registrada en `modelo_preseleccion`).
- Para reprocesar, la respuesta **debe repetir** la versión pedida; si no, el puntaje no se registra.
- **Pendiente con la DUBSS y la tarea B3:** las ponderaciones suman 100, pero `puntaje_total` solo admite 0–70 (`CHECK`, decisión del CU07: el auditor suma hasta 30 más). Hoy el backend acepta de la IA un puntaje de 0 a 100 y la base lo rechaza por encima de 70; **la IA tiene que normalizar** el resultado a 0–70 (por ejemplo `puntaje_100 × 0,7`) o la DUBSS tiene que definir otra regla.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Análisis (063) / Secuencia (133) | Un solo `ModeloPreseleccionController` para consultar, guardar y reprocesar | Dos controllers: `ModeloPreseleccionController` (versiones) y `ReprocesamientoController` (reprocesar y avance), con sus servicios | Controllers delgados y una responsabilidad por clase ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)) |
| Secuencia (133) | `create(modelo)` y `create(parametros)` directos | Antes se calcula la versión siguiente (`VersionModelo`) y se archiva la vigente (`archivarVigente`); todo en una transacción con índices únicos como red de seguridad (`ERR_MOD_07`) | Solo puede haber una versión vigente y las versiones no se repiten |
| Secuencia (133) | `validarParametros` solo en el controlador | Se valida en la web (Zod, suma en vivo) **y** en el servicio (`ERR_MOD_01`) | El servidor no confía en el cliente |
| Secuencia (133) | El bloque `opt` termina con `reprocesoFinalizado()` y `mostrarListaPriorizada()` como respuesta | El reprocesamiento es **asíncrono**: el `POST` responde `202` con el lote y la web consulta su avance; al terminar ofrece el enlace a la lista priorizada | El recálculo masivo no puede retener la petición ni la conexión de base (RNF) |
| Secuencia (133) | `create(lote)`, `calcularPuntaje()`, `create(puntaje)` y `update(lote)` en el mismo flujo del controlador | El lote nace `PENDIENTE`; la ejecución arranca después del `commit` (evento) en el ejecutor `ejecutorPrecalificacion`; cada postulación es una transacción propia; el avance se escribe aparte (`procesadas`, `fallidas`) | Ver el avance en vivo y que una falla no deshaga lo ya recalculado (A5) |
| Secuencia (133) | `calcularPuntaje(postulacion)` como autollamada, sin la IA | `PrecalificacionService.recalcular()` llama al módulo de IA (`POST /api/v1/puntaje`) con `modelo` y `parametros` | La IA no guarda estado (la nota del diagrama dice «el cálculo del Módulo IA es una autollamada del controlador») |
| Secuencia (133) | Sin lo que pasa si la IA no responde | Con la IA caída el lote termina `FALLIDO`; los lotes huérfanos al reiniciar también | A5; no se insiste contra un servicio caído |
| Secuencia (133) | Los bloqueos A3 y A4 son una nota | `ERR_MOD_04` (publicados), `ERR_MOD_03` (lote en curso, también por índice único), más `ERR_MOD_02` (sin versión) y `ERR_MOD_06` (nada por recalcular) | Cada regla tiene un código catalogado |
| Secuencia (133) | `ModeloPreseleccion`, `ParametroModelo` y `LoteReprocesamiento` como entidades que el controlador crea | Escritas con SQL (`ModeloPreseleccionConsultaRepository`, `ReprocesamientoConsultaRepository`); `ModeloPreseleccion` sigue siendo entidad JPA de solo lectura, `ParametroModelo` y `LoteReprocesamiento` no tienen clase de modelo | Operaciones que mueven varias tablas a la vez |
| Secuencia (133) | `PuntajePostulacion` se crea | Antes se retira (`vigente = false`) el puntaje vigente de la postulación | Historial: solo uno vigente por postulación (RF-03, RF-21) |
| Mockup (146) | Fila «v1.4 · —  · Borrador» en el historial | No existe como versión: el «borrador» es solo lo escrito en pantalla; el historial solo trae versiones guardadas (vigente o archivada) | Decisión: guardar crea una versión vigente de inmediato |
| Mockup (146) | Diálogo con «La versión v1.4 pasará a ser la vigente al confirmar» (guardar y reprocesar juntos) | Guardar y reprocesar son dos pasos: reprocesar usa la versión ya guardada y se bloquea con cambios sin guardar; el diálogo no tiene ese aviso | Reprocesar es siempre sobre la versión vigente |
| Mockup (146) | Campo «Puntaje mínimo para ser finalista» editable | Deshabilitado, «Por definir con la DUBSS», con la nota «Se configura en cada convocatoria» | Es de la convocatoria ([CU13](CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md)), no del modelo |
| Mockup (146) | «Nuevo (v1.4)» y «Guardar versión v1.4» | El número sale de la vigente (`siguienteVersion`: `v1.3` → `v1.4`; sin versión, `v1.0`), igual que el backend | El servidor decide; la web solo lo anticipa |
| Mockup (146) | Panel de reprocesar con un solo botón | Agrega el selector de convocatoria, el resumen (calificadas y por recalcular), el avance, las fallidas, los avisos de bloqueo y «Ver la lista priorizada» | Pasos 6, 7, 9 y 11 del CU |
| Mockup (146) | Sin descripción de la versión | Campo «Descripción de la versión (opcional)» | `modelo_preseleccion.descripcion` ya existe |
| Mockup (146) | Historial sin paginación visible | Historial paginado (10/20/50) con tabla y tarjetas | Todo listado se pagina ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)) |
| Texto del CU | Variables: ingresos, dependientes, procedencia, **carrera**, patrimonio, vulnerabilidad y promedio | Diez variables con ponderación: las seis del mockup y cuatro del formulario oficial de la DUBSS (`dependencia_economica`, `ocupacion_sosten`, `discapacidad`, `hijos`); la carrera viaja a la IA pero no tiene peso | El mockup lista seis pesos y se hizo antes de tener el formulario real; la carrera es un dato del perfil. Una versión guardada antes con seis variables sigue sirviendo (la IA da 0 a las que faltan), pero para guardar una nueva hay que completar las diez |
| Texto del CU (A3) | «advierte o bloquea (por definir con la DUBSS)» | **Bloquea** (`ERR_MOD_04`) | Decisión provisional; cambia en `ReprocesamientoService.reprocesar()` si la DUBSS decide otra cosa |
| Diseño lógico (091) | `lote_reprocesamiento` sin `procesadas` ni `fallidas`; `parametro_modelo` con `UNIQUE (modelo_id, clave)` | Se agregan `procesadas` y `fallidas`, y el índice único parcial de un lote en curso por convocatoria | Mostrar el avance y las fallidas (A5) sin una tabla de detalle |
| Casos de uso (050) | Solo el Administrador DUBSS | Igual; el Módulo IA actúa como sistema secundario del CU-03 que este CU invoca | — |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 133 | Sí | Ver especificación abajo | [ ] |
| Análisis | 063 | Sí | Dos controllers y sus servicios; el ejecutor en segundo plano; la IA como participante | [ ] |
| Mockup | 146 | Sí | Quitar «Borrador» y el aviso del diálogo; agregar descripción, selector de convocatoria, resumen, avance y enlace a la lista priorizada | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. `LoteReprocesamiento` suma `procesadas` y `fallidas`; `ParametroModelo` con `clave`, `ponderacion`, `valor` | [ ] |
| Diseño lógico BD | 091 | Sí | Migración `V20261120100000__crear_parametro_modelo_y_lote_reprocesamiento.sql`; coincide con [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid) | [ ] |
| Casos de uso | 050 | No | — | — |

Arquitectura (118–120): cambia poco; se suma un evento transaccional y un ejecutor ya existente para el lote. Texto de grado: el CU-26 dice «el sistema ejecuta el reprocesamiento en lote y muestra el avance» (paso 9) y «muestra la lista priorizada actualizada» (paso 11); ahora el paso 11 es un enlace que la persona abre.

### Especificación de cada diagrama afectado

#### Secuencia — img 133 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` ParametrosModeloScreen · `Control` ModeloPreseleccionController · `Control` ModeloPreseleccionService · `Control` ReprocesamientoController · `Control` ReprocesamientoService · `Control` ReprocesamientoEjecucionService · `Control` PrecalificacionService · `Object` ModuloIA · `Entity` ModeloPreseleccion · `Entity` ParametroModelo · `Entity` LoteReprocesamiento · `Entity` PuntajePostulacion.

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador → ParametrosModeloScreen | `abrirParametros()` | [nuevo] |
| 2 | ParametrosModeloScreen → ModeloPreseleccionController | `GET /api/v1/modelos-preseleccion/vigente` → `200` | [nuevo] |
| 3 | ModeloPreseleccionController → ModeloPreseleccionService | `vigente()` | [nuevo] |
| 4 | ModeloPreseleccionService → ModeloPreseleccion / ParametroModelo | `vigente()`, `parametros(modeloId)` | [cambia] (análisis 1.2 y 1.3: `find(modeloActivo)`, `list(parametros)`) |
| 5 | Administrador → ParametrosModeloScreen | `actualizarParametros(parametros)` | [igual] |
| 6 | ParametrosModeloScreen → ParametrosModeloScreen | `validarParametros(parametros)` (suma 100, rangos) | [nuevo] |
| 7 | ParametrosModeloScreen → ModeloPreseleccionController | `POST /api/v1/modelos-preseleccion` → `201` | [cambia] |
| 8 | ModeloPreseleccionController → ModeloPreseleccionService | `guardar(actorCodigo, request)` | [cambia] (`guardarNuevaVersion`) |
| 9 | ModeloPreseleccionService → ModeloPreseleccionService | `validar(request)` (`ERR_MOD_01`) | [cambia] (`validarParametros`) |
| 10 | ModeloPreseleccionService → ModeloPreseleccion | `versiones()`, `archivarVigente()`, `crear(creadoPor, version, descripcion)` → `modelo` | [cambia] |
| 11 | ModeloPreseleccionService → ParametroModelo | `insertarParametro(modeloId, clave, ponderacion)` × 6 | [cambia] |
| 12 | ModeloPreseleccionService → ParametrosModeloScreen | `ModeloPreseleccionResponse` → `mostrarConfirmacion()` | [cambia] (`versionGuardada()`) |
| 13 | Administrador → ParametrosModeloScreen | `reprocesar(convocatoria)` + confirmar | [cambia] |
| 14 | ParametrosModeloScreen → ReprocesamientoController | `POST /api/v1/convocatorias/{codigo}/reprocesar` → `202` | [cambia] |
| 15 | ReprocesamientoController → ReprocesamientoService | `reprocesar(actorCodigo, convocatoriaCodigo)` | [cambia] (`reprocesarPuntajes`) |
| 16 | ReprocesamientoService → LoteReprocesamiento | `crearLote(convocatoriaId, modeloId, solicitadoPor, total)` → `lote` (`PENDIENTE`) | [cambia] |
| 17 | ReprocesamientoService → ReprocesamientoEjecucionService | `LoteReprocesamientoCreado(loteId)` (**tras confirmar**, asíncrono) | [nuevo] |
| 18 | ReprocesamientoEjecucionService → LoteReprocesamiento | `marcarEnProceso(loteId, total)` | [nuevo] |
| 19 | ReprocesamientoEjecucionService → PrecalificacionService | `recalcular(postulacionId, modelo)` (por cada postulación) | [cambia] (`calcularPuntaje`) |
| 20 | PrecalificacionService → ModuloIA | `POST /api/v1/puntaje` con `modelo` y `parametros` | [nuevo] |
| 21 | PrecalificacionService → PuntajePostulacion | `retirarVigentes(postulacionId)`, `saveAndFlush(nuevo)` | [cambia] (`create(puntaje)`) |
| 22 | ReprocesamientoEjecucionService → LoteReprocesamiento | `sumarAvance(loteId, fallida)` | [cambia] (`update(lote)`) |
| 23 | ReprocesamientoEjecucionService → LoteReprocesamiento | `terminar(loteId, COMPLETADO \| FALLIDO)` | [nuevo] |
| 24 | ParametrosModeloScreen → ReprocesamientoController | `GET /api/v1/lotes-reprocesamiento/{codigo}` (cada 2 s) → `LoteReprocesamientoResponse` | [nuevo] (reemplaza `reprocesoFinalizado()`) |
| 25 | ParametrosModeloScreen → Administrador | `mostrarAvance()`; al terminar, enlace a `mostrarListaPriorizada()` | [cambia] |

**Bloques `alt`/`opt`:**
- `[parámetros no válidos (A1)]` (tras el 9) → `ValidationException` `ERR_MOD_01` → `mostrarErrores(errores)` [igual, con código].
- `[guardado simultáneo]` (tras el 10) → `ConflictException` `ERR_MOD_07` [nuevo].
- `opt [reprocesar puntajes de una convocatoria]` (pasos 13 a 25) [igual].
- `[resultados publicados (A3)]` (tras el 15) → `ERR_MOD_04`; `[lote en curso (A4)]` → `ERR_MOD_03`; `[sin versión vigente]` → `ERR_MOD_02`; `[nada por recalcular]` → `ERR_MOD_06` [nuevo].
- `loop [por cada postulación]` (pasos 19 a 22) con `alt [la IA rechaza los datos (A5)]` → `fallida`, se sigue; `alt [la IA no responde]` → se corta el lote: `terminar(FALLIDO)` [nuevo].
- `[cancelar antes de confirmar (A6)]` (paso 13): la pantalla cierra el diálogo, sin llamadas [igual].

#### Análisis (comunicación) — img 063 (rehacer completo)

**Clases:** actor Administrador DUBSS · `Boundary` ParametrosModeloScreen · `Control` ModeloPreseleccionController · `Control` ModeloPreseleccionService · `Control` ReprocesamientoController · `Control` ReprocesamientoService · `Control` ReprocesamientoEjecucionService · `Control` PrecalificacionService · `Entity` ModeloPreseleccion · `Entity` ParametroModelo · `Entity` LoteReprocesamiento · `Entity` PuntajePostulacion.

**Enlaces y mensajes:** Administrador → ParametrosModeloScreen `1: consultarParametros()` [igual], `2: actualizarParametros(parametros)` [igual], `3: reprocesar(convocatoriaId)` [igual]; ParametrosModeloScreen → ModeloPreseleccionController `1.1: vigente()` [cambia: `listarParametros`], `2.1: guardar(parametros)` [cambia: `guardarNuevaVersion`]; ParametrosModeloScreen → ReprocesamientoController `3.1: reprocesar(convocatoriaId)` [cambia], `3.6: avance(lote)` [nuevo]; controllers → sus servicios [nuevo]; ModeloPreseleccionService → ModeloPreseleccion `1.2: vigente()`, `2.2: crear(modelo)`; → ParametroModelo `1.3: parametros()`, `2.3: insertarParametro()`; ReprocesamientoService → LoteReprocesamiento `3.2: crearLote()`; ReprocesamientoService → ReprocesamientoEjecucionService `3.3: LoteReprocesamientoCreado` [nuevo]; ReprocesamientoEjecucionService → PrecalificacionService `3.4: recalcular(postulacion)` [cambia: `calcularPuntaje`]; PrecalificacionService → PuntajePostulacion `3.5: crear(puntaje)`; ReprocesamientoEjecucionService → LoteReprocesamiento `3.7: sumarAvance()` y `3.8: terminar()` [cambia: `update(lote)`].

#### Mockup — img 146 (rehacer)

- **Pantalla «Parámetros del modelo de preselección»** [cambia]: insignia «Versión vigente v1.3» (o «Sin versión vigente»); subtítulo «Ajuste los pesos del puntaje de vulnerabilidad y reprocese las postulaciones».
- **Tarjeta «Pesos por variable (puntos máximos)»**: tabla «Variable · Actual (v1.3) · Nuevo (v1.4)» con las diez variables (Ingresos familiares, Dependientes, Procedencia, Patrimonio, Estado de vulnerabilidad, Promedio académico, Dependencia económica, Ocupación de quien sostiene, Discapacidad, Hijos), cada «Nuevo» un campo de texto numérico con su error inline («Un número de 0 a 100 con hasta 2 decimales»); fila «Suma» con el total actual y el nuevo más la etiqueta «✓ válido» o «Deben sumar 100»; campo «Descripción de la versión (opcional)» [nuevo]; campo deshabilitado «Puntaje mínimo para ser finalista» con «Por definir con la DUBSS» y la nota «Se configura en cada convocatoria» [cambia]; botones «Guardar versión v1.4» (bloqueado si no suma 100) y «Restablecer» (bloqueado sin cambios).
- **Tarjeta «Reprocesar puntajes»** [cambia]: texto «Recalcula con la versión v1.3 las postulaciones calificadas de una convocatoria. El puntaje anterior se conserva en la traza.»; selector «Convocatoria»; «Calificadas: N · Por recalcular: M»; aviso «Cambiará la posición en la lista priorizada de algunos postulantes.»; motivo de bloqueo cuando aplica; botón «Reprocesar puntajes»; barra de avance «X de Y procesadas · Z con error»; mensajes de estado (terminado, con fallidas, interrumpido); botón «Ver la lista priorizada» [nuevo].
- **Diálogo «¿Reprocesar los puntajes?»** [cambia]: «Se recalcularán las postulaciones de {convocatoria} con la versión {v}. El proceso puede tardar varios minutos y quedará registrado en la bitácora.», nota «Se recalcularán N postulaciones. El puntaje anterior de cada una se conserva en la traza.» (sin el aviso de que la versión pasará a ser la vigente), botones «Cancelar» y «Confirmar reprocesamiento».
- **Tarjeta «Historial de versiones»** [cambia]: tabla «Versión · Fecha · Estado» con «Vigente» o «Archivada» (sin fila «Borrador») y paginación.

#### Diseño lógico de la BD — img 091

[nuevo en el CU] `parametro_modelo`: `id bigint PK`, `codigo_parametro_modelo uuid UQ`, `modelo_id bigint FK → modelo_preseleccion`, `clave varchar(60) NOT NULL`, `ponderacion numeric(5,2) NOT NULL` (`CHECK` 0–100), `valor jsonb NULL`, `creado_en`, `actualizado_en`, `eliminado_en`; índice único parcial `(modelo_id, clave)`.
[nuevo en el CU] `lote_reprocesamiento`: `id bigint PK`, `codigo_lote_reprocesamiento uuid UQ`, `convocatoria_id bigint FK`, `modelo_id bigint FK`, `solicitado_por bigint FK → usuario`, `estado varchar(20)` (`PENDIENTE`, `EN_PROCESO`, `COMPLETADO`, `FALLIDO`), `total_postulaciones int`, `procesadas int` [nuevo respecto de la imagen], `fallidas int` [nuevo respecto de la imagen], `iniciado_en`, `terminado_en`, `creado_en`, `actualizado_en`, `eliminado_en`; índice único parcial `(convocatoria_id)` donde `estado IN ('PENDIENTE','EN_PROCESO')`. Relaciones: `modelo_preseleccion 1 — N parametro_modelo`, `convocatoria 1 — N lote_reprocesamiento`, `modelo_preseleccion 1 — N lote_reprocesamiento`, `usuario 1 — N lote_reprocesamiento`.
