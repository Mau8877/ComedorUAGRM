# CU17 — Guardar borrador de postulación

- **Caso de uso:** [CU17](../CasosDeUso/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu17)
- **Diagramas de grado:** CU img 046 · análisis img 059 · secuencia img 129 · mockup img 142 (y sus secciones §5.4.17, §6.2.17, §7.4.17, §7.5.17). Revisadas las imágenes 059, 129 y 142; **no se abrieron** la 046, la 090 ni la 091 (ver «Para rehacer»).
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementados el backend, la web y la app móvil. **Sin probar contra Postgres real ni contra el stack levantado**: el backend se probó con Mockito (`BorradorPostulacionServiceTest`), la web con el cliente HTTP simulado y la app con el servicio simulado. Están escritos pero **no se pudieron ejecutar** (Docker apagado, Postgres local rechaza la contraseña) `EnvioPostulacionConsultaRepositoryTest` (`guardadoEn_…` y `borradorAbierto_…`) y `BorradorPostulacionControllerTest`.

## Flujo real implementado

Todo exige sesión de Postulante (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol). El postulante sale del token, nunca de la URL ni del cuerpo. No hay migración: `respuesta_socioeconomica` ya admitía campos vacíos (CU02). El borrador también guarda el bloque `complementarios` del formulario oficial de la DUBSS (en `datos_extra`, ver [CU02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)): ahí nada es obligatorio, un valor presente se valida igual (`ERR_ENV_05`) y `camposPendientes` cuenta ahora los seis campos de columna **más** los siete obligatorios del formulario oficial (13 en un borrador vacío).

**Guardar** — `PUT /api/v1/postulaciones/{codigo}/borrador` (`200`):

1. `PostularScreen` (web) / `postular_screen.dart` (móvil), paso 2 o 3 → «Guardar borrador». El cliente convierte el formulario con `aBorrador()` / `ValoresFormulario.aBorrador()`: un campo vacío va en `null`; uno con texto debe cumplir la regla del campo. Si algo escrito no la cumple **no se llama al servidor** y se lista qué corregir (A2).
2. `BorradorPostulacionController.guardar` → `BorradorPostulacionService.guardar(usuario, codigo, request)`, `@Transactional`.
3. `EnvioPostulacionConsultaRepository.cabecera()` (ajena o inexistente: `404` `ERR_ENV_02`), `bloquearPostulacion()` (`FOR UPDATE`) y `estadoActual()` releído tras el bloqueo. Si no está en `borrador` ni `devuelta`: `409` `ERR_ENV_03`.
4. `ValidacionPostulacionService.validar()` (CU18): convocatoria abierta y dentro de fechas, elegibilidad y duplicados → `ERR_VAL_03` (A1), `ERR_VAL_02`, `ERR_VAL_01`.
5. `ReglasRespuestaSocioeconomica.invalidos()` revisa solo los campos con valor (los mismos límites y opciones que el envío); si alguno no vale: `400` `ERR_ENV_05` y no se guarda **nada**.
6. `guardarRespuesta()` (el mismo `UPSERT` del CU02, con nulos) y `guardadoEn()` (`respuesta_socioeconomica.actualizado_en`).
7. Respuesta `BorradorGuardadoResponse {postulacion, guardadoEn, camposPendientes}`; la pantalla muestra «✓ Borrador guardado · hh:mm» (hora de La Paz) y no cambia de paso. Un error del servidor o de red se muestra como en el envío y lo escrito se conserva (A3).

El guardado **no cambia el estado**, no deja fila en `postulacion_historial`, no se anota en la bitácora, no llama a la IA y no cuenta como postulación vigente (CU18 ya ignora el `borrador`).

**Retomar** — es el `POST /api/v1/postulaciones` del [CU02](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md): `EnvioPostulacionService.iniciar` devuelve la misma postulación con `respuesta` (datos guardados) y ahora `respuesta.guardadoEn`; la pantalla precarga los campos y el aviso con la hora. Si no hay borrador crea uno en blanco (A4).

