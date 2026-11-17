# CU24 — Consultar historial de asistencia y estado de beca

- **Caso de uso:** [enlace](../CasosDeUso/CU24%20-%20Consultar%20historial%20de%20asistencia%20y%20estado%20de%20beca.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu24)
- **Diagramas de grado:** CU img 049 · análisis img 062 · secuencia img 132 · mockup img 145 · diseño lógico img 091 (§5.4.24, §6.2.24, §7.4.24, §7.5.24 del documento de taller)
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

Todo exige sesión de Postulante (`401` sin sesión, `403` `ERR_AUTH_07` con otro rol). El usuario sale siempre del token (`@CurrentUserId`): ningún endpoint recibe el código de una persona ni de una beca, así que solo se ve la propia (A4).

**Entrar a «Mi beca»** (web `/mi-beca`; móvil, sección «Mi beca» del shell del Estudiante)

1. `MiBecaScreen` (web) o `MiBecaVista` (móvil) → `GET /api/v1/mi-beca` (`200`) → `MiBecaController.miBeca()` → `MiBecaService.miBeca()` (`@PreAuthorize('POSTULANTE')`, `@Transactional(readOnly)`) → `MiBecaConsultaRepository.buscarBeca()`: une `usuario`, `beca`, `convocatoria`, `gestion_academica` y `adjudicacion`; trae el motivo del último movimiento `BAJA`/`SUSPENSION` y el total de asistencias en la misma consulta; prefiere la beca vigente y, si no hay, la más reciente.
2. Sin beca (A1): `404` `ERR_MBE_01`. La pantalla muestra «No cuentas con una beca» con el botón «Ir a Mis postulaciones» (CU04) y no pide el historial.
3. Con beca, la pantalla muestra el estado (vigente, suspendida o dada de baja), la convocatoria, la gestión, la fecha de adjudicación, el inicio, el total de asistencias y, si no está vigente, el motivo (A3).

**Historial de asistencia**

4. La pantalla → `GET /api/v1/mi-beca/asistencias?page&pageSize&filter[desde]&filter[hasta]&sort` (`200`, `data` array + `meta`) → `MiBecaController.asistencias()` (`ParametrosListado.validarFiltros`) → `MiBecaService.asistencias()` → `MiBecaConsultaRepository.contarAsistencias()` y `listarAsistencias()` (orden por `fecha`, por defecto la más reciente primero). Cada elemento lleva `fecha` y `horaRegistro`.
5. Sin asistencias o sin ninguna en el rango (A2): página vacía con `meta`; la pantalla muestra un mensaje informativo distinto para cada caso.
6. Errores: `filter[desde]` posterior a `filter[hasta]` → `400` `ERR_MBE_02` (la web no manda un rango invertido y lo avisa en pantalla; la app lo evita con el selector de rango); `sort` distinto de `fecha`/`-fecha` → `400` `ERR_MBE_03`; fecha mal formada o filtro desconocido → `400` `ERR_SYS_05`; `page`/`pageSize` no positivos → `400` `ERR_SYS_01`. Un error de red o `5xx` muestra el aviso con «Reintentar» (A6).

