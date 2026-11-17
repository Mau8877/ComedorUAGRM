# UT-CU16-FE-04 — Pantalla, ruta y acceso desde el detalle

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-FE-04` |
| **Nombre** | Pantalla, ruta y acceso desde el detalle |
| **Caso de uso** | CU16-FE |
| **Depende de** | `UT-CU16-FE-03` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El postulante debe poder llegar a sus documentos desde su postulación devuelta.

## Objetivo

Existe la ruta `/mis-postulaciones/{codigo}/documentos` y el botón «Gestionar documentos» en el detalle de una postulación devuelta.

## Alcance

- `F/features/postulaciones/screens/DocumentosPostulacionScreen.tsx`: `EstudianteLayout` + «Volver a la postulación» + el panel.
- `F/routes/_authenticated/mis-postulaciones_.$codigo.documentos.tsx`: ruta hermana del detalle, solo `POSTULANTE`.
- `F/features/postulaciones/screens/DetallePostulacionScreen.tsx`: sección «Tus documentos» con el botón, solo si `puedeCorregir`.
- `F/features/postulaciones/index.ts`: exporta `DocumentosPostulacionPanel` para el CU02.
- Tests: dos casos en `tests/DetallePostulacionScreen.test.tsx` (con y sin `puedeCorregir`).

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Agregar la opción al menú lateral: se llega desde una postulación.
- El formulario de postulación (CU02).

## Resultado esperado

Desde el detalle de una postulación devuelta se llega a sus documentos y se vuelve al detalle.

## Validación

1. `pnpm test` pasa completo.
2. `pnpm lint` y `pnpm exec tsc -b` sin errores; `pnpm exec vite build` regenera `routeTree.gen.ts` con la ruta nueva.
