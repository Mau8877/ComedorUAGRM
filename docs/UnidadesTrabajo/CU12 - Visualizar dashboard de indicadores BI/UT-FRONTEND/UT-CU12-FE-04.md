# UT-CU12-FE-04 — Descarga del dashboard en PDF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-04` |
| **Nombre** | Descarga del dashboard en PDF |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-FE-01`, `UT-CU12-FE-03` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (mutaciones) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `UT-CU06-FE-05` y `ExportarListaButton.tsx`, el util `F/utils/descargarArchivo.ts` y el sistema de toasts (`F/components/ui/sonner.tsx`).

## Solicitud de negocio

El Directivo se lleva el dashboard tal como lo está viendo, en un PDF.

## Objetivo

Un botón descarga el PDF de la selección aplicada.

## Alcance

- `F/features/dashboard/components/DescargarDashboardButton.tsx`, en el encabezado de `DashboardScreen`, que usa `useDescargarDashboard` con **la misma selección aplicada** (gestión y convocatoria) que se está viendo, no la que se edita en los selectores.
- Mientras genera: el botón se bloquea y dice «Generando…» (evita el doble clic); al terminar llama a `descargarArchivo` con el nombre que mandó el servidor y muestra un aviso de éxito.
- Deshabilitado cuando `sinDatos` es verdadero o mientras se cargan los indicadores.
- Errores: `ERR_DAS_02` → «No hay datos para exportar»; `ERR_DAS_01` → como en UT-03; error de red o `5xx` → mensaje claro con la posibilidad de reintentar (A4). Nunca se descarga un archivo vacío.
- Sin guardar el PDF en caché ni en el estado global.
- Tests: manda exactamente la selección aplicada; durante la generación el botón está bloqueado; al terminar se descarga con el nombre del servidor; con `sinDatos` el botón está deshabilitado; `ERR_DAS_02` y el error de red muestran su mensaje; la descarga ocurre una sola vez por clic.

Archivos esperados (crear): `DescargarDashboardButton.tsx` y su test. (Tocar): `DashboardScreen.tsx`.

## Fuera de alcance (no tocar)

- El armado del PDF (backend, `UT-CU12-BE-06`).
- Otros formatos, vista previa o envío por correo.
- Imprimir los gráficos desde el navegador.

## Resultado esperado

El Directivo descarga el PDF de lo que está viendo con un clic.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con los datos de prueba, el botón descarga un PDF que abre y muestra la selección; con una gestión sin datos el botón está deshabilitado.
