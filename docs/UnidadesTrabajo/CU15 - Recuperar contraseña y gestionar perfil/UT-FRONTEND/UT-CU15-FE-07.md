# UT-CU15-FE-07 — Cambiar la contraseña desde el perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-07` |
| **Nombre** | Cambiar la contraseña desde el perfil |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-01`, `UT-CU15-FE-02`, `UT-CU15-FE-05` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El usuario cambia su contraseña cuando quiere, confirmando la actual, sin perder la sesión en la que está.

## Objetivo

La tarjeta «Cambiar contraseña» del perfil cambia la contraseña y deja abierta esta sesión.

## Alcance

- `F/features/perfil/components/CambiarContrasenaForm.tsx` (TanStack Form + `cambiarContrasenaSchema`): «Contraseña actual», «Contraseña nueva» y «Confirmar contraseña nueva» (`type="password"`, `autoComplete` correcto en cada una), con el requisito visible («De 6 a 72 caracteres») y errores inline solo en campos tocados (CA07). Botón «Cambiar contraseña» con estado de carga.
- Éxito → aviso «Contraseña actualizada. Cerramos tu sesión en los demás dispositivos.», el formulario se limpia y **la sesión actual sigue abierta** (no se navega ni se limpia la caché).
- `ERR_AUTH_09` → error en «Contraseña actual» (A6); `ERR_SYS_01` → mensaje del backend; red → conexión con «Reintentar». Un `400` nunca desloguea (el backend responde `400` y no `401` justamente para esto).
- La tarjeta tiene el `id="cambiar-contrasena"` y, si la URL llega con ese hash (desde «Cambiar contraseña» del header), la pantalla hace scroll hasta ella y enfoca el primer campo.
- Las contraseñas no se guardan fuera del estado del formulario ni se escriben en la consola.
- `PerfilScreen.tsx`: monta el componente en la tercera sección.
- Tests: contraseña corta o sin coincidir → error inline y sin llamada; envío correcto manda `contrasenaActual`, `contrasenaNueva` y `confirmacionContrasena` y **no** manda ningún token; éxito limpia el formulario y deja la sesión; `ERR_AUTH_09` marca el campo; el hash `#cambiar-contrasena` hace scroll al formulario.

Archivos esperados (crear): `CambiarContrasenaForm.tsx` y su test. (Tocar): `PerfilScreen.tsx` y el barrel de `components`.

## Fuera de alcance (no tocar)

- Cerrar la sesión local tras cambiar la contraseña.
- Forzar el cambio de la contraseña temporal en el primer acceso.
- Recuperar la contraseña (UT-03 y UT-04).

## Resultado esperado

El usuario cambia su contraseña desde el perfil y sigue en la misma sesión.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
