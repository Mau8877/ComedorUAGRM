# UT-CU05-FE-04 — API de la decisión: hooks, schema, tipos y mensajes de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-04` |
| **Nombre** | API de la decisión: hooks, schema, tipos y mensajes de error |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-BE-08` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md).
- Como referencia: `cierreSchema.ts`, `useArchivarPostulacion.ts`, `archivarPostulacionApi` y `mapearErrorExpedientes.ts` (CU-20).

## Solicitud de negocio

La pantalla del expediente necesita hablar con los endpoints de adjudicar y no adjudicar y entender sus errores.

## Objetivo

Existen los hooks de las dos mutaciones, el schema del motivo y los mensajes de error de la decisión.

## Alcance

- Todo en `F/features/expedientes/`.
- `types`: `NoAdjudicarRequest` (inferido del schema) y `DecisionAdjudicacionResultado` (`finalista`, `postulacion`, `resultado`, `estado`, `puntajeFinal` —puede ser `null`—, `fechaAdjudicacion`, `decididaPor`, `cuposDisponibles`).
- `schemas/noAdjudicarSchema.ts`: `motivo` obligatorio (`trim`, mínimo 1: «El motivo es obligatorio») y de **máximo 500 caracteres** («El motivo no puede pasar de 500 caracteres»). Adjudicar no tiene schema (no lleva cuerpo).
- `api/expedientesApi.ts`: `adjudicarFinalistaApi(finalista)` → `POST /finalistas/{codigo}/adjudicar` y `noAdjudicarFinalistaApi(finalista, pedido)` → `POST /finalistas/{codigo}/no-adjudicar`. **Sin** header de idempotencia.
- `api/useAdjudicarFinalista.ts` y `api/useNoAdjudicarFinalista.ts`: `useMutation` que al terminar invalidan `expedientesKeys.detail(codigo)`, `finalistasKeys.lists()` y `adjudicacionKeys.panels()` (importado del barrel de `features/adjudicacion`), para que los cupos se refresquen.
- `mapearErrorExpedientes`: `ERR_ADJ_01` → «La postulación ya no está calificada o los resultados ya se publicaron; se actualizó el expediente.» (con la marca para refrescar); `ERR_ADJ_02` → «El expediente debe estar auditado y validado para adjudicar. Puede auditarlo de nuevo o usar “No adjudicar”.» (si no hay auditoría, «Primero hay que auditar el expediente.»); `ERR_ADJ_03` → «El puntaje total no alcanza el puntaje mínimo de la convocatoria.»; `ERR_ADJ_04` → «Ya no quedan cupos disponibles. Solo se admite “No adjudicar”.» (con la marca para refrescar); `ERR_ADJ_05` → «La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.»; `ERR_FIN_01` ya está.
- Tests: schema (vacío, solo espacios, 500 y 501), cada llamada a la API con el cliente simulado, la invalidación de las tres claves y el mapeo de cada código.

Archivos esperados (crear): `noAdjudicarSchema.ts`, `useAdjudicarFinalista.ts`, `useNoAdjudicarFinalista.ts` y sus tests. (Tocar): `types`, `expedientesApi.ts`, `api/index.ts`, `schemas/index.ts` y `mapearErrorExpedientes.ts`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (UT-05, UT-06).
- Claves de idempotencia: no se usan.
- Un hook para leer el historial o la adjudicación (lo hará el CU-04 o el CU-08).
- Tocar `cierreSchema`, `devolucionSchema` ni los hooks del CU-19 y CU-20.

## Resultado esperado

La capa de datos de la decisión está lista y probada.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
