# Unidades de trabajo — CU13-FE

**Caso de uso:** Pantallas web de la gestión de convocatorias de la Beca Comedor y de las gestiones académicas para el Administrador DUBSS: lista de convocatorias con búsqueda y filtros, alta y edición con documentos requeridos, abrir, cerrar y eliminar, y una pantalla para mantener las gestiones académicas.
**Fuente CU:** [`CU13 - Gestionar convocatoria y gestión académica`](../../../CasosDeUso/CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md)
**Diagramas de grado:** `images/055` (análisis: `ConvocatoriaView`), `125` (secuencia) y `138` (mockup). El mockup es la referencia del contenido de la pantalla, pero manda el sistema de diseño ya hecho del panel (header, sidebar y contenedor) y hay diferencias deliberadas con él (ver decisiones).
**Recurso HTTP / contrato:** Rutas `/convocatorias` y `/gestiones-academicas`; consume `/api/v1/convocatorias`, `/api/v1/gestiones-academicas` y `GET /api/v1/tipos-documento` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `TESTING_FRONTEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`); la feature nueva es `features/convocatorias/`, con la forma de [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature). Es el mismo módulo que en el backend: las gestiones académicas viven dentro de él.

- **Dos pantallas, una feature.** `ConvocatoriasScreen` (`/convocatorias`) y `GestionesAcademicasScreen` (`/gestiones-academicas`). Cada una tiene su entrada en el sidebar del Administrador.
- **Sin store.** Los datos vienen de TanStack Query; la página, el tamaño, la búsqueda, los filtros y el orden viven en `useState` de la pantalla; los modales, en estado local (`ESTADO_GLOBAL_FRONTEND.md`).
- **Tabla 100% server-side** con el paquete `components/ui/tanstack-table` (`DataTable` para la vista ancha, `DataCards` para la angosta, `DataTableToolbar`, `DataTablePagination` y `RowActionButton`); no se arma una tabla a mano. Tamaño de página 10/20/50 (inicial 20).
- **Guard por rol.** Las dos rutas usan `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')` (como `actas.tsx`). Es solo la capa de interfaz: el backend niega con `403` `ERR_AUTH_07`.
- **El estado solo cambia con acciones.** El mockup dibuja un selector de estado dentro del formulario; **no se implementa**. El estado se muestra como etiqueta y cambia con «Abrir» (convocatorias en `BORRADOR`) y «Cerrar» (en `ABIERTA`). _Diferencia con el mockup, a registrar en `docs/Implementacion/`._
- **Acciones según el estado** (solo ayuda de interfaz; el backend valida): `BORRADOR` → Editar, Abrir, Eliminar; `ABIERTA` → Editar, Cerrar; `CERRADA` y `PUBLICADA` → solo ver. Con `tienePostulaciones`, el formulario muestra deshabilitados la fecha de apertura y la lista de documentos con el aviso de por qué (A6); el backend responde `409` `ERR_CV_09` igual si se intenta.
- **Formulario en un modal** (`components/ui/modal-layout`), con el estilo de `CrearUsuarioModal`: nombre, gestión (selector que carga `GET /gestiones-academicas`), fecha de apertura, fecha de cierre, cupos, puntaje mínimo, cantidad de finalistas y la lista de documentos (selector de tipo + casilla «Obligatorio», con agregar y quitar). Cupos, puntaje mínimo y cantidad de finalistas son **opcionales** (la DUBSS no los define aún); un campo vacío se envía como `null`.
- **Fechas.** Se usan campos de fecha del navegador (`yyyy-MM-dd`) y se muestran `dd/mm/aaaa`; el backend las interpreta en hora de Bolivia. Nunca se convierten con `new Date(...)` a UTC (correrían un día).
- **`cantidadFinalistas` solo la ve el Administrador:** está en el formulario y la tabla del panel de administración; ninguna pantalla de Postulante la muestra.
- **Errores del backend → mensajes** (el cliente solo los muestra): `ERR_SYS_01`, `ERR_CV_04`, `ERR_CV_05` y `ERR_CV_06` se muestran como error de formulario (A2; `ERR_CV_04` junto a la fecha de cierre); `ERR_CV_07` («ya hay una convocatoria abierta en esa gestión») como aviso al abrir (A1); `ERR_CV_08`, `ERR_CV_09` y `ERR_CV_10` como aviso sin cambiar nada y refrescando la lista (A6, A7, A9); `ERR_CV_01`, `ERR_CV_02` y `ERR_CV_03` («ya no existe»), refrescando la lista (A8); `ERR_CV_12` en el campo etiqueta y `ERR_CV_13` junto a la fecha de fin; `ERR_AUTH_07` lleva al módulo del rol (A10); error de red o `5xx` → mensaje de conexión con opción de reintentar. Un `401` lo resuelve el flujo de sesión ya existente.
- **Confirmaciones.** Abrir, cerrar y eliminar piden confirmación (`modal-destructive` para cerrar y eliminar).
- **Textos** en español neutro; errores de formulario **inline debajo del campo** y solo si el campo fue tocado (`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`).
- **Sin variables de entorno nuevas.**

## Contrato de datos / recurso HTTP

