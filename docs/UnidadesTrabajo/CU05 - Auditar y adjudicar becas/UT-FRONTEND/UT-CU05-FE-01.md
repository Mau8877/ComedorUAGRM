# UT-CU05-FE-01 — API del panel y la selección: hooks, schema, tipos y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-01` |
| **Nombre** | API del panel y la selección: hooks, schema, tipos y mensajes de error |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-BE-08` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md).
- Como referencia: `F/features/finalistas/api/` (`finalistasApi.ts`, `keys.ts`, `mapearErrorFinalistas.ts`, `useFinalistas.ts`) y su `tests/clienteSimulado.ts`.

## Solicitud de negocio

La pantalla necesita leer el panel de una convocatoria, seleccionar finalistas y entender los errores.

## Objetivo

Existe la feature `adjudicacion` con sus tipos, hooks, schema y mensajes de error para el panel y la selección.

## Alcance

- Feature nueva `F/features/adjudicacion/` con la forma estándar (`api/`, `components/`, `schemas/`, `screens/`, `tests/`, `types/`, `index.ts`).
- `types`: `PanelAdjudicacion` (convocatoria `{ codigo, nombre, estado }`, `cupos`, `puntajeMinimo`, `adjudicados`, `noAdjudicados`, `cuposDisponibles`, `cantidadFinalistas`, `totalFinalistas`, `calificadasSinSeleccionar`, `resultadosPublicados`, `decisionesHabilitadas`; `cupos`, `puntajeMinimo`, `cuposDisponibles` y `cantidadFinalistas` pueden ser `null`), `SeleccionarFinalistasRequest` (inferido del schema) y `SeleccionFinalistasResultado` (`convocatoria`, `seleccionados`, `finalistas[]` con `codigo`, `postulacion`, `posicion`).
- `schemas/seleccionSchema.ts`: `cantidad` entero **positivo** («La cantidad debe ser un número entero positivo»). El campo vacío del formulario se envía como «sin cantidad» (el backend usa la configurada).
- `api/keys.ts`: `adjudicacionKeys` (`all: ['adjudicacion']`, `panels()`, `panel(convocatoriaCodigo)`), mismo patrón que `finalistasKeys`.
- `api/adjudicacionApi.ts`: `obtenerPanelApi(convocatoria)` → `GET /convocatorias/{codigo}/adjudicacion` y `seleccionarFinalistasApi(convocatoria, pedido)` → `POST /convocatorias/{codigo}/finalistas`. **Sin** header de idempotencia.
- `api/usePanelAdjudicacion.ts` (`useQuery`, deshabilitado mientras no haya convocatoria elegida) y `api/useSeleccionarFinalistas.ts` (`useMutation` que al terminar invalida `adjudicacionKeys.panel(convocatoria)` y `finalistasKeys.lists()`).
- `api/mapearErrorAdjudicacion.ts`: `ERR_PRI_01` → «La convocatoria no existe o fue eliminada.»; `ERR_ADJ_01` → «Los resultados de esta convocatoria ya se publicaron; no se pueden seleccionar finalistas.»; `ERR_ADJ_05` → «La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.»; `ERR_SYS_01` con el mensaje del backend (cantidad inválida o mayor que las disponibles); cualquier otro, mensaje genérico en español sin detalles técnicos.
- Barrel `index.ts` de la feature: exporta lo que otras partes pueden usar (`usePanelAdjudicacion` y los tipos del panel, para el expediente en UT-05) y la pantalla (UT-02).
- Tests: schema (vacío, cero, negativo, decimal, positivo), cada llamada a la API con el cliente simulado, `enabled` del panel y el mapeo de cada código.

Archivos esperados (crear): los de la feature indicados y sus tests.

## Fuera de alcance (no tocar)

- Componentes y pantalla (UT-02, UT-03).
- La API de adjudicar y no adjudicar (UT-04).
- Claves de idempotencia: no se usan.
- Un hook para editar la cantidad de finalistas de la convocatoria (CU-13).
- Cambios en `features/finalistas/` ni `features/priorizacion/`.

## Resultado esperado

La capa de datos del panel y la selección está lista y probada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
