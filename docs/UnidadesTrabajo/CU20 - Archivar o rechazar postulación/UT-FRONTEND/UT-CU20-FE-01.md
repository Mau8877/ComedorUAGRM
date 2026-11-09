# UT-CU20-FE-01 — API: hooks, schema, tipos y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-01` |
| **Nombre** | API: hooks, schema, tipos y mensajes de error |
| **Caso de uso** | CU20-FE |
| **Depende de** | `UT-CU20-BE-06` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md).
- Como referencia: `devolucionSchema.ts`, `useDevolverPostulacion.ts` y `devolverPostulacionApi` (CU-19).

## Solicitud de negocio

La pantalla necesita hablar con los endpoints de archivar y rechazar y entender su error.

## Objetivo

Existen los hooks de las mutaciones, el schema del motivo y el mensaje de error del cierre.

## Alcance

- Todo en `F/features/expedientes/`.
- `types`: `CerrarPostulacionRequest` (inferido del schema) y `CierrePostulacionResultado` (`postulacion`, `estado`, `motivo`, `fecha`, `decididaPor`).
- `schemas/cierreSchema.ts`: `motivo` obligatorio (`trim`, mínimo 1: «El motivo es obligatorio») y de **máximo 500 caracteres** («El motivo no puede pasar de 500 caracteres»).
- `api/expedientesApi.ts`: `archivarPostulacionApi(finalista, pedido)` → `POST /finalistas/{codigo}/archivar` y `rechazarPostulacionApi(...)` → `POST /finalistas/{codigo}/rechazar`. **Sin** header de idempotencia.
- `api/useArchivarPostulacion.ts` y `api/useRechazarPostulacion.ts`: `useMutation` que al terminar invalidan `expedientesKeys.detail(codigo)` y `finalistasKeys.lists()`.
- `mapearErrorExpedientes`: `ERR_ARC_01` → «La postulación ya no está calificada o los resultados ya se publicaron; se actualizó el expediente.» (con la marca para refrescar); `ERR_FIN_01` ya está.
- Tests: schema (vacío, solo espacios, 500 y 501), cada llamada a la API con el cliente simulado y el mapeo de `ERR_ARC_01`.

Archivos esperados (crear): `cierreSchema.ts`, `useArchivarPostulacion.ts`, `useRechazarPostulacion.ts` y sus tests. (Tocar): `types`, `expedientesApi.ts`, `api/index.ts`, `schemas/index.ts` y `mapearErrorExpedientes.ts`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (UT-02, UT-03).
- Claves de idempotencia: no se usan.
- Un hook para leer el historial (lo hará el CU-04).
- Tocar `devolucionSchema` ni los hooks del CU-19.

## Resultado esperado

La capa de datos del cierre está lista y probada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
