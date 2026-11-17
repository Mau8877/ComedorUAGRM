# UT-CU10-MOB-03 — Pantalla, ruta y destino del rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-MOB-03` |
| **Nombre** | Pantalla, ruta y destino del rol |
| **Caso de uso** | CU10-MOB |
| **Depende de** | UT-CU10-MOB-02 |

## Antes de implementar

[`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) y [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md). Mockup: img 110.

## Solicitud de negocio

El Personal del Comedor abre la app, escribe el CI, ve al becado y registra su asistencia con un toque.

## Objetivo

La pantalla «Registrar asistencia» funciona y es el inicio del rol `PERSONAL_COMEDOR`.

## Alcance

- `M/features/asistencia/screens/registrar_asistencia_screen.dart`: campo CI (obligatorio, máx. 20), «Buscar», tarjeta del becado con su estado, «Registrar asistencia» (solo si es vigente y no registró hoy), avisos (ya registrada, beca no vigente, errores) y «Siguiente becado». `AppBar` con `MenuUsuario`.
- `M/core/routing/app_router.dart`: ruta `/asistencia`.
- `M/features/auth/rol_destino.dart`: `PERSONAL_COMEDOR` → `/asistencia`; ajustar el comentario de `sin_modulo_screen.dart` y el test de `logica_test.dart`.

Archivos esperados (crear/tocar): la pantalla, `app_router.dart`, `rol_destino.dart`, `sin_modulo_screen.dart` (comentario) y `logica_test.dart`.

## Fuera de alcance (no tocar)

Un shell para el rol, escaneo/foto de carnet, turno, `DIRECTIVO`.

## Resultado esperado

Al iniciar sesión como `PERSONAL_COMEDOR` se ve la pantalla y se puede registrar de punta a punta.

## Validación

1. `flutter test` completo en verde y `flutter analyze` sin hallazgos.
2. Con el backend levantado: iniciar sesión como `PERSONAL_COMEDOR`, buscar un CI de la planilla, registrar y repetir: la segunda vez dice «ya registrada» con la hora (prueba manual, ver el registro de implementación).
