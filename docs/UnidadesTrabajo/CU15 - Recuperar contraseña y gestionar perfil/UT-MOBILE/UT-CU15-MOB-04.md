# UT-CU15-MOB-04 — Pantalla «Mi perfil» y acceso desde el menú de cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-04` |
| **Nombre** | Pantalla «Mi perfil» y acceso desde el menú de cuenta |
| **Caso de uso** | CU15-MOB |
| **Depende de** | `UT-CU15-MOB-02` |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)
- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)
- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md#layouts-un-shell-por-rol-mismo-criterio-que-el-frontend-web)
- Como referencia: `M/features/auth/widgets/menu_usuario.dart`, `M/features/auth/screens/sin_modulo_screen.dart` y `M/layouts/estudiante/estudiante_shell.dart`

## Solicitud de negocio

Cada usuario encuentra su perfil desde el menú de su cuenta y ve sus datos, con el CI como dato que no se puede modificar.

## Objetivo

`/perfil` muestra los datos del usuario y el menú de cuenta de cualquier rol lleva hasta ahí.

## Alcance

- `M/features/perfil/widgets/perfil_vista.dart`: widget con el contenido del perfil (usa `perfilDetailProvider`): sección **Mis datos** de solo lectura (nombres, apellidos, CI —campo bloqueado con la nota «El CI no se puede modificar», A4—, rol, estado y, si es Postulante, registro universitario y carrera con su facultad) y los espacios de **Datos de contacto** y **Cambiar contraseña** (se llenan en UT-05 y UT-06). Estados de carga y de error de red con «Reintentar» (A10).
- `M/features/perfil/screens/perfil_screen.dart`: `Scaffold` con `AppBar` («Mi perfil», botón atrás) que muestra `PerfilVista`.
- `M/core/routing/app_router.dart`: `GoRoute('/perfil')` privada (el `redirect` global ya exige sesión).
- `M/features/auth/widgets/menu_usuario.dart`: nueva opción «Mi perfil» (antes de «Cerrar sesión») que hace `context.push('/perfil')`. Como el menú ya está en `AdminShell`, `EstudianteShell` y `SinModuloScreen`, el perfil queda disponible para los cuatro roles; si `SinModuloScreen` no usa el menú, se le agrega un botón «Mi perfil».
- `M/layouts/estudiante/estudiante_shell.dart`: la sección «Perfil» muestra `PerfilVista` en lugar del texto de prueba.
- Tests: la ruta `/perfil` exige sesión (`redirectDeSesion`); el modelo de vista del perfil de un Postulante incluye registro y carrera, el de un Administrador no; el CI no se ofrece como campo editable.

Archivos esperados (crear): `perfil_vista.dart` y `perfil_screen.dart`. (Tocar): `app_router.dart`, `menu_usuario.dart`, `estudiante_shell.dart` y, si hace falta, `sin_modulo_screen.dart`.

## Fuera de alcance (no tocar)

- Los formularios de contacto y de contraseña (UT-05 y UT-06).
- Editar nombres, apellidos, registro universitario o carrera.

## Resultado esperado

Cualquier usuario autenticado abre «Mi perfil» desde su menú y ve sus datos.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
