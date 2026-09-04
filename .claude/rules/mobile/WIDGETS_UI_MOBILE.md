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

Definido en `lib/core/theme/app_theme.dart` — mismos valores de marca que
[TAILWIND_STYLES_FRONTEND.md](../frontend/TAILWIND_STYLES_FRONTEND.md) del
lado web, traducidos a `ColorScheme`/`TextTheme` de Material 3 (no hay
forma de compartir el archivo de tokens entre Tailwind/CSS y Dart, así que
los valores se mantienen sincronizados a mano entre los dos documentos si
la paleta cambia):

- `AppTheme.light` / `AppTheme.dark` — dos `ThemeData` completos (colores +
  tipografía), expuestos como getters estáticos. `lib/main.dart` los pasa a
  `MaterialApp(theme: AppTheme.light, darkTheme: AppTheme.dark)` — no se
  arma el `ThemeData` inline en `main.dart` (eso era el boilerplate de
  `flutter create` con `ColorScheme.fromSeed(seedColor: Colors.deepPurple)`,
  ya reemplazado).
- `AppColors` (mismo archivo) — las constantes `Color(0xFF...)` de la
  paleta, separadas en `light*`/`dark*`, para no repetir valores hex sueltos
  en otros widgets. Un widget que necesita un color de marca fuera del
  `Theme.of(context)` (raro — la mayoría debería salir de `colorScheme`)
  importa `AppColors`, no hardcodea el hex de nuevo. Incluye `lightWarning`/
  `darkWarning` y `lightInfo`/`darkInfo` (sin equivalente en
  `ColorScheme` de Material 3, igual que `lightSuccess`/`darkSuccess` — se
  usan como constantes sueltas donde haga falta un estado de advertencia/
  información, ej. un `SnackBar`), y `header`/`headerForeground` — una
  única superficie fija de marca (no varía entre claro/oscuro, mismo
  criterio que `--header` del lado web) usada en `appBarTheme` de ambos
  temas.
- Tipografía vía el paquete **`google_fonts`** (no hay equivalente a
  Fontsource en Flutter): **Familjen Grotesk** para títulos
  (`display*`/`headline*`/`title*` del `TextTheme`) y **Jost** para cuerpo/
  botones/formularios/etiquetas (el resto del `TextTheme`, y el default que
  hereda cualquier widget de Material). Por default `google_fonts` descarga
  el archivo de fuente la primera vez que se usa (no lo bundlea en el
  `.apk`/`.ipa`) — si en algún momento la app necesita funcionar 100% offline
  desde el primer arranque, se evalúa fijar las fuentes como asset local
  (`GoogleFonts.config.allowRuntimeFetching = false` + fuentes en
  `pubspec.yaml`), no antes de que sea un problema real.
