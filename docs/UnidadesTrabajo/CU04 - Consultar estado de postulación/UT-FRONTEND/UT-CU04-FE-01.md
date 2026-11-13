# UT-CU04-FE-01 — API: tipos, keys, hooks y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-01` |
| **Nombre** | API: tipos, keys, hooks y mensajes de error |
| **Caso de uso** | CU04-FE |
| **Depende de** | `UT-CU04-BE-04` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (factory de keys, `apiClient`, sin `queryKey` en componentes), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `F/features/resultados/api/` (CU-22).

## Solicitud de negocio

Las pantallas necesitan hablar con el backend de la consulta sin armar llamadas a mano.

## Objetivo

Existen los tipos, las keys, las funciones de API y los hooks de la feature `postulaciones`, con mensajes de error en español.

## Alcance

- `F/features/postulaciones/types/`: `MiPostulacionResumen`, `MiPostulacionDetalle`, `PasoLineaTiempo` (`situacion`: `COMPLETADO` o `PENDIENTE`), `Observaciones`, `FinalistaPostulante` y `EstadoPostulacion` (los seis estados visibles). Campos exactos del contrato; los bloques opcionales pueden ser `null`.
- `F/features/postulaciones/api/keys.ts`: `postulacionesKeys` (`all`, `mias(params)`, `detalle(codigo)`).
- `F/features/postulaciones/api/`: `listarMisPostulacionesApi` y `obtenerPostulacionApi` con `apiClient`; hooks `useMisPostulaciones` y `usePostulacion` (solo lectura, sin mutaciones).
- `mapearErrorPostulaciones.ts`: texto en español para `ERR_POS_01` («La postulación no existe o no es tuya»); el resto usa `getErrorMessage`.
- `F/features/postulaciones/index.ts`: barrel (los hooks y las pantallas, a medida que existan).
- Tests (mockeando `apiClient`): rutas y parámetros correctos (`page`, `pageSize`, `filter[gestionAcademica]`, `sort`), keys por módulo, traducción de `ERR_POS_01`.

Archivos esperados (crear): tipos, `keys.ts`, API, hooks, `mapearErrorPostulaciones.ts`, `index.ts` y tests en `tests/`.

## Fuera de alcance (no tocar)

- Componentes, pantallas y rutas (UT-02 a UT-04).
- Mutaciones de envío o corrección (CU-02).
- `features/resultados/` y `features/notificaciones/`.

## Resultado esperado

Las pantallas importan hooks ya armados y reciben errores legibles.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
