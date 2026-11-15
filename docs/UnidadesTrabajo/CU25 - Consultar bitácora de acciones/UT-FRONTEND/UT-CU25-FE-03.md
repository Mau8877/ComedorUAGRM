# UT-CU25-FE-03 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-FE-03` |
| **Nombre** | Pruebas |
| **Caso de uso** | CU25-FRONTEND |
| **Depende de** | UT-CU25-FE-01, UT-CU25-FE-02 |

## Antes de implementar

[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md) (los tests de una feature viven en su
carpeta `tests/`).

## Solicitud de negocio

Que el contrato con el backend (nombres de filtros, campos ordenables) no se rompa sin avisar.

## Objetivo

Pruebas de la API, las claves de caché, las columnas y la pantalla completa.

## Alcance

- `bitacoraApi.test.ts`: manda los filtros como `filter[...]` y omite lo vacío; `/bitacora/acciones`; claves de
  caché; solo fecha, acción y entidad son ordenables; «Sistema» sin usuario; fecha en hora de La Paz.

- `BitacoraScreen.test.tsx` (con `apiClient` simulado): muestra usuario, entidad, IP y la fecha en hora de La Paz;
  «Sistema» sin usuario; sin acciones de fila; sin ids internos ni `detalle` crudo; primera consulta sin filtros;
  orden solo por fecha, acción y entidad; búsqueda con un solo pedido y vuelta a la página 1; fechas como
  `filter[desde]`/`filter[hasta]`; rango invertido avisa y no se manda; «Sin resultados»; error con «Reintentar».

Archivos esperados (crear/tocar): `bitacoraApi.test.ts`, `BitacoraScreen.test.tsx`, `montar.tsx`,
`clienteSimulado.ts` (en `src/features/bitacora/tests/`).

## Fuera de alcance (no tocar)

- El selector de acción (el `Select` de base-ui no se maneja bien en jsdom); lo cubre la prueba manual de FE-02.

## Resultado esperado

El contrato de la API de bitácora queda cubierto.

## Validación

1. `pnpm exec vitest run src/features/bitacora` → 16 pruebas en verde.
