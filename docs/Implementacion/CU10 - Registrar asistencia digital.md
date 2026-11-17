# CU10 — Registrar asistencia digital

- **Caso de uso:** [enlace](../CasosDeUso/CU10%20-%20Registrar%20asistencia%20digital.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu10)
- **Diagramas de grado:** CU img 069 · análisis img 082 · secuencia img 097 · mockup img 110 (§5.4.10, §6.2.10, §7.4.10, §7.5.10 del documento de taller)
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

1. El Personal del Comedor inicia sesión en la app: `rutaInicioPorRol('PERSONAL_COMEDOR')` lo lleva a `/asistencia` → `RegistrarAsistenciaScreen` (muestra «Hoy, dd/mm/aaaa» con la hora de La Paz).
2. Ingresa el CI (obligatorio, máx. 20) y pulsa «Buscar» → `AsistenciaNotifier.buscar()` → `AsistenciaService.buscarBecado()` → `POST /api/v1/asistencias/buscar-becado` con `{ ci }` (`200`).
3. `AsistenciaController.buscarBecado()` (`@Valid`: CI no vacío, `ERR_SYS_01`) → `AsistenciaService.buscarBecado()` (`@PreAuthorize('PERSONAL_COMEDOR')`, `@Transactional(readOnly)`) → `AsistenciasConsultaRepository.buscarPorCi(ci, hoy)`: une `usuario`, `beca`, `postulante`, `carrera`, `convocatoria` y la `asistencia` de hoy; prefiere la beca vigente. Sin resultado: `404` `ERR_ASI_01` (A1).
4. La pantalla muestra la tarjeta: nombre, CI, carrera, convocatoria y estado de la beca. «Registrar asistencia» solo aparece si la beca está `VIGENTE` y no hay asistencia hoy (si no, avisa el motivo: A2 o A3).
5. «Registrar asistencia» → `AsistenciaNotifier.registrar()` → `POST /api/v1/asistencias` con `{ becaCodigo }`.
6. `AsistenciaController.registrar()` → `AsistenciaService.registrar()` (`@Transactional`): `UsuarioService.obtenerPorCodigo` (actor), `AsistenciasConsultaRepository.cabeceraBeca()` (`404` `ERR_ASI_03`), exige `VIGENTE` (`409` `ERR_ASI_02`).
7. `AsistenciaRepository.insertarSiNoExiste(becaId, actorId, hoy, ahora)` (`INSERT … ON CONFLICT (beca_id, fecha) WHERE eliminado_en IS NULL DO NOTHING`) y luego `findByBecaIdAndFecha()`. La fecha es la de La Paz según el reloj del servidor.
8. Respuesta `AsistenciaResponse`: `201` con `yaRegistrada = false` si la creó; `200` con `yaRegistrada = true` y la hora del primer registro si ya existía (A3).
9. La app muestra «Asistencia registrada a las HH:mm» (o «ya registrada hoy a las HH:mm») y «Siguiente becado» limpia la pantalla.

