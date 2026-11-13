# UT-CU12-FE-02 — Layout y ruta del Directivo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-FE-02` |
| **Nombre** | Layout y ruta del Directivo |
| **Caso de uso** | CU12-FE |
| **Depende de** | — |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md): layouts por rol (un directorio por rol, barrel), guard `_authenticated` y mapeo rol → layout.
- Como referencia: `F/layouts/admin/` y `F/routes/_authenticated/publicar-resultados.tsx`.

## Solicitud de negocio

El Directivo necesita su propia entrada al sistema, con un menú que solo muestre lo suyo.

## Objetivo

El Directivo entra y aterriza en `/dashboard`, dentro de su propio layout.

## Alcance

- `F/layouts/directivo/`: `DirectivoLayout.tsx` (arma `<AppShell>` con `directivoNavItems` y el nombre de la app), `directivoNavItems.ts` (un solo ítem: «Dashboard», `/dashboard`, ícono de `lucide-react`), `directivoTypes.ts` (`DirectivoLayoutProps`) e `index.ts` (exporta `DirectivoLayout` y su tipo). `directivoNavItems` y `directivoTypes` no se exportan por el barrel.
- `F/layouts/index.ts`: agrega `export * from './directivo'`.
- `F/routes/_authenticated/dashboard.tsx`: `createFileRoute('/_authenticated/dashboard')`, `beforeLoad: exigirRol('DIRECTIVO')`, y monta `DashboardScreen` (UT-03) con `titulo="Dashboard de indicadores"` y `activeHref="/dashboard"`. Hasta que exista la pantalla, un contenido provisional mínimo que no se deja en producción.
- `F/features/auth/rutaInicioPorRol.ts`: `DIRECTIVO` → `/dashboard` (se amplía el tipo de retorno); se actualiza su comentario y los tests de `logica.test.ts` y `guards.test.ts`.
- Tests: el menú del Directivo tiene un solo ítem; un usuario con otro rol que entra a `/dashboard` es rechazado por el guard; `rutaInicioPorRol('DIRECTIVO')` devuelve `/dashboard`.

Archivos esperados (crear): el directorio `layouts/directivo/`, la ruta y sus tests. (Tocar): `layouts/index.ts`, `rutaInicioPorRol.ts` y sus tests.

## Fuera de alcance (no tocar)

- La pantalla del dashboard (UT-03).
- El menú de los layouts Admin y Estudiante.
- Layouts de `PERSONAL_COMEDOR` (lo fija el CU de asistencia).
- `routeTree.gen.ts` (se regenera solo y no se versiona).

## Resultado esperado

Un Directivo que inicia sesión llega a `/dashboard` y ve solo su menú.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con el Directivo de prueba (`UT-CU12-BE-08`), el login lleva a `/dashboard`; con un Administrador, `/dashboard` no se abre.
