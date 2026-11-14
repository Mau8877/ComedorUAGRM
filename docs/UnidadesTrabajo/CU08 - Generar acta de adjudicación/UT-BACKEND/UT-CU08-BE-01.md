# UT-CU08-BE-01 — Migración: tabla `acta_adjudicacion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-01` |
| **Nombre** | Migración: tabla `acta_adjudicacion` |
| **Caso de uso** | CU08-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md): diseño lógico antes que la migración, nombre `V{yyyyMMddHHmmss}__descripcion.sql`, `codigo_<tabla>`, índices únicos parciales.
- Como referencia: la migración de `publicacion_resultado` del CU-22 (`V20261111100000__crear_publicacion_resultado.sql`).

## Solicitud de negocio

El acta oficial tiene que quedar registrada con su número, su archivo y quién la generó, una sola vez por convocatoria.

## Objetivo

La base de datos tiene la tabla que guarda el acta de cada convocatoria.

## Alcance

- Actualizar **antes** el diseño lógico (`diseno_base_de_datos.md`, `docs/DiagramaConceptual.mermaid` y la nota de la imagen 091): `acta_adjudicacion` con `convocatoria_id` **único entre las filas no eliminadas** (cambio nuevo respecto del diseño de grado) y la nota de que hay una acta por convocatoria.
- `R/db/migration/V{timestamp}__crear_acta_adjudicacion.sql`: crea `acta_adjudicacion` con `id`, `codigo_acta_adjudicacion` (`uuid` v7, único), `convocatoria_id` (FK), `numero` (`varchar(30)`, no nulo, **único**), `storage_path` (`varchar(500)`, no nulo), `generada_por` (FK a `usuario`), `generada_en` (`timestamptz`, no nulo) y las columnas comunes (`creado_en`, `actualizado_en`, `eliminado_en`).
  - Índice único parcial `uq_acta_adjudicacion_convocatoria` sobre `convocatoria_id` donde `eliminado_en IS NULL`.
  - Índice de `generada_por` (parcial) y el único de `numero`.
- Tests de migración con Testcontainers (se saltan sin Docker): la tabla existe; rechaza dos filas para la misma convocatoria; rechaza un `numero` repetido; rechaza `convocatoria_id` o `generada_por` inexistentes.

Archivos esperados (crear/tocar): la migración, su test y los documentos de diseño lógico.

## Fuera de alcance (no tocar)

- Tocar `adjudicacion`, `publicacion_resultado`, `convocatoria`, `finalista` ni `bitacora`.
- Una tabla de secuencias para el número (el correlativo se calcula en la UT-05).
- Código Java (UT-02).

## Resultado esperado

Flyway aplica la migración y la tabla acepta una sola acta por convocatoria y nunca dos con el mismo número.

## Validación

1. `./mvnw test` pasa (Flyway aplica sobre PostgreSQL real).
2. Una segunda fila para la misma convocatoria falla por la unicidad, igual que un número repetido.
3. El diseño lógico muestra la tabla igual que la migración.
