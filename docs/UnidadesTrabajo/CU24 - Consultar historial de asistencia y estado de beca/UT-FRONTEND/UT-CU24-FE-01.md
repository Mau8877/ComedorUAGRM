# UT-CU24-FE-01 — Tipos, API y hooks de Mi beca

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-FE-01` |
| **Nombre** | Tipos, API y hooks de Mi beca |
| **Caso de uso** | CU24-FE |
| **Depende de** | UT-CU24-BE-02 (el contrato HTTP) |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (`queryKey`, `apiClient`).

## Solicitud de negocio

La pantalla necesita pedir la beca y el historial del usuario sin armar `queryKey` ni llamadas a mano.

## Objetivo

La feature `mibeca` tiene sus tipos, su fábrica de keys y los hooks de lectura.

## Alcance

- `features/mibeca/types/index.ts`: `MiBeca`, `AsistenciaBecado`, `EstadoBeca`, `Pagina<T>`, `AsistenciasParams`.
- `features/mibeca/api/keys.ts` (`mibecaKeys.detail`, `list`), `mibecaApi.ts` (`obtenerMiBecaApi`, `listarAsistenciasApi` con `filter[desde]`/`filter[hasta]`/`sort`; sin ningún identificador) y `hooks.ts` (`useMiBeca` sin reintentos, `useAsistenciasBecado` con `keepPreviousData` y `enabled`).
- Barrels `api/index.ts` e `index.ts` de la feature.

Archivos esperados (crear/tocar): los de `features/mibeca/api/`, `types/` e `index.ts`.

## Fuera de alcance (no tocar)

`apiClient`, otras features, la pantalla y la ruta (FE-02).

## Resultado esperado

`useMiBeca()` y `useAsistenciasBecado(params)` devuelven lo del backend ya desempaquetado.

## Validación

1. `pnpm exec tsc -b` y `pnpm lint` sin errores.
2. `mibecaApi.test.ts` (ver FE-03).
