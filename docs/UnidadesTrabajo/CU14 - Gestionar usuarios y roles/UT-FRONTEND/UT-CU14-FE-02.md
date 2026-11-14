# UT-CU14-FE-02 — Llamadas a la API, hooks de TanStack Query y mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-02` |
| **Nombre** | Llamadas a la API, hooks de TanStack Query y mapeo de errores |
| **Caso de uso** | CU14-FE |
| **Depende de** | `UT-CU14-FE-01` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey` e invalidación)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Las pantallas necesitan leer y modificar las cuentas del personal y mostrar mensajes claros cuando el backend rechaza una operación.

## Objetivo

Existen los hooks de lista y de las cinco mutaciones, con la invalidación de la caché y el mapeo de los errores del backend a mensajes.

## Alcance

- `F/features/usuarios/api/keys.ts`: `usuariosKeys` con la forma `['usuarios', 'list', params]` (`all`, `lists()`, `list(params)`).
- `F/features/usuarios/api/usuariosApi.ts`: funciones sobre `apiClient` (nunca una instancia nueva de axios): `listarUsuariosApi(params)` (`page`, `pageSize`, `search`, `filter[rol]`, `filter[activo]`, `sort`; devuelve los datos y el `meta`), `crearUsuarioApi`, `editarUsuarioApi`, `cambiarRolApi`, `desactivarUsuarioApi` y `reactivarUsuarioApi`.
- Hooks: `useUsuarios(params)` (`useQuery`, conserva los datos previos mientras carga la página siguiente) y `useCrearUsuario`, `useEditarUsuario`, `useCambiarRol`, `useDesactivarUsuario`, `useReactivarUsuario` (`useMutation`). Cada mutación invalida `['usuarios', 'list']` al terminar bien. `useCrearUsuario` **no** deja la contraseña temporal en la caché (`gcTime: 0` o equivalente) y no la escribe en la consola.
- `F/features/usuarios/api/mapearErrorUsuarios.ts`: `mapearErrorUsuarios(error)` → `{ mensaje, campo?, red?, refrescar? }`: `ERR_US_01` → campo `ci`; `ERR_US_02` → campo `correo`; `ERR_SYS_01` y `ERR_US_10` → mensaje del backend; `ERR_US_06` y `ERR_US_07` → mensaje del backend con `refrescar: true`; `ERR_US_08` y `ERR_US_09` → mensaje del backend; `ERR_AUTH_07` → «No tienes permiso para esta acción»; sin respuesta (`red`) → mensaje de conexión; cualquier otro → mensaje genérico. Nunca incluye datos sensibles.
- Tests (mockeando `apiClient`): los parámetros que se mandan al listar (incluido `filter[rol]` y `sort`), la invalidación de `['usuarios', 'list']` tras cada mutación, que la contraseña temporal no queda en la caché, y cada código de error mapeado.

Archivos esperados (crear): los cuatro de arriba, el barrel `api/index.ts` y sus tests.

## Fuera de alcance (no tocar)

- Las pantallas y los modales que usan los hooks (UT 04 a 07).
- Un store de Zustand con los datos de la lista (prohibido por `ESTADO_GLOBAL_FRONTEND.md`).
- El manejo de `401` y la renovación de sesión (ya existen en `features/auth`).

## Resultado esperado

Una pantalla puede listar y modificar cuentas del personal solo con estos hooks.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