**Rutas nuevas**

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/convocatorias` | Solo `ADMINISTRADOR_DUBSS` | Lista de convocatorias y sus acciones |
| `/gestiones-academicas` | Solo `ADMINISTRADOR_DUBSS` | Lista de gestiones académicas y su alta y edición |

**Verificación contra el backend** (`TANSTACK_TABLE_FRONTEND.md`):

| Qué | Convocatorias | Gestiones académicas |
|-----|---------------|----------------------|
| Campos de la fila (`accessorKey`) | `codigo`, `nombre`, `gestion.etiqueta`, `fechaApertura`, `fechaCierre`, `cupos`, `puntajeMinimo`, `cantidadFinalistas`, `estado` | `codigo`, `etiqueta`, `fechaInicio`, `fechaFin` |
| `search` | `nombre` | `etiqueta` |
| `filter[...]` | `filter[estado]` (`BORRADOR`, `ABIERTA`, `CERRADA`, `PUBLICADA`) y `filter[gestion]` (código de la gestión) | ninguno |
| `sort` (un solo campo, `campo` o `-campo`) | `nombre`, `fechaApertura`, `fechaCierre`, `estado`, `creadoEn`; por defecto `-creadoEn` | `etiqueta`, `fechaInicio`, `fechaFin`; por defecto `-fechaInicio` |
| `meta` | `page`, `pageSize`, `totalItems`, `totalPages` | igual |

**Formularios** (schemas de Zod, mismas reglas que el backend; el servidor vuelve a validar): `gestionSchema` (`etiqueta` obligatoria hasta 50 caracteres, `fechaInicio` y `fechaFin` obligatorias con el fin posterior al inicio) y `convocatoriaSchema` (`nombre` obligatorio hasta 150, `gestionCodigo` obligatorio, `fechaApertura` y `fechaCierre` obligatorias con el cierre posterior a la apertura, `cupos` y `cantidadFinalistas` enteros positivos o vacíos, `puntajeMinimo` entre 0 y 100 con hasta dos decimales o vacío, `documentos` con al menos uno, sin tipos repetidos y con al menos uno obligatorio).

**Respuestas:** `Convocatoria` (resumen), `ConvocatoriaDetalle` (con `documentos` y `tienePostulaciones`), `GestionAcademica` y `TipoDocumento`, con los campos del contrato del backend.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU13-FE-01](./UT-CU13-FE-01.md) | Tipos y schemas de Zod | — |
| [UT-CU13-FE-02](./UT-CU13-FE-02.md) | Llamadas a la API, hooks de TanStack Query y mapeo de errores | 01 |
| [UT-CU13-FE-03](./UT-CU13-FE-03.md) | Entradas del sidebar del Administrador | — |
| [UT-CU13-FE-04](./UT-CU13-FE-04.md) | Pantalla de convocatorias: lista, búsqueda y filtros, y su ruta | 02, 03 |
| [UT-CU13-FE-05](./UT-CU13-FE-05.md) | Crear y editar una convocatoria (formulario con documentos) | 01, 02, 04 |
| [UT-CU13-FE-06](./UT-CU13-FE-06.md) | Abrir, cerrar y eliminar una convocatoria | 02, 04 |
| [UT-CU13-FE-07](./UT-CU13-FE-07.md) | Gestiones académicas: lista, alta y edición, y su ruta | 01, 02, 03 |

**Cadena:** `01 → 02`; `02, 03 → 04`; `01, 02, 04 → 05`; `02, 04 → 06`; `01, 02, 03 → 07`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1–2 | Entrada del sidebar y lista con gestión, fechas, cupos y estado | 02, 03, 04 |
| Flujo principal, pasos 3–8 | Formulario de alta y edición, confirmación y lista actualizada | 01, 02, 05 |
| Gestiones académicas | Lista, alta y edición | 01, 02, 03, 07 |
| A1 | Otra convocatoria abierta en la gestión: aviso | 06 |
| A2 | Errores de formulario inline | 01, 05, 07 |
| A3, A4 | Abrir y cerrar con confirmación | 06 |
| A6, A7 | Postulaciones: campos deshabilitados y aviso; sin eliminar | 05, 06 |
| A8, A9 | Inexistente o estado inválido: aviso y lista refrescada | 02, 05, 06 |
| A10 | Sin el rol: fuera de las rutas; error de conexión con reintento | 03, 04, 05, 06, 07 |
| CA01–CA04 | Alta, fechas, documentos y rangos validados en el formulario | 01, 05 |
| CA05, CA06 | Una abierta por gestión y abrir | 06 |
| CA07 | Cerrar | 06 |
| CA09–CA11 | Con postulaciones y estados que no se editan | 04, 05, 06 |
| CA12 | Sin sesión o sin el rol | 03, 04 |
| CA15, CA16 | Gestiones: alta y errores | 01, 07 |
| CA17 | Eliminar una en borrador | 06 |

## Follow-ups (no se implementan en estas UT)

- Mostrar en el listado el número de postulaciones de cada convocatoria (el backend hoy solo devuelve si tiene alguna).
- Selector de estado dentro del formulario, como dibuja el mockup (hoy el estado cambia solo con las acciones).

## Qué NO incluyen estas UT

- La pantalla de Postulante con las convocatorias abiertas (CU02) y cualquier pantalla móvil: este CU es solo web.
- El catálogo de tipos de documento: solo se consulta para elegirlos.
- La eliminación de gestiones académicas.
