---
globs: projects/mobile/**/*
---

# Navegación — Mobile

## `go_router` con redirect global

Un único `GoRouter` (`appRouter`) ya armado en
[lib/core/routing/app_router.dart](../../../projects/mobile/lib/core/routing/app_router.dart).
El chequeo de sesión **no se repite pantalla por pantalla**: se resuelve
una sola vez en el callback `redirect` de la configuración raíz del router
— hoy es un placeholder que no redirige a nada (`return null` siempre, con
un comentario `TODO`), mismo estado que `_authenticated.tsx` del lado web
antes de que exista el provider de auth real:

```dart
// lib/core/routing/app_router.dart
final appRouter = GoRouter(
  // TODO: reemplazar por la validación real de sesión una vez que exista
  // TODO: el provider de auth.
  redirect: (context, state) => null,
  routes: [ /* ... */ ],
);
```

Cuando exista el provider de auth, este `redirect` pasa a resolver contra
él (ver [Fuente del estado de sesión](#fuente-del-estado-de-sesión) abajo)
— no se reemplaza el mecanismo, solo se completa la condición.

Mismo concepto que el guard de rutas del frontend web (ver
[RUTAS_NAVEGACION_FRONTEND.md](../frontend/RUTAS_NAVEGACION_FRONTEND.md)):
la lógica de "¿puede entrar acá?" vive en **un solo lugar**, no se
reimplementa un `if` de sesión al principio de cada pantalla.

## Fuente del estado de sesión

El `redirect` necesita leer si hay sesión válida — esto se resuelve contra
el provider de autenticación de `features/auth/providers/` (ver
[ESTADO_GLOBAL_MOBILE.md](ESTADO_GLOBAL_MOBILE.md)), no leyendo
`flutter_secure_storage` directo desde el router. El router depende del
provider de auth, no al revés.

## Estructura

- Definición de rutas y del `GoRouter` en `lib/core/routing/`.
- Cada feature expone sus rutas (o al menos las pantallas a las que el
  router apunta) desde `features/{feature}/screens/`, pero el árbol de
  rutas completo se ensambla en un solo lugar dentro de `core/routing/` —
  no queda esparcido un `GoRoute` por archivo suelto en cada feature sin un
  punto central que las junte.
