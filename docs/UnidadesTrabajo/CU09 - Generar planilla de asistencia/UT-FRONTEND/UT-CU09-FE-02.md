# UT-CU09-FE-02 — Validación del periodo (Zod)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-02` |
| **Nombre** | Validación del periodo (Zod) |
| **Caso de uso** | CU09-FE |
| **Depende de** | `UT-CU09-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (un schema, mensajes en español, solo se muestra el error de un campo tocado).
- Como referencia: `F/features/becas/schemas/motivoSchema.ts`.

## Solicitud de negocio

El Administrador debe ver enseguida que el periodo es inválido, sin esperar la respuesta del servidor.

## Objetivo

`crearPeriodoSchema` valida el periodo con las mismas reglas que el backend.

## Alcance

- `F/features/planillas/schemas/periodoSchema.ts`: `MAX_DIAS_PLANILLA = 31` y la fábrica `crearPeriodoSchema(gestion?)` con `desde` y `hasta` (`AAAA-MM-DD`): ambas obligatorias; final no anterior a la inicial (`ERR_PLA_01`); dentro de la gestión cuando se conocen sus límites (`ERR_PLA_02`), con el error en el campo que se sale; no más de 31 días (`ERR_PLA_03`). Sin los límites todavía (el resumen no llegó) solo valida las fechas y su orden.
- Tests: periodo válido, de un solo día y en los límites de la gestión, fechas faltantes, final anterior, fuera de la gestión por cada lado, 31 días sí y 32 no, y sin límites de gestión.

Archivos esperados (crear): `periodoSchema.ts`, `schemas/index.ts` y `tests/periodoSchema.test.ts`.

## Fuera de alcance (no tocar)

- El formulario y la pantalla (FE-03).
- Fechas por defecto o cálculo de periodos sugeridos.

## Resultado esperado

El schema devuelve el mensaje exacto para cada regla rota.

## Validación

1. `pnpm test -- src/features/planillas/tests/periodoSchema.test.ts` pasa (7 tests).