Errores de red o del servidor (A5): la app muestra el aviso y conserva al becado; reintentar es seguro por la idempotencia. No hay tareas en segundo plano, no se anota en la bitácora, no cambia la beca y no se avisa a nadie. `PUT`, `PATCH` y `DELETE` sobre `/api/v1/asistencias` responden `405`.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 097) | `AsistenciaControllerMobile` consulta directo `Beca.find(beca)` y `Asistencia.find/create` | Hay `AsistenciaService`, `AsistenciasConsultaRepository` y `AsistenciaRepository` entre el controller y las tablas; el controller se llama `AsistenciaController` | Controllers delgados, sin acceso a repositorios ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)); el backend es uno solo para web y móvil, no hay un controller «Mobile» |
| Secuencia (img 097) | `find(asistenciaDelDia)` y, si no hay, `create(asistencia)` (dos pasos) | Un solo `INSERT … ON CONFLICT DO NOTHING` y luego `findByBecaIdAndFecha`; el bloque `alt` se decide por el resultado del insert (1 = creada, 0 = ya existía) | Con dos pasos, dos peticiones simultáneas podían duplicar; el índice único parcial lo garantiza (RNF-03) |
| Secuencia (img 097) | `buscarBecado(ci)` → `find(beca)` sin validaciones | `POST` con el CI en el cuerpo; `404` si el CI no figura (`ERR_ASI_01`); devuelve también becas suspendidas o de baja con su estado | Flujo A1 y A2 del CU; el CI no va en la URL por ser dato personal |
| Secuencia (img 097) | `registrarAsistencia(becaId, fecha)`: la fecha llega del cliente | `registrarAsistencia(becaCodigo)`: la fecha de hoy (La Paz) la fija el servidor; exige beca vigente (`ERR_ASI_02`) y existente (`ERR_ASI_03`) | El cliente no debe poder registrar otro día; el diagrama no dibuja el rechazo por estado (A2) |
| Secuencia (img 097) | Rama `duplicado()` sin datos | Respuesta `200` con `yaRegistrada = true` y la hora del primer registro | A3 pide informar la hora del registro existente |
| Secuencia / análisis | Sin actor del sistema ni usuario que registra | `registrado_por` sale del token (`@CurrentUserId`) y se resuelve con `UsuarioService` | La asistencia guarda quién la registró (CU, paso 8) |
| Secuencia (img 097) | Sin bitácora | No se anota en la bitácora | La fila ya guarda quién y cuándo y se escribiría un evento por comida de cada becado; decisión propuesta, a confirmar con la DUBSS |
| Análisis (img 082) | `RegistrarAsistenciaView`, `AsistenciaControllerMobile`, `Beca`, `Asistencia` | En la app: `RegistrarAsistenciaScreen`, `AsistenciaNotifier` (provider), `AsistenciaService` (Flutter). En el backend: `AsistenciaController`, `AsistenciaService`, `AsistenciasConsultaRepository`, `AsistenciaRepository`, `Asistencia` | Capas de la arquitectura móvil y backend |
| Mockup (img 110) | Encabezado «Comedor · Asistencia» con «Personal ▾», «Hoy, 30/09/2026 · Turno almuerzo» | `AppBar` «Comedor · Asistencia» con el menú de cuenta (`MenuUsuario`); «Hoy, dd/mm/aaaa» sin turno | El diseño lógico no tiene turno; no se inventa |
| Mockup (img 110) | Recuadro «Buscar por CI o escanear carnet» | Solo el campo «CI del becado» y «Buscar»; sin escaneo | El escaneo de carnet no está en el CU ni en los RF; queda como follow-up |
| Mockup (img 110) | Tarjeta con «Foto / carnet» | Tarjeta con nombre, CI, carrera, convocatoria y estado, sin foto | El sistema no guarda la foto del carnet del becado |
| Mockup (img 110) | Chip «Beca vigente» y botón «✔ Registrar asistencia»; aviso amarillo «Asistencia ya registrada» | Igual; además chip por estado (suspendida, de baja), avisos de beca no vigente y de errores, y «Siguiente becado» | Flujos A1, A2, A3 y paso 9 del CU |
| Clases (img 090) | `Asistencia` con beca, fecha, hora de registro y usuario | Igual (`becaId`, `registradoPor`, `fecha`, `horaRegistro`) | — |
| Diseño lógico (img 091) | `asistencia` con `UNIQUE (beca_id, fecha)` | Igual, como índice único parcial `WHERE eliminado_en IS NULL` más índices por `fecha` y `registrado_por` | Convención de borrado lógico ([PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)) |
| Navegación móvil | `PERSONAL_COMEDOR` iba a `/sin-modulo` | Va a `/asistencia`, sin shell | Primer caso de uso del rol |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 097 | Sí | Ver especificación abajo | [ ] |
| Análisis | 082 | Sí | Ver especificación abajo | [ ] |
| Mockup | 110 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | No | — | — |
| Diseño lógico BD | 091 | No | — (el índice único parcial es detalle de implementación) | — |
| Casos de uso | 069 | No | — | — |

Arquitectura (img 118 a 120): sin cambios. Texto de grado: en `04_detalle_casos_uso.tex` (CU-10) agregar que la búsqueda también informa becas suspendidas o dadas de baja, que la fecha la fija el servidor y que el rol inicia en la pantalla de asistencia.

### Especificación de cada diagrama afectado