**«Mi borrador»** — `GET /api/v1/postulaciones/mi-borrador` (`200`; `data: null` si no hay): `BorradorPostulacionService.obtener` → `EnvioPostulacionConsultaRepository.borradorAbierto()` (solo `borrador` de una convocatoria `ABIERTA` y vigente; una cerrada queda bloqueada y no se lista, A6), cuenta los campos vacíos y los documentos obligatorios sin archivo (`DocumentosPostulacionConsultaRepository`). La web (`BorradorPendienteCard` sobre la tabla de `MisPostulacionesScreen`) y la app (`TarjetaBorrador` en `MisPostulacionesVista`, también cuando no hay enviadas) lo muestran con «Continuar» → `/postular`.

**Enviar un borrador** (A5): lo hace el CU02 sin cambios; el envío vuelve a exigir todo y reemplaza las respuestas.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (129) / Análisis (059) | `BorradorController` con `guardarBorrador(postulanteId, formulario)`, `obtenerBorrador(postulanteId)` y `actualizarBorrador(borradorId, formulario)` (guardar, retomar y actualizar como tres mensajes) | `BorradorPostulacionController` con `PUT /postulaciones/{codigo}/borrador` y `GET /postulaciones/mi-borrador`; guardar y «actualizar» son la misma operación, y retomar es `POST /postulaciones` del CU02 | El borrador **es** la postulación en estado `borrador` (CU02): no hay un recurso aparte que crear o actualizar |
| Secuencia (129) | `create(borrador)` sobre `Postulacion` al guardar la primera vez | No se crea nada: la postulación ya existe desde «Nueva postulación» | Los documentos del CU16 necesitan la postulación antes de guardar |
| Secuencia (129) | `create(respuesta)` / `update(respuesta)` como mensajes distintos | Un solo `guardarRespuesta()` con `INSERT … ON CONFLICT DO UPDATE` | La tabla es 1:1 con la postulación |
| Secuencia (129) | `find(convocatoriaAbierta)` → `convocatoria()` en el controlador | `ValidacionPostulacionService.validar()` (CU18), que además revisa elegibilidad y duplicados | Servicio incluido; misma validación que el envío |
| Secuencia (129) | Sin bloqueo, sin comprobación de estado ni de dueño | `FOR UPDATE`, estado releído (`ERR_ENV_03`) y dueño (`ERR_ENV_02`) | Un guardado simultáneo con el envío se atiende en fila |
| Secuencia (129) / CU | A2: un valor inválido «se señala y no se guarda **ese valor**» | Se rechaza **toda** la petición con `ERR_ENV_05`; el cliente además la valida antes de enviarla | Guardar a medias dejaría al cliente sin saber qué quedó guardado |
| Secuencia (129) | Solo se guarda un `borrador` | También una `devuelta` mientras se corrige | El postulante que corrige no debe perder su avance |
| Secuencia (129) | El borrador se «descarta o bloquea» al cerrar la convocatoria (A6) por CU13 | Queda **bloqueado** (ni guardar ni enviar pasan con la convocatoria cerrada) y deja de listarse; no se borra | No hace falta un job; el dato se conserva |
| Secuencia (129) | Sin la hora del último guardado | `guardadoEn` en la confirmación, en `respuesta` del formulario y en «Mi borrador» | Postcondición del CU: «fecha y hora del último guardado» |
| Mockup (142), pantalla 1 | «Carrera» como lista editable | Es un dato del perfil, de solo lectura, en el paso 1 (decisión del CU02) | La carrera no se falsea |
| Mockup (142), pantalla 1 | «Continuar» atenuado mientras faltan campos | «Continuar» activo; al pulsarlo marca los campos que faltan (CU02) | Mismo comportamiento que sin borrador |
| Mockup (142), pantalla 1 | Botón «Guardar borrador» solo en el paso de datos | También en el paso 3 (documentos), donde guarda lo escrito en el paso 2 | Quien llega al paso 3 sin guardar no debe perder el formulario |
| Mockup (142), pantalla 2 | Secciones «Borradores» y «Postulaciones enviadas»; barra de avance, «Avance: paso 2 de 3 · faltan 3 campos y los documentos», botones «Continuar» y **«Eliminar»** | Una tarjeta «Borrador» sobre el listado de enviadas, con «Última modificación», «Faltan N campos y M documentos.», el aviso del cierre y «Continuar». Sin barra, sin paso y sin «Eliminar»; sin enviadas se ve el vacío del CU04 | El CU no describe eliminar ni el paso del asistente; el avance se mide en lo que falta |
| Mockup (142), pantalla 1 | Lista de datos a corregir | Un aviso «No se guardó el borrador. Corrige estos datos:» con cada campo y su motivo | A2 |
| Casos de uso (046) | Extiende al CU02 | Igual | — |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 129 | Sí | Ver especificación abajo | [ ] |
| Análisis | 059 | Sí | Ver especificación abajo | [ ] |
| Mockup | 142 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. No hay clases nuevas del dominio: el borrador es una `Postulacion` en estado `borrador` con su `RespuestaSocioeconomica` (atributos opcionales). Verificar que el diagrama no tenga una clase `Borrador` aparte | [ ] |
| Diseño lógico BD | 091 | No | Sin migración nueva en este CU; la tabla `respuesta_socioeconomica` ya tiene las columnas de datos *nullable* (ver CU02) y coincide con [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid). Verificar en la imagen que esas columnas no figuren como obligatorias | [ ] |
| Casos de uso | 046 | Por verificar | No se abrió la imagen. Debe mostrar `Guardar borrador` con `«extend»` hacia el CU02 y a **Postulante** como único actor | [ ] |

