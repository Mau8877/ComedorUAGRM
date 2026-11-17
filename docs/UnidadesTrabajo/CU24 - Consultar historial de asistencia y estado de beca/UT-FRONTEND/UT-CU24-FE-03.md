# UT-CU24-FE-03 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-FE-03` |
| **Nombre** | Pruebas del frontend de Mi beca |
| **Caso de uso** | CU24-FE |
| **Depende de** | UT-CU24-FE-02 |

## Antes de implementar

[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md) (tests de una feature en su carpeta `tests/`, `apiClient` simulado).

## Solicitud de negocio

Garantizar que la pantalla refleja el contrato y los flujos alternativos del CU.

## Objetivo

La API y la pantalla tienen pruebas con el `apiClient` simulado.

## Alcance

- `features/mibeca/tests/mibecaApi.test.ts`: la beca sin identificadores; `page`, `pageSize`, `filter[desde]`, `filter[hasta]` y `sort`; omite lo opcional.
- `features/mibeca/tests/MiBecaScreen.test.tsx`: estado y resumen, baja con motivo, hora en La Paz, solo lectura, historial vacío, rango, rango invertido, rango sin resultados, sin beca (A1), error con reintento y sin ids internos.
- `layouts/estudiante/estudianteNavItems.test.ts`: la entrada «Mi beca».

Archivos esperados (crear/tocar): los tests de arriba y sus ayudas (`montar.tsx`, `clienteSimulado.ts`).

## Fuera de alcance (no tocar)

Tests de otras features.

## Resultado esperado

CA03 a CA06 y CA11 quedan cubiertos del lado de la web.

## Validación

1. `pnpm exec vitest run src/features/mibeca src/layouts` en verde; `pnpm test` completo sin regresiones.
