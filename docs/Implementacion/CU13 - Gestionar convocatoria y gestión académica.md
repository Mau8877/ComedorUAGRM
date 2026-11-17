# CU13 — Gestionar convocatoria y gestión académica

- **Caso de uso:** [CU13](../CasosDeUso/CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu13)
- **Diagramas de grado:** CU img 042 · análisis img 055 · secuencia img 125 · mockup img 138 (§5.4.13, §6.2.13, §7.4.13, §7.5.13)
- **Estado frente a los diagramas:** Difiere (ver tabla). Revisadas las imágenes 055, 125 y 138; **no se abrieron** la 042, la 090 ni la 091 (ver «Para rehacer»).

## Flujo real implementado

Todo exige sesión de Administrador DUBSS (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol). Las rutas son `/api/v1/...`; el actor sale del token.

**Gestiones académicas** (no figuran en los diagramas)

1. `GestionesAcademicasScreen` → `GET /gestiones-academicas` (`200`, con `meta`) → `GestionAcademicaController.listar` → `GestionAcademicaService.listar()`.
2. «Nueva gestión» → `GestionFormModal` → `POST /gestiones-academicas` (`201`) → `GestionAcademicaService.crear()`: fin posterior al inicio (`400` `ERR_CV_13`), etiqueta libre (`409` `ERR_CV_12`), guarda y anota `CREACION_GESTION`.
3. «Editar» → `PUT /gestiones-academicas/{codigo}` (`200`) → `GestionAcademicaService.editar()` (`404` `ERR_CV_02`; no anota si nada cambia, `EDICION_GESTION`).

**Listar y consultar convocatorias**

1. `ConvocatoriasScreen` → `GET /convocatorias?page&pageSize&search&filter[estado]&filter[gestion]&sort` → `ConvocatoriaController.listar` → `ConvocatoriaService.listar()` → `ConvocatoriaRepository.buscar()`. Devuelve fila con gestión, fechas `yyyy-MM-dd` (hora de Bolivia), cupos, puntaje mínimo, cantidad de finalistas y estado.
2. «Ver detalle» / «Editar» → `GET /convocatorias/{codigo}` → `ConvocatoriaService.obtener()` → `ConvocatoriaMapper.detalle()` (documentos y `tienePostulaciones`).
3. El formulario carga el catálogo: `GET /tipos-documento` y `GET /gestiones-academicas`.

**Crear** (flujo principal)

1. `ConvocatoriaFormModal` valida con `convocatoriaSchema` (Zod) y manda `POST /convocatorias` (`201`).
2. `ConvocatoriaController.crear` → `ConvocatoriaService.crear()`:
   1. fechas: cierre posterior a apertura (`400` `ERR_CV_04`);
   2. gestión existente (`404` `ERR_CV_02`);
   3. `DocumentosConvocatoriaService.validar()`: sin repetidos (`400` `ERR_CV_06`), al menos uno obligatorio (`400` `ERR_CV_05`), tipos existentes (`404` `ERR_CV_03`);
   4. guarda la `Convocatoria` en **BORRADOR** (apertura 00:00 y cierre 23:59:59 en `America/La_Paz`);
   5. `DocumentosConvocatoriaService.reemplazar()` guarda los `ConvocatoriaDocumento`;
   6. `@Auditar` anota `CREACION_CONVOCATORIA`.
3. La lista se refresca y se avisa con un toast.

**Editar**

1. `PUT /convocatorias/{codigo}` (convocatoria completa; los opcionales omitidos se vacían) → `ConvocatoriaService.editar()`.
2. Orden de validaciones: existe (`404` `ERR_CV_01`) → estado BORRADOR o ABIERTA (`409` `ERR_CV_08`) → fechas, gestión, documentos (como al crear) → la gestión solo cambia en BORRADOR (`409` `ERR_CV_08`) → **con postulaciones** (`PostulacionesDeConvocatoriaService.existenPara`) no cambian la apertura ni los documentos (`409` `ERR_CV_09`).
3. Los documentos se reemplazan: los que ya no están se eliminan lógicamente; uno que se quitó y se vuelve a agregar **reactiva** la fila (`ConvocatoriaDocumentoRepository.reactivar`, porque la clave incluye a las eliminadas). Anota `EDICION_CONVOCATORIA` con los nombres de los campos cambiados; sin cambios no anota.

**Abrir y cerrar** (acciones separadas, con confirmación)

