# UT-CU15-MOB-02 — Providers del perfil y mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-02` |
| **Nombre** | Providers del perfil y mapeo de errores |
| **Caso de uso** | CU15-MOB |
| **Depende de** | `UT-CU15-MOB-01` |

## Antes de implementar

- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (nombres: `{feature}DetailProvider`, `{feature}Provider`, `{feature}UiProvider`)
- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- Como referencia: `M/features/auth/providers/` y `M/features/auth/auth_error_mapper.dart`

## Solicitud de negocio

Las pantallas necesitan datos del perfil y acciones que informen claramente cada resultado, sin repetir la traducción de errores.

## Objetivo

Existen los providers del perfil, las acciones de actualizar y cambiar la contraseña, y el mapeo de errores del CU.

## Alcance

- `M/features/perfil/providers/perfil_servicios_provider.dart`: provider del `PerfilService` sobre `ApiClient.instance`.
- `M/features/perfil/providers/perfil_provider.dart`: `perfilDetailProvider` (`FutureProvider.autoDispose<Perfil>`) y `perfilProvider` (`Notifier` con `actualizar(request)` y `cambiarContrasena(...)`, que lee el refresh token de `TokenStorage` justo antes de llamar y no lo conserva). `actualizar` invalida `perfilDetailProvider` y refresca `authUsuarioProvider`. Cambiar la contraseña **no** toca la sesión local.
- `M/features/auth/auth_error_mapper.dart` (o `M/features/perfil/perfil_error_mapper.dart` si crece): casos del CU: `ERR_AUTH_09` → campo `contrasenaActual`; `ERR_US_02` → campo `correo`; `ERR_US_13` y `ERR_SYS_01` → mensaje del backend; red → `red: true` con el mensaje de conexión. Se amplía `CampoAuth` con `contrasenaActual` si hace falta.
- Un provider para la solicitud de recuperación en `features/auth/providers/auth_provider.dart` (acción `solicitarRecuperacion`), con estado cargando/éxito/error, que ignora el detalle de la respuesta.
- Nada de lo anterior imprime ni guarda contraseñas ni el refresh token.
- Tests (con servicios falsos): el mapeo de cada código y de la red; `actualizar` invalida el detalle; `cambiarContrasena` envía el refresh token guardado y no lo expone; un `400` de contraseña actual no cierra la sesión.

Archivos esperados (crear): los providers del perfil y sus tests. (Tocar): `auth_error_mapper.dart`, `auth_provider.dart` y sus tests.

## Fuera de alcance (no tocar)

- Pantallas y widgets (UT-03 a UT-06).
- El flujo de renovación de sesión de `ApiClient`.

## Resultado esperado

Las pantallas consumen providers y reciben errores ya traducidos.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
