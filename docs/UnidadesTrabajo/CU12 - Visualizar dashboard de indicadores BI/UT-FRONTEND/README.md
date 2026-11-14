# Unidades de trabajo — CU12-FE

**Caso de uso:** Pantalla «Dashboard de indicadores» del Directivo (indicadores, gráficos por mes, filtros por gestión y convocatoria y descarga en PDF), con el layout propio del rol Directivo.
**Fuente CU:** [`CU12 - Visualizar dashboard de indicadores BI`](../../../CasosDeUso/CU12%20-%20Visualizar%20dashboard%20de%20indicadores%20BI.md)
**Diagramas de grado:** CU-12 (maqueta `112`, análisis `084` y secuencia `099`).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Feature nueva `features/dashboard/`** con una pantalla, `DashboardScreen`, y sus componentes (`TarjetaIndicador`, `FiltrosDashboard`, `GraficoPostulacionesPorMes`, `GraficoAsistenciaPorMes`, `DescargarDashboardButton`).
- **Layout propio del rol Directivo:** `F/layouts/directivo/` (`DirectivoLayout`, `directivoNavItems`, `directivoTypes`, `index.ts`), con la misma forma que `admin/`, y su export en `F/layouts/index.ts`. Mapeo del rol: `DIRECTIVO` → `DirectivoLayout`. Un único ítem de menú, «Dashboard». La opción «Reportes» del mockup **no** se incluye.
- **Ruta:** `F/routes/_authenticated/dashboard.tsx`, con `beforeLoad: exigirRol('DIRECTIVO')`, igual que `publicar-resultados.tsx`. El backend sigue siendo quien niega de verdad (`403` `ERR_AUTH_07`).
- **Destino al entrar:** `rutaInicioPorRol('DIRECTIVO')` pasa de `/panel` a `/dashboard` (el tipo de retorno se amplía y se actualizan sus tests).
- **Gráficos:** el componente `chart` de shadcn (basado en `recharts`), agregado con `pnpm dlx shadcn@latest add chart` (rule de Tailwind/shadcn: no se edita a mano su salida). Barras para postulaciones por mes y línea para la asistencia, con los colores de tema (`primary`, `accent`); nunca valores arbitrarios.
- **Filtros:** selector de gestión (por defecto la `vigente` del catálogo), selector de convocatoria («Todas» por defecto, solo las de la gestión elegida) y botón «Aplicar», como el mockup. Cambiar la gestión reinicia la convocatoria a «Todas». Son estado local de la pantalla (`useState`); no se usa Zustand ni TanStack Form (no hay envío de datos).
- **Estados de la pantalla:** cargando (esqueletos), error (mensaje de `getErrorMessage`), **sin datos** (A1: mensaje informativo y ceros), **lote sin ejecutar** (A2: aviso «Aún no se calculan indicadores» si `actualizadoEn` es nulo; si no, «Actualizado el …»), asistencia **«Sin datos»** mientras llegue `null`. Con el comentario `// TODO CU-10: mostrar la asistencia real cuando el backend la entregue`.
- **Descarga PDF:** botón «Descargar PDF» con el mismo patrón del CU-06 (`ExportarListaButton`, `descargarArchivo`): se bloquea mientras genera, deshabilitado si `sinDatos`, y manda exactamente la selección aplicada. No se guarda en caché de TanStack Query.
- **Keys:** `dashboardKeys` (`['dashboard', 'filtros']`, `['dashboard', 'indicadores', {gestion, convocatoria}]`). Es solo lectura: no hay mutaciones que invaliden.
- **Solo web:** no hay mobile (decisión del CU12).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU12-FE-01](./UT-CU12-FE-01.md) | API: hooks, tipos, keys y mensajes de error | UT-CU12-BE-07 |
| [UT-CU12-FE-02](./UT-CU12-FE-02.md) | Layout y ruta del Directivo | — |
| [UT-CU12-FE-03](./UT-CU12-FE-03.md) | Pantalla del dashboard: filtros, tarjetas y gráficos | 01, 02 |
| [UT-CU12-FE-04](./UT-CU12-FE-04.md) | Descarga del dashboard en PDF | 01, 03 |

**Cadena:** `01, 02 → 03 → 04`.

## Alcance de cada UT

- **01:** tipos, `dashboardKeys`, funciones de API (filtros, indicadores, PDF), `useFiltrosDashboard`, `useIndicadores`, `useDescargarDashboard` y `mapearErrorDashboard` (`ERR_DAS_01`, `ERR_DAS_02`).
- **02:** `DirectivoLayout`, su menú, la ruta `/dashboard` con `exigirRol('DIRECTIVO')` y el destino del rol Directivo.
- **03:** `DashboardScreen`, filtros con «Aplicar», cuatro tarjetas, dos gráficos y los estados (cargando, error, sin datos, lote sin ejecutar, asistencia sin datos).
- **04:** el botón «Descargar PDF».

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Paso 1 (opción «Dashboard de indicadores») | 02, 03 |
| Pasos 2–3 (indicadores de la gestión vigente y fecha de actualización) | 01, 03 |
| Pasos 4–5 (filtrar y actualizar) | 01, 03 |
| Paso 6, A4 (descargar PDF) | 04 |
| A1 (sin datos), A2 (lote sin ejecutar) | 03 |
| A3 (`ERR_DAS_01`) | 01, 03 |
| `401` lleva al login por el cliente HTTP; `403` la ruta exige el rol | 02 |
| CA01–CA05, CA08, CA10–CA12 | 01–04 |

## Follow-ups (no se implementan en estas UT)

- **Asistencia real (CU-10):** mostrar el promedio y la serie cuando el backend los entregue.
- **Proyecciones y tendencias:** descartadas por el taller.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- La opción «Reportes» del mockup y otras exportaciones.
- El panel de inicio del Administrador DUBSS.
- La app móvil.
