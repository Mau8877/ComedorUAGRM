# UT-CU22-BE-01 — Migración: tabla `publicacion_resultado`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-01` |
| **Nombre** | Migración: tabla `publicacion_resultado` |
| **Caso de uso** | CU22-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md): diseño lógico antes que la migración, nombre `V{yyyyMMddHHmmss}__descripcion.sql`, `codigo_<tabla>`.
- Como referencia: la migración de `adjudicacion` del CU-05 (`UT-CU05-BE-01`).

## Solicitud de negocio

Para que los resultados sean oficiales hace falta dejar constancia de cuándo y quién los publicó, una sola vez por convocatoria.

## Objetivo

La base de datos tiene la tabla que marca una convocatoria como publicada.

## Alcance

- Actualizar **antes** el diseño lógico (`diseno_base_de_datos.md`, `docs/DiagramaConceptual.mermaid` y la nota de la imagen 091) con `publicacion_resultado` completa: `id`, `codigo_publicacion_resultado` (UUID único), `convocatoria_id` (FK, **único**), `publicado_por` (FK al usuario), `publicado_en` (`timestamptz`, no nulo) y las columnas comunes (`created_at`, `updated_at`, `deleted_at`).
- `R/db/migration/V{timestamp}__add_publicacion_resultado.sql`: crea la tabla con sus claves, el índice de `publicado_por` y la unicidad de `convocatoria_id`.
- Si el estado de la convocatoria es un enum de PostgreSQL que aún no tiene el valor `publicada`, la misma migración lo agrega (se revisa antes en el modelo actual; si ya existe, no se toca).
- Tests de migración con Testcontainers (se saltan sin Docker): la tabla existe, rechaza dos filas para la misma convocatoria y rechaza `convocatoria_id` o `publicado_por` inexistentes.

Archivos esperados (crear/tocar): la migración, su test y los documentos de diseño lógico.

## Fuera de alcance (no tocar)

- Tocar `adjudicacion`, `finalista`, `notificacion` o `comunicado`.
- Crear la tabla de acta (CU-08) ni columnas del padrón (CU-23).
- Código Java (UT-02).

## Resultado esperado

Flyway aplica la migración y la tabla acepta una sola publicación por convocatoria.

## Validación

1. `./mvnw test` pasa (Flyway aplica sobre PostgreSQL real).
2. Una segunda fila para la misma convocatoria falla por la unicidad.
3. El diseño lógico muestra la tabla igual que la migración.
