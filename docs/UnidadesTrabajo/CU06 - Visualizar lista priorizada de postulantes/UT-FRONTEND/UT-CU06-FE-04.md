# UT-CU06-FE-04 — Detalle de solo lectura de una postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-04` |
| **Nombre** | Detalle de solo lectura de una postulación |
| **Caso de uso** | CU06-FE |
| **Depende de** | `UT-CU06-FE-03` |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: los modales de `F/features/usuarios/components/` y `RowActionButton` en `F/components/ui/tanstack-table/actions/`.

## Solicitud de negocio

El administrador abre una fila de la lista para ver sus datos completos antes de pasar a auditar.

## Objetivo

Cada fila tiene una acción «Ver» que abre un diálogo con los datos de esa postulación, sin editar nada.

## Alcance

- `F/features/priorizacion/components/DetallePostulacionDialog.tsx`: muestra posición, nombre completo, CI, carrera, puntaje, fecha de cálculo y estado de la fila elegida (los datos que ya vienen en la lista; **no** se pide nada más al backend). Solo lectura.
- En `ListaPriorizadaScreen`: la acción «Ver» de cada fila (`RowActionButton`, ícono y tooltip) abre el diálogo; se cierra con «Cerrar» o `Escape` y devuelve el foco a la fila.
- El diálogo **no** incluye ningún botón de auditoría ni de adjudicación (los agrega el CU-05).
- Tests: «Ver» abre el diálogo con los datos de la fila; no hay campos editables; se cierra con «Cerrar» y con `Escape`; funciona también desde la vista de tarjetas.

Archivos esperados (crear): `DetallePostulacionDialog.tsx` y su test. (Tocar): `ListaPriorizadaScreen.tsx`.

## Fuera de alcance (no tocar)

- Mostrar respuestas socioeconómicas o documentos del expediente (CU-07).
- El paso a la auditoría y la actualización de una postulación que cambió de estado (CU-05).
- Endpoints nuevos.

## Resultado esperado

El administrador ve el detalle de una postulación de la lista sin salir de la pantalla.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