#### Secuencia — img 097 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Personal del Comedor · `Boundary` RegistrarAsistenciaScreen · `Control` AsistenciaController · `Control` AsistenciaService · `Control` AsistenciasConsultaRepository · `Control` AsistenciaRepository · `Entity` Beca · `Entity` Asistencia

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Personal del Comedor → RegistrarAsistenciaScreen | `buscarBecado(ci)` | [igual] |
| 2 | RegistrarAsistenciaScreen → AsistenciaController | `POST /api/v1/asistencias/buscar-becado` `{ ci }` → `200` | [cambia] |
| 3 | AsistenciaController → AsistenciaService | `buscarBecado(ci)` | [cambia] |
| 4 | AsistenciaService → AsistenciasConsultaRepository | `buscarPorCi(ci, hoy)` | [nuevo] |
| 5 | AsistenciasConsultaRepository → Beca | `find(beca)` (une usuario, carrera, convocatoria y la asistencia de hoy) | [cambia] |
| 6 | RegistrarAsistenciaScreen → Personal del Comedor | `mostrarBecado(beca)` con su estado | [igual] |
| 7 | Personal del Comedor → RegistrarAsistenciaScreen | `registrarAsistencia()` | [igual] |
| 8 | RegistrarAsistenciaScreen → AsistenciaController | `POST /api/v1/asistencias` `{ becaCodigo }` → `201` / `200` | [cambia] |
| 9 | AsistenciaController → AsistenciaService | `registrar(actorCodigo, becaCodigo)` | [cambia] |
| 10 | AsistenciaService → AsistenciasConsultaRepository | `cabeceraBeca(becaCodigo)`; si no está vigente → `409` `ERR_ASI_02` (bloque `opt` [beca no vigente]) | [nuevo] |
| 11 | AsistenciaService → AsistenciaRepository | `insertarSiNoExiste(becaId, actorId, hoy, ahora)` → `1` o `0` | [cambia] |
| 12 | AsistenciaService → AsistenciaRepository | `findByBecaIdAndFecha(becaId, hoy)` → `Asistencia` | [cambia] |
| alt | [creada = 1] | `AsistenciaResponse(yaRegistrada = false)` → `201` → `mostrarConfirmacion()` | [igual] |
| alt | [ya registrada = 0] | `AsistenciaResponse(yaRegistrada = true, horaRegistro)` → `200` → `mostrarAvisoDuplicado()` | [cambia] |

Alternativos: CI desconocido → `404` `ERR_ASI_01` en el mensaje 3; beca inexistente → `404` `ERR_ASI_03` en el mensaje 10.

#### Análisis (comunicación) — img 082 (rehacer completo)

**Clases:** `Actor` Personal del Comedor · `boundary` RegistrarAsistenciaScreen · `control` AsistenciaNotifier · `control` AsistenciaController · `control` AsistenciaService · `entity` Beca · `entity` Asistencia

| Enlace | Mensajes numerados | Estado |
| --- | --- | --- |
| Personal del Comedor ↔ RegistrarAsistenciaScreen | `1: buscarBecado(ci)` · `2: registrarAsistencia()` | [igual] |
| RegistrarAsistenciaScreen ↔ AsistenciaNotifier | `1.1: buscar(ci)` · `2.1: registrar()` | [nuevo] |
| AsistenciaNotifier ↔ AsistenciaController | `1.2: POST /asistencias/buscar-becado` · `2.2: POST /asistencias` | [cambia] |
| AsistenciaController ↔ AsistenciaService | `1.3: buscarBecado(ci)` · `2.3: registrar(actorCodigo, becaCodigo)` | [nuevo] |
| AsistenciaService ↔ Beca | `1.4: find(beca)` | [igual] |
| AsistenciaService ↔ Asistencia | `2.4: insertarSiNoExiste(becaId, fecha)` · `2.5: find(asistenciaDelDia)` | [cambia] |

#### Mockup — img 110 (rehacer)

Pantalla «Registrar asistencia» (móvil):

- **Barra superior:** título «Comedor · Asistencia» y menú de cuenta (nombre, rol, «Mi perfil», «Cerrar sesión») [cambia: antes «Personal ▾»].
- **Encabezado:** «Registrar asistencia» y «Hoy, dd/mm/aaaa» (sin «Turno almuerzo») [cambia].
- **Campo:** «CI del becado», texto, obligatorio, máx. 20, marcador «Ingrese CI...»; error «El CI es obligatorio» [cambia: sin el recuadro «Buscar por CI o escanear carnet»].
- **Botón** «Buscar» (muestra un indicador mientras busca) [igual].
- **Tarjeta del becado:** nombre completo, «CI … · carrera», convocatoria, chip de estado («Beca vigente», «Beca suspendida» o «Beca dada de baja»); sin «Foto / carnet» [cambia].
- **Si la beca es vigente y no registró hoy:** botón «✔ Registrar asistencia» [igual].
- **Aviso rojo** si la beca no está vigente («La beca está suspendida: no se puede registrar la asistencia») y **aviso** «Asistencia ya registrada hoy a las HH:mm» o «Asistencia registrada a las HH:mm»; botón «Siguiente becado» [nuevo].
- **Aviso rojo de error:** «El estudiante no figura en el padrón de becados.» y errores de conexión [nuevo].
