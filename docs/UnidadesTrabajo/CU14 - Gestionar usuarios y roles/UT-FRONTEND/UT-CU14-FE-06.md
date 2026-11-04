# UT-CU14-FE-06 — Editar los datos y cambiar el rol de una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-06` |
| **Nombre** | Editar los datos y cambiar el rol de una cuenta |
| **Caso de uso** | CU14-FE |
| **Depende de** | `UT-CU14-FE-01`, `UT-CU14-FE-02`, `UT-CU14-FE-04` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (acciones de fila con `RowActionButton`)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

Los datos de un funcionario cambian y las personas cambian de funciones: el Administrador corrige sus datos y le cambia el rol (flujos C y D).

## Objetivo

Cada fila ofrece «Editar» y «Cambiar rol», cada uno con su modal.

## Alcance

- `F/features/usuarios/components/EditarUsuarioModal.tsx`: formulario con `editarUsuarioPersonalSchema` y los datos actuales precargados (`nombres`, `apellidos`, `correo`); el **CI se muestra solo de lectura** (CA10). Envía solo lo que cambió, usando `useEditarUsuario`. Errores inline y solo si el campo fue tocado: `ERR_US_02` marca el correo; `ERR_US_06` avisa que la cuenta ya no existe, cierra el modal y refresca la lista (A8).
- `F/features/usuarios/components/CambiarRolModal.tsx`: select con los **otros** roles del personal (no el actual ni `POSTULANTE`) y una confirmación que explica que la persona tendrá que iniciar sesión de nuevo. Usa `useCambiarRol`. Errores: `ERR_US_08` y `ERR_US_09` (A5) se muestran como aviso sin cambiar nada; `ERR_US_12` y `ERR_US_06` refrescan la lista (A9, A8). Al terminar bien, un toast («X ahora es Directivo») y la cuenta sale de la vista actual porque la lista se refresca.
- Conecta ambas acciones a las filas de `UsuariosPersonalScreen` con `RowActionButton` (ícono y tooltip) en `renderActions`, tanto en la tabla como en las cards. En la fila de la **propia cuenta** (`codigo` igual al de `GET /auth/usuario-actual`) no se ofrece «Cambiar rol» (solo ayuda de interfaz: el backend responde `403` igual).
- Tests: precarga y CI solo de lectura; se envía solo lo modificado; `ERR_US_02` marca el correo; el selector de rol no ofrece el rol actual ni Postulante; la propia fila no ofrece «Cambiar rol»; `ERR_US_08`, `ERR_US_09` y `ERR_US_06` muestran su mensaje y la lista se refresca.

Archivos esperados (crear/tocar): los dos modales, `UsuariosPersonalScreen.tsx`, el barrel de `components/` y sus tests.

## Fuera de alcance (no tocar)

- Editar el CI o la contraseña.
- Desactivar y reactivar (`UT-CU14-FE-07`).
- Cambiar el rol de varias cuentas a la vez.

## Resultado esperado

El Administrador edita los datos de una cuenta y le cambia el rol, y la lista queda al día.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