1. `POST /convocatorias/{codigo}/abrir` → `ConvocatoriaEstadoService.abrir()`: solo BORRADOR (`409` `ERR_CV_08`); si hay otra ABIERTA en la gestión (`409` `ERR_CV_07`, también si dos aperturas chocan con el índice único); pasa a ABIERTA; anota `APERTURA_CONVOCATORIA`.
2. `POST /convocatorias/{codigo}/cerrar` → `cerrar()`: solo ABIERTA (`409` `ERR_CV_08`); pasa a CERRADA; anota `CIERRE_CONVOCATORIA`. No toca postulaciones: los borradores quedan bloqueados porque el envío exige convocatoria ABIERTA (CU02 y CU17).

**Eliminar**

`DELETE /convocatorias/{codigo}` (`200`, `data: null`) → `ConvocatoriaService.eliminar()`: con postulaciones `409` `ERR_CV_10` (se comprueba primero); si no está en BORRADOR `409` `ERR_CV_08`; si no, borrado lógico de la convocatoria y de sus documentos, y anota `ELIMINACION_CONVOCATORIA`.

**Cierre automático** (tarea en segundo plano, no está en los diagramas)

`CierreAutomaticoConvocatoriasJob` (`@Scheduled` cada 5 minutos, `America/La_Paz`, y al arrancar) busca las ABIERTA con `fecha_cierre` vencida y llama a `ConvocatoriaEstadoService.cerrarVencida(id)` de una en una, cada una en su transacción. Anota `CIERRE_AUTOMATICO_CONVOCATORIA` con **actor nulo**.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Análisis (055) / Secuencia (125) | `GestionAcademica` solo se consulta (`find(gestion)`) | Además hay alta, edición y lista de gestiones (`GestionAcademicaController`, `GestionAcademicaService`, pantalla propia) | Sin gestiones no se puede crear una convocatoria y ninguna pantalla las creaba (decisión 1 del CU) |
| Análisis (055) / Secuencia (125) | Entidades `Convocatoria`, `GestionAcademica`, `ConvocatoriaDocumento` | Se agrega `TipoDocumento` (catálogo de la lista de documentos, `GET /tipos-documento`) | El formulario elige los tipos de documento del catálogo |
| Secuencia (125) | Al crear: `find(convocatoriaVigente, gestion)` y rechazo si hay una vigente | Crear **no** comprueba otra vigente: nace en BORRADOR. La comprobación (`ERR_CV_07`) se hace al **abrir** | «Una abierta por gestión» solo restringe las ABIERTA (índice único parcial) |
| Secuencia (125) | `validarFechasYDocumentos(convocatoria)` como autollamada del controlador | Se reparte entre `ConvocatoriaService` (fechas, gestión) y `DocumentosConvocatoriaService.validar()` (documentos) | Los controllers son delgados; la lógica vive en servicios |
| Secuencia (125) / Mockup (138) | `abrirOCerrar(convocatoriaId, estado)` y un selector de estado en el formulario | Dos acciones, `abrir` y `cerrar`, con confirmación; el formulario no tiene selector de estado | Cada transición tiene sus propias reglas y errores (decisión 2 del CU) |
| Secuencia (125) | No hay eliminar | `DELETE` de una convocatoria en BORRADOR y sin postulaciones | A7 del CU lo da por existente |
| Secuencia (125) | No hay cierre automático (solo una nota, A5) | `CierreAutomaticoConvocatoriasJob` y `ConvocatoriaEstadoService.cerrarVencida()` | A5 del CU |
| Secuencia (125) | Nota: «cada operación se registra en la bitácora», sin participante | `@Auditar` + `BitacoraService` anotan cada operación (8 acciones nuevas) | Mecanismo común del proyecto |
| Secuencia (125) | `update(convocatoria)` en la edición | `PUT` con la convocatoria completa y reglas por estado y por postulaciones (`ERR_CV_08`, `ERR_CV_09`) | A6 y A9 del CU; los opcionales deben poder vaciarse |
| Análisis (055) | Un solo controlador, `ConvocatoriaController` | Tres: `ConvocatoriaController`, `GestionAcademicaController` y `TipoDocumentoController` | Un recurso REST por controlador |
| Mockup (138) | Columnas: Convocatoria, Gestión académica («2026 · 2.º semestre»), Inicio, Fin, Cupos, Estado, Acciones | Convocatoria, Gestión (etiqueta), Apertura, Cierre, Cupos, **Puntaje mín., Finalistas**, Estado | El CU-05 usa puntaje mínimo y cantidad de finalistas |
| Mockup (138) | Cupos sin definir: «Por definir» | Se muestra «—» | Un valor opcional vacío |
| Mockup (138) | Acciones: Editar y Cerrar (abierta), Ver (cerrada), Editar (borrador) | Ver detalle (todas), Editar (borrador y abierta), **Abrir** (borrador), Cerrar (abierta), **Eliminar** (borrador) | Decisiones 2 y 7 del CU |
| Mockup (138) | Formulario fijo debajo de la tabla, con «Inicio/Fin de postulación» y «Cupos de becas» | Modal con «Fecha de apertura / cierre», cupos, **puntaje mínimo, cantidad de finalistas y lista de documentos requeridos** (selector de tipo + «Obligatorio») | Decisión 6 del CU y sistema de diseño del panel |
| Mockup (138) | Sin búsqueda ni filtros | Búsqueda por nombre y filtros por estado y por gestión, orden y paginación | Contrato de listados del proyecto |
| Mockup (138) | Mensaje: «Solo puede haber una convocatoria abierta a la vez» | Una abierta **por gestión**; el mensaje se muestra solo cuando el backend rechaza (`ERR_CV_07`) | Decisión 3 del CU |
| Mockup (138) | Un solo título, «Convocatorias y gestión académica» | Dos pantallas y dos entradas del sidebar: «Convocatorias» y «Gestiones académicas» | Separar los dos recursos |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 125 | Sí | Ver especificación abajo | [ ] |
| Análisis | 055 | Sí | Ver especificación abajo | [ ] |
| Mockup | 138 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. Revisar que `Convocatoria` tenga `cantidadFinalistas` y el estado `PUBLICADA`, y que exista `TipoDocumento` y `ConvocatoriaDocumento` (clave compuesta convocatoria + tipo) | [ ] |
| Diseño lógico BD | 091 | Por verificar | No se abrió la imagen. Este CU **no agregó migraciones**: solo debe coincidir con `convocatoria` (con `cantidad_finalistas`), `gestion_academica`, `tipo_documento` y `convocatoria_documento` | [ ] |
| Casos de uso | 042 | Por verificar | No se abrió la imagen. El CU ahora cubre también las gestiones académicas, eliminar y el cierre automático por el sistema | [ ] |

