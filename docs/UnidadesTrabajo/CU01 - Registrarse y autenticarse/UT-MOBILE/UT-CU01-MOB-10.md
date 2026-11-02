# UT-CU01-MOB-10 — Cerrar sesión desde los shells

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-10` |
| **Nombre** | Cerrar sesión desde los shells |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-06`, `UT-CU01-MOB-09` |

## Antes de implementar

- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)

## Solicitud de negocio

El usuario debe poder terminar su sesión desde la app y el AppBar debe mostrar quién es.

## Objetivo

`AdminShell` y `EstudianteShell` tienen la opción «Cerrar sesión» y muestran el nombre del usuario.

## Alcance

- `lib/layouts/admin/admin_shell.dart` y `lib/layouts/estudiante/estudiante_shell.dart`: en el `AppBar`, un menú (`PopupMenuButton`) con el nombre y el rol del usuario (de `authUsuarioProvider`) y la opción «Cerrar sesión», que llama a `authProvider.cerrarSesion()`. Sin importar features desde `layouts/` más allá del provider de auth (los shells ya montan contenido de prueba).
- `sin_modulo_screen.dart` (de `UT-CU01-MOB-09`) usa la misma acción.
- Tras cerrar sesión el router lleva a `/login` y el botón «atrás» no vuelve a una pantalla privada (CA18).

Archivos esperados (tocar): `admin_shell.dart`, `estudiante_shell.dart`.

## Fuera de alcance (no tocar)

- Cerrar la sesión en otros dispositivos.
- Rediseñar los shells.

## Resultado esperado

Se puede cerrar sesión desde cualquier shell y volver al acceso.

## Validación

1. `flutter analyze` y `flutter test` pasan.
2. En el emulador: iniciar sesión, abrir el menú, «Cerrar sesión» lleva a `/login` y la tecla atrás no vuelve al shell.
3. Una sesión abierta en la web sigue activa después de cerrar en la app.
