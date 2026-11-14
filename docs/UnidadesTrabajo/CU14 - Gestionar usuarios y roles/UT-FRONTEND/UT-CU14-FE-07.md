# UT-CU14-FE-07 — Desactivar y reactivar una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-07` |
| **Nombre** | Desactivar y reactivar una cuenta |
| **Caso de uso** | CU14-FE |
| **Depende de** | `UT-CU14-FE-02`, `UT-CU14-FE-04` |

## Antes de implementar

- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (acciones de fila)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)

## Solicitud de negocio

Cuando alguien deja el cargo se desactiva su cuenta, y si vuelve se reactiva, siempre con confirmación (flujos E y F).

## Objetivo

Cada fila ofrece «Desactivar» (si está activa) o «Reactivar» (si está desactivada), con confirmación.

## Alcance

- `F/features/usuarios/components/CambiarEstadoUsuarioDialog.tsx`: confirmación con el sistema de modales existente (`modal-destructive` para desactivar), que nombra a la persona y explica la consecuencia («perderá el acceso y se cerrarán sus sesiones»; para reactivar, «recupera su rol anterior»). Usa `useDesactivarUsuario` o `useReactivarUsuario`. Mientras envía bloquea el botón.
- Errores: `ERR_US_08` y `ERR_US_09` (A5) se muestran como aviso sin cambiar la cuenta; `ERR_US_07` (A9) y `ERR_US_06` (A8) avisan, cierran el diálogo y refrescan la lista; error de red, con reintento (A10). Al terminar bien, un toast y la lista refrescada.
- Conecta la acción a las filas de `UsuariosPersonalScreen` con `RowActionButton`: «Desactivar» solo en cuentas activas y «Reactivar» solo en desactivadas. En la fila de la **propia cuenta** no se ofrece «Desactivar» (solo ayuda de interfaz: el backend responde `403` `ERR_US_08` igual).
- Tests: cada fila ofrece la acción que corresponde a su estado; la propia fila no ofrece «Desactivar»; confirmar llama al endpoint correcto y refresca la lista; cancelar no llama a nada; `ERR_US_09` muestra el aviso sin cerrar la cuenta como desactivada; `ERR_US_07` y `ERR_US_06` refrescan.

Archivos esperados (crear/tocar): el diálogo, `UsuariosPersonalScreen.tsx`, el barrel de `components/` y sus tests.

## Fuera de alcance (no tocar)

- Eliminar una cuenta: no existe esa acción.
- Editar y cambiar el rol (`UT-CU14-FE-06`).
- Desactivar o reactivar varias cuentas a la vez.

## Resultado esperado

El Administrador desactiva y reactiva cuentas con confirmación y ve el cambio de estado en la lista.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con el backend levantado, una cuenta desactivada no puede iniciar sesión y, reactivada, sí.
