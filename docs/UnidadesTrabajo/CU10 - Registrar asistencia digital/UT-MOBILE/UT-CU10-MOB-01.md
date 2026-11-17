# UT-CU10-MOB-01 — Modelos, servicio y traducción de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-MOB-01` |
| **Nombre** | Modelos, servicio y traducción de errores |
| **Caso de uso** | CU10-MOB |
| **Depende de** | — (la API de UT-CU10-BE-03 para probar de punta a punta) |

## Antes de implementar

[`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md). Molde: `perfil_service.dart` y `documentos_error_mapper.dart`.

## Solicitud de negocio

La app debe hablar con los dos endpoints de asistencia y mostrar mensajes claros cuando algo falla.

## Objetivo

Existen los modelos, el servicio y el traductor de errores de la feature `asistencia`.

## Alcance

- `M/features/asistencia/models/becado_asistencia.dart` y `asistencia_registrada.dart` (`fromJson`, `vigente`, `yaRegistroHoy`, `conHora`).
- `M/features/asistencia/services/asistencia_service.dart`: `buscarBecado(ci)` y `registrar(becaCodigo)`, ambos `POST` con el CI o el código en el cuerpo.
- `M/features/asistencia/asistencia_error_mapper.dart`: `ERR_ASI_01..03`, `ERR_AUTH_07`, `ERR_SYS_00/01`, red y servidor (`red = true` permite reintentar).
- `M/features/asistencia/formatos_asistencia.dart`: hora y fecha de La Paz.

Archivos esperados (crear/tocar): los cinco anteriores.

## Fuera de alcance (no tocar)

`core/network/api_client.dart`; los formatos de `postulaciones`.

## Resultado esperado

El servicio manda el CI en el cuerpo (nunca en la URL) y los errores salen en español sin datos del becado.

## Validación

1. `flutter test test/features/asistencia/asistencia_test.dart` (grupos de modelos, formatos, servicio y errores) en verde.
