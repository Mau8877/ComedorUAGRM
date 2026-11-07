# UT-CU07-FE-01 — API: hooks, keys, schemas y mensajes de error del expediente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-01` |
| **Nombre** | API: hooks, keys, schemas y mensajes de error del expediente |
| **Caso de uso** | CU07-FE |
| **Depende de** | `UT-CU07-BE-10` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey`) y [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md).
- Como referencia: `F/features/finalistas/api/` (`finalistasApi.ts`, `keys.ts`, `useFinalistas.ts`, `mapearErrorFinalistas.ts`).

## Solicitud de negocio

La pantalla necesita hablar con el backend del expediente con la misma convención que el resto.

## Objetivo

Existe la feature `expedientes` con su capa de API, sus schemas y sus tipos.

## Alcance

- `F/features/expedientes/api/`: `expedientesApi.ts` (sobre `apiClient`), `keys.ts` (`['expedientes', 'detail', codigo]`), `useExpediente(codigo)`, `useRegistrarAuditoria()` y `useAccesoDocumento()` (mutación que pide la URL temporal). Tras registrar una auditoría invalida `['expedientes', 'detail', codigo]` y `['finalistas', 'list']`.
- `F/features/expedientes/schemas/auditoriaSchema.ts` (Zod): `resultado` (`VALIDADO` | `OBSERVADO` | `NO_VALIDO`); `observaciones` de hasta **500** caracteres y **obligatoria** si el resultado no es «Validado»; `puntajeAcademico` de 0 a 30 con hasta 2 decimales y **obligatorio** si es «Validado». Mensajes en español.
- `F/features/expedientes/types/` con `Expediente`, `DocumentoExpediente`, `Auditoria` y `ResultadoAuditoria`.
- `mapearErrorExpedientes.ts`: textos para `ERR_EXP_01` («Primero registra la entrega de la documentación física»), `ERR_EXP_02` («La postulación ya no está calificada»), `ERR_EXP_03`, `ERR_EXP_04` y `ERR_EXP_05`, sobre `getErrorMessage`.
- Tests: el schema (cada regla), la API contra un cliente simulado, y el mapeo de errores.

Archivos esperados (crear): los de arriba, `index.ts` del feature y sus tests.

## Fuera de alcance (no tocar)

- Componentes y pantallas (UT-02 a UT-05).
- Un `useQuery` o `axios` directo desde un componente.

## Resultado esperado

Una pantalla puede pedir el expediente, registrar una auditoría y abrir un documento sin tocar `apiClient`.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