Arquitectura (118–120): no cambia. Texto de grado: el RF-08 y el CU-13 deben mencionar las gestiones académicas como parte del caso de uso, eliminar una convocatoria en borrador y que «una abierta» es por gestión.

### Especificación de cada diagrama afectado

#### Secuencia — img 125 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` ConvocatoriasScreen · `Control` ConvocatoriaController · `Control` ConvocatoriaService · `Entity` Convocatoria · `Entity` GestionAcademica · `Entity` ConvocatoriaDocumento · `Entity` Bitacora

**Bloque 1 — crear (camino feliz)**

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador → ConvocatoriasScreen | registrar(datosConvocatoria) | [igual] |
| 2 | ConvocatoriasScreen → ConvocatoriaController | `POST /api/v1/convocatorias` → `201` | [cambia] |
| 3 | ConvocatoriaController → ConvocatoriaService | crear(actorCodigo, request) | [cambia] |
| 4 | ConvocatoriaService → ConvocatoriaService | validarFechas() (cierre posterior a apertura) | [cambia] |
| 5 | ConvocatoriaService → GestionAcademica | find(gestion) / gestion() | [igual] |
| 6 | ConvocatoriaService → ConvocatoriaService | DocumentosConvocatoriaService.validar(documentos) | [cambia] |
| 7 | ConvocatoriaService → Convocatoria | create(convocatoria) en BORRADOR / convocatoria() | [cambia] |
| 8 | ConvocatoriaService → ConvocatoriaDocumento | create(documentosRequeridos) / documentos() | [igual] |
| 9 | ConvocatoriaService → Bitacora | registrar(CREACION_CONVOCATORIA) | [nuevo] |
| 10 | ConvocatoriaController → ConvocatoriasScreen | confirmacion() | [igual] |
| 11 | ConvocatoriasScreen → Administrador | mostrarListaActualizada() | [igual] |

[quitar] el mensaje `find(convocatoriaVigente, gestion)` del bloque de crear.

