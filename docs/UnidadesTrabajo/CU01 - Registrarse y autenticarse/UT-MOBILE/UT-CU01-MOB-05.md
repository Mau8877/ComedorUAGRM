# UT-CU01-MOB-05 — Providers de sesión y de usuario actual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-05` |
| **Nombre** | Providers de sesión y de usuario actual |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-01`, `UT-CU01-MOB-03`, `UT-CU01-MOB-04` |

## Antes de implementar

- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Todo el árbol de pantallas necesita saber si hay sesión y quién es el usuario, la sesión debe sobrevivir al cierre de la app y el router debe reaccionar a sus cambios.

## Objetivo

Existen `authSesionProvider` y `authUsuarioProvider`, la app restaura la sesión al arrancar y la cierra cuando ya no se puede renovar.

## Alcance

- `lib/features/auth/providers/auth_sesion_provider.dart`: `AsyncNotifier` (`authSesionProvider`) con el estado de sesión (`cargando`, `sin-sesion`, `con-sesion`). Al arrancar lee el refresh token de `TokenStorage` y lo canjea con `AuthService.renovar` (`POST /auth/refresh`); si no hay refresh token o el backend responde `ERR_AUTH_06`, queda `sin-sesion`. Expone `abrir(Sesion)` (guarda los tokens) y `cerrar()` (borra los tokens).
- `lib/features/auth/providers/auth_usuario_provider.dart`: `FutureProvider<Usuario?>` (`authUsuarioProvider`) que, con sesión, devuelve el usuario de la `Sesion` o llama a `AuthService.usuarioActual()`; sin sesión devuelve `null`. Ante `ERR_AUTH_02`/`ERR_AUTH_03` cierra la sesión.
- Registro de `ApiClient.onRenovar` (renovar con el refresh token guardado) y `ApiClient.onSesionInvalida` (cerrar la sesión e invalidar el provider de usuario, A8).
- `lib/features/auth/rol_destino.dart`: `rutaInicioPorRol(String rol)` con el mapa del README (rol desconocido → `/login`).
- Tests (`ProviderContainer`, `TokenStorage` y `AuthService` simulados): sin refresh token → `sin-sesion`; con refresh token válido → `con-sesion` y el usuario; refresh inválido → `sin-sesion`; `onSesionInvalida` cierra la sesión; `rutaInicioPorRol` para los 4 roles y uno desconocido.

Archivos esperados (crear): los dos providers, `rol_destino.dart` y los tests.

## Fuera de alcance (no tocar)

- Iniciar, registrar o cerrar sesión desde un botón (`UT-CU01-MOB-06`).
- Cambios en el router (`UT-CU01-MOB-09`).

## Resultado esperado

El estado de sesión y el usuario están disponibles y probados, y la sesión se restaura al reabrir la app.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
