# UT-CU04-FE-04 — Rutas, menú y apertura desde un aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-04` |
| **Nombre** | Rutas, menú y apertura desde un aviso |
| **Caso de uso** | CU04-FE |
| **Depende de** | `UT-CU04-FE-02`, `UT-CU04-FE-03`, `UT-CU04-BE-05` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) (layout por rol, guard, `routeTree.gen.ts` no se edita ni se commitea).
- Como referencia: `F/routes/_authenticated/resultados.tsx` y `estudianteNavItems.ts`.

## Solicitud de negocio

El postulante debe llegar a sus postulaciones desde el menú y desde el aviso de un cambio de estado.

## Objetivo

Las dos pantallas son accesibles por ruta y menú, y tocar un aviso abre el detalle de su postulación.

## Alcance

- `F/routes/_authenticated/mis-postulaciones.tsx` (lista) y `mis-postulaciones.$codigo.tsx` (detalle): `beforeLoad: exigirRol('POSTULANTE')`, montando cada pantalla con el layout del postulante. Si el segmento dinámico necesita su propio archivo hermano (no hijo), seguir el patrón de `finalistas_.$codigo.tsx`.
- `estudianteNavItems.ts`: opción «Mis postulaciones» (`/mis-postulaciones`) y actualización de `estudianteNavItems.test.ts`.
- `EstudianteLayout`: al tocar un aviso de la bandeja con `postulacionCodigo`, navegar a `/mis-postulaciones/{postulacionCodigo}`; sin código, comportamiento actual. Se actualiza el tipo de aviso del frontend (campo opcional) en `features/notificaciones/`.
- Tests: la ruta exige el rol `POSTULANTE`, el menú muestra la opción y el aviso con código navega al detalle (y el aviso sin código no navega).

Archivos esperados (crear): las dos rutas y sus tests. (Tocar): `estudianteNavItems.ts` y su test, `EstudianteLayout.tsx`, tipo de aviso en `features/notificaciones/`.

## Fuera de alcance (no tocar)

- Las pantallas (UT-02 y UT-03) y los hooks (UT-01).
- `routeTree.gen.ts` (se genera solo).
- El layout del Administrador y la bandeja del Administrador.
- Marcar avisos como leídos.

## Resultado esperado

Mis postulaciones es un módulo navegable y el aviso lleva directo al detalle.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Con sesión de postulante, el menú abre la lista y desde un aviso de devolución se abre el detalle de esa postulación.
3. Un usuario con otro rol no accede a las rutas.
