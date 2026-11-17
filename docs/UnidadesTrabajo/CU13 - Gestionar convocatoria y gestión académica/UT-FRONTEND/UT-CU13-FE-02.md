# UT-CU13-FE-02 — Llamadas a la API, hooks de TanStack Query y mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-02` |
| **Nombre** | Llamadas a la API, hooks de TanStack Query y mapeo de errores |
| **Caso de uso** | CU13-FE |
| **Depende de** | `UT-CU13-FE-01` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`, `apiClient`, invalidación)
- `F/features/usuarios/api/` (`keys.ts`, `usuariosApi.ts`, `useUsuarios.ts`, `mapearErrorUsuarios.ts`) como referencia directa

## Solicitud de negocio

Las pantallas necesitan pedir y cambiar convocatorias sin que cada componente arme URLs, claves de caché ni mensajes de error por su cuenta.

## Objetivo

La feature tiene sus llamadas al backend, sus hooks de TanStack Query con claves de caché ordenadas y el traductor de errores a mensajes para el Administrador.

## Alcance

- `F/features/convocatorias/api/keys.ts`: `convocatoriasKeys` (`all: ['convocatorias']`, `lists()`, `list(params)`, `details()`, `detail(codigo)`), `gestionesKeys` (`['gestiones-academicas', 'list', params]`) y `tiposDocumentoKeys` (`['tipos-documento', 'list', params]`), con el módulo primero y `list`/`detail` como segundo elemento.
- `F/features/convocatorias/api/convocatoriasApi.ts` (usa el `apiClient` de `@/store`, nunca otra instancia de axios): `listarConvocatorias(params)` (con `filter[estado]` y `filter[gestion]`), `obtenerConvocatoria(codigo)`, `crearConvocatoria(body)`, `editarConvocatoria(codigo, body)` (`PUT`), `abrirConvocatoria(codigo)`, `cerrarConvocatoria(codigo)`, `eliminarConvocatoria(codigo)`, `listarGestiones(params)`, `crearGestion(body)`, `editarGestion(codigo, body)` (`PUT`) y `listarTiposDocumento(params)`. Los listados devuelven `{ datos, meta }` como el resto de features.
- `F/features/convocatorias/api/hooks.ts`: `useConvocatorias(params)`, `useConvocatoria(codigo)` (deshabilitado sin código), `useGestiones(params)`, `useTiposDocumento(params)`, y las mutaciones `useCrearConvocatoria`, `useEditarConvocatoria`, `useAbrirConvocatoria`, `useCerrarConvocatoria`, `useEliminarConvocatoria`, `useCrearGestion` y `useEditarGestion`. Tras cada mutación de convocatoria se invalida `convocatoriasKeys.lists()` (y `detail(codigo)` si hay uno cacheado); tras crear o editar una gestión, `gestionesKeys` completo.
- `F/features/convocatorias/api/mapearErrorConvocatorias.ts`: `mapearErrorConvocatorias(error)` → `{ mensaje, campo?, refrescar?, red? }` con la tabla de errores de las [decisiones del README](./README.md#decisiones-de-diseño) (`ERR_CV_04` → campo `fechaCierre`, `ERR_CV_12` → campo `etiqueta`, `ERR_CV_13` → campo `fechaFin`, `ERR_CV_01`/`02`/`03`/`08`/`09`/`10` → `refrescar`, error de red → `red`). El mensaje es el del backend cuando existe; nunca se loguea ni se muestra un dato sensible.
- `index.ts` de `api/`.
- Tests (cliente simulado, sin backend real; ver `features/usuarios/tests/clienteSimulado.ts`): cada función llama al método y la ruta correctos con sus parámetros; los hooks invalidan las claves esperadas; `mapearErrorConvocatorias` cubre cada código, un error desconocido y un error de red.

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

- Componentes y pantallas (UT 04 a 07).
- Cambiar `apiClient` o `createBaseApi`.
- Guardar datos del backend en Zustand.

## Resultado esperado

Cualquier pantalla de la feature obtiene y cambia datos con un hook y muestra un error entendible, sin repetir nada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
