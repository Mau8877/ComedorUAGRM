# UT-CU06-FE-05 — Exportar la lista a PDF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-05` |
| **Nombre** | Exportar la lista a PDF |
| **Caso de uso** | CU06-FE |
| **Depende de** | `UT-CU06-FE-01`, `UT-CU06-FE-03` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (mutaciones) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: la acción `actions` del `DataTableToolbar` y el sistema de toasts (`F/components/ui/sonner.tsx`).

## Solicitud de negocio

El administrador se lleva la lista tal como la está viendo, en un PDF.

## Objetivo

Un botón exporta la lista (con la búsqueda y el filtro vigentes) a PDF y descarga el archivo.

## Alcance

- En la barra de acciones del toolbar de `ListaPriorizadaScreen`, el botón «Exportar a PDF» que usa `useExportarLista` con **los mismos** `convocatoriaId`, `carreraId` y `search` que la lista (no la página, no las filas por página).
- Mientras genera: el botón se bloquea y dice «Generando…» (evita el doble clic); al terminar llama a `descargarArchivo` con el nombre que mandó el servidor y muestra un aviso de éxito.
- Deshabilitado cuando la lista no tiene filas (sin calificadas o sin resultados).
- Errores: `ERR_PRI_03` → «No hay postulaciones para exportar»; `ERR_PRI_01` → como en UT-02; error de red o `5xx` → mensaje claro con la posibilidad de reintentar (A12). Nunca se muestra un enlace roto ni se descarga un archivo vacío.
- Sin guardar el PDF en caché de TanStack Query ni en el estado global.
- Tests: manda exactamente los filtros vigentes; durante la generación el botón está bloqueado; al terminar se descarga con el nombre del servidor; con la lista vacía el botón está deshabilitado; `ERR_PRI_03` y el error de red muestran su mensaje; la descarga ocurre una sola vez por clic.

Archivos esperados (crear): el componente del botón (`ExportarListaButton.tsx`) y su test. (Tocar): `ListaPriorizadaScreen.tsx`.

## Fuera de alcance (no tocar)

- El armado del PDF (backend, `UT-CU06-BE-07`).
- Otros formatos o envío por correo.
- Vista previa del PDF.

## Resultado esperado

El administrador exporta la lista filtrada a PDF con un clic, y los casos de error se explican solos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el backend y los datos de prueba, el PDF descargado se abre y contiene la misma información y orden que la pantalla.
