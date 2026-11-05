# UT-CU15-FE-03 — Pantalla de solicitud de recuperación y enlace en el login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-03` |
| **Nombre** | Pantalla de solicitud de recuperación y enlace en el login |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-01`, `UT-CU15-FE-02` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (error inline solo si el campo fue tocado)
- Como referencia: `F/features/auth/screens/LoginScreen.tsx` y `F/routes/login.tsx`

## Solicitud de negocio

Quien olvidó su contraseña la recupera desde el login, sin ayuda del personal y sin que se sepa si su correo existe en el sistema.

## Objetivo

Desde el login se llega a una pantalla que pide el correo y muestra siempre el mismo mensaje.

## Alcance

- `F/features/auth/screens/RecuperarContrasenaScreen.tsx`: tarjeta como la del login (`PublicHeader`, `Card`), un campo de correo (`CampoFormulario`), botón «Enviar enlace» con estado de carga y enlace «Volver a iniciar sesión». Tras enviar, **reemplaza el formulario** por el mensaje fijo «Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos. Revisa también la carpeta de spam.» (sin repetir el correo y sin distinguir casos) y un botón «Enviar de nuevo» que vuelve al formulario (A11). Un error de red muestra el mensaje con «Reintentar» (A10); no hay otro mensaje de error.
- `F/routes/recuperar-contrasena.tsx`: ruta pública con `redirigirSiHaySesion` (igual que `/login`).
- `F/features/auth/screens/LoginScreen.tsx`: un enlace «¿Olvidó su contraseña?» debajo del campo de contraseña que lleva a `/recuperar-contrasena` (CA21).
- Barrel: exportar la pantalla.
- Tests (Testing Library): el login muestra el enlace y navega; con un correo vacío o mal formado muestra el error inline y no llama a la API; con un correo válido llama a la API y muestra el mensaje neutro; el mensaje neutro es idéntico para cualquier respuesta `200` del backend (no depende de si la cuenta existe); un error de red muestra el aviso de conexión con «Reintentar»; el botón se deshabilita mientras envía.

Archivos esperados (crear): `RecuperarContrasenaScreen.tsx`, `routes/recuperar-contrasena.tsx` y su test. (Tocar): `LoginScreen.tsx`, `features/auth/screens/index.ts`, y el test del login (el test existente comprueba la presencia del enlace).

## Fuera de alcance (no tocar)

- La pantalla de la contraseña nueva (UT-04).
- Cambiar el diseño del resto del login.
- Mostrar el correo, un contador o cualquier pista sobre si la cuenta existe.

## Resultado esperado

Desde el login se pide el enlace y la persona ve siempre el mismo mensaje.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. Con el stack levantado, pedir el enlace con un correo registrado deja el correo en `http://localhost:8025`.
