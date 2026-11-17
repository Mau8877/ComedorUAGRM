# UT-CU10-BE-01 — Migración y modelo de `asistencia`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-01` |
| **Nombre** | Migración y modelo de `asistencia` |
| **Caso de uso** | CU10-BE |
| **Depende de** | — |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md). Molde: `V20261116100000__crear_planilla_asistencia.sql`.

## Solicitud de negocio

Cada asistencia debe quedar guardada una sola vez por becado y día, con quién la registró y a qué hora.

## Objetivo

Existen la tabla `asistencia` con su índice de idempotencia y el modelo JPA inmutable.

## Alcance

- Migración `V20261117100000__crear_asistencia.sql`: `id`, `codigo_asistencia` (UUID v7), `beca_id` y `registrado_por` (FK), `fecha`, `hora_registro`, columnas comunes; índice único parcial `uq_asistencia_beca_fecha (beca_id, fecha) WHERE eliminado_en IS NULL` e índices por `fecha` y `registrado_por`.
- `B/features/asistencias/model/Asistencia.java`: extiende `BaseModel`, `@AttributeOverride` a `codigo_asistencia`, solo getters.
- `DiagramaConceptual.mermaid` ya trae `ASISTENCIA` con estas columnas: se verifica que coincida.

Archivos esperados (crear/tocar): la migración y `Asistencia.java`.

## Fuera de alcance (no tocar)

Tablas `planilla_asistencia` y `cotejo_asistencia` (CU09/CU11), vistas de BI y cualquier columna que el diagrama no dibuje (p. ej. turno).

## Resultado esperado

Flyway crea la tabla y el modelo la mapea.

## Validación

1. Arrancar contra una base vacía: Flyway aplica la migración sin error.
2. Insertar dos filas con la misma `beca_id` y `fecha` activas: la segunda viola `uq_asistencia_beca_fecha`.
3. `AsistenciaRepositoryTest` pasa (ver UT-BE-04).
