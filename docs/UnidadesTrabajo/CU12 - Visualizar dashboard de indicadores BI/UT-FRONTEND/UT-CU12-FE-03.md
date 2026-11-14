# UT-CU12-FE-03 — Pantalla del dashboard: filtros, tarjetas y gráficos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-03` |
| **Nombre** | Pantalla del dashboard: filtros, tarjetas y gráficos |
| **Caso de uso** | CU12-FE |
| **Depende de** | `UT-CU12-FE-01`, `UT-CU12-FE-02` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (tokens de tema, nada de valores arbitrarios, componentes con el CLI de shadcn) y [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).
- Mirar el mockup `112` en la documentación de grado.
- Como referencia: `F/features/resultados/screens/PublicarResultadosScreen.tsx` (selector de convocatoria, tarjetas de resumen).

## Solicitud de negocio

El Directivo quiere ver de un vistazo cuántas postulaciones hubo, cuántos finalistas, cuántas becas se adjudicaron y cómo viene la asistencia, y cambiar de gestión o convocatoria.

## Objetivo

La pantalla muestra los indicadores y los gráficos de la selección y permite cambiarla.

## Alcance

- Agregar el componente `chart` de shadcn con `pnpm dlx shadcn@latest add chart` (salida en `F/components/ui/`, sin editarla).
- `F/features/dashboard/screens/DashboardScreen.tsx`, montada por la ruta de UT-02 dentro de `DirectivoLayout`:
  - Encabezado «Dashboard de indicadores» y la leyenda «Gestión {etiqueta} · actualizado el {fecha}» (o «Aún no se calculan indicadores» si `actualizadoEn` es nulo, A2).
  - `FiltrosDashboard`: selector de gestión (por defecto la `vigente`), selector de convocatoria («Todas» por defecto, solo las de la gestión elegida) y botón «Aplicar». Cambiar la gestión reinicia la convocatoria a «Todas». La consulta usa la selección **aplicada**, no la que se está editando.
  - Cuatro `TarjetaIndicador`: postulaciones recibidas, finalistas, becas adjudicadas y asistencia promedio (esta muestra «Sin datos» mientras llegue `null`, con el comentario `// TODO CU-10: mostrar la asistencia real cuando el backend la entregue`).
  - `GraficoPostulacionesPorMes` (barras) y `GraficoAsistenciaPorMes` (líneas, con «Sin datos» mientras `asistencia` sea `null`), con tooltip y meses legibles («Mar 2026»); colores de tema.
  - Estados: cargando (esqueletos), error (mensaje de `mapearErrorDashboard`; con `ERR_DAS_01` se vuelve a la selección vigente), y **sin datos** (A1: mensaje informativo y ceros).
- Números con separador de miles (`5.230`) y porcentaje con un decimal como máximo.
- Responsive: en móvil las tarjetas y los gráficos se apilan.
- Tests: muestra los totales y la fecha; con `actualizadoEn` nulo muestra el aviso; con `sinDatos` muestra el mensaje y ceros; la asistencia muestra «Sin datos»; «Aplicar» cambia la consulta y cambiar la gestión reinicia la convocatoria; el selector de convocatoria ofrece solo las de la gestión elegida; un error muestra su mensaje.

Archivos esperados (crear): `DashboardScreen.tsx`, `FiltrosDashboard.tsx`, `TarjetaIndicador.tsx`, los dos gráficos y sus tests. (Tocar): la ruta de UT-02, `index.ts` de la feature, `F/components/ui/chart.tsx` (generado).

## Fuera de alcance (no tocar)

- La descarga del PDF (UT-04).
- Proyecciones, tendencias o la opción «Reportes».
- La asistencia real (CU-10).
- El panel de inicio del Administrador.

## Resultado esperado

El Directivo ve los indicadores de la gestión vigente y los cambia con los filtros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con los datos de prueba, la pantalla muestra cifras y gráficos; con una gestión sin datos muestra el mensaje y ceros.
