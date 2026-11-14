# UT-CU05-BE-05 — Panel de la convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-05` |
| **Nombre** | Panel de la convocatoria |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-03`, `UT-CU05-BE-04` |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).
- Como referencia: `ListaPriorizadaService` (CU-06) para el estilo de un `service` de solo lectura.

## Solicitud de negocio

Antes de seleccionar o decidir, el Administrador necesita ver cuántos cupos hay, cuántos se usaron, el puntaje mínimo y cuántos finalistas faltan por elegir.

## Objetivo

`AdjudicacionService.panel(...)` entrega el estado de adjudicación de una convocatoria.

## Alcance

- `B/features/postulaciones/service/AdjudicacionService.java` con `@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` y, en este método, `@Transactional(readOnly = true)`. Método `panel(UUID convocatoriaCodigo)` que devuelve `PanelAdjudicacionResponse`:
  - convocatoria inexistente → `404` `ERR_PRI_01`;
  - `cupos` y `puntajeMinimo` tal como están (pueden ser `null` mientras la DUBSS no los defina, nunca se inventan);
  - `adjudicados`, `noAdjudicados`, `totalFinalistas` y `calificadasSinSeleccionar` de la consulta; `cuposDisponibles` = cupos − adjudicados (`null` si no hay cupos; nunca negativo);
  - `cantidadFinalistas` (la configurada, o `null`);
  - `resultadosPublicados` y `decisionesHabilitadas` = convocatoria `CERRADA` **y** cupos definidos **y** puntaje mínimo definido **y** resultados no publicados.
- El panel se puede consultar con la convocatoria en **cualquier** estado (A12: se puede ver la lista, no decidir); lo que cambia es `decisionesHabilitadas`.
- No loguea (es lectura, ver `LOGGING_BACKEND.md`) ni anota la bitácora.
- Tests unitarios: convocatoria inexistente; panel con cupos y mínimo definidos; sin cupos o sin mínimo (`decisionesHabilitadas = false`, `cuposDisponibles = null`); convocatoria abierta; cupos agotados (`cuposDisponibles = 0`); resultados publicados simulados; cantidad de finalistas con y sin valor.

Archivos esperados (crear): `AdjudicacionService` (con `panel`) y su test.

## Fuera de alcance (no tocar)

- Seleccionar finalistas (UT-06) y adjudicar o no adjudicar (UT-07); ambos se agregan a este mismo archivo o viven en su servicio, según se resuelva en esas UT.
- Endpoints (UT-08).
- Calcular o guardar cupos disponibles como columna.
- Exponer la cantidad de finalistas en algo que no sea este endpoint del Administrador.

## Resultado esperado

El `service` devuelve un panel completo y correcto para cualquier convocatoria, con o sin cupos y puntaje mínimo definidos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
