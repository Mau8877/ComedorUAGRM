---
globs: projects/mobile/**/*
---

# Navegación — Mobile

## `go_router` con redirect global

Un único `GoRouter`, expuesto como `appRouterProvider` (un `Provider<GoRouter>` de
Riverpod) en
[lib/core/routing/app_router.dart](../../../projects/mobile/lib/core/routing/app_router.dart).
El chequeo de sesión **no se repite pantalla por pantalla**: se resuelve
una sola vez en el callback `redirect` de la configuración raíz, que delega
en la función pura `redirectDeSesion(sesion, ubicacion)` (para poder probar
todos los casos sin montar la app) y se re-evalúa cada vez que cambia
`authSesionProvider` (`refreshListenable`):

- Mientras la sesión se restaura al abrir la app → `/cargando` (sin parpadeo
  de pantallas privadas ni del login).
- Sin sesión → solo se ven `/login` y `/registro`; cualquier otra ruta lleva a `/login`
  (también cuando la sesión se invalida a mitad de uso, A8).
- Con sesión → `/login`, `/registro`, `/` y `/cargando` llevan al módulo del rol
  (`rutaInicioPorRol`: `/estudiante`, `/admin` o `/sin-modulo`).

Mismo concepto que el guard de rutas del frontend web (ver
[RUTAS_NAVEGACION_FRONTEND.md](../frontend/RUTAS_NAVEGACION_FRONTEND.md)):
la lógica de "¿puede entrar acá?" vive en **un solo lugar**, no se
reimplementa un `if` de sesión al principio de cada pantalla.

## Fuente del estado de sesión

El `redirect` necesita leer si hay sesión válida — esto se resuelve contra
el provider de autenticación de `features/auth/providers/` (ver
[ESTADO_GLOBAL_MOBILE.md](ESTADO_GLOBAL_MOBILE.md)), no leyendo
el almacenamiento de tokens directo desde el router. La sesión es el JWT
propio del backend: el provider de auth carga al arrancar el refresh token
guardado en `flutter_secure_storage` y lo canjea por un token de acceso
(`POST /api/v1/auth/refresh`); si no hay refresh token o ya no sirve, no hay
sesión. El router depende del provider de auth, no al revés.

## Estructura

- Definición de rutas y del `GoRouter` en `lib/core/routing/`.
- Cada feature expone sus rutas (o al menos las pantallas a las que el
  router apunta) desde `features/{feature}/screens/`, pero el árbol de
  rutas completo se ensambla en un solo lugar dentro de `core/routing/` —
  no queda esparcido un `GoRoute` por archivo suelto en cada feature sin un
  punto central que las junte.
