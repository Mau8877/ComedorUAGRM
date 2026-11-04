# UT-CU14-FE-03 — Navegación del Administrador y guard por rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-03` |
| **Nombre** | Navegación del Administrador y guard por rol |
| **Caso de uso** | CU14-FE |
| **Depende de** | — (edita archivos de `layouts/admin` y `features/auth` ya existentes) |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#regla-de-dependencia)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El Administrador debe llegar a la gestión de cuentas desde el sidebar, y nadie más debe poder entrar a esas pantallas (A7).

## Objetivo

El sidebar del Administrador tiene las tres entradas de la gestión de usuarios y existe un guard que exige el rol de Administrador DUBSS.

## Alcance

- `F/layouts/admin/adminNavItems.ts`: reemplaza la entrada genérica «Usuarios» (`/usuarios`) por tres entradas planas (`NavItem` no tiene submenús): «Administradores DUBSS» (`/usuarios/administradores`), «Directivos» (`/usuarios/directivos`) y «Personal del Comedor» (`/usuarios/personal-comedor`), cada una con un ícono de `lucide-react`. No se toca el resto de las entradas (Dashboard, Menús, Pedidos).
- `F/features/auth/guards.ts`: `exigirRol(rol)`, un `beforeLoad` que llama a `exigirSesion()` y, si el rol del usuario no es el pedido, hace `throw redirect({ to: rutaInicioPorRol(usuario.rol) })`; si lo es, devuelve `{ usuario }` en el contexto. Se exporta desde el barrel de `features/auth`.
- Tests: `exigirRol` deja pasar al rol pedido, redirige a su módulo a cualquier otro rol (Postulante, Directivo, Personal del Comedor) y redirige a `/login` sin sesión (por `exigirSesion`); las tres entradas del sidebar apuntan a las rutas correctas.

Archivos esperados (tocar): `adminNavItems.ts`, `features/auth/guards.ts` y sus tests.

## Fuera de alcance (no tocar)

- Crear las rutas `/usuarios/...` y su pantalla (`UT-CU14-FE-04`).
- Submenús o agrupaciones en el sidebar, y una entrada para Postulantes.
- Cambiar `exigirSesion` o `redirigirSiHaySesion` (CU01).

## Resultado esperado

El Administrador ve las tres entradas en el sidebar y `exigirRol('ADMINISTRADOR_DUBSS')` queda listo para proteger sus rutas.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
