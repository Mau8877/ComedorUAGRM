# UT-CU05-BE-04 — Consulta de adjudicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-04` |
| **Nombre** | Consulta de adjudicación |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-01` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `B/features/postulaciones/repository/CierrePostulacionConsultaRepository.java` (CU-20), `ListaPriorizadaRepository` (CU-06, el `ROW_NUMBER()`) y `ExpedienteConsultaRepository` (CU-07, cómo se lee la última auditoría y el puntaje).

## Solicitud de negocio

Para mostrar el panel, elegir finalistas y decidir hace falta leer cupos, puntajes, auditorías y la lista priorizada, y bloquear la convocatoria mientras se decide.

## Objetivo

Existe una consulta propia que entrega todo lo que el panel, la selección y la decisión necesitan, y que bloquea la convocatoria y la postulación en orden fijo.

## Alcance

- `B/features/postulaciones/repository/AdjudicacionConsultaRepository.java` (`NamedParameterJdbcTemplate`, respeta `eliminado_en IS NULL`; no lee el repository de otro módulo):
  - `convocatoria(UUID codigo)`: `Optional` con `id`, `codigo`, `nombre`, `estado` (texto), `cupos`, `puntajeMinimo` y `cantidadFinalistas`; vacío si no existe o fue eliminada.
  - `bloquearConvocatoria(long convocatoriaId)`: `SELECT ... FOR UPDATE` sobre la fila de la convocatoria, para que dos decisiones simultáneas se atiendan una después de la otra y no superen los cupos.
  - `cabeceraFinalista(UUID finalistaCodigo)`: `Optional` con `finalistaId`, `finalistaCodigo`, `postulacionId`, `postulacionCodigo`, `convocatoriaId`, `convocatoriaCodigo`, `estado` (texto) y `puntajeIa` (el vigente); vacío si el finalista no existe o fue eliminado.
  - `ultimaAuditoria(long finalistaId)`: `Optional` con `resultado` y `puntajeAcademico` de la auditoría más reciente (mismo criterio de «última» que el CU-07).
  - `bloquearPostulacion(long postulacionId)`: `SELECT ... FOR UPDATE`.
  - `contarAdjudicadas(long convocatoriaId)` y `contarNoAdjudicadas(long convocatoriaId)`: filas de `adjudicacion` no eliminadas de las postulaciones de esa convocatoria, por resultado. Cupos disponibles = cupos − adjudicadas; **se calcula, no se guarda**.
  - `contarFinalistas(long convocatoriaId)` y `contarCalificadasSinFinalista(long convocatoriaId)`.
  - `siguientesSinFinalista(long convocatoriaId, int limite)`: las postulaciones «calificadas» de la convocatoria que aún no son finalistas, con la **posición que ocupan en la lista priorizada completa** (`ROW_NUMBER()` con el mismo orden del CU-06: puntaje vigente descendente, fecha de envío ascendente con nulos al final, id ascendente), ordenadas por posición y recortadas a `limite`.
  - `resultadosPublicados(long convocatoriaId)`: **reutiliza** el criterio que dejó el CU-20 (hoy `false`, con `TODO` hacia el CU-22); si el CU-22 ya definió la marca, se lee de ahí. Esta UT no crea la marca.
- **Cuidado con los bloqueos:** siempre se bloquea la convocatoria y después la postulación, nunca al revés.
- Tests con Testcontainers (se saltan sin Docker): convocatoria inexistente y eliminada; cabecera completa del finalista; última auditoría con varias auditorías; cuentas de adjudicadas, no adjudicadas, finalistas y calificadas sin finalista; `siguientesSinFinalista` entrega la posición de la lista **completa** (no la de la página) y deja fuera a quien ya es finalista; el orden coincide con el que entrega `ListaPriorizadaRepository` para la misma convocatoria; los dos bloqueos no fallan.

Archivos esperados (crear): `AdjudicacionConsultaRepository` y su test.

## Fuera de alcance (no tocar)

- Escribir en la base (los `service` guardan por JPA, UT-06 y UT-07).
- Reglas de negocio y errores (UT-05, UT-06 y UT-07).
- Modificar `ListaPriorizadaRepository`, `ExpedienteConsultaRepository` ni `CierrePostulacionConsultaRepository`.
- Publicar resultados o crear la marca de publicación (CU-22).

## Resultado esperado

Los `service` pueden consultar, contar y bloquear sin tocar tablas de otros módulos por JPA.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
