# UT-CU05-FE-07 — Insignias «Adjudicada» y «No adjudicada» en la lista de finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-07` |
| **Nombre** | Insignias «Adjudicada» y «No adjudicada» en la lista de finalistas |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-BE-09`, `UT-CU05-FE-06` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- `F/features/finalistas/components/` (`columnasFinalistas.tsx`, `EstadoBadges.tsx`) tal como los dejó `UT-CU20-FE-04`.

## Solicitud de negocio

En la lista de finalistas la DUBSS debe ver quién ya fue adjudicado o no adjudicado para no intentar auditarlo ni decidirlo otra vez.

## Objetivo

La tabla de finalistas muestra las insignias «Adjudicada» y «No adjudicada» en la columna «Auditoría».

## Alcance

- `Finalista.estadoPostulacion` ya existe (CU-19 y CU-20); no se agrega nada al tipo.
- `EstadoBadges`/`columnasFinalistas`: cuando `estadoPostulacion` es `adjudicada`, la columna «Auditoría» muestra la insignia «Adjudicada» (tokens del tema, sin valores arbitrarios: p. ej. `success`) en vez del resultado de la última auditoría *(estilo por revisar)*; `no_adjudicada` ya muestra «No adjudicada» por el CU-20 y no cambia (no se distingue aquí entre rechazo y decisión final: ambas son «No adjudicada» para la DUBSS). «Auditar» sigue abriendo el expediente, que es donde se decide.
- `DataCards` (vista mobile) usa la misma definición de columnas, sin duplicar.
- Tests: la insignia «Adjudicada»; que «calificada», «devuelta», «archivada» y «no adjudicada» no cambian y que «Auditar» sigue visible.

Archivos esperados (tocar): `EstadoBadges.tsx`, `columnasFinalistas.tsx` y los tests de finalistas.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Quitar al finalista decidido de la lista: sigue apareciendo.
- Cambios en notificar y registrar la entrega (CU-21) ni en las insignias «Devuelta», «Archivada» y «No adjudicada» del CU-19 y CU-20.
- Un botón de adjudicar en la tabla: la decisión se toma desde el expediente.

## Resultado esperado

La lista de finalistas distingue a los adjudicados de los demás estados.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