### Especificación de cada diagrama afectado

#### Secuencia — img 129 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Postulante · `Boundary` PostularScreen (formulario socioeconómico) · `Control` BorradorPostulacionController · `Control` BorradorPostulacionService · `Control` ValidacionPostulacionService · `Entity` Postulacion · `Entity` RespuestaSocioeconomica

**Flujo A: guardar borrador**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | `guardarBorrador(formulario)` (clic «Guardar borrador») | [igual] |
| 2 | PostularScreen → PostularScreen | `aBorrador(valores)`: vacío → `null`, escrito → se valida | [nuevo] |
| 3 | PostularScreen → BorradorPostulacionController | `PUT /api/v1/postulaciones/{codigo}/borrador` (GuardarBorradorRequest) | [cambia] |
| 4 | BorradorPostulacionController → BorradorPostulacionService | `guardar(usuarioCodigo, postulacionCodigo, request)` | [cambia] |
| 5 | BorradorPostulacionService → Postulacion | `cabecera(codigo, usuario)` → `CabeceraEnvio` | [nuevo] |
| 6 | BorradorPostulacionService → Postulacion | `bloquearPostulacion(id)` (`FOR UPDATE`) y `estadoActual(id)` | [nuevo] |
| 7 | BorradorPostulacionService → ValidacionPostulacionService | `validar(usuarioCodigo, convocatoriaCodigo)` (convocatoria abierta, elegibilidad, duplicados) | [cambia] (antes `find(convocatoriaAbierta)`) |
| 8 | BorradorPostulacionService → BorradorPostulacionService | `ReglasRespuestaSocioeconomica.invalidos(respuesta)` | [nuevo] |
| 9 | BorradorPostulacionService → RespuestaSocioeconomica | `guardarRespuesta(postulacionId, respuesta)` (`UPSERT`) | [cambia] (antes `create(borrador)` y `create(respuesta)`) |
| 10 | BorradorPostulacionService → RespuestaSocioeconomica | `guardadoEn(postulacionId)` → `Instant` | [nuevo] |
| 11 | BorradorPostulacionService → BorradorPostulacionController | `BorradorGuardadoResponse {postulacion, guardadoEn, camposPendientes}` | [cambia] (antes `borradorGuardado()`) |
| 12 | PostularScreen → Postulante | `mostrarConfirmacion()`: «Borrador guardado · hh:mm» | [igual] |

