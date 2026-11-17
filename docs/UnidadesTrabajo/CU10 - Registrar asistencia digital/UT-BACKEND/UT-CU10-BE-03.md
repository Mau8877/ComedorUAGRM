# UT-CU10-BE-03 — Servicio, controller y errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-03` |
| **Nombre** | Servicio, controller y errores |
| **Caso de uso** | CU10-BE |
| **Depende de** | UT-CU10-BE-02 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

La app móvil necesita dos operaciones para el Personal del Comedor: buscar al becado y confirmar su asistencia.

## Objetivo

Los dos endpoints responden según el contrato del README, con sus errores catalogados y documentados.

## Alcance

- `AsistenciasErrorCodes` (`ERR_ASI_01..03`) y su documentación en `docs/errors/asistencias/ERRORES_ASISTENCIAS.md` + índice del `README.md`.
- DTOs: `BuscarBecadoRequest`, `RegistrarAsistenciaRequest`, `BecadoAsistenciaResponse`, `AsistenciaResponse`.
- `AsistenciaService.buscarBecado` / `registrar` (`@PreAuthorize("hasRole('PERSONAL_COMEDOR')")`; fecha de hoy en `America/La_Paz`; solo beca `VIGENTE`; log INFO solo al crear, sin CI ni nombres).
- `AsistenciaController` (`/api/v1/asistencias`): `201` al crear, `200` si ya existía.

Archivos esperados (crear/tocar): `exception/`, `dto/` (4), `service/AsistenciaService.java`, `controller/AsistenciaController.java`, `docs/errors/…`.

## Fuera de alcance (no tocar)

Bitácora, notificaciones, cambios de estado de la beca, `SecurityConfig` (el rol se protege en el `service`).

## Resultado esperado

Un `PERSONAL_COMEDOR` busca por CI y registra; repetir devuelve `200` `yaRegistrada = true`.

## Validación

1. `mvn compile` sin errores y `checkstyle:check` limpio.
2. `AsistenciaServiceTest` y `AsistenciasErrorCodesTest` (ver BE-04).
