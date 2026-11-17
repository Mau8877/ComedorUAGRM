# UT-CU09-FE-03 — Pantalla «Planillas», ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-03` |
| **Nombre** | Pantalla «Planillas», ruta y menú |
| **Caso de uso** | CU09-FE |
| **Depende de** | `UT-CU09-FE-01`, `UT-CU09-FE-02` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (siempre el paquete `tanstack-table`, 100 % del lado del servidor, `enableSorting` solo para campos que el endpoint documenta), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (solo tokens de tema).
- Como referencia: `F/features/becas/screens/PadronScreen.tsx` y `F/features/actas/components/GenerarActaPanel.tsx`. Mockup img 109.

## Solicitud de negocio

El Administrador necesita generar y descargar las planillas de asistencia desde el panel.

## Objetivo

La ruta `/planillas` muestra la pantalla del CU, con su entrada en el menú del administrador.

## Alcance

- `F/features/planillas/components/`: `columnasPlanillas` (Periodo, Becados, Generada, Generada por; ordenables solo Periodo → `fechaDesde` y Generada → `generadaEn`), `formatos` (fecha, periodo y fecha-hora en `America/La_Paz`) y `useDescargarPlanilla` (pide la dirección firmada en cada clic y la abre en otra pestaña).
- `F/features/planillas/screens/PlanillasScreen.tsx` dentro de `AdminLayout`:
  - Selector de convocatoria (las publicadas), «Desde», «Hasta» (`<input type="date">` acotado a la gestión), «Vista previa» y «Generar planilla».
  - Aviso «Se incluirán N becados VIGENTES, con espacio para firma y fotocopia de carnet»; sin vigentes lo dice y bloquea los dos botones.
  - Errores del periodo bajo cada campo (solo si fue tocado) con `crearPeriodoSchema`; errores del servidor en una alerta con «Reintentar» si son de red.
  - Vista previa en `<iframe>` con dirección de objeto liberada al pedir otra, al cambiar convocatoria o periodo y al desmontar.
  - Tras generar: aviso con el periodo y los becados, y botón «Descargar».
  - Tabla de planillas (`DataTable` y `DataCards`) con orden, paginación y la acción «Descargar» por fila; estado vacío y error con «Reintentar».
- `F/routes/_authenticated/planillas.tsx` con `exigirRol('ADMINISTRADOR_DUBSS')` y la entrada «Planillas» en `adminNavItems`, después de «Padrón de becados».
- Tests: contenido de la pantalla, sin convocatorias, sin becas vigentes, validaciones del periodo sin llamar al servidor, generar con éxito y descargar, errores del servidor y de red con reintento, vista previa y su descarte, descarga desde la tabla y su error, estado vacío, orden y error de carga; la ruta exige el rol; el menú tiene la entrada.

Archivos esperados (crear): `columnasPlanillas.tsx`, `formatos.ts`, `useDescargarPlanilla.ts`, `PlanillasScreen.tsx`, `planillas.tsx` (ruta) y los tests `PlanillasScreen.test.tsx`, `rutaPlanillas.test.ts`, `montar.tsx` y `clienteSimulado.ts`; tocar `adminNavItems.ts` y su test.

## Fuera de alcance (no tocar)

- La pantalla del padrón o del acta.
- Una ventana de confirmación, fechas por defecto o un código por planilla.
- La app móvil.

## Resultado esperado

El Administrador elige el periodo, ve cuántos becados entran, previsualiza, genera la planilla y la descarga.

## Validación

1. `pnpm test` pasa, `pnpm lint` y `pnpm exec tsc -b` sin errores y `pnpm exec vite build` compila.
2. Con el stack levantado: entrar como administrador, abrir «Planillas», elegir una convocatoria publicada con becas vigentes, ingresar un periodo, ver la vista previa, generar y descargar el PDF.