Es de solo lectura: no escribe, no se anota en la bitácora, no hay tareas en segundo plano y `POST`, `PUT`, `PATCH` y `DELETE` responden `405`. En la app el historial se carga por páginas de 20 con scroll infinito.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 132) | `MiBecaController` accede directo a `Beca`, `MovimientoBeca` y `Asistencia` (`find`, `list`) | Hay `MiBecaService` y `MiBecaConsultaRepository` entre el controller y las tablas; una sola consulta trae la beca, su motivo y el total | Controllers delgados, sin acceso a repositories ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)); el backend es uno solo para web y móvil |
| Secuencia (img 132) | `consultarEstadoBeca(postulanteId)`: el cliente manda su id | `GET /api/v1/mi-beca` sin parámetros: el usuario sale del token | El código siempre sale del token verificado (A4, [SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)); evita consultar la beca de otra persona |
| Secuencia (img 132) | `find(beca)` y `list(movimientos)` como dos mensajes y devuelve `estadoBeca(beca, movimientos)` | Un solo `buscarBeca()` con una subconsulta del último movimiento de baja o suspensión; se devuelve `motivo`, no la lista de movimientos | La pantalla solo necesita el motivo vigente |
| Secuencia (img 132) | Rama `[no figura como becado]`: `sinBeca()` | `404` `ERR_MBE_01` | Convención de errores del proyecto; el cliente lo traduce al aviso |
| Secuencia (img 132) | `listarAsistencias(becaId, rango)`: el cliente manda `becaId` | `GET /api/v1/mi-beca/asistencias` con `filter[desde]`/`filter[hasta]`; la beca se resuelve en el servidor | Mismo motivo que arriba |
| Secuencia (img 132) | `historial(asistencias)` y `mostrarHistorial(asistencias, total)` sin paginación | Respuesta paginada con `meta` (`totalItems` es el total) y orden por `fecha` | Todo listado se pagina ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)) |
| Secuencia (img 132) | Sin rechazos de validación | `ERR_MBE_02` (rango invertido) y `ERR_MBE_03` (`sort` no soportado) | Contrato de listados |
| Análisis (img 062) | `MiBecaView`, `MiBecaController`, `Beca`, `MovimientoBeca`, `Asistencia` | Web: `MiBecaScreen`; móvil: `MiBecaVista` y providers de Riverpod; backend: `MiBecaController`, `MiBecaService`, `MiBecaConsultaRepository` | Capas de la arquitectura de cada proyecto |
| Mockup (img 145) | Pantalla 1: tarjeta «Beca Comedor · Convocatoria 2026-II», chip de estado, «Adjudicada el 28/08/2026 · Posición en la lista: 5» | Tarjeta con convocatoria, gestión, chip de estado y «Adjudicada el …»; sin «Posición en la lista» | La posición viene de la preselección (puntaje) y no es parte de este CU ni de RF-19 |
| Mockup (img 145) | Resumen «13 Asistencias · 1 Faltas · 93 % Cumplimiento» | Solo «Asistencias registradas» (el total) | El sistema guarda únicamente las asistencias registradas: no tiene calendario de días de comedor, así que no puede saber qué es una falta. No se inventa |
| Mockup (img 145) | Aviso azul «El registro lo realiza el personal del comedor…» y botón «Ver historial completo» | Igual el aviso; no hay botón: el historial va debajo del resumen, paginado | Una sola pantalla; el historial ya es completo |
| Mockup (img 145) | Pantalla 2 «Historial de asistencia»: calendario mensual (asistió, falta, sin registro) y «Últimos registros» con «Almuerzo» y «Asistió»/«Falta» | Filtro «Desde/Hasta» (web) o «Filtrar por fechas» (app) y lista «fecha · hora de registro» | Sin calendario de comedor no hay «Falta» ni «Sin registro»; el diseño lógico no tiene turno (almuerzo) |
| Mockup (img 145) | Barra inferior «Mi beca · Postulación · Avisos · Perfil» | Shell del Estudiante: «Mi Menú · Mis Pedidos · Mis postulaciones · Mi beca · Perfil» (en la web, ítem «Mi beca» del menú) | Se agregó a la navegación que ya existía |
| Casos de uso (img 049) | Actor «Postulante» | Igual; el backend exige el rol `POSTULANTE` | — |
| Clases (img 090) / diseño lógico (img 091) | `Beca`, `MovimientoBeca`, `Asistencia` | Sin cambios; este CU no crea tablas ni columnas | Solo lee lo que dejan CU10 y CU23 |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 132 | Sí | Ver especificación abajo | [ ] |
| Análisis | 062 | Sí | Ver especificación abajo | [ ] |
| Mockup | 145 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | No | — | — |
| Diseño lógico BD | 091 | No | — | — |
| Casos de uso | 049 | No | — | — |

Arquitectura (img 118 a 120): sin cambios. Texto de grado: en `04_detalle_casos_uso.tex` (CU-24) agregar que sin beca el sistema responde con un aviso y no un redireccionamiento automático, que el historial es paginado y que el resumen muestra el total de asistencias registradas (no faltas ni cumplimiento).

### Especificación de cada diagrama afectado

#### Secuencia — img 132 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Postulante · `Boundary` MiBecaScreen (web) / MiBecaVista (móvil) · `Control` MiBecaController · `Control` MiBecaService · `Control` MiBecaConsultaRepository · `Entity` Beca · `Entity` MovimientoBeca · `Entity` Asistencia

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → MiBecaScreen | `verEstadoBeca()` | [igual] |
| 2 | MiBecaScreen → MiBecaController | `GET /api/v1/mi-beca` → `200` | [cambia] |
| 3 | MiBecaController → MiBecaService | `miBeca(usuarioCodigo)` | [cambia] |
| 4 | MiBecaService → MiBecaConsultaRepository | `buscarBeca(usuarioCodigo)` | [nuevo] |
| 5 | MiBecaConsultaRepository → Beca | `find(beca)` (une convocatoria, gestión y adjudicación) | [cambia] |
| 6 | MiBecaConsultaRepository → MovimientoBeca | `ultimoMovimiento(baja o suspensión)` → motivo | [cambia] |
| alt | [figura en el padrón de becados] | `MiBecaResponse(estado, motivo, totalAsistencias, …)` → `200` → `mostrarEstadoBeca(estado, motivo)` | [cambia] |
| alt | [no figura como becado (A1)] | `404` `ERR_MBE_01` → `mostrarAvisoSinBeca()` con «Ir a Mis postulaciones» | [cambia] |
| 7 | Postulante → MiBecaScreen | `verHistorialAsistencia(rango)` (bloque `opt` [consultar historial]) | [igual] |
| 8 | MiBecaScreen → MiBecaController | `GET /api/v1/mi-beca/asistencias?page&pageSize&filter[desde]&filter[hasta]&sort` → `200` | [cambia] |
| 9 | MiBecaController → MiBecaService | `asistencias(usuarioCodigo, page, pageSize, desde, hasta, sort)`; con `desde` > `hasta` → `400` `ERR_MBE_02`; `sort` inválido → `400` `ERR_MBE_03` | [cambia] |
| 10 | MiBecaService → MiBecaConsultaRepository | `contarAsistencias(becaId, desde, hasta)` y `listarAsistencias(becaId, desde, hasta, descendente, límite, desplazamiento)` | [nuevo] |
| 11 | MiBecaConsultaRepository → Asistencia | `list(asistencias)` | [igual] |
| 12 | MiBecaScreen → Postulante | `mostrarHistorial(asistencias, meta)`; si está vacío, mensaje informativo (A2) | [cambia] |

