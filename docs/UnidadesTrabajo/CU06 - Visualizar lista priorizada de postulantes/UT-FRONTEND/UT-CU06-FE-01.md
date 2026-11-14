# UT-CU06-FE-01 — Tipos, claves y llamadas a la API; descarga de archivos; `useCarreras` a `shared/`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-01` |
| **Nombre** | Tipos, claves y llamadas a la API; descarga de archivos; `useCarreras` a `shared/` |
| **Caso de uso** | CU06-FE |
| **Depende de** | — |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`, `api/` de la feature, `shared/api/`, cliente HTTP) y [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md).
- Como referencia: `F/features/usuarios/api/` (`keys.ts`, `usuariosApi.ts`, `useUsuarios.ts`, `mapearErrorUsuarios.ts`) y `F/store/apiClient.ts`.

## Solicitud de negocio

La pantalla necesita pedir al backend las convocatorias, el resumen, la lista y el PDF, sin repetir código ni romper el manejo de errores.

## Objetivo

La feature `priorizacion` tiene sus tipos, sus claves de caché y sus hooks de datos, y el cliente HTTP sabe descargar un archivo.

## Alcance

- `F/features/priorizacion/types/`: `ConvocatoriaPriorizable`, `ResumenListaPriorizada`, `PostulacionPriorizada`, `FiltrosLista` (campos del README).
- `F/features/priorizacion/api/keys.ts`: factory `priorizacionKeys` (`['priorizacion', 'convocatorias']`, `['priorizacion', 'resumen', convocatoriaId]`, `['priorizacion', 'list', params]`).
- `F/features/priorizacion/api/listaPriorizadaApi.ts`: `listarConvocatorias()`, `obtenerResumen(convocatoriaId)`, `listar({ convocatoriaId, carreraId, search, page, pageSize })` (manda `filter[convocatoriaId]`, `filter[carreraId]`, `search`, `page`, `pageSize`; **nunca** `sort`) y `exportar(filtros)` (`POST /lista-priorizada/exportaciones`, `responseType: 'blob'`, devuelve el `Blob` y el nombre del archivo leído de `Content-Disposition`).
- Hooks: `useConvocatoriasPriorizables`, `useResumenLista(convocatoriaId)` y `useListaPriorizada(params)` (con `placeholderData` para no parpadear al cambiar de página; solo se ejecutan si hay `convocatoriaId`), y la mutación `useExportarLista`.
- `F/features/priorizacion/api/mapearErrorPriorizacion.ts`: convierte `ApiError` en el mensaje que ve el usuario según el README (`ERR_PRI_01`, `ERR_PRI_03`, red/`5xx`).
- **Descarga de archivos en `apiClient`** (`F/store/apiClient.ts`): una petición con `responseType: 'blob'` **no** se desempaqueta como sobre; si el servidor responde un error, el cuerpo llega como `Blob` con el sobre de error dentro: se lee (`blob.text()` → JSON) y se rechaza con `ApiError` igual que cualquier otro. Una función de ayuda en `F/utils/` (`descargarArchivo(blob, nombre)`) dispara la descarga en el navegador con un enlace temporal y lo revoca. Los endpoints existentes no cambian de comportamiento.
- **`useCarreras` a `shared/`:** mover el endpoint y el hook de `F/features/auth/api/` a `F/shared/api/` (con su `keys.ts` de `shared`, `['shared', 'carreras']`), actualizar `features/auth/` para importarlo desde ahí y que los tests de `auth` sigan pasando. Es la única modificación de `features/auth/`.
- Tests: `listaPriorizadaApi` manda los parámetros correctos (y no manda `sort`); el mapeo de errores; `apiClient` con `blob`: éxito devuelve el blob y un error se convierte en `ApiError` con su código; `descargarArchivo`; `useCarreras` sigue funcionando para el registro.

Archivos esperados (crear): los de `features/priorizacion/{types,api}`, `utils/descargarArchivo.ts`, `shared/api/` y sus tests. (Tocar): `store/apiClient.ts`, `features/auth/api/*` y sus imports.

## Fuera de alcance (no tocar)

- Pantallas y componentes (UT-02 a 05).
- Otros cambios en `features/auth/`.
- Cambiar cómo se desempaquetan las respuestas normales.

## Resultado esperado

Hay hooks listos para pedir los datos del CU y descargar el PDF, y el registro sigue funcionando con `useCarreras` desde `shared/`.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
