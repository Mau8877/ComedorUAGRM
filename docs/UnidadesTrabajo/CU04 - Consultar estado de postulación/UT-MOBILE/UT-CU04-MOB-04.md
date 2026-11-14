# UT-CU04-MOB-04 — Rutas, sección del shell y apertura desde un push

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-04` |
| **Nombre** | Rutas, sección del shell y apertura desde un push |
| **Caso de uso** | CU04-MOB |
| **Depende de** | `UT-CU04-MOB-02`, `UT-CU04-MOB-03`, `UT-CU04-BE-05` |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (un único `GoRouter`, `redirect` global), [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (layouts por rol), [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md).
- Como referencia: `M/lib/core/routing/app_router.dart`, `M/lib/layouts/estudiante/estudiante_shell.dart` y `M/lib/features/notificaciones/` (UT de CU-EXTRA-01).

## Solicitud de negocio

El postulante debe llegar a sus postulaciones desde la app y desde el aviso de un cambio de estado.

## Objetivo

La sección «Mis postulaciones» es navegable y tocar un push abre el detalle de su postulación.

## Alcance

- `app_router.dart`: `GoRoute` de `/mis-postulaciones/:codigo` hacia `DetallePostulacionScreen`; el `redirect` global ya exige sesión (sin sesión, login). La lista no necesita ruta propia si vive dentro del shell.
- `estudiante_shell.dart`: nueva sección «Mis postulaciones» con `MisPostulacionesVista` (reemplaza el contenido de prueba que corresponda o se suma a la barra, con el menor cambio posible; la `NavigationBar` sigue siendo de pocas secciones).
- `features/notificaciones/`: al tocar el push (segundo plano, app cerrada) o el aviso en primer plano, si `data.postulacionCodigo` viene, navegar a `/mis-postulaciones/{codigo}` vía el router; si no viene, solo abrir la app como hoy. `notificaciones` no importa nada de `postulaciones/`: emite el código hacia el router.
- Tests de lo no trivial: el mapeo del dato del push a la ruta (con y sin código) y que el `redirect` lleva al login sin sesión.

Archivos esperados (tocar): `app_router.dart`, `estudiante_shell.dart`, el servicio/proveedor de push de `features/notificaciones/` y sus tests.

## Fuera de alcance (no tocar)

- Las pantallas (UT-02 y UT-03) y los providers (UT-01).
- Registro del token, permisos y canal del push (CU-EXTRA-01).
- `AdminShell` y las demás secciones del shell.

## Resultado esperado

Mis postulaciones es un módulo navegable en la app y el push lleva directo al detalle.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Con sesión de postulante, la sección abre la lista y el detalle; al tocar un push de devolución con `postulacionCodigo`, se abre el detalle de esa postulación.
3. Sin sesión, el mismo toque lleva primero al login.
