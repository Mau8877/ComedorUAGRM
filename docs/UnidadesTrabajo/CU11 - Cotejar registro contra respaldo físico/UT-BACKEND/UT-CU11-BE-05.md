# UT-CU11-BE-05 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-05` |
| **Nombre** | Pruebas del backend del cotejo |
| **Caso de uso** | CU11-BE |
| **Depende de** | UT-CU11-BE-04 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`PostgresTestDatabase`, forma del `ApiResponse`, base vacía al terminar).

## Solicitud de negocio

Garantizar las reglas del cotejo, la privacidad de los datos y el rol.

## Objetivo

Hay pruebas unitarias del servicio y de los códigos de error, y una de integración contra una base real.

## Alcance

- `T/features/cotejos/service/CotejoAsistenciaServiceTest`: guardado, fecha de Bolivia, bitácora sin observaciones, `ERR_COT_01` a `04`, total cero, anotaciones de rol, transacción y auditoría.
- `T/features/cotejos/exception/CotejosErrorCodesTest`: códigos documentados e indexados.
- `T/features/cotejos/CotejoIntegrationTest`: datos y registros del periodo (excluye otro periodo, otra convocatoria y becas posteriores a la planilla), orden y paginación, sin CI, registrar conforme y con discrepancias, varios cotejos, reglas `400`, `404`, `403`, `401`, `405`, bitácora y que nada más cambie.

Archivos esperados (crear/tocar): los tres tests.

## Fuera de alcance (no tocar)

Tests de otros módulos.

## Resultado esperado

Los CA01 a CA12 quedan cubiertos.

## Validación

1. `./mvnw -o test -Dtest='Cotejo*,Cotejos*'` en verde; con `-Dtest.postgres.url=…` contra un Postgres vacío (sin Docker conectable, los de base real quedan SKIPPED y se dice).
