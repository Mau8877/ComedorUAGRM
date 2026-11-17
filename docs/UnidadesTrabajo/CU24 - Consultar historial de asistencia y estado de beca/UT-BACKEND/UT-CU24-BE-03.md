# UT-CU24-BE-03 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-BE-03` |
| **Nombre** | Pruebas del backend de Mi beca |
| **Caso de uso** | CU24-BE |
| **Depende de** | UT-CU24-BE-02 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`PostgresTestDatabase`, forma del `ApiResponse`).

## Solicitud de negocio

Garantizar que cada becado ve solo lo suyo y que los errores y el rango se cumplen.

## Objetivo

Hay pruebas unitarias del servicio y de los códigos de error, y de integración contra una base real.

## Alcance

- `T/features/mibeca/service/MiBecaServiceTest`: sin beca, estado y motivo, paginación y `meta`, orden por defecto, rango inválido, `sort` inválido, `page` inválido y las anotaciones `@PreAuthorize`/`@Transactional(readOnly)`.
- `T/features/mibeca/exception/MiBecaErrorCodesTest`: códigos documentados e indexados.
- `T/features/mibeca/MiBecaIntegrationTest` (base real): estado con motivo, historial con rango y orden, aislamiento entre dos becados, `404`, `403`, `401`, `400` y `405`.

Archivos esperados (crear/tocar): los tres tests.

## Fuera de alcance (no tocar)

Tests de otros módulos.

## Resultado esperado

Los CA01 a CA10 quedan cubiertos.

## Validación

1. `./mvnw -o test -Dtest='MiBeca*'` en verde; los de base real corren con `-Dtest.postgres.url=…` o quedan SKIPPED sin Docker (decirlo).
