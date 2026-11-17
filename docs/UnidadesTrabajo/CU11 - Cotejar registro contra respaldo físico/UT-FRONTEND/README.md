# Unidades de trabajo — CU11-FRONTEND

**Caso de uso:** Pantalla web «Cotejo de asistencia»: elegir una planilla, ver sus registros digitales y registrar el cotejo.
**Fuente CU:** [`CU11 - Cotejar registro contra respaldo físico.md`](../../../CasosDeUso/CU11%20-%20Cotejar%20registro%20contra%20respaldo%20f%C3%ADsico.md)
**Diagramas de grado:** contexto img 070 · análisis img 083 · secuencia img 098 · mockup img 111
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md) (`/api/v1/planillas/{codigo}/cotejos…`).

> `F/` = `projects/frontend/comedoruagrm/src`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND`, `frontend/TANSTACK_QUERY_FRONTEND`, `frontend/TANSTACK_TABLE_FRONTEND`, `frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND`, `frontend/RUTAS_NAVEGACION_FRONTEND`, `frontend/TAILWIND_STYLES_FRONTEND`, `frontend/ESTADO_GLOBAL_FRONTEND` y `frontend/TESTING_FRONTEND`. Molde: la feature `planillas` (CU09).

## Decisiones de diseño

- **Feature `cotejos`** con una sola pantalla `CotejoScreen`, ruta `/cotejo-asistencia` (solo `ADMINISTRADOR_DUBSS`, guard `exigirRol`) y entrada «Cotejo de asistencia» en el menú, justo después de «Planillas».
- **Selectores de convocatoria y planilla**: la convocatoria viene de `useConvocatorias` (CU13, solo publicadas) y las planillas de `usePlanillas` (CU09) por sus barrels; no se duplican hooks.
- **El mockup (img 111) marca «Firmó / Sin firma» fila por fila.** Eso exige capturar cada firma (hasta decenas de miles de celdas) o leer la planilla escaneada. Se difiere a la lectura automática: la pantalla muestra los registros digitales y el Administrador declara el total y las coincidencias. Es la diferencia principal con el mockup.
- **Resultado deducido en pantalla**: discrepancias = total − coincidencias; el resultado y la obligatoriedad de las observaciones salen de ahí. Lo enviado es siempre consistente; el backend vuelve a validar.
- Dos tablas con paginación y orden en el servidor (registros digitales y cotejos registrados), con `DataTable` en escritorio y `DataCards` en móvil.

## Contrato de datos / recurso HTTP

Ver el [`README` de UT-BACKEND](../UT-BACKEND/README.md#contrato-de-datos--recurso-http). El cliente usa los tipos de `F/features/cotejos/types/` (`DatosCotejo`, `RegistroDigital`, `Cotejo`, `RegistrarCotejoRequest`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU11-FE-01](./UT-CU11-FE-01.md) | Tipos, API, hooks, errores y esquema del formulario | — |
| [UT-CU11-FE-02](./UT-CU11-FE-02.md) | Pantalla, ruta y menú | FE-01 |
| [UT-CU11-FE-03](./UT-CU11-FE-03.md) | Pruebas | FE-02 |

**Cadena:** `01 → 02 → 03`. Requiere el backend (UT-BACKEND) para probar de punta a punta.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 1 a 3 | Elegir la planilla y ver sus registros digitales | 01, 02, 03 |
| Flujo principal 5 a 8 | Ingresar, validar, guardar y ver el historial | 01, 02, 03 |
| A1, A4, A5 | Sin registros, sin convocatorias, sin planillas | 02, 03 |
| A2 | Observaciones obligatorias | 01, 02, 03 |
| A6, A7, A8 | Errores traducidos y reintento | 01, 02, 03 |
| CA13 a CA15 | Pantalla | 01 a 03 |

## Follow-ups (no se implementan en estas UT)

- Marcar «Firmó / Sin firma» por becado y día (columnas «Planilla firmada» y «Resultado» del mockup) cuando exista la lectura automática de la planilla escaneada.
- Mostrar la dirección de descarga de la planilla desde esta pantalla.

## Qué NO incluyen estas UT

- La generación de la planilla (CU-09), el registro de asistencia móvil (CU-10) y el historial del becado (CU-24).
