# Unidades de trabajo — CU06-FE

**Caso de uso:** Pantalla web «Lista priorizada» del Administrador DUBSS: elegir una convocatoria, ver el resumen, recorrer la lista paginada ordenada por puntaje (con búsqueda y filtro por carrera), ver el detalle de una postulación y exportar la lista a PDF.
**Fuente CU:** [`CU06 - Visualizar lista priorizada de postulantes`](../../../CasosDeUso/CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)
**Diagramas de grado:** los del CU-06 en la documentación de grado (contexto `065`, análisis `078`, secuencia `093`). **La maqueta `106` no se sigue**: manda el CU y el sistema de diseño ya hecho del panel.
**Recurso HTTP / contrato:** ruta `/lista-priorizada` (con sesión, solo `Administrador DUBSS`); consume `GET /lista-priorizada/convocatorias`, `GET /lista-priorizada/resumen`, `GET /lista-priorizada` y `POST /lista-priorizada/exportaciones` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Feature nueva `features/priorizacion/`** con la forma de [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature) (`api/`, `components/`, `screens/`, `types/`, `tests/`, `index.ts`). Una sola pantalla, `ListaPriorizadaScreen`, dentro de `AdminLayout`.
- **Tabla server-side con el paquete existente.** La lista usa `useDataTable`, `DataTable`, `DataCards` (para ancho chico), `DataTableToolbar` y `DataTablePagination` de `components/ui/tanstack-table/`: **no se arma una tabla a mano** ([`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md)). `page`/`pageSize` viven en `useState` y se mandan al backend; filas por página 10, 20 o 50 (20 por defecto). **Sin ordenamiento en el cliente:** `enableSorting` queda apagado en todas las columnas, porque el orden es fijo (el backend rechaza `sort`). Una columna lleva `meta.grow` (la del nombre) y `cardTitle`.
- **Contrato verificado contra el backend** (regla de la tabla): `accessorKey` con los nombres exactos del `Response` (`posicion`, `nombres`, `apellidos`, `ci`, `carrera.nombre`, `puntaje`, `fechaCalculo`); `search` y `filter[carreraId]`/`filter[convocatoriaId]` son los que el backend acepta y ningún otro; `meta` de paginación siempre presente.
- **Convocatoria obligatoria.** Primero se elige convocatoria con un `Select` (la más reciente viene preseleccionada); mientras no hay convocatoria elegida no se pide la lista. Con **ninguna** convocatoria → mensaje «Todavía no hay convocatorias» (A9). Una convocatoria que el backend responde `404` (`ERR_PRI_01`) → aviso y se vuelve al selector (A10).
- **Resumen arriba de la tabla:** «N calificadas · M pendientes de precalificación». Lista vacía → mensaje «No hay postulaciones calificadas en esta convocatoria» y el resumen de pendientes (A1, A4); búsqueda o filtro sin resultados → «Sin resultados» con «Limpiar» (A2).
- **Búsqueda con retardo.** El texto buscado usa el `debounce` de `src/utils/` para no pedir en cada tecla; cambiar la búsqueda, la carrera, la convocatoria o las filas por página vuelve a la página 1. La posición que muestra cada fila es la que manda el backend (no se calcula en el cliente).
- **Filtro de carreras.** Necesita la lista de carreras. Hoy `useCarreras` vive en `features/auth/api/` (lo usa el registro) y esta pantalla es el **segundo consumidor**: por la regla de `shared/api/` ([`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md#endpoints-usados-por-más-de-una-feature)) el endpoint y su hook se **mueven** a `src/shared/api/` en `UT-CU06-FE-01`, y `features/auth/` lo importa desde ahí. Es la única razón de tocar `features/auth/`.
- **Detalle de solo lectura.** «Ver» abre un diálogo con los datos de la fila (posición, nombre, CI, carrera, puntaje, fecha de cálculo y estado), sin otra llamada al backend. Cuando exista el CU-05 se le agrega el paso a la auditoría; **este CU no la construye**. La parte de A5 sobre una postulación que ya dejó de estar «calificada» llega con ese CU; aquí la lista se refresca al volver a consultar.
- **Exportar a PDF.** El botón «Exportar a PDF» manda los **mismos** filtros y búsqueda que la lista (nunca la página), muestra «Generando…» y bloquea el botón mientras tarda, y al terminar descarga el archivo con el nombre que manda el servidor (`Content-Disposition`). Es una respuesta **binaria**, fuera del sobre: `apiClient` no puede desempaquetarla como un sobre. `UT-CU06-FE-01` hace que las peticiones con `responseType: 'blob'` se salten el desempaquetado y que un error devuelto como blob se lea como el sobre de error (`ERR_PRI_03` → «No hay postulaciones para exportar»; error de red o `5xx` → mensaje con reintento, A12). Sin lista no se puede exportar: el botón queda deshabilitado.
- **El nombre del archivo.** La API está en otro origen (`localhost:8080`), así que el navegador solo deja leer `Content-Disposition` si el backend lo expone: se agregó `setExposedHeaders` en `SecurityConfig`. Sin eso, la descarga cae en el nombre genérico `lista-priorizada.pdf`.
- **Sin tokens en el cliente.** La sesión sigue en cookies `HttpOnly`; nada se guarda en `localStorage` ni en stores. Un `401` lo resuelve el flujo de sesión existente (renovar o ir a `/login`, A6); un `403` lleva al usuario a `/panel`.
- **Acceso.** La ruta es `/_authenticated/lista-priorizada`; el ítem «Lista priorizada» se agrega a `adminNavItems.ts` (ícono de `lucide-react`). Un usuario que no es Administrador DUBSS no ve el ítem ni la ruta (la ruta lo redirige).
- **Estado de UI.** Todo es estado local de la pantalla (`useState`) y de TanStack Query; no se crea un store de Zustand. Las `queryKey` siguen `['priorizacion', 'convocatorias' | 'resumen' | 'list', params]` con una factory en `api/keys.ts`.
- **Solo web.** Este CU no tiene versión móvil, así que no hay `UT-MOBILE`.
- **Textos** en español neutro y **sin variables de entorno nuevas**.

## Contrato de datos / recurso HTTP

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/lista-priorizada` | Con sesión, `Administrador DUBSS` | Selector de convocatoria, resumen, barra de búsqueda y carrera, tabla paginada, exportación |

**Tipos** (los de las respuestas del backend): `ConvocatoriaPriorizable` (`codigo`, `nombre`, `gestion`, `estado`, `fechaApertura`, `fechaCierre`), `ResumenListaPriorizada` (`totalCalificadas`, `totalPendientes`), `PostulacionPriorizada` (`codigo`, `posicion`, `nombres`, `apellidos`, `ci`, `carrera`, `puntaje`, `fechaCalculo`, `estado`) y `FiltrosLista` (`convocatoriaId`, `carreraId?`, `search?`).

**Errores del backend → mensajes:** `ERR_PRI_01` → «La convocatoria ya no existe»; `ERR_PRI_02` no debería ocurrir (el cliente no manda `sort` ni otros filtros): se muestra el mensaje del backend; `ERR_PRI_03` → «No hay postulaciones para exportar»; error de red o `5xx` → mensaje de conexión con «Reintentar» (A8).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU06-FE-01](./UT-CU06-FE-01.md) | Tipos, claves y llamadas a la API; descarga de archivos; `useCarreras` a `shared/` | — |
| [UT-CU06-FE-02](./UT-CU06-FE-02.md) | Ruta, menú y pantalla con selector de convocatoria y resumen | 01 |
| [UT-CU06-FE-03](./UT-CU06-FE-03.md) | Tabla de la lista: paginación, búsqueda y filtro por carrera | 01, 02 |
| [UT-CU06-FE-04](./UT-CU06-FE-04.md) | Detalle de solo lectura de una postulación | 03 |
| [UT-CU06-FE-05](./UT-CU06-FE-05.md) | Exportar la lista a PDF | 01, 03 |

**Cadena:** `01 → 02 → 03 → 04` y `03 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Paso 1 | Entrada desde el menú | 02 |
| Pasos 2 y 3 | Elegir convocatoria | 02 |
| Pasos 4 y 5 | Lista ordenada con posición y resumen | 02, 03 |
| Paso 6 | Paginar, buscar, filtrar | 03 |
| Paso 7 | Exportar a PDF | 05 |
| Paso 8 | Detalle | 04 |
| A1, A4 | Lista vacía y pendientes | 02, 03 |
| A2 | Búsqueda o filtro sin resultados | 03 |
| A6, A7 | `401` y `403` | 02 |
| A8 | Error de conexión con reintento | 02, 03, 05 |
| A9, A10 | Sin convocatorias; convocatoria inexistente | 02 |
| A11 | Volver a consultar muestra los puntajes nuevos | 03 |
| A12 | Exportar con lista vacía o con falla | 05 |
| CA01–CA08 | Lista, paginación, búsqueda y filtro | 02, 03 |
| CA11–CA13 | `403`, `401`, `404` | 02 |
| CA15 | Detalle de una postulación | 04 |
| CA16–CA19 | PDF completo con los mismos filtros y sin lista vacía | 05 |

## Follow-ups (no se implementan en estas UT)

- Paso a la auditoría desde el detalle (CU-05) y la actualización de una postulación que ya no esté «calificada» (A5).
- Seleccionar finalistas desde la lista (CU-05).

## Qué NO incluyen estas UT

- El backend y los datos de prueba (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Una versión móvil.
- Ordenar por otras columnas: el orden es fijo.
- Otras pantallas de convocatorias o postulaciones (CU-13 y CU-02).
