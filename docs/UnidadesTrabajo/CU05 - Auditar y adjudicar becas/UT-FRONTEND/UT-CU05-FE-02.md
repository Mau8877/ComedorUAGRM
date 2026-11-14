# UT-CU05-FE-02 — Pantalla «Auditoría y adjudicación» con el panel de cupos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-02` |
| **Nombre** | Pantalla «Auditoría y adjudicación» con el panel de cupos |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-FE-01` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md).
- Como referencia: `F/features/finalistas/screens/FinalistasScreen.tsx`, `F/routes/_authenticated/finalistas.tsx` y `F/layouts/admin/adminNavItems.ts`.

## Solicitud de negocio

El Administrador necesita un punto de entrada donde elegir la convocatoria y ver cuántos cupos hay, cuántos se usaron y cuántos finalistas faltan.

## Objetivo

El menú tiene la opción «Auditoría y adjudicación», y la pantalla muestra el panel de la convocatoria elegida.

## Alcance

- Ruta `F/routes/_authenticated/adjudicacion.tsx` con `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')` (el backend niega de verdad con `403`) que monta `AdjudicacionScreen` con el usuario actual y el `activeHref="/adjudicacion"`.
- Opción de menú «Auditoría y adjudicación» (`/adjudicacion`) en `adminNavItems.ts`, junto a «Lista priorizada» y «Finalistas»; se actualiza `adminNavItems.test.ts`.
- `F/features/adjudicacion/screens/AdjudicacionScreen.tsx`, dentro de `AdminLayout`:
  - selector de convocatoria con `useConvocatoriasPriorizables` importado del barrel de `features/priorizacion` (el mismo que usa `FinalistasScreen`), recordando la elección en el estado de la pantalla;
  - tarjetas del panel con `usePanelAdjudicacion`: cupos, adjudicados, no adjudicados, cupos disponibles, puntaje mínimo, **cantidad de finalistas configurada** (solo aquí: es un dato que solo ve el Administrador), total de finalistas y postulaciones sin seleccionar. Valores `null` se muestran «Sin definir» (nunca se inventa un número);
  - avisos claros cuando `decisionesHabilitadas` es falso («La convocatoria debe estar cerrada…», «Faltan cupos o puntaje mínimo por definir con la DUBSS», «Los resultados ya se publicaron»);
  - accesos a «Ver lista priorizada» (`/lista-priorizada`) y «Ver finalistas» (`/finalistas`);
  - estados de carga y error con `getErrorMessage`/`mapearErrorAdjudicacion`; `404` de la convocatoria vuelve al selector con el aviso.
  - mensaje «No hay postulaciones para auditar» cuando no hay calificadas ni finalistas (A1).
- El botón «Seleccionar finalistas» se agrega en UT-03; aquí queda el espacio reservado, sin lógica.
- Tests de pantalla con la API simulada: selector y panel, valores `null`, cada aviso, sin postulaciones, `404`, carga y error.

Archivos esperados (crear): `AdjudicacionScreen.tsx`, `screens/index.ts` (si no existe), la ruta y sus tests. (Tocar): `adminNavItems.ts`, `adminNavItems.test.ts` y el barrel de la feature.

## Fuera de alcance (no tocar)

- La ventana y el botón de seleccionar finalistas (UT-03).
- Mostrar la lista de finalistas o la lista priorizada dentro de esta pantalla (se llega a ellas por enlace; siguen siendo las del CU-06 y CU-21).
- Botones de acta y de publicar resultados (CU-08, CU-22).
- Cambiar `features/priorizacion/` o mover `useConvocatoriasPriorizables` a `shared/api/`.

## Resultado esperado

El Administrador entra por el menú, elige la convocatoria y ve su panel con los cupos y su estado.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con los datos de prueba del backend (UT-CU05-BE-10), `/adjudicacion` muestra el panel de `[PRUEBA]`.
