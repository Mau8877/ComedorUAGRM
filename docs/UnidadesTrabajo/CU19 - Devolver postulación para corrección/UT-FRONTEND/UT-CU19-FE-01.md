# UT-CU19-FE-01 — API: hook, schema, tipos y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-01` |
| **Nombre** | API: hook, schema, tipos y mensajes de error |
| **Caso de uso** | CU19-FE |
| **Depende de** | `UT-CU19-BE-07` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md).
- Como referencia: `F/features/expedientes/api/` y `schemas/auditoriaSchema.ts` (CU-07).

## Solicitud de negocio

La pantalla necesita hablar con el endpoint de devolución y entender sus errores.

## Objetivo

Existen el hook de la mutación, el schema del motivo y los mensajes de error de la devolución.

## Alcance

- Todo en `F/features/expedientes/` (la devolución se hace desde el expediente; no se crea una feature nueva).
- `types`: `DevolverPostulacionRequest` (inferido del schema) y `DevolucionResultado` (`postulacion`, `estado`, `motivo`, `fecha`, `devueltaPor`).
- `schemas/devolucionSchema.ts`: `motivo` obligatorio (`trim`, mínimo 1: «El motivo es obligatorio») y de **máximo 500 caracteres** («El motivo no puede pasar de 500 caracteres»).
- `api/expedientesApi.ts`: `devolverPostulacionApi(finalista, pedido)` → `POST /finalistas/{codigo}/devolver`. **Sin** header de idempotencia.
- `api/useDevolverPostulacion.ts`: `useMutation` que al terminar invalida `expedientesKeys.detail(codigo)` y `finalistasKeys.lists()`.
- `mapearErrorExpedientes`: `ERR_DEV_01` → «La postulación ya no está calificada; se actualizó el expediente.» (con una marca para refrescar), `ERR_DEV_02` → «Primero registra la auditoría del expediente.»; `ERR_FIN_01` ya está.
- Tests: schema (vacío, solo espacios, 500 y 501), llamada a la API con el cliente simulado y mapeo de cada código.

Archivos esperados (crear): `devolucionSchema.ts`, `useDevolverPostulacion.ts` y sus tests. (Tocar): `types`, `expedientesApi.ts`, `api/index.ts`, `schemas/index.ts` y `mapearErrorExpedientes.ts`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (UT-02, UT-03).
- Claves de idempotencia: no se usan.
- Un hook para leer el historial (lo hará el CU-04).

## Resultado esperado

La capa de datos de la devolución está lista y probada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
