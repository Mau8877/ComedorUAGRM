# UT-CU23-BE-06 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-06` |
| **Nombre** | Pruebas |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | UT-CU23-BE-02, BE-03, BE-04, BE-05 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Que las reglas del padrón (estados, alta sin duplicados, reemplazo) no se rompan sin que nadie lo note.

## Objetivo

Pruebas unitarias del servicio y de integración contra Postgres real del CU23.

## Alcance

- `BecaEstadoServiceTest` (Mockito): estados válidos e inválidos, motivo recortado, reemplazo con candidato distinto.
- `PadronIntegrationTest` (Postgres real, JWT firmados de verdad): `401`/`403`; resumen y listado (publicados o no,
  búsqueda por nombre y CI, filtro por estado, orden); alta con y sin duplicados y que solo cuentan las `ADJUDICADA`;
  baja y suspensión con su movimiento y su bitácora; reemplazo completo y sus errores; que ningún `GET` escribe.
- Confirmar que Flyway aplica la migración y el esquema valida (el contexto arranca).

Archivos esperados (crear/tocar): `BecaEstadoServiceTest.java`, `PadronIntegrationTest.java`.

## Fuera de alcance (no tocar)

- Los tests de otros módulos.

## Resultado esperado

El comportamiento del CU23 queda cubierto de punta a punta.

## Validación

1. `./mvnw test -Dtest=BecaEstadoServiceTest` en verde.
2. `./mvnw test -Dtest=PadronIntegrationTest -Dtest.postgres.url=jdbc:postgresql://localhost:55432/<base_vacia>` en
   verde (con Docker, o con una base descartable si Testcontainers no conecta).
3. `./mvnw checkstyle:check` sin errores.
