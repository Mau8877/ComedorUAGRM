# UT-CU10-MOB-02 — Estado de la pantalla (provider)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-MOB-02` |
| **Nombre** | Estado de la pantalla (provider) |
| **Caso de uso** | CU10-MOB |
| **Depende de** | UT-CU10-MOB-01 |

## Antes de implementar

[`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md). Molde: `perfil_provider.dart`.

## Solicitud de negocio

La pantalla necesita saber si está buscando, qué becado encontró y qué pasó al registrar.

## Objetivo

Un notifier con las acciones buscar, registrar y siguiente, y los providers que lo exponen.

## Alcance

- `M/features/asistencia/providers/asistencia_servicios_provider.dart` (`asistenciaServiceProvider`).
- `M/features/asistencia/providers/asistencia_provider.dart`: `EstadoAsistencia` y `AsistenciaNotifier` (`buscar`, `registrar`, `siguiente`). Ignora llamadas mientras hay una en curso y no llama a la API si la beca no está vigente o ya registró hoy; si el backend dice «ya registrada» (A3) no es error.

Archivos esperados (crear/tocar): los dos providers.

## Fuera de alcance (no tocar)

La pantalla (UT-MOB-03) y el provider de sesión.

## Resultado esperado

Tras registrar, el estado trae la hora y «recién registrada»; tras un error de red conserva al becado para reintentar.

## Validación

1. `flutter test test/features/asistencia/asistencia_test.dart`, grupo `AsistenciaNotifier`, en verde.