Bloques `alt` sobre los pasos 5 a 8: **[postulación ajena o inexistente]** → `404` `ERR_ENV_02`; **[estado distinto de `borrador` y `devuelta`]** → `409` `ERR_ENV_03`; **[convocatoria cerrada (A1)]** → `409` `ERR_VAL_03` y `mostrarAviso()` «el plazo finalizó» (antes `plazoFinalizado()`); **[no elegible o duplicada]** → `403` `ERR_VAL_02` / `409` `ERR_VAL_01`; **[dato inválido (A2)]** → `400` `ERR_ENV_05` y `mostrarErrores()`; **[falla la conexión (A3)]** → la pantalla avisa que no se guardó. Nota de la figura: «El borrador no se envía: no se procesa con la IA ni cuenta como postulación vigente».

**Flujo B: retomar (`opt`)**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | `retomarBorrador()` (abre «Nueva postulación» o «Continuar» en «Mi borrador») | [igual] |
| 2 | PostularScreen → EnvioPostulacionController | `POST /api/v1/postulaciones` (CU02) | [cambia] |
| 3 | EnvioPostulacionController → EnvioPostulacionService | `iniciar(usuarioCodigo)`: `postulacionDe(...)`, `respuesta(id)`, `guardadoEn(id)` | [cambia] (antes `obtenerBorrador(postulanteId)`, `find(borrador)`, `find(respuesta)`) |
| 4 | EnvioPostulacionController → PostularScreen | `FormularioPostulacionResponse` con `respuesta` y `respuesta.guardadoEn` | [cambia] (antes `borrador()`) |
| 5 | PostularScreen → Postulante | `mostrarBorrador()`: campos precargados y «Borrador guardado · hh:mm» | [igual] |

**Flujo C: «Mi borrador» (nuevo)**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | abre «Mis postulaciones» | [nuevo] |
| 2 | PostularScreen → BorradorPostulacionController | `GET /api/v1/postulaciones/mi-borrador` | [nuevo] |
| 3 | BorradorPostulacionController → BorradorPostulacionService | `obtener(usuarioCodigo)` | [nuevo] |
| 4 | BorradorPostulacionService → Postulacion | `borradorAbierto(usuarioCodigo, ahora)` (solo convocatoria abierta) | [nuevo] |
| 5 | BorradorPostulacionService → RespuestaSocioeconomica | `respuesta(id)` y `guardadoEn(id)`; cuenta campos vacíos y documentos obligatorios sin archivo | [nuevo] |
| 6 | PostularScreen → Postulante | tarjeta «Borrador» con lo que falta y «Continuar» | [nuevo] |

#### Análisis (comunicación) — img 059 (rehacer completo)

**Clases:** actor `Postulante`; `boundary` PostularScreen (en el diagrama `BorradorView`); `control` BorradorPostulacionController (en el diagrama `BorradorController`), `control` BorradorPostulacionService (nueva), `control` ValidacionPostulacionService (nueva, CU18), `control` EnvioPostulacionController (para retomar, CU02); `entity` Postulacion, RespuestaSocioeconomica y Convocatoria.

