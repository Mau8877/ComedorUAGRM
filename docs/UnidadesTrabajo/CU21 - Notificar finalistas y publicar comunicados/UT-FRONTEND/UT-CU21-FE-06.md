# UT-CU21-FE-06 — Pantalla, ruta y entrada del sidebar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-06` |
| **Nombre** | Pantalla, ruta y entrada del sidebar |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-FE-02`, `UT-CU21-FE-03`, `UT-CU21-FE-04`, `UT-CU21-FE-05` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) (guard, layouts por rol) y [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).
- Como referencia: `F/routes/_authenticated/lista-priorizada.tsx`, `ListaPriorizadaScreen.tsx` y `F/layouts/admin/adminNavItems.ts`.

## Solicitud de negocio

El administrador entra a «Finalistas» y trabaja el CU en una sola pantalla.

## Objetivo

Existe la pantalla «Finalistas: notificación y documentación física», accesible solo para el administrador.

## Alcance

- `F/features/finalistas/screens/FinalistasScreen.tsx`: selector de convocatoria (`useConvocatoriasPriorizables` de `features/priorizacion`, la más reciente preseleccionada, A9) y debajo la tabla a la izquierda y, a la derecha, `RegistrarEntregaPanel` y `ComunicadoForm`. Un `404` de la convocatoria vuelve al selector (A9); sin convocatorias, «Todavía no hay convocatorias».
- `F/routes/_authenticated/finalistas.tsx` (misma forma que `lista-priorizada.tsx`, que hoy es donde viven las rutas del administrador) y la entrada «Finalistas» en `adminNavItems.ts`; un rol distinto al administrador no ve la opción ni la pantalla (A12). El `401` lo resuelve el cliente HTTP (A11).
- Estados de carga y de error con «Reintentar» (A14).
- Tests: pantalla completa contra un cliente simulado (elegir convocatoria, notificar, registrar entrega y publicar), convocatoria inexistente y la ruta/entrada del menú.

Archivos esperados (crear): `FinalistasScreen.tsx`, la ruta y sus tests. (Tocar): `adminNavItems.ts` y su test, barrels.

## Fuera de alcance (no tocar)

- El atajo desde la auditoría de expedientes (CU-07).
- Una sección «Adjudicación» en el menú.

## Resultado esperado

El administrador completa los flujos A, B y C desde una sola pantalla.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el backend y los datos de prueba (`UT-CU21-BE-04` y `12`), la pantalla muestra los 10 finalistas y permite notificar, registrar una entrega y publicar un comunicado.
