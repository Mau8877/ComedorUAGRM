# UT-CU24-BE-02 — Servicio, controller y errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-BE-02` |
| **Nombre** | Servicio, controller y errores |
| **Caso de uso** | CU24-BE |
| **Depende de** | UT-CU24-BE-01 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

La web y la app necesitan consultar la beca y su historial, y solo la propia.

## Objetivo

Los dos `GET` responden según el contrato del README, con sus errores catalogados y documentados.

## Alcance

- `MiBecaErrorCodes` (`ERR_MBE_01..03`) y su documentación en `docs/errors/mibeca/ERRORES_MIBECA.md` + índice del `README.md`.
- `MiBecaService.miBeca` y `asistencias` (`@PreAuthorize("hasRole('POSTULANTE')")`, `@Transactional(readOnly = true)`): `404` sin beca, `400` por rango o `sort` inválido, `PageMeta`.
- `MiBecaController` (`/api/v1/mi-beca`): `@CurrentUserId`, `ParametrosListado.validarFiltros`, fechas `AAAA-MM-DD`.

Archivos esperados (crear/tocar): `exception/`, `service/MiBecaService.java`, `controller/MiBecaController.java`, `docs/errors/…`.

## Fuera de alcance (no tocar)

Bitácora, `SecurityConfig` (el rol se protege en el `service`), endpoints de escritura.

## Resultado esperado

Un `POSTULANTE` becado ve su beca y su historial; sin beca recibe `404` `ERR_MBE_01`.

## Validación

1. `./mvnw -o compile` sin errores y `checkstyle:check` limpio.
2. `MiBecaServiceTest` y `MiBecaErrorCodesTest` (ver BE-03).
