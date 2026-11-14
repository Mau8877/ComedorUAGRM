# UT-CU22-FE-01 — API: hooks, tipos, keys y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-01` |
| **Nombre** | API: hooks, tipos, keys y mensajes de error |
| **Caso de uso** | CU22-FE |
| **Depende de** | `UT-CU22-BE-08` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (factory de keys, `apiClient`, sin `queryKey` en componentes), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `F/features/expedientes/api/` (CU-19 y CU-20).

## Solicitud de negocio

Las pantallas necesitan hablar con el backend de resultados sin armar llamadas a mano.

## Objetivo

Existen los tipos, las keys, las funciones de API y los hooks de la feature `resultados`, con mensajes de error en español.

## Alcance

- `F/features/resultados/types/`: tipos de `ResumenPublicacion`, `DecisionResultado`, `PublicacionResultado`, `ConvocatoriaPublicada`, `AdjudicadoPublico` y `MiResultado` (campos exactos del contrato; `puntajeFinal` y `nota` pueden ser `null`).
- `F/features/resultados/api/keys.ts`: `resultadosKeys` (`all`, `resumen(codigo)`, `decisiones(codigo, params)`, `publicados(params)`, `adjudicados(codigo, params)`, `mio(codigo)`).
- `F/features/resultados/api/` con `apiClient` (sin instancia nueva de axios): `obtenerResumenApi`, `listarDecisionesApi`, `publicarResultadosApi`, `listarConvocatoriasPublicadasApi`, `listarAdjudicadosApi` y `obtenerMiResultadoApi`.
- Hooks: `useResumenPublicacion`, `useDecisionesResultado`, `useConvocatoriasPublicadas`, `useAdjudicadosPublicos`, `useMiResultado` y `usePublicarResultados` (mutación: al terminar invalida `['resultados']`, `['adjudicacion']` y `['finalistas','list']`).
- `F/features/resultados/api/mapearErrorResultados.ts`: textos en español para `ERR_PRI_01` (convocatoria no encontrada), `ERR_PUB_01` («Faltan N decisiones» tomando el mensaje del backend), `ERR_PUB_02` (ya publicada), `ERR_PUB_03` (convocatoria no cerrada) y `ERR_PUB_04` (resultados aún no publicados); cualquier otro usa `getErrorMessage`.
- `F/features/resultados/index.ts`: barrel con lo que otras partes pueden importar (los hooks y las pantallas).
- Tests (mockeando `apiClient`): cada hook llama a la ruta correcta con sus parámetros, la mutación invalida las keys, cada código de error se traduce, y los tipos aceptan `null`.

Archivos esperados (crear): tipos, `keys.ts`, funciones de API, hooks, `mapearErrorResultados.ts`, `index.ts` y sus tests (en `tests/`).

## Fuera de alcance (no tocar)

- Componentes y pantallas (UT-02 a UT-04).
- Duplicar el hook del selector de convocatoria (se reutiliza `useConvocatoriasPriorizables`).
- `features/expedientes/` y `features/adjudicacion/`.

## Resultado esperado

Las pantallas importan hooks ya armados y reciben errores legibles.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
