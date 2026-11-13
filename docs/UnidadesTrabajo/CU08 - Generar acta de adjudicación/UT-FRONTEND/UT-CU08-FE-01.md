# UT-CU08-FE-01 — API: hooks, tipos, keys y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-01` |
| **Nombre** | API: hooks, tipos, keys y mensajes de error |
| **Caso de uso** | CU08-FE |
| **Depende de** | `UT-CU08-BE-09` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (factory de keys, `apiClient`, sin `queryKey` en componentes), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `F/features/resultados/api/` (CU-22) y `F/features/priorizacion/api/listaPriorizadaApi.ts` (descarga de un `Blob`).

## Solicitud de negocio

La pantalla necesita hablar con el backend de actas sin armar llamadas a mano.

## Objetivo

Existen los tipos, las keys, las funciones de API y los hooks de la feature `actas`, con mensajes de error en español.

## Alcance

- `F/features/actas/types/`: tipos de `Acta`, `ConvocatoriaPendienteActa` y `AccesoActa` (campos exactos del contrato; sin la ruta del archivo) y los parámetros `ActasParams` (`page`, `pageSize`, `search`, `sort`) y `PendientesParams` (`page`, `pageSize`).
- `F/features/actas/api/keys.ts`: `actasKeys` (`all`, `lists()`, `list(params)`, `pendientes(params)`, `detail(convocatoriaCodigo)`).
- `F/features/actas/api/` con `apiClient` (sin instancia nueva de axios): `listarActasApi`, `listarPendientesApi`, `obtenerActaApi`, `obtenerAccesoActaApi`, `generarActaApi` y `obtenerVistaPreviaApi` (pide el PDF con `responseType: 'blob'` y devuelve el `Blob`; los errores del sobre llegan como `ApiError`, incluso en una respuesta `blob`).
- Hooks: `useActas`, `useActasPendientes`, `useActa`, `useAccesoActa` (mutación: se pide al hacer clic, la dirección vence en minutos y no se guarda en caché), `useGenerarActa` (mutación: al terminar invalida `['actas']` y `['resultados']`) y `useVistaPreviaActa` (mutación que devuelve el `Blob`; nunca se guarda en la caché).
- `F/features/actas/api/mapearErrorActas.ts`: textos en español para `ERR_PRI_01` (convocatoria no encontrada), `ERR_ACT_01` (los resultados aún no están publicados), `ERR_ACT_02` (la convocatoria ya tiene acta), `ERR_ACT_03` (aún no tiene acta) y `ERR_ACT_04` (el archivo no está disponible, avisar a soporte); cualquier otro usa `getErrorMessage`.
- `F/features/actas/index.ts`: barrel con lo que otras partes pueden importar (hooks, pantalla y tipos).
- Tests (mockeando `apiClient`): cada hook llama a la ruta correcta con sus parámetros, la generación invalida las keys, la vista previa devuelve un `Blob` y un error en una respuesta `blob` se lee como `ApiError`, cada código de error se traduce y la dirección de descarga no queda en la caché.

Archivos esperados (crear): tipos, `keys.ts`, funciones de API, hooks, `mapearErrorActas.ts`, `index.ts` y sus tests (en `tests/`).

## Fuera de alcance (no tocar)

- Componentes y pantallas (UT-02 a UT-04).
- `features/resultados/` y `features/priorizacion/`.
- Guardar el PDF o la dirección firmada en la caché o en el estado global.

## Resultado esperado

Las pantallas importan hooks ya armados y reciben errores legibles.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