**Bloque `alt` — errores de crear:** `[fechas incoherentes, sin documento obligatorio o repetidos]` → `400` `ERR_CV_04`/`ERR_CV_05`/`ERR_CV_06`; `[gestión o tipo inexistente]` → `404` `ERR_CV_02`/`ERR_CV_03`; todos terminan en `errorValidacion(motivo)` → `mostrarErrores(motivo)`.

**Bloque `opt` — abrir (A3)** `[convocatoria en BORRADOR]`: `abrir` → `POST /convocatorias/{codigo}/abrir` → `ConvocatoriaEstadoService.abrir()` → `find(otraAbiertaEnLaGestion)` en Convocatoria; `[hay otra abierta]` → `409` `ERR_CV_07`; si no, `update(estado = ABIERTA)` → `registrar(APERTURA_CONVOCATORIA)` → `mostrarListaActualizada()`. [nuevo]

**Bloque `opt` — cerrar (A4)** `[convocatoria ABIERTA]`: `cerrar` → `POST /convocatorias/{codigo}/cerrar` → `cerrar()` → `update(estado = CERRADA)` → `registrar(CIERRE_CONVOCATORIA)`. [cambia: reemplaza `abrirOCerrar` y `cambiarEstado`]

**Bloque `opt` — editar:** `PUT /convocatorias/{codigo}` → `editar()` → `[estado CERRADA o PUBLICADA]` `409` `ERR_CV_08` · `[con postulaciones y cambia apertura o documentos]` `409` `ERR_CV_09` · si no, `update(convocatoria)` y `update(documentosRequeridos)` → `registrar(EDICION_CONVOCATORIA)`. [cambia]

**Bloque `opt` — eliminar:** `DELETE /convocatorias/{codigo}` → `eliminar()` → `[con postulaciones]` `409` `ERR_CV_10` · `[no está en BORRADOR]` `409` `ERR_CV_08` · si no, borrado lógico de Convocatoria y ConvocatoriaDocumento → `registrar(ELIMINACION_CONVOCATORIA)`. [nuevo]

**Participante nuevo — cierre automático:** `Control` CierreAutomaticoConvocatoriasJob (cada 5 min y al arrancar) → `find(ABIERTA con fecha_cierre vencida)` en Convocatoria → `ConvocatoriaEstadoService.cerrarVencida(id)` → `update(estado = CERRADA)` → `registrar(CIERRE_AUTOMATICO_CONVOCATORIA)` con actor nulo. [nuevo]

**Alterno — gestiones:** `listar`/`crear`/`editar` de `GestionAcademicaController` → `GestionAcademicaService` → entidad GestionAcademica y Bitacora (`CREACION_GESTION`, `EDICION_GESTION`); errores `ERR_CV_12`, `ERR_CV_13`, `ERR_CV_02`. [nuevo, puede ir en un diagrama aparte]

#### Análisis (comunicación) — img 055 (rehacer completo)

**Clases:** `Actor` Administrador DUBSS · `boundary` ConvocatoriaView (en el código: `ConvocatoriasScreen` y `GestionesAcademicasScreen`) · `control` ConvocatoriaController · `control` GestionAcademicaController [nuevo] · `control` TipoDocumentoController [nuevo] · `entity` Convocatoria · `entity` GestionAcademica · `entity` ConvocatoriaDocumento · `entity` TipoDocumento [nuevo] · `entity` Bitacora [nuevo]

