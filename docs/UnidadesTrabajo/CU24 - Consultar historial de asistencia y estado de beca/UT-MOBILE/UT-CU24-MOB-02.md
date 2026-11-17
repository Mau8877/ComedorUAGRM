# UT-CU24-MOB-02 — Estado de la pantalla (providers)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-MOB-02` |
| **Nombre** | Estado de la pantalla (providers) |
| **Caso de uso** | CU24-MOB |
| **Depende de** | UT-CU24-MOB-01 |

## Antes de implementar

[`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (nombres de providers: datos vs. estado de UI).

## Solicitud de negocio

La vista necesita la beca, el historial acumulado por páginas y el rango de fechas elegido.

## Objetivo

Los providers de «Mi beca» están listos para que la vista los consuma.

## Alcance

- `M/features/mibeca/providers/mibeca_servicios_provider.dart`: `miBecaServiceProvider` (sustituible en tests).
- `M/features/mibeca/providers/mibeca_provider.dart`: `miBecaProvider` (`FutureProvider.autoDispose`), `miBecaRangoUiProvider` (estado de UI: el rango) y `miBecaAsistenciasProvider` (`AsyncNotifier` con páginas acumuladas, `cargarSiguiente`, error de la página siguiente sin perder lo cargado, reinicio al cambiar el rango).

Archivos esperados (crear/tocar): los dos de arriba.

## Fuera de alcance (no tocar)

La vista, el shell y el backend. Sin mutaciones: es solo lectura.

## Resultado esperado

Al cambiar el rango, el historial se pide otra vez desde la página 1; `cargarSiguiente` suma la página siguiente.

## Validación

1. `flutter analyze` sin avisos.
2. Tests de providers (ver MOB-04).