**Enlaces y mensajes:**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | `guardarBorrador(formulario)` | [igual] |
| 1.1 | PostularScreen → BorradorPostulacionController | `PUT …/borrador` | [cambia] (antes `guardarBorrador(postulanteId, formulario)`) |
| 1.2 | BorradorPostulacionController → BorradorPostulacionService | `guardar(usuarioCodigo, postulacionCodigo, request)` | [nuevo] |
| 1.3 | BorradorPostulacionService → ValidacionPostulacionService | `validar(usuarioCodigo, convocatoriaCodigo)` | [cambia] (antes `find(convocatoriaAbierta)` hacia `Convocatoria`) |
| 1.4 | BorradorPostulacionService → Postulacion | `cabecera()`, `bloquearPostulacion()`, `estadoActual()` | [nuevo] |
| 1.5 | BorradorPostulacionService → RespuestaSocioeconomica | `guardarRespuesta()` y `guardadoEn()` | [cambia] (antes `create(borrador)`, `create(respuesta)`) |
| 2 | Postulante → PostularScreen | `retomarBorrador()` | [igual] |
| 2.1 | PostularScreen → EnvioPostulacionController | `POST /postulaciones` (CU02) | [cambia] (antes `obtenerBorrador(postulanteId)`) |
| 2.2 | EnvioPostulacionController → Postulacion y RespuestaSocioeconomica | `postulacionDe()`, `respuesta()`, `guardadoEn()` | [cambia] (antes `find(borrador)`, `find(respuesta)`) |
| 3 | Postulante → PostularScreen | `guardarBorrador(formulario)` otra vez | [cambia] (antes `actualizarBorrador(formulario)`: ya no hay una operación aparte) |
| 3.1 | PostularScreen → BorradorPostulacionController | el mismo `PUT …/borrador` | [cambia] |
| 4 | PostularScreen → BorradorPostulacionController | `GET …/mi-borrador` | [nuevo] |
| 4.1 | BorradorPostulacionController → BorradorPostulacionService | `obtener(usuarioCodigo)` → `Postulacion`, `RespuestaSocioeconomica` | [nuevo] |

#### Mockup — img 142 (rehacer: dos pantallas)

**Pantalla 1 — «Nueva postulación», paso 2 «Datos socioeconómicos»** (barra de 3 pasos, el 2 activo)

- Aviso verde «✓ Borrador guardado · hh:mm» (solo si ya se guardó). [igual]
- Campos obligatorios (*): «Ingresos familiares mensuales (Bs)» (texto numérico), «N.º de dependientes» (entero), «Procedencia» (lista), «Patrimonio» (lista), «Estado de vulnerabilidad» (lista), «Promedio académico» (texto numérico, ejemplo «Ej. 72»). [cambia] «Carrera» **ya no está**: es un dato del perfil de solo lectura en el paso 1.
- Texto de ayuda: «Puedes salir y continuar después; los campos pendientes no bloquean el borrador.» [igual]
- Botones: «Atrás», «Guardar borrador» (secundario; muestra «Guardando…» mientras guarda) y «Continuar» (activo). [cambia]
- Aviso rojo «No se guardó el borrador. Corrige estos datos:» con una viñeta por campo inválido. [nuevo]
- Error de servidor o de red como aviso rojo; lo escrito se conserva. [nuevo]

**Pantalla 1b — paso 3 «Documentos y envío»:** aviso «✓ Borrador guardado · hh:mm» (si ya se guardó), el listado de documentos (CU16), botones «Atrás», «Guardar borrador» y «Enviar postulación». [nuevo]

**Pantalla 2 — «Mis postulaciones»**

- Tarjeta «Borrador» (solo si hay un borrador en la convocatoria abierta): título con el nombre de la convocatoria, chip «Borrador», «Última modificación: dd/mm/aaaa, hh:mm» (o «Todavía no guardaste ningún dato.»), «Faltan N campos y M documentos.» (o «No falta nada: ya puedes enviarlo.»), aviso azul «Tu borrador se conserva hasta el cierre de la convocatoria (dd/mm/aaaa). Un borrador no cuenta como postulación enviada.» y el botón «Continuar». [cambia] No se dibujan la barra de avance, «paso 2 de 3», ni «Eliminar» (no implementado).
- Debajo, el listado de postulaciones enviadas del CU04 (o su mensaje «Todavía no enviaste ninguna postulación» con «Postular»). [cambia] (el mockup usa dos secciones tituladas «Borradores» y «Postulaciones enviadas»)
