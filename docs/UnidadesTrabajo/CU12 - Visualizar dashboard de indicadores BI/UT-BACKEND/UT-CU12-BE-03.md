# UT-CU12-BE-03 — Códigos de error y DTOs

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-03` |
| **Nombre** | Códigos de error y DTOs |
| **Caso de uso** | CU12-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (catálogo y documentación obligatoria).
- Como referencia: `ResultadosErrorCodes` y los DTOs del módulo `resultados` (CU22).

## Solicitud de negocio

Los errores del dashboard deben ser claros y catalogados, y el contrato con el cliente debe estar fijado antes de construir la consulta.

## Objetivo

Existen los códigos `ERR_DAS_01` y `ERR_DAS_02`, su documentación y los DTOs del módulo.

## Alcance

- `B/features/dashboard/exception/DashboardErrorCodes.java` con `ERR_DAS_01` (gestión o convocatoria inexistente, o la convocatoria no es de la gestión) y `ERR_DAS_02` (no hay datos que exportar).
- `docs/errors/dashboard/ERRORES_DASHBOARD.md` con las dos secciones en el formato de `GUIA_ERRORES_BACKEND.md` (HTTP `404` y `400`, excepción `NotFoundException` y `ValidationException`, significado y dónde se lanza) y su link en `docs/errors/README.md` bajo «Dashboard (DAS)».
- DTOs (`record`) en `B/features/dashboard/dto/`: `FiltrosDashboardResponse` (con `GestionFiltro` y `ConvocatoriaFiltro`), `IndicadoresResponse` (con `GestionResumen`, `ConvocatoriaResumen`, `Totales` y `IndicadorMensual`), con los campos del contrato del README (`asistenciaPromedio` y `asistencia` son `Double` y valen `null` por ahora).
- Tests: el catálogo no repite códigos y cada código tiene su sección en `docs/errors/`; los DTOs se serializan con los nombres del contrato (`yyyy-MM` para `mes`).

Archivos esperados (crear): `DashboardErrorCodes`, los DTOs, `ERRORES_DASHBOARD.md` y los tests. (Tocar): `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT-04 a UT-06).
- Códigos de otros módulos.
- Bitácora: el dashboard no se anota.

## Resultado esperado

El módulo tiene su catálogo de errores documentado y sus DTOs listos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. `docs/errors/README.md` enlaza `ERR_DAS_01` y `ERR_DAS_02` a su sección.
