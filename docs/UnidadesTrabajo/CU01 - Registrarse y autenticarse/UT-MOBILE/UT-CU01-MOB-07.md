# UT-CU01-MOB-07 — Pantalla de registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-07` |
| **Nombre** | Pantalla de registro |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-02`, `UT-CU01-MOB-05`, `UT-CU01-MOB-06` |

## Antes de implementar

- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)
- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)

## Solicitud de negocio

Un estudiante debe poder crear su cuenta de Postulante desde la app.

## Objetivo

La pantalla `RegistroScreen` (ruta `/registro`) muestra el formulario y lleva al Postulante a su módulo al terminar.

## Alcance

- `lib/features/auth/screens/registro_screen.dart`: `Form` + `TextFormField` con el `validator` nativo y los mensajes en español (misma regla de negocio que la web: campos obligatorios, registro universitario de exactamente 9 dígitos y sin cero inicial, contraseña de al menos 6 caracteres sin más exigencias, confirmación igual, correo con formato válido). `carreraCodigo` es un `DropdownButtonFormField` con las carreras (estado de carga y de error con reintento).
- Botón «Crear cuenta» deshabilitado mientras envía; los conflictos del backend (`ci`, `correo`, `registroUniversitario`) y los errores de Firebase se muestran en su campo; el error de red con mensaje y reintento (A9). Enlace «¿Ya tienes cuenta? Inicia sesión».
- Sin selector de rol ni forma de crear cuentas del personal (CA16).
- Al terminar bien: navega con `context.go(rutaInicioPorRol(rol))`.
- Estilos con el tema (`Theme.of(context)`), sin colores hardcodeados.
- Usa `authProvider` y `authCarrerasProvider` (`FutureProvider` en `providers/` que llama a `AuthService.listarCarreras()`; se crea en esta UT).

Archivos esperados (crear): `registro_screen.dart`, `providers/auth_carreras_provider.dart`.

## Fuera de alcance (no tocar)

- La ruta en el router (`UT-CU01-MOB-09`).
- Widget tests obligatorios (`TESTING_MOBILE.md`); la lógica ya se prueba en `UT-CU01-MOB-06`.
- Verificación de correo.

## Resultado esperado

Un Postulante puede registrarse desde la app y entrar a su módulo.

## Validación

1. `flutter analyze` y `flutter test` pasan.
2. En un emulador Android con el stack y Firebase configurados: completar el formulario crea la cuenta y entra a `/estudiante`; existen las filas en `usuario` y `postulante`.
3. Repetir con el mismo CI muestra el error en el campo CI sin crear otra cuenta.
