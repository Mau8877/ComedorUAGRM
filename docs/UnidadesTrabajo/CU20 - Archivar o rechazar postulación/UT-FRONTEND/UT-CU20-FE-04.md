# UT-CU20-FE-04 — Insignias «Archivada» y «No adjudicada» en la lista de finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-04` |
| **Nombre** | Insignias «Archivada» y «No adjudicada» en la lista de finalistas |
| **Caso de uso** | CU20-FE |
| **Depende de** | `UT-CU20-BE-05`, `UT-CU20-FE-03` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- `F/features/finalistas/components/` (`columnasFinalistas.tsx`, `EstadoBadges.tsx`) tal como los dejó `UT-CU19-FE-04`.

## Solicitud de negocio

En la lista de finalistas la DUBSS debe ver quién fue archivado o no adjudicado para no intentar auditarlo.

## Objetivo

La tabla de finalistas muestra las insignias «Archivada» y «No adjudicada» en la columna «Auditoría».

## Alcance

- `Finalista.estadoPostulacion` ya existe (CU-19); no se agrega nada al tipo.
- `EstadoBadges`/`columnasFinalistas`: cuando `estadoPostulacion` es `archivada` o `no_adjudicada`, la columna «Auditoría» muestra la insignia correspondiente en vez del resultado de la última auditoría (tokens del tema, sin valores arbitrarios: p. ej. `muted` para «Archivada» y `destructive` para «No adjudicada») *(estilo por revisar)*; «Auditar» sigue abriendo el expediente.
- `DataCards` (vista mobile) usa la misma definición de columnas, sin duplicar.
- Tests: cada insignia, que «calificada» y «devuelta» no cambian y que «Auditar» sigue visible.

Archivos esperados (tocar): `EstadoBadges.tsx`, `columnasFinalistas.tsx` y los tests de finalistas.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Quitar al finalista cerrado de la lista: sigue apareciendo.
- Cambios en notificar y registrar la entrega (CU-21) ni en la insignia «Devuelta».

## Resultado esperado

La lista de finalistas distingue a los archivados y a los no adjudicados.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
