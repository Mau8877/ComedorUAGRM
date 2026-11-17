# UT-CU09-BE-01 — Migración y modelo de `planilla_asistencia`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-01` |
| **Nombre** | Migración y modelo de `planilla_asistencia` |
| **Caso de uso** | CU09-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`id` + `codigo_<tabla>`, borrado lógico, nombres de migración con timestamp, diagrama como fuente de verdad) y [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok, estructura de paquetes).
- Como referencia: `V20261113100000__crear_acta_adjudicacion.sql` y `B/features/actas/model/ActaAdjudicacion.java`.

## Solicitud de negocio

Cada planilla generada tiene que quedar registrada (periodo, cuántos becados incluyó, archivo, quién y cuándo) para poder consultarla, descargarla y, más adelante, cotejarla.

## Objetivo

Existen la tabla `planilla_asistencia`, su modelo JPA inmutable y su repository.

## Alcance

- Migración `V20261116100000__crear_planilla_asistencia.sql`: tabla con `id`, `codigo_planilla_asistencia` (UUID v7 único), `convocatoria_id` y `generada_por` (FK), `fecha_desde`, `fecha_hasta`, `total_becados`, `storage_path`, `generada_en` y las columnas comunes; `CHECK (fecha_hasta >= fecha_desde)` y `CHECK (total_becados > 0)`; índices parciales `(convocatoria_id, generada_en DESC)` y `(generada_por)` con `eliminado_en IS NULL`. Sin unicidad: se pueden generar varias planillas del mismo periodo.
- `B/features/planillas/model/PlanillaAsistencia.java`: extiende `BaseModel`, `@AttributeOverride` de `codigo` a `codigo_planilla_asistencia`, `@Getter` sin setters (campos `updatable = false`), constructor con todos los datos.
- `B/features/planillas/repository/PlanillaAsistenciaRepository.java`: `JpaRepository<PlanillaAsistencia, Long>` con `findByCodigo(UUID)`.
- `docs/DiagramaConceptual.mermaid`: agregar `total_becados` a `PLANILLA_ASISTENCIA`.

Archivos esperados (crear/tocar): la migración, `PlanillaAsistencia.java`, `PlanillaAsistenciaRepository.java` y `DiagramaConceptual.mermaid` (tocar).

## Fuera de alcance (no tocar)

- Las consultas JDBC (BE-02) y cualquier `service`.
- La tabla `cotejo_asistencia` (CU-11) y `asistencia` (CU-10).
- La columna `numero` o un índice único por periodo.

## Resultado esperado

Flyway crea la tabla y Hibernate mapea la entidad sin errores.

## Validación

1. `./mvnw test -Dtest=PlanillasConsultaRepositoryTest -Dtest.postgres.url=...` crea la base con la migración y pasa (incluye `migracion_rechazaUnPeriodoInvertido`, `migracion_rechazaUnTotalEnCero` y `migracion_permiteVariasPlanillasDelMismoPeriodo`).
2. `docs/DiagramaConceptual.mermaid` muestra `total_becados` en `PLANILLA_ASISTENCIA`.
