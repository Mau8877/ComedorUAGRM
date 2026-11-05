# UT-CU15-MOB-03 — Pantalla de solicitud de recuperación y enlace en el login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-03` |
| **Nombre** | Pantalla de solicitud de recuperación y enlace en el login |
| **Caso de uso** | CU15-MOB |
| **Depende de** | `UT-CU15-MOB-01`, `UT-CU15-MOB-02` |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (el `redirect` global)
- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)
- Como referencia: `M/features/auth/screens/login_screen.dart` y `M/core/routing/app_router.dart`

## Solicitud de negocio

Quien olvidó su contraseña la recupera desde el login de la app sin ayuda del personal y sin que se sepa si su correo existe.

## Objetivo

Desde el login se llega a una pantalla que pide el correo y muestra siempre el mismo mensaje.

## Alcance

- `M/features/auth/screens/recuperar_contrasena_screen.dart`: `Scaffold` con `Form` y `TextFormField` del correo (validador de `auth_validators.dart`: obligatorio y con formato), botón «Enviar enlace» con indicador de carga y botón «Volver a iniciar sesión». Tras enviar, reemplaza el formulario por el mensaje fijo «Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos. Ábrelo desde tu correo: se abre en el navegador» con un botón «Enviar de nuevo» (A11). Un error de red muestra el mensaje con «Reintentar» (A9, A10); ningún otro error revela nada.
- `M/features/auth/screens/login_screen.dart`: enlace de texto «¿Olvidó su contraseña?» debajo de la contraseña, que va a `/recuperar-contrasena` (CA21).
- `M/core/routing/app_router.dart`: `GoRoute('/recuperar-contrasena')`; la función pura `redirectDeSesion` la trata como pública (sin sesión se puede entrar; con sesión lleva al módulo del rol, como `/login` y `/registro`).
- Tests: `redirectDeSesion` deja entrar a `/recuperar-contrasena` sin sesión y con sesión redirige al módulo; el mensaje neutro es el mismo para cualquier respuesta correcta; un error de red ofrece reintentar y un `400` de correo vacío no llama a la API (validación de formulario).

Archivos esperados (crear): `recuperar_contrasena_screen.dart`. (Tocar): `login_screen.dart`, `app_router.dart` y su test del redirect.

## Fuera de alcance (no tocar)

- Definir la contraseña nueva desde la app: se hace en la web con el enlace.
- Deep links hacia la app.
- Mostrar el correo o cualquier pista sobre si la cuenta existe.

## Resultado esperado

Desde el login de la app se pide el enlace y siempre se ve el mismo mensaje.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Con el stack levantado, pedir el enlace con un correo registrado deja el correo en `http://localhost:8025`.
