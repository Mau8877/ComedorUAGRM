# UT-CU10-BE-04 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-04` |
| **Nombre** | Pruebas del backend de asistencia |
| **Caso de uso** | CU10-BE |
| **Depende de** | UT-CU10-BE-03 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`PostgresTestDatabase`, tests de controller con la forma del `ApiResponse`).

## Solicitud de negocio

Garantizar que la idempotencia, los estados de beca y el rol se cumplen.

## Objetivo

Hay pruebas unitarias del servicio y de los códigos de error, y de integración contra una base real.

## Alcance

- `T/features/asistencias/service/AsistenciaServiceTest`: CI inexistente, beca no vigente, beca inexistente, creación, duplicado, fecha de Bolivia, anotaciones de rol y transacción.
- `T/features/asistencias/exception/AsistenciasErrorCodesTest`: códigos documentados.
- `T/features/asistencias/AsistenciaIntegrationTest` (base real): flujo completo, `201`/`200`, una sola fila, `409`, `404`, `403`, `400`, `405`.

Archivos esperados (crear/tocar): los tres tests.

## Fuera de alcance (no tocar)

Tests de otros módulos.

## Resultado esperado

Los CA01 a CA10 quedan cubiertos.

## Validación

1. `./mvnw -o test -Dtest='Asistencia*'` en verde; los de base real corren con `-Dtest.postgres.url=…` o quedan SKIPPED sin Docker (decirlo).
