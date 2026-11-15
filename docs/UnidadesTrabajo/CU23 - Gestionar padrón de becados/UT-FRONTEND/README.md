# Unidades de trabajo — CU23-FRONTEND

**Caso de uso:** pantalla «Padrón de becados»: elegir la convocatoria, dar de alta a los adjudicados, ver el padrón y dar de baja, suspender o reemplazar.
**Fuente CU:** [`CU23 - Gestionar padrón de becados.md`](../../../CasosDeUso/CU23%20-%20Gestionar%20padr%C3%B3n%20de%20becados.md)
**Diagramas de grado:** mockup img 116 · secuencia img 103 · análisis img 088
**Recurso HTTP / contrato:** ver [UT-BACKEND](../UT-BACKEND/README.md#contrato-de-datos--recurso-http)

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md),
[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md),
[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md),
[`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) y
[`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Decisiones de diseño

- **Feature nueva `features/becas/`**, ruta `/padron-becados` y ítem «Padrón de becados» en el menú del Administrador.
- **Selector de convocatoria** con las convocatorias en estado `PUBLICADA`, desde el hook del CU13
  (`GET /api/v1/convocatorias?filter[estado]=PUBLICADA`): el padrón solo existe tras publicar los resultados (A7).
- **Alta con un botón «Dar de alta»** que muestra cuántos adjudicados faltan (`pendientesDeAlta`) y se oculta cuando
  son 0 (A8). Pide confirmación (paso 3 del flujo D).
- **Baja, suspensión y reemplazo** con diálogos de confirmación (A5: cancelar no cambia nada). El motivo es
  obligatorio en baja y suspensión (mensaje inline, A1). El reemplazo primero pide el candidato y muestra a quién se
  propone (paso 13); si no hay, informa (A4).
- **Acciones por fila** con `RowActionButton` (ícono + tooltip), habilitadas según el estado de la beca: baja y
  suspender solo en vigentes, reemplazar solo en bajas. El mockup las pone como botones de la barra sobre filas
  seleccionadas; aquí van por fila, como el resto de los listados del panel.
- **Sin «Historial»** (el mockup lo tiene): queda como follow-up junto con el endpoint de movimientos.

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU23-FE-01](./UT-CU23-FE-01.md) | Tipos, API y hooks del padrón | UT-CU23-BE-05 |
| [UT-CU23-FE-02](./UT-CU23-FE-02.md) | Pantalla del padrón, alta, ruta y menú | FE-01 |
| [UT-CU23-FE-03](./UT-CU23-FE-03.md) | Diálogos de baja, suspensión y reemplazo | FE-01, FE-02 |
| [UT-CU23-FE-04](./UT-CU23-FE-04.md) | Pruebas | FE-01 a FE-03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Flujo D | Dar de alta | 02 |
| Flujo A | Ver el padrón | 02 |
| Flujo B | Baja y suspensión | 03 |
| Flujo C | Reemplazo | 03 |
| A1, A4, A5, A7, A8 | Alternativas visibles en la pantalla | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Botón «Historial» y `GET /becas/{codigo}/movimientos`.
- Reactivar una beca suspendida.
- Selección múltiple de filas del mockup (acciones masivas).

## Qué NO incluyen estas UT

- Planillas, asistencia e historial del becado (CU-09, CU-10, CU-24).
