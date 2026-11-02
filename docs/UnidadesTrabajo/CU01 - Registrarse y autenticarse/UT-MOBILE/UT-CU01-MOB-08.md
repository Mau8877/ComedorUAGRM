# UT-CU01-MOB-08 — Pantalla de inicio de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-08` |
| **Nombre** | Pantalla de inicio de sesión |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-05`, `UT-CU01-MOB-06` |

## Antes de implementar

- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)

## Solicitud de negocio

Cualquier usuario con cuenta debe poder entrar a la app con su correo y contraseña.

## Objetivo

La pantalla `LoginScreen` (ruta `/login`) permite iniciar sesión y lleva al módulo del rol.

## Alcance

- `lib/features/auth/screens/login_screen.dart`: `Form` con correo y contraseña (validators en español), botón «Iniciar sesión» con estado de carga, y enlace «Crear cuenta» hacia `/registro`.
- Los mensajes de la tabla del README (credenciales incorrectas siempre con el mismo texto, bloqueo temporal, cuenta desactivada, error de red con reintento) se muestran en una alerta sobre el formulario, sin indicar cuál dato falló.
- Al terminar bien: `context.go(rutaInicioPorRol(rol))`.
- No incluye «¿Olvidó su contraseña?» (CU15).

Archivos esperados (crear): `login_screen.dart`.

## Fuera de alcance (no tocar)

- La ruta en el router (`UT-CU01-MOB-09`).
- Recuperar contraseña (CU15).

## Resultado esperado

Se puede iniciar sesión desde la app.

## Validación

1. `flutter analyze` y `flutter test` pasan.
2. En el emulador: login correcto lleva al shell del rol y deja una fila `INICIO_SESION` en `bitacora`; contraseña incorrecta muestra el mensaje genérico; cuenta con `activo = false` muestra el aviso y no entra.
