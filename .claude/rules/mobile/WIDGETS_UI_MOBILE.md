---
globs: projects/mobile/**/*
---

# Widgets / UI — Mobile

## Compartidos vs. de una sola feature

Mismo criterio que `common/` en el backend
([CONVENCIONES_JAVA_BACKEND.md](../backend/CONVENCIONES_JAVA_BACKEND.md)) y
`shared/` en el frontend web
([TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)):

- Un widget que usa **una sola feature** vive dentro de esa feature. El
  scaffold actual (`lib/features/auth/{providers,screens,services}`) no
  tiene todavía una subcarpeta `widgets/` — se agrega cuando una feature
  concreta la necesita: `lib/features/{feature}/widgets/`.
- Un widget que usan **dos o más features** va en
  `lib/shared/widgets/` (carpeta nueva, no existe todavía en el scaffold —
  se crea recién cuando aparece el primer widget realmente compartido, no
  de forma anticipada "por si hace falta").

No se mueve un widget a `shared/widgets/` apenas "parece que podría
reusarse" — se mueve cuando una segunda feature efectivamente lo necesita,
igual que el criterio de `shared/api/` del lado del consumo de API (ver
[CONSUMO_API_MOBILE.md](CONSUMO_API_MOBILE.md)).

## Tema (`ThemeData`)

`lib/core/theme/` ya está scaffoldeado pero vacío (solo `.gitkeep`) — **no
hay un `ThemeData` del proyecto definido todavía**. Mismo placeholder que
[TAILWIND_STYLES_FRONTEND.md](../frontend/TAILWIND_STYLES_FRONTEND.md) del
lado web: hasta que exista la paleta final de ComedorUAGRM, no se inventan
colores de marca acá. Cuando se defina, el `ThemeData` se arma en
`lib/core/theme/app_theme.dart` y se referencia desde `MaterialApp` (hoy
`lib/main.dart` usa `ThemeData(colorScheme: .fromSeed(seedColor: Colors.deepPurple))`
directo desde el boilerplate de `flutter create`, sin pasar por
`core/theme/` — se reemplaza ese bloque cuando el tema real esté definido,
no antes).
