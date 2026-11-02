# UT-CU01-FE-09 — Cerrar sesión desde el header y usuario real en el layout

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-09` |
| **Nombre** | Cerrar sesión desde el header y usuario real en el layout |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-05`, `UT-CU01-FE-08` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El usuario debe poder terminar su sesión desde cualquier pantalla privada, y el header debe mostrar quién es.

## Objetivo

El menú de usuario del `AppHeader` tiene una opción «Cerrar sesión» que funciona y muestra el nombre y rol reales.

## Alcance

- `src/components/layout/AppHeader.tsx` y `AppShell.tsx`: nueva prop opcional `onCerrarSesion` que se conecta al ítem «Cerrar sesión» del menú (hoy sin acción). Sin importar nada de `features/` (el componente recibe la función por props, regla de dependencia).
- Los layouts por rol (`AdminLayout`, `EstudianteLayout`) pasan la prop hacia abajo; quien monte el layout (las pantallas de cada rol) le entrega `useCerrarSesion()` y `LayoutUser` con el `nombre` y `rol` de `useUsuarioActual`.
- `panel.tsx` (de `UT-CU01-FE-08`): para `ADMINISTRADOR_DUBSS` y `POSTULANTE` se muestra dentro de su layout con el usuario real y el cierre de sesión funcionando; para los otros dos roles, el aviso con un botón «Cerrar sesión».
- Tests: el ítem llama a `onCerrarSesion`; cerrar sesión lleva a `/login` y ya no se puede abrir `/panel`; con la sesión expirada el cierre no muestra error.

Archivos esperados (tocar): `AppHeader.tsx`, `AppShell.tsx`, layouts de rol, `_authenticated/panel.tsx` y tests.

## Fuera de alcance (no tocar)

- Cambiar el diseño del header o de los layouts.
- Cerrar la sesión en otros dispositivos.
- Las pantallas reales de cada rol.

## Resultado esperado

Se puede cerrar sesión desde la web y volver al acceso, y el header muestra al usuario real.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. En el navegador: iniciar sesión, abrir el menú de usuario, «Cerrar sesión» lleva a `/login`; volver atrás no muestra `/panel`.
3. Sesión abierta en otro navegador sigue activa después de cerrar en este.
