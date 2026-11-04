# UT-CU14-FE-05 — Crear cuenta y mostrar la contraseña temporal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-05` |
| **Nombre** | Crear cuenta y mostrar la contraseña temporal |
| **Caso de uso** | CU14-FE |
| **Depende de** | `UT-CU14-FE-01`, `UT-CU14-FE-02`, `UT-CU14-FE-04` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (un campo solo muestra su error si fue tocado)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)

## Solicitud de negocio

El Administrador crea la cuenta de un funcionario y le entrega la contraseña temporal que el sistema genera; esa contraseña no se debe poder volver a ver (flujo B).

## Objetivo

El botón «Nueva cuenta» abre un formulario que crea la cuenta y, al terminar, muestra la contraseña temporal una única vez.

## Alcance

- `F/features/usuarios/components/CrearUsuarioModal.tsx`: usa el sistema de modales existente (`modal-layout`) y `@tanstack/react-form` con `crearUsuarioPersonalSchema`: campos `nombres`, `apellidos`, `ci`, `correo` y `rol` (select con los tres roles del personal, por defecto el rol de la vista), errores inline y solo si el campo fue tocado, `onBlur` en cada campo. Al enviar usa `useCrearUsuario`; mientras envía bloquea el botón (evita el doble envío, CA22).
- Errores (`mapearErrorUsuarios`): `ERR_US_01` y `ERR_US_02` se marcan en el campo CI o correo (A1); `ERR_SYS_01` y `ERR_US_10`, en el formulario; error de red, con mensaje de conexión y reintento (A10). Mientras haya error el formulario conserva lo escrito.
- Pantalla de resultado dentro del mismo modal: «Cuenta creada», con la contraseña temporal, un botón **Copiar** (con confirmación visible) y el aviso de que **no se volverá a mostrar**; solo se cierra con un botón explícito. La contraseña vive solo en el estado del modal: no se guarda en `localStorage`, `sessionStorage`, un store ni la URL, no se escribe en la consola y se descarta al cerrar.
- Conecta el botón «Nueva cuenta» de la toolbar de `UsuariosPersonalScreen` (editando ese archivo); al cerrar, la lista ya está refrescada y se avisa con un toast si la cuenta creada es de otro rol distinto al de la vista.
- Tests: los errores inline de cada campo; envío válido con cada rol; `ERR_US_01` y `ERR_US_02` marcan su campo; la contraseña aparece solo en la pantalla de resultado, el botón Copiar usa el portapapeles, y tras cerrar el modal ya no está en el DOM ni en el almacenamiento del navegador; el doble clic envía una sola vez.

Archivos esperados (crear/tocar): `CrearUsuarioModal.tsx`, `UsuariosPersonalScreen.tsx`, el barrel de `components/` y sus tests.

## Fuera de alcance (no tocar)

- El formulario de edición y el cambio de rol (`UT-CU14-FE-06`).
- Obligar a cambiar la contraseña en el primer acceso, o enviarla por correo.
- Mostrar de nuevo una contraseña temporal ya entregada.

## Resultado esperado

El Administrador crea una cuenta y ve la contraseña temporal una sola vez para entregársela al funcionario.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` y `pnpm build` sin errores.
3. Con el backend levantado, la persona creada inicia sesión con la contraseña temporal mostrada.
