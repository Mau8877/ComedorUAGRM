# UT-CU19-FE-04 — Insignia «Devuelta» en la lista de finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-04` |
| **Nombre** | Insignia «Devuelta» en la lista de finalistas |
| **Caso de uso** | CU19-FE |
| **Depende de** | `UT-CU19-BE-06`, `UT-CU19-FE-03` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- `F/features/finalistas/components/` (`columnasFinalistas.tsx`, `EstadoBadges.tsx`) y el CU-21.

## Solicitud de negocio

En la lista de finalistas la DUBSS debe ver quién fue devuelto para no intentar auditarlo.

## Objetivo

La tabla de finalistas muestra la insignia «Devuelta» y su estado en la columna «Auditoría».

## Alcance

- `F/features/finalistas/types`: `Finalista` gana `estadoPostulacion` (`string`, como lo manda el backend).
- `EstadoBadges`/`columnasFinalistas`: cuando `estadoPostulacion` es `devuelta`, la columna «Auditoría» muestra la insignia «Devuelta» (token `warning` del tema, sin valores arbitrarios) en vez del resultado de la última auditoría; el botón «Auditar» sigue abriendo el expediente.
- `DataCards` (vista mobile) usa la misma definición de columnas, sin duplicar.
- Se actualizan fixtures y tests de finalistas y del expediente que construyen un `Finalista`.
- Tests: insignia «Devuelta», que un finalista «calificada» sigue mostrando el resultado de su auditoría y que «Auditar» sigue visible.

Archivos esperados (tocar): `types/index.ts`, `EstadoBadges.tsx`, `columnasFinalistas.tsx` y los tests de finalistas.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Quitar de la lista al finalista devuelto: sigue apareciendo.
- Cambios en notificar y registrar la entrega (CU-21).

## Resultado esperado

La lista de finalistas distingue a los devueltos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