Alternativas: la beca suspendida o dada de baja se muestra igual, con su motivo (A3), y el historial sigue disponible.

#### Análisis (comunicación) — img 062 (rehacer completo)

**Clases:** `Actor` Postulante · `boundary` MiBecaScreen / MiBecaVista · `control` MiBecaController · `control` MiBecaService · `entity` Beca · `entity` MovimientoBeca · `entity` Asistencia

| Enlace | Mensajes numerados | Estado |
| --- | --- | --- |
| Postulante ↔ MiBecaScreen | `1: verEstadoBeca()` · `2: verHistorialAsistencia(rango)` | [igual] |
| MiBecaScreen ↔ MiBecaController | `1.1: GET /mi-beca` · `2.1: GET /mi-beca/asistencias` | [cambia] |
| MiBecaController ↔ MiBecaService | `1.2: miBeca(usuarioCodigo)` · `2.2: asistencias(usuarioCodigo, rango, página)` | [nuevo] |
| MiBecaService ↔ Beca | `1.3: find(beca)` | [igual] |
| MiBecaService ↔ MovimientoBeca | `1.4: list(movimientos)` (solo el último de baja o suspensión) | [cambia] |
| MiBecaService ↔ Asistencia | `2.3: list(asistencias)` | [igual] |

#### Mockup — img 145 (rehacer)

**Pantalla 1 «Mi beca» (móvil, sección del shell del Estudiante; en la web, ítem «Mi beca» del menú):**

- **Barra superior:** «ComedorU» y menú de cuenta [cambia: el mockup dice «Mi beca»].
- **Tarjeta de estado:** «{convocatoria} · Gestión {gestión}», «Estado de la beca» con chip («Vigente», «Suspendida» o «Dada de baja»), «Adjudicada el {dd/mm/aaaa}» (o «Inicio de la beca: …» sin adjudicación), `Motivo: …` solo si no está vigente [cambia: sin «Posición en la lista»].
- **Resumen:** número grande con «Asistencias registradas» [cambia: sin «Faltas» ni «Cumplimiento»].
- **Aviso informativo:** «El registro lo realiza el personal del comedor. Si detecta un error, comuníquese con la DUBSS.» [igual].
- **Sin botón «Ver historial completo»:** el historial está debajo [cambia].

**Historial de asistencia (debajo, en la misma pantalla):**

- **Título** «Historial de asistencia» [cambia: antes pantalla aparte con flecha de volver].
- **Filtro:** botón «Filtrar por fechas» (selector de rango; muestra «dd/mm/aaaa - dd/mm/aaaa») y «Quitar filtro» (web: dos campos «Desde» y «Hasta» y «Refrescar») [nuevo].
- **Lista:** una fila por asistencia, «dd/mm/aaaa» y la hora «HH:mm» (web: tabla «Fecha» · «Hora de registro», con orden por fecha y paginación; móvil: scroll infinito) [cambia: sin calendario mensual, sin «Almuerzo», «Asistió» ni «Falta»].
- **Estado vacío:** «Todavía no hay asistencias registradas» o «No hay asistencias en ese rango de fechas» [nuevo].

**Estado «sin beca»:** «No cuentas con una beca», «No figuras en el padrón de becados. Revisa el estado de tu postulación.» y botón «Ir a Mis postulaciones» [nuevo].

**Errores:** aviso rojo con el mensaje y «Reintentar» en errores de conexión o del servidor [nuevo]. En la web, el aviso «La fecha «desde» no puede ser posterior a «hasta»» bajo el filtro.
