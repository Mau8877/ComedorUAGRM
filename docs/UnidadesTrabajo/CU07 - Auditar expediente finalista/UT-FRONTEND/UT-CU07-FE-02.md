# UT-CU07-FE-02 — Columna «Auditoría» y acción «Auditar» en la tabla de finalistas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-02` |
| **Nombre** | Columna «Auditoría» y acción «Auditar» en la tabla de finalistas |
| **Caso de uso** | CU07-FE |
| **Depende de** | `UT-CU07-FE-01` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md).
- Las fichas del CU-21 (`UT-CU21-FE-02`) y los archivos `F/features/finalistas/components/TablaFinalistas.tsx`, `columnasFinalistas.tsx` y `EstadoBadges.tsx`.

## Solicitud de negocio

El administrador ve quién ya fue auditado y entra a auditar desde la misma lista de finalistas.

## Objetivo

La tabla de finalistas del CU-21 muestra el resultado de la última auditoría y tiene la acción «Auditar».

## Alcance

- Los tipos de `finalistas` ganan `resultadoAuditoria` y `puntajeTotal`.
- `columnasFinalistas.tsx`: columna **Auditoría** con una insignia («Sin auditar», «Validado», «Observado», «No válido») y, en el puntaje, el total (IA + académico) como dato secundario cuando exista. `meta` de cards: `cardLabel` para la nueva columna.
- `TablaFinalistas` recibe `onAuditar(finalista)` y agrega un `RowActionButton` «Auditar» (ícono + tooltip) en las acciones de la fila.
- Sin `sort` ni filtro nuevo.
- Tests: las cuatro insignias, el total cuando existe, y que «Auditar» llama a `onAuditar` con el finalista.

Archivos esperados (tocar): `EstadoBadges.tsx`, `columnasFinalistas.tsx`, `TablaFinalistas.tsx`, los tipos y los tests de finalistas.

## Fuera de alcance (no tocar)

- La ruta y la pantalla del expediente (UT-05).
- Notificar y registrar entrega desde la lista (siguen igual).

## Resultado esperado

La lista muestra el resultado de cada auditoría y la acción «Auditar».

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
