# UT-CU20-BE-03 — Consulta del cierre

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-03` |
| **Nombre** | Consulta del cierre |
| **Caso de uso** | CU20-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `B/features/postulaciones/repository/DevolucionConsultaRepository.java` (CU-19).

## Solicitud de negocio

Para archivar o rechazar hace falta saber el estado de la postulación del finalista y si los resultados de su convocatoria ya se publicaron.

## Objetivo

Existe una consulta propia que entrega lo que el `service` necesita y bloquea la postulación mientras se decide.

## Alcance

- `B/features/postulaciones/repository/CierrePostulacionConsultaRepository.java` (`NamedParameterJdbcTemplate`, respeta `eliminado_en IS NULL`; no lee el repository de otro módulo):
  - `cabecera(UUID finalistaCodigo)`: `Optional` con `postulacionId`, `postulacionCodigo`, `finalistaId`, `finalistaCodigo`, `convocatoriaId`, `estado` (texto), `postulanteUsuarioCodigo`, nombre del postulante y nombre de la convocatoria; vacío si el finalista no existe o fue eliminado.
  - `bloquearPostulacion(long postulacionId)`: `SELECT ... FOR UPDATE`, para que dos decisiones simultáneas se atiendan una después de la otra.
  - `resultadosPublicados(long convocatoriaId)`: verdadero si la convocatoria ya tiene sus resultados publicados. **El CU-22 todavía no existe**: esta UT no crea la marca de publicación ni agrega columnas. Si en el modelo actual no hay dónde leerla, el método devuelve `false` con un comentario `TODO` que apunta al CU-22 *(por revisar)*; si el CU-22 ya la definió, se lee de ahí.
- Tests con Testcontainers (se saltan sin Docker): finalista inexistente, finalista eliminado, cabecera completa, `bloquearPostulacion` no falla y `resultadosPublicados` devuelve `false` en una convocatoria sin publicar.

Archivos esperados (crear): `CierrePostulacionConsultaRepository` y su test.

## Fuera de alcance (no tocar)

- Escribir en la base (el `service` guarda por JPA, UT-04).
- Reglas de negocio y errores (UT-04).
- Modificar `DevolucionConsultaRepository` ni `ExpedienteConsultaRepository`.
- Publicar resultados o crear la marca de publicación (CU-22).

## Resultado esperado

El `service` puede consultar, bloquear y saber si hay resultados publicados sin tocar tablas de otros módulos por JPA.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
