---
globs: projects/mobile/**/*
---

# Arquitectura Mobile

**Feature-based**, mismo criterio conceptual que el frontend web (ver
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md)): el código se
organiza por funcionalidad de negocio (`features/{feature}/`), y lo
transversal a toda la app vive en `core/`.

## Estructura de `lib/`

Ya scaffoldeada — la estructura real que existe hoy en el repo (`core/`
con sus tres subcarpetas ya implementadas, `layouts/` con un shell por rol,
y `features/auth/` como ejemplo de referencia para toda feature nueva):

```
lib/
├── core/
│   ├── network/     # ApiClient (Dio + interceptor de sobre), ver CONSUMO_API_MOBILE.md -- ya implementado
│   ├── routing/     # GoRouter único (app_router.dart), con el redirect global -- ver NAVEGACION_MOBILE.md
│   └── theme/       # AppTheme/AppColors -- paleta de marca ya definida, ver WIDGETS_UI_MOBILE.md
├── layouts/
│   ├── admin/       # AdminShell -- AppBar + NavigationDrawer (Material 3)
│   └── estudiante/  # EstudianteShell -- AppBar + NavigationBar inferior (Material 3)
├── shared/
│   └── widgets/     # Widgets usados por 2+ features (ver WIDGETS_UI_MOBILE.md) -- no existe todavía
├── features/
│   └── {feature}/   # Un feature de negocio (ver estructura abajo)
└── main.dart         # Entry point: ProviderScope + MaterialApp.router con el GoRouter de core/routing/
```

`core/` es la única carpeta de nivel superior con código transversal real
además de `features/`/`layouts/` — no hay una carpeta `common/`/`utils/`
genérica todavía; si aparece una necesidad concreta de utilidades
puramente transversales (no ligadas a red, routing ni tema), se define esa
carpeta en su momento, no se anticipa vacía.

### `layouts/`: un shell por rol, mismo criterio que el frontend web

Igual que `src/layouts/{rol}/` en el frontend
([ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md#layouts-por-rol)):
un directorio por rol dentro de `lib/layouts/`, cada uno con su propio
Shell (`AdminShell`, `EstudianteShell`) que arma la navegación de nivel
superior de ese rol. La implementación concreta diverge a propósito de la
del frontend porque el patrón mobile idiomático no es el mismo:

- **`AdminShell`**: `Scaffold` + `NavigationDrawer` (Material 3) — un
  Drawer calza mejor que un sidebar fijo en una pantalla angosta.
- **`EstudianteShell`**: `Scaffold` + `NavigationBar` (Material 3) inferior
  — patrón mobile estándar para navegación con pocas secciones (3, en este
  caso), en vez de un Drawer.

El `GoRouter` de `core/routing/app_router.dart` es el único que decide qué
ruta monta qué Shell — un Shell no se instancia nunca directo desde una
`screen/` de una feature. Hoy cada Shell muestra contenido de prueba
(texto placeholder por sección) — se reemplaza sección por sección a
medida que exista la feature real detrás de cada ítem de navegación,
mismo criterio que `_authenticated/panel.tsx` del lado web.

## Estructura de un feature (`lib/features/{feature}/`)

Ya scaffoldeada como ejemplo de referencia en `lib/features/auth/` (hoy
vacía, solo `.gitkeep` en cada subcarpeta) — cualquier feature nueva sigue
esta misma forma:

```
lib/features/usuarios/
├── providers/    # Riverpod: AsyncNotifier/FutureProvider (datos) + *UiProvider (estado de UI)
├── screens/      # Pantallas completas que apunta el GoRouter de core/routing/
├── services/     # Llamadas a la API de ESTE feature, vía el ApiClient de core/network/
└── widgets/      # Widgets específicos de esta feature -- se agrega recién cuando hace falta uno
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_MOBILE.md](mobile/ESTADO_GLOBAL_MOBILE.md) (`providers/`,
convención de nombres), [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md)
(`services/`), [NAVEGACION_MOBILE.md](mobile/NAVEGACION_MOBILE.md) (cómo
`screens/` se conecta con el router central), [WIDGETS_UI_MOBILE.md](mobile/WIDGETS_UI_MOBILE.md)
(`widgets/`, y cuándo algo pasa a `shared/widgets/`).

> **Modelos de datos (DTOs Dart) — todavía sin convención definida.** Ni el
> scaffold actual ni ninguna rule dice hoy dónde viven las clases que
> representan el JSON de request/response de la API (¿un `models/` nuevo
> por feature? ¿adentro de `services/`?). No se asume ninguna de las dos —
> se define explícitamente en la rule correspondiente ([CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md))
> antes de escribir el primer `service` real que necesite parsear una
> respuesta.

## Regla de dependencia

`features/{feature}/` puede depender de `core/` y `shared/` — nunca al
revés (`core/`/`shared/` no importan nada de `features/`). Una feature no
depende de otra feature directo salvo que la relación de negocio lo
justifique explícitamente, mismo criterio que
[ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md#regla-de-dependencia).
