# UT-CU19-BE-04 — Consulta de la devolución

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-04` |
| **Nombre** | Consulta de la devolución |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-01` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `B/features/expedientes/repository/ExpedienteConsultaRepository.java` (JDBC de lectura propia y `bloquearFinalista`).

## Solicitud de negocio

Para decidir si se puede devolver hace falta saber el estado de la postulación del finalista y si ya fue auditado después de la última devolución.

## Objetivo

Existe una consulta propia que entrega lo que el `service` necesita y bloquea la postulación mientras se devuelve.

## Alcance

- `B/features/postulaciones/repository/DevolucionConsultaRepository.java` (`NamedParameterJdbcTemplate`, respeta `eliminado_en IS NULL`; no lee el repository de otro módulo):
  - `cabecera(UUID finalistaCodigo)`: `Optional` con `postulacionId`, `postulacionCodigo`, `finalistaId`, `finalistaCodigo`, `estado` (texto), `postulanteUsuarioCodigo`, nombre del postulante y nombre de la convocatoria; vacío si el finalista no existe o fue eliminado.
  - `bloquearPostulacion(long postulacionId)`: `SELECT ... FOR UPDATE` sobre la fila, para que dos devoluciones simultáneas se atiendan una después de la otra.
  - `hayAuditoriaPosteriorALaUltimaDevolucion(long finalistaId, long postulacionId)`: verdadero si el finalista tiene al menos una auditoría y, si la postulación ya fue devuelta antes (`estado_nuevo = 'devuelta'` en el historial), alguna es **posterior a la fecha de la última devolución**.
- Tests con Testcontainers (se saltan sin Docker): finalista inexistente, sin auditorías, con auditoría y sin devoluciones, con auditoría anterior a la última devolución (falso), con auditoría posterior (verdadero) y que `bloquearPostulacion` no falla.

Archivos esperados (crear): `DevolucionConsultaRepository` y su test.

## Fuera de alcance (no tocar)

- Escribir en la base (el `service` guarda por JPA, UT-05).
- Reglas de negocio ni errores (UT-05).
- Modificar `ExpedienteConsultaRepository`.

## Resultado esperado

El `service` puede consultar y bloquear sin tocar tablas de otros módulos por JPA.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
