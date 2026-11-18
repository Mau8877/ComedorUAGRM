# UT-CU12-BE-11 — Consulta del análisis BI

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-11` |
| **Nombre** | Consulta del análisis BI |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-05`, `UT-CU12-BE-10` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize` en el `service`).

## Solicitud de negocio

Con las vistas listas, el Directivo necesita pedir el análisis con los mismos filtros que los indicadores.

## Objetivo

Un servicio que arma el análisis BI de una selección de gestión y convocatoria.

## Alcance

- `B/features/dashboard/dto/BiResponse.java` (`record` con `gestion`, `convocatoria`, `embudo`, `facultades` con sus `carreras`, `convocatorias`, `puntaje`, `puntajePromedio`, `cobertura`, `asistencia` con `porDiaSemana`, `tendencia`, `actualizadoEn`, `sinDatos`). Los porcentajes van de 0 a 100 con un decimal y son `null` si el denominador es cero.
- `B/features/dashboard/repository/BiRepository.java` (`NamedParameterJdbcTemplate`): lecturas de las cuatro vistas y de `mv_indicadores_convocatoria`.
- `B/features/dashboard/service/BiService.java`: `consultar(gestion, convocatoria)` con `@PreAuthorize("hasRole('DIRECTIVO')")` y `@Transactional(readOnly = true)`. Reusa `IndicadoresService.resolver()` (gestión por defecto y `ERR_DAS_01`). El embudo, la cobertura y el puntaje salen de las convocatorias elegidas; la comparación y la tendencia ignoran el filtro de convocatoria y marcan la elegida; siempre 7 tramos de puntaje y 7 días de la semana.
- `IndicadoresService`: `resolver()` pasa a ser reutilizable; el promedio y la serie de asistencia vienen del registro real (asistieron sobre esperados) en lugar de `null`.

Archivos esperados (crear/tocar): `BiResponse`, `BiRepository`, `BiService`, `IndicadoresService`, `IndicadoresRepository`.

## Fuera de alcance (no tocar)

- Los endpoints HTTP (`UT-CU12-BE-12`).
- Escribir en la bitácora o en cualquier tabla.

## Resultado esperado

`BiService.consultar` devuelve el análisis de la selección sin modificar datos.

## Validación

1. `BiServiceTest` (12 pruebas): embudo y conversiones, ranking de facultades, variación sin convocatoria anterior, tramos siempre completos, denominadores en cero, filtro de convocatoria solo marca la elegida.
2. `IndicadoresServiceTest` (14): asistencia real y `sinDatos`.
