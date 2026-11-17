# CU25 — Consultar bitácora de acciones

- **Caso de uso:** [enlace](../CasosDeUso/CU25%20-%20Consultar%20bit%C3%A1cora%20de%20acciones.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu25)
- **Diagramas de grado:** CU img 076 · análisis img 089 · secuencia img 104 · mockup img 117
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

1. `BitacoraScreen` (ruta `/bitacora`, solo `ADMINISTRADOR_DUBSS`) → `GET /api/v1/bitacora/acciones` (`200`) para llenar el filtro de acciones.
2. `BitacoraScreen` → `GET /api/v1/bitacora?page=1&pageSize=20&sort=-fecha` (`200`, `data` array + `meta`).
3. `BitacoraController.listar()` valida los `filter[...]` permitidos y el formato de las fechas (`400` `ERR_SYS_05`) → `BitacoraConsultaService.listar()`.
4. `BitacoraConsultaService.listar()` comprueba el rol (`@PreAuthorize`, `403` `ERR_AUTH_07`), el rango de fechas (`400` `ERR_BIT_02`) y el `sort` (`400` `ERR_BIT_01`); pasa los días a instantes en `America/La_Paz` → `BitacoraRepository.buscar()`.
5. `BitacoraRepository.buscar()` aplica `BitacoraSpecs.filtrar()` (usuario por nombres/apellidos/correo, acción exacta, fechas) y trae el usuario junto con la página.
6. El controller devuelve `BitacoraResponse` (sin `entidadId`) y `meta`; la pantalla muestra la tabla o «Sin resultados».
7. Opcional: el Administrador cambia un filtro, el orden o la página y se repiten los pasos 2 a 6.

No se escribe nada en la bitácora al consultar y no hay tareas en segundo plano.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 104) | `BitacoraController` consulta directo a `Bitacora` | Hay `BitacoraConsultaService` y `BitacoraRepository` entre los dos | Controllers delgados: sin lógica ni acceso a repositorios ([ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)) |
| Secuencia (img 104) | Una sola consulta al abrir la pantalla | Además pide `GET /bitacora/acciones` para el filtro de acciones | El selector se llena con las acciones que ya figuran en la bitácora |
| Secuencia (img 104) | `filtrarAcciones(usuario, fecha, accion)` con «fecha» | Dos filtros: `filter[desde]` y `filter[hasta]` | RF-20 pide rango de fechas |
| Mockup (img 117) | Campo «Usuario...», «Acción: Todas», «Desde», «Hasta» y botón «Buscar» | Sin botón «Buscar»: la búsqueda se aplica con retardo y los demás filtros al cambiar; agrega «Refrescar» | Mismo comportamiento que los demás listados del panel |
| Mockup (img 117) | Entidad como `adjudicacion #512` | Solo `entidad` (el nombre), sin `#id` | El id interno nunca se expone ([PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)) |
| Mockup (img 117) | Usuario como `admin.dubss` | Nombres y apellidos, o «Sistema» si lo hizo el sistema | La cuenta no tiene un nombre de usuario; se identifica por nombre y correo |
| Mockup (img 117) | Las acciones con nombres como `ADJUDICAR`, `AUDITAR`, `BAJA_BECA` | Los valores reales de `BitacoraAcciones` (`ADJUDICACION_BECA`, `AUDITORIA_EXPEDIENTE`...) | El mockup usa nombres ilustrativos; `BAJA_BECA` aún no existe (B10) |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 104 | Sí | Ver especificación abajo | [ ] |
| Análisis | 089 | Sí | Agregar `BitacoraConsultaService` y `BitacoraRepository` entre `BitacoraController` y `Bitacora` | [ ] |
| Mockup | 117 | Sí | Quitar «Buscar», agregar «Refrescar», columnas sin `#id`, acciones con sus nombres reales | [ ] |
| Clases / dominio | 090 | No | — | — |
| Diseño lógico BD | 091 | No | — (no hay migración) | — |
| Casos de uso | 076 | No | — | — |

### Especificación de cada diagrama afectado

#### Secuencia — img 104 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador DUBSS · `Boundary` BitacoraScreen · `Control` BitacoraController · `Control` BitacoraConsultaService · `Control` BitacoraRepository · `Entity` Bitacora

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador DUBSS → BitacoraScreen | `consultarBitacora()` | [igual] |
| 2 | BitacoraScreen → BitacoraController | `GET /api/v1/bitacora/acciones` → `200` | [nuevo] |
| 3 | BitacoraController → BitacoraConsultaService | `acciones()` | [nuevo] |
| 4 | BitacoraConsultaService → BitacoraRepository | `accionesRegistradas()` | [nuevo] |
| 5 | BitacoraScreen → BitacoraController | `GET /api/v1/bitacora?page&pageSize&sort` → `200` | [cambia] |
| 6 | BitacoraController → BitacoraConsultaService | `listar(page, pageSize, search, accion, desde, hasta, sort)` | [cambia] |
| 7 | BitacoraConsultaService → BitacoraRepository | `buscar(patron, accion, desde, hasta, pageable)` | [cambia] |
| 8 | BitacoraRepository → Bitacora | `list(acciones)` | [igual] |
| 9 | BitacoraRepository → BitacoraConsultaService → BitacoraController | `Page<Bitacora>` → `PaginaBitacora(datos, meta)` | [cambia] |
| 10 | BitacoraScreen → Administrador DUBSS | `mostrarAcciones(acciones)` | [igual] |

Bloque `opt` «El Administrador filtra las acciones»: repite los mensajes 5 a 10 con `filtrarAcciones(usuario, desde, hasta, accion)` [cambia]. Bloque `alt` de error: `desde` posterior a `hasta` → `400` `ERR_BIT_02`; `sort` no soportado → `400` `ERR_BIT_01`; sin rol → `403` `ERR_AUTH_07` [nuevo].

#### Análisis — img 089 (parchear)

Clases: `Administrador DUBSS` (actor), `BitacoraView` (`boundary`, en el código `BitacoraScreen`) [cambia de nombre], `BitacoraController` (`control`) [igual], `BitacoraConsultaService` (`control`) [nuevo], `BitacoraRepository` (`control`) [nuevo], `Bitacora` (`entity`) [igual].
Enlaces: Administrador–BitacoraView, BitacoraView–BitacoraController, BitacoraController–BitacoraConsultaService [nuevo], BitacoraConsultaService–BitacoraRepository [nuevo], BitacoraRepository–Bitacora [cambia].

#### Mockup — img 117 (parchear)

Pantalla «Bitácora del sistema» · subtítulo «Registro de acciones (solo lectura)» [igual].
Controles: campo de texto «Usuario (nombre o correo)...» [cambia] · desplegable «Acción: todas» (con las acciones registradas) [cambia] · fechas «Desde» y «Hasta» [cambia: tipo fecha, sin valor por defecto] · botón «Refrescar» [nuevo] · botón «Buscar» [se quita] · botón «Limpiar filtros» (aparece con algún filtro) [nuevo].
Columnas: «Fecha y hora» (ordenable) · «Usuario» · «Acción» (ordenable) · «Entidad» (ordenable, sin `#id`) · «IP» [cambia].
Estado vacío: «Sin resultados». Error: mensaje con «Reintentar». Rango invertido: «La fecha «desde» no puede ser posterior a «hasta»».
