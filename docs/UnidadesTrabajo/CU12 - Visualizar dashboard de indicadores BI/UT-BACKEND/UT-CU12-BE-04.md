# UT-CU12-BE-04 — Catálogo de filtros (gestiones y convocatorias)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-04` |
| **Nombre** | Catálogo de filtros (gestiones y convocatorias) |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-03` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (un módulo no lee el repository de otro) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize` en el `service`).
- Como referencia: `ResultadosConsultaRepository` (CU22), consultas JDBC propias.

## Solicitud de negocio

Para filtrar, el Directivo necesita elegir entre las gestiones y las convocatorias que existen, y saber cuál es la gestión vigente.

## Objetivo

El `service` entrega las gestiones (con su vigencia) y sus convocatorias para armar los filtros, solo al Directivo.

## Alcance

- `B/features/dashboard/repository/FiltrosDashboardRepository.java` (JDBC): una consulta de gestiones no eliminadas (`etiqueta`, fechas) y **una sola** consulta de las convocatorias no eliminadas de todas ellas (sin consultar dentro de un bucle). Las convocatorias en `BORRADOR` no se ofrecen (no tienen datos).
- `B/features/dashboard/service/FiltrosDashboardService.java`:
  - `@PreAuthorize("hasRole('DIRECTIVO')")` y `@Transactional(readOnly = true)`.
  - `FiltrosDashboardResponse obtener()`: marca como `vigente` la gestión que contiene la fecha de hoy (`America/La_Paz`); si ninguna la contiene, la de `fecha_inicio` más reciente; sin gestiones, lista vacía.
  - Método auxiliar reutilizado por UT-05: `GestionVigente gestionVigente()`.
  - Orden: gestiones por fecha de inicio descendente; convocatorias por fecha de apertura descendente.
- Tests: un usuario que no es Directivo recibe `403`; con una gestión vigente se marca `vigente`; sin gestión vigente se toma la más reciente; sin gestiones la lista viene vacía; las convocatorias aparecen bajo su gestión; los borradores no aparecen; el número de consultas no crece con la cantidad de gestiones.

Archivos esperados (crear): el repository, el `service` y sus tests.

## Fuera de alcance (no tocar)

- El endpoint (UT-07).
- Crear, editar o borrar gestiones y convocatorias (CU-13).
- Los indicadores (UT-05).

## Resultado esperado

El cliente puede armar el filtro de gestión y convocatoria con un solo dato.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
