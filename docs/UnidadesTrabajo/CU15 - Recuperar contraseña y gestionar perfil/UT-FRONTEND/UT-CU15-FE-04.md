# UT-CU15-FE-04 — Pantalla de restablecimiento desde el enlace

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-04` |
| **Nombre** | Pantalla de restablecimiento desde el enlace |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-01`, `UT-CU15-FE-02` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Quien abre el enlace de su correo define una contraseña nueva y vuelve a entrar; si el enlace ya no sirve, lo sabe y puede pedir otro.

## Objetivo

El enlace del correo abre una pantalla para definir la contraseña nueva y, al terminar, lleva al login.

## Alcance

- `F/routes/restablecer-contrasena.tsx`: ruta pública con `redirigirSiHaySesion` y `validateSearch` que acepta `token` (cadena opcional).
- `F/features/auth/screens/RestablecerContrasenaScreen.tsx`:
  - Lee el `token` de la URL, lo guarda en el estado de la pantalla y lo **quita de la dirección** (`navigate({ replace: true, search: {} })`) para que no quede en el historial ni en el `Referer`.
  - Sin token (o vacío) muestra directamente el aviso de enlace inválido.
  - Formulario: «Nueva contraseña» y «Confirmar contraseña» (`type="password"`, `autoComplete="new-password"`), con el requisito visible («De 6 a 72 caracteres») y los errores inline del schema (CA07). Botón «Guardar contraseña» con estado de carga.
  - Éxito → toast «Contraseña actualizada» («Inicia sesión con tu nueva contraseña.», sin datos sensibles) y navega a `/login`. Se usa el toast global (`toastSuccess`) y no estado de navegación: así el login no necesita cambios.
  - `ERR_AUTH_08` → reemplaza el formulario por «El enlace ya no sirve (venció, ya se usó o se pidió uno más reciente)» con el botón «Solicitar un enlace nuevo» → `/recuperar-contrasena` (A2). `ERR_SYS_01` y red se muestran sobre el formulario sin perder lo escrito.
  - El token y las contraseñas no se guardan fuera del estado del formulario y no se escriben en la consola.
- `F/main.tsx`: `/recuperar-contrasena` y `/restablecer-contrasena` se agregan a `RUTAS_PUBLICAS` (una sesión perdida ahí no redirige al login).
- Tests: sin token → aviso de enlace inválido; con token válido el formulario se muestra y el token ya no está en la URL; contraseña corta o sin coincidir → error inline y sin llamada a la API; éxito → toast y navega al login; `ERR_AUTH_08` → aviso con enlace a la solicitud; el cuerpo enviado lleva `token`, `contrasena` y `confirmacionContrasena` y el token no está en ningún `localStorage` ni `sessionStorage`.

Archivos esperados (crear): la ruta, la pantalla y su test. (Tocar): `main.tsx` y el barrel de pantallas.

## Fuera de alcance (no tocar)

- Iniciar sesión automáticamente tras restablecer.
- Validar el token contra el backend antes de mostrar el formulario (el backend no expone ese endpoint; el rechazo llega al guardar).
- La pantalla de solicitud (UT-03).

## Resultado esperado

El enlace del correo permite definir la contraseña nueva y entrar con ella.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. Con el stack levantado: pedir el enlace, abrirlo desde Mailpit, definir la contraseña, entrar con ella y comprobar que el mismo enlace ya no sirve.
