# UT-CU03-FE-01 — Pantalla «Precalificación con IA»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-FE-01` |
| **Nombre** | Pantalla «Precalificación con IA» |
| **Caso de uso** | CU03-FRONTEND |
| **Depende de** | UT-CU03-BE-10 |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (queryKey `['preseleccion', 'precalificacion', codigo]`) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

La DUBSS necesita ver por qué una postulación tiene su puntaje: cuánto aportó cada variable y qué pasó con ella.

## Objetivo

Una pantalla de solo lectura, enlazada desde el detalle de la lista priorizada.

## Alcance

- Tipos, `obtenerPrecalificacionApi`, `usePrecalificacion`, clave de caché y `ERR_MOD_08` en `mapearErrorPreseleccion`.
- Componentes: `PuntajeGauge`, `AporteVariables`, `TrazaProceso`, `EstadoPrecalificacionBadge`; `nombreVariable()` y `formatearFechaHora()` (hora de La Paz).
- `PrecalificacionScreen` y la ruta `precalificacion.$codigo.tsx` (guard `exigirRol('ADMINISTRADOR_DUBSS')`, sidebar en «Lista priorizada»).
- Botón «Ver precalificación con IA» en `DetallePostulacionDialog`.
- Tests: `PrecalificacionScreen.test.tsx` (8 casos); el test de la lista priorizada simula el router.

Archivos esperados (crear/tocar): lo anterior más `types/index.ts`, `api/*`, `schemas/parametrosSchema.ts` y los índices.

## Fuera de alcance (no tocar)

- Editar nada: no hay acciones sobre la postulación.
- Duración del cálculo, ítem de menú, desmarcar una postulación marcada.

## Resultado esperado

Desde el detalle de una postulación priorizada se abre su precalificación con el puntaje de 70, el aporte de cada variable y la traza; una sin calificar o marcada para revisión lo dice.

## Validación

1. `pnpm exec vitest run src/features/preseleccion src/features/priorizacion` en verde.
2. `pnpm lint` y `pnpm build` sin errores.
3. Manual: lista priorizada → «Ver» en una fila → «Ver precalificación con IA».
