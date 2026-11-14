# UT-CU22-FE-02 — Pantalla «Publicar resultados» del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-02` |
| **Nombre** | Pantalla «Publicar resultados» del Administrador |
| **Caso de uso** | CU22-FE |
| **Depende de** | `UT-CU22-FE-01` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (verificar el contrato contra `ENDPOINTS_BACKEND.md`), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `AdjudicacionScreen` (CU-05) y `FinalistasScreen` (CU-21).

## Solicitud de negocio

Antes de publicar, el Administrador revisa qué decidió y qué falta.

## Objetivo

El Administrador entra a «Publicar resultados», elige una convocatoria y ve el resumen y las decisiones.

## Alcance

- Ruta del Administrador dentro de `_authenticated/_admin/` (misma forma que la de `/adjudicacion`), con su opción en el menú del layout admin.
- `F/features/resultados/screens/PublicarResultadosScreen.tsx`:
  - Selector de convocatoria con `useConvocatoriasPriorizables` (importado del barrel de `features/priorizacion/`).
  - Tarjetas del resumen con `useResumenPublicacion`: adjudicados, no adjudicados, decisiones pendientes (en «calificada»), finalistas a avisar y, si ya se publicó, quién y cuándo.
  - Tabla de decisiones con `useDataTable` + `DataTable` (y `DataCards` en móvil): nombre, apellido, resultado (insignia «Adjudicada» / «No adjudicada») y puntaje final («—» si es `null`); `enableSorting` solo en `puntajeFinal`, `apellidos` y `nombres`; filtro por resultado en `DataTableToolbar` (único filtro que el backend documenta) y buscador por nombre/apellido; `errorMessage` con `getErrorMessage`.
  - Botón «Publicar resultados»: deshabilitado, con su motivo visible, si `puedePublicar` es falso (faltan decisiones, ya publicada, convocatoria no cerrada); si ya se publicó, muestra «Resultados publicados el …» en lugar del botón.
  - El botón abre la ventana de la UT-03.
- Estados de carga, vacío y error de cada consulta.
- Tests: carga y muestra el resumen, `puedePublicar` falso deshabilita con motivo, tabla con paginación y orden, filtro por resultado, ya publicada sin botón y sin sesión de Administrador la ruta no se muestra.

Archivos esperados (crear): `PublicarResultadosScreen.tsx`, columnas de la tabla, la ruta y su test. (Tocar): `screens/index.ts`, el menú del layout admin.

## Fuera de alcance (no tocar)

- La ventana de confirmación y la mutación (UT-03).
- La pantalla del postulante (UT-04).
- Adjudicar o no adjudicar (CU-05).

## Resultado esperado

El Administrador ve con claridad si puede publicar y qué va a publicar.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