**Enlaces:** Administrador–ConvocatoriaView · ConvocatoriaView–ConvocatoriaController · ConvocatoriaView–GestionAcademicaController [nuevo] · ConvocatoriaController–Convocatoria · ConvocatoriaController–GestionAcademica · ConvocatoriaController–ConvocatoriaDocumento · ConvocatoriaController–Bitacora [nuevo] · GestionAcademicaController–GestionAcademica [nuevo] · TipoDocumentoController–TipoDocumento [nuevo] · ConvocatoriaDocumento–TipoDocumento [nuevo]

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador → ConvocatoriaView | registrar() | [igual] |
| 1.1 | ConvocatoriaView → ConvocatoriaController | registrarConvocatoria(convocatoria) | [igual] |
| 1.2 | ConvocatoriaController → GestionAcademica | find(gestion) | [igual] |
| 1.3 | ConvocatoriaController → Convocatoria | create(convocatoria) | [igual] |
| 1.4 | ConvocatoriaController → ConvocatoriaDocumento | create(documentosRequeridos) | [igual] |
| 2 | Administrador → ConvocatoriaView | listar() | [igual] |
| 2.1 | ConvocatoriaView → ConvocatoriaController | listarConvocatorias() | [igual] |
| 2.2 | ConvocatoriaController → Convocatoria | list(convocatorias) | [igual] |
| 3 | Administrador → ConvocatoriaView | actualizar() | [igual] |
| 3.1 | ConvocatoriaView → ConvocatoriaController | update(convocatoria) | [igual] |
| 3.2 | ConvocatoriaController → Convocatoria | update(convocatoria) | [igual] |
| 3.3 | ConvocatoriaController → ConvocatoriaDocumento | update(documentosRequeridos) | [igual] |
| 4 | Administrador → ConvocatoriaView | abrirOCerrar() | [cambia: `abrir()` y `cerrar()`] |
| 4.1 | ConvocatoriaView → ConvocatoriaController | cambiarEstado(convocatoriaId, estado) | [cambia: `abrir(codigo)` / `cerrar(codigo)`] |
| 4.2 | ConvocatoriaController → Convocatoria | update(estado) | [igual] |
| 5 | Administrador → ConvocatoriaView | eliminar() | [nuevo] |
| 5.1 | ConvocatoriaView → ConvocatoriaController | eliminar(codigo) | [nuevo] |
| 5.2 | ConvocatoriaController → Convocatoria | delete(convocatoria) | [nuevo] |
| 6 | ConvocatoriaController → Bitacora | registrar(accion) | [nuevo] |

#### Mockup — img 138 (rehacer)

**Pantalla «Convocatorias»** (título «Convocatorias»; antes «Convocatorias y gestión académica»)

- Encabezado: botón «Nueva convocatoria».
- Barra: búsqueda «Buscar por nombre...», filtro «Estado» (Borrador, Abierta, Cerrada, Publicada), filtro «Gestión», botón «Refrescar».
- Tabla (orden por Convocatoria, Apertura, Cierre y Estado): Convocatoria · Gestión · Apertura (dd/mm/aaaa) · Cierre · Cupos · Puntaje mín. · Finalistas · Estado · Acciones. Opcionales vacíos: «—».
- Acciones por fila: Ver detalle (todas) · Editar (borrador y abierta) · Abrir (borrador) · Cerrar (abierta) · Eliminar (borrador). Paginación 10/20/50.
- Estado vacío: «Todavía no hay convocatorias».

**Modal «Nueva convocatoria» / «Editar convocatoria» / «Detalle de la convocatoria»** (solo lectura en cerrada y publicada)

| Control | Tipo | Obligatorio | Notas |
| --- | --- | --- | --- |
| Nombre | texto, máx. 150 | Sí | |
| Gestión académica | selector | Sí | Deshabilitado si no está en borrador |
| Fecha de apertura | fecha | Sí | «Rige desde las 00:00»; deshabilitada con postulaciones |
| Fecha de cierre | fecha | Sí | Posterior a la apertura; «Rige hasta las 23:59» |
| Cupos | entero positivo | No | «Opcional: se define con la DUBSS» |
| Puntaje mínimo | 0 a 100, 2 decimales | No | |
| Cantidad de finalistas | entero positivo | No | «Solo la ve el Administrador» |
| Documentos requeridos | filas: selector de tipo + «Obligatorio» + quitar; «Agregar documento» | Al menos uno, sin repetir, uno obligatorio | Deshabilitados con postulaciones |

Se muestra el estado como etiqueta (no es un campo). Botones: «Crear convocatoria» / «Guardar cambios» y «Cancelar». Aviso con postulaciones: «Esta convocatoria ya tiene postulaciones: no se pueden cambiar la fecha de apertura ni los documentos».

**Confirmaciones:** «Abrir convocatoria» («Al abrirla, empezará a recibir postulaciones. Solo puede haber una convocatoria abierta por gestión académica»), «¿Cerrar convocatoria?» («dejará de recibir postulaciones y los borradores que no se enviaron quedarán bloqueados») y «¿Eliminar convocatoria?» («solo se pueden eliminar las convocatorias en borrador y sin postulaciones»).

**Pantalla nueva «Gestiones académicas»:** búsqueda «Buscar por etiqueta...», botón «Nueva gestión»; tabla Gestión · Inicio · Fin · Acciones (Editar); modal con Etiqueta (máx. 50), Fecha de inicio y Fecha de fin (posterior al inicio). Estado vacío: «Todavía no hay gestiones académicas».
