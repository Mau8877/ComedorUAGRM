# UT-CU01-MOB-09 — Router: guard de sesión, rutas de acceso y destino por rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-09` |
| **Nombre** | Router: guard de sesión, rutas de acceso y destino por rol |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-05`, `UT-CU01-MOB-06`, `UT-CU01-MOB-07`, `UT-CU01-MOB-08` |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)
- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Las pantallas privadas no deben verse sin sesión y cada rol debe llegar a su módulo.

## Objetivo

El `GoRouter` redirige según la sesión de Firebase, tiene las rutas `/login`, `/registro` y `/sin-modulo`, y manda a cada rol a su shell.

## Alcance

- `lib/core/routing/app_router.dart`: el router pasa a ser un provider (`appRouterProvider`) que escucha `authSesionProvider` y `authUsuarioProvider` (`refreshListenable`). `redirect`: sin sesión → `/login` salvo en `/login` y `/registro`; con sesión en `/login`, `/registro` o `/` → `rutaInicioPorRol(rol)`; mientras la sesión se resuelve no redirige (pantalla de carga). Sustituye el `redirect` con `TODO` actual.
- Rutas nuevas: `/login` (`LoginScreen`), `/registro` (`RegistroScreen`) y `/sin-modulo` (aviso para `DIRECTIVO` y `PERSONAL_COMEDOR` con botón de cerrar sesión).
- `lib/main.dart`: `MaterialApp.router(routerConfig: ref.watch(appRouterProvider))` (la app pasa a `ConsumerWidget`).
- `test/widget_test.dart`: ajustado al nuevo provider del router.
- `NAVEGACION_MOBILE.md`: reemplazar el `TODO` descrito en la regla por el redirect real.
- Tests del `redirect` (función pura o con `ProviderContainer`): sin sesión → `/login`; con sesión en `/login` → módulo del rol; rol sin shell → `/sin-modulo`; sesión inválida (A8) → `/login`.

Archivos esperados (tocar/crear): `app_router.dart`, `main.dart`, `widget_test.dart`, `NAVEGACION_MOBILE.md`, pantalla `sin_modulo_screen.dart` y los tests.

## Fuera de alcance (no tocar)

- Los shells nuevos de los otros roles.
- Recordar la dirección a la que se quería entrar.

## Resultado esperado

Sin sesión no se ve nada privado; con sesión, cada rol aterriza en su módulo.

## Validación

1. `flutter analyze` y `flutter test` pasan.
2. En el emulador: abrir la app sin sesión muestra `/login`; con sesión, entra directo al shell del rol sin pedir credenciales; reabrir la app conserva la sesión.
