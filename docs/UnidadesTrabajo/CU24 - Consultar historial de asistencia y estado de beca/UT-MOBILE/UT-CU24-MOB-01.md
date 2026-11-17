# UT-CU24-MOB-01 — Modelos, servicio y traducción de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-MOB-01` |
| **Nombre** | Modelos, servicio y traducción de errores |
| **Caso de uso** | CU24-MOB |
| **Depende de** | UT-CU24-BE-02 (el contrato HTTP) |

## Antes de implementar

[`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (el `ApiClient` ya desempaqueta el sobre; `ApiException` en un `DioException`).

## Solicitud de negocio

La app necesita pedir la beca y el historial del usuario y traducir los errores a mensajes entendibles.

## Objetivo

La feature `mibeca` tiene sus modelos, su servicio y su traductor de errores.

## Alcance

- `M/features/mibeca/models/mi_beca.dart` (`MiBeca`) y `pagina_asistencias.dart` (`AsistenciaBecado`, `MetaAsistencias`, `PaginaAsistencias`).
- `M/features/mibeca/services/mibeca_service.dart`: `obtener()` y `listarAsistencias({desde, hasta, page, pageSize})` (lo vacío no viaja).
- `M/features/mibeca/mibeca_error_mapper.dart`: `ErrorMiBeca` y `mapearErrorMiBeca` (`ERR_MBE_01` = sin beca; red y 5xx reintentables).
- `M/features/mibeca/formatos_mibeca.dart`: fechas y horas en La Paz, `diaParaApi` y `etiquetaEstadoBeca`.

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

`ApiClient`, el router, otras features y los providers (MOB-02).

## Resultado esperado

El servicio devuelve modelos tipados y los errores salen en español sin datos sensibles.

## Validación

1. `flutter analyze` sin avisos.
2. Tests de modelos, formatos y errores (ver MOB-04).
