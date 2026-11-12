# UT-CU05-BE-01 — Migración: tabla `adjudicacion` y `convocatoria.cantidad_finalistas`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-01` |
| **Nombre** | Migración: tabla `adjudicacion` y `convocatoria.cantidad_finalistas` |
| **Caso de uso** | CU05-BE |
| **Depende de** | — (usa las tablas `postulacion`, `convocatoria`, `usuario` y `finalista`, que ya existen) |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (consultar el diagrama conceptual antes de crear una tabla; nombres de migración con timestamp).
- Como referencia: `R/db/migration/V20261108100000__crear_postulacion_historial.sql` y `V20261107110000__crear_auditoria_expediente_y_tablas_del_expediente.sql` (enum de PostgreSQL, índice único parcial).

## Solicitud de negocio

La DUBSS necesita guardar la decisión final sobre cada finalista y definir, en la convocatoria, cuántos finalistas se eligen.

## Objetivo

La base tiene la tabla `adjudicacion` y la columna `convocatoria.cantidad_finalistas`, y el diseño lógico las refleja.

## Alcance

- **Primero el diseño lógico:** agregar `cantidad_finalistas` a `convocatoria` en `diseno_base_de_datos.md` y en `docs/DiagramaConceptual.mermaid` (y anotar que la imagen 091 queda por actualizar), y confirmar que `adjudicacion` ya figura ahí con su forma final. Si algo contradice al CU, **parar y avisar**.
- Migración nueva `R/db/migration/V{yyyyMMddHHmmss}__crear_adjudicacion_y_cantidad_finalistas.sql` (timestamp posterior a la última migración existente, nunca `V1__`):
  - `CREATE TYPE resultado_adjudicacion AS ENUM ('ADJUDICADA', 'NO_ADJUDICADA')`.
  - `adjudicacion`: `id` (identity), `codigo_adjudicacion` (`uuid NOT NULL DEFAULT uuidv7() UNIQUE`), `postulacion_id` FK→`postulacion`, `resultado resultado_adjudicacion NOT NULL`, `puntaje_final numeric(5,2)` (**nullable** *(por revisar, ver README)*, con `CHECK (puntaje_final IS NULL OR puntaje_final BETWEEN 0 AND 100)`), `decidido_por` FK→`usuario`, `fecha_adjudicacion timestamptz NOT NULL DEFAULT now()`, `creado_en`, `actualizado_en`, `eliminado_en`. **Sin** columna de motivo ni de clave de idempotencia.
  - Índice único parcial `uq_adjudicacion_postulacion` sobre `postulacion_id` `WHERE eliminado_en IS NULL` (una decisión por postulación) e índice por `decidido_por` si se va a consultar.
  - `ALTER TABLE convocatoria ADD COLUMN cantidad_finalistas int` con `CONSTRAINT ck_convocatoria_cantidad_finalistas CHECK (cantidad_finalistas IS NULL OR cantidad_finalistas > 0)`.
  - Comentario de cabecera con la fuente y el aviso de que `cantidad_finalistas` la escribe después el CU-13.
- No se tocan `finalista` ni `postulacion_historial` (se reutilizan tal cual).
- Test con Testcontainers (se salta sin Docker): la migración corre sobre las anteriores; se puede insertar una adjudicación y no una segunda para la misma postulación; los dos valores del enum funcionan; `puntaje_final` rechaza valores fuera de 0 a 100; `cantidad_finalistas` acepta `NULL` y positivos y rechaza `0` y negativos.

Archivos esperados (crear): la migración y su test. (Tocar): `diseno_base_de_datos.md` y `docs/DiagramaConceptual.mermaid`.

## Fuera de alcance (no tocar)

- Modificar migraciones ya aplicadas (cambiaría su checksum en Flyway).
- Columnas de motivo, de clave de idempotencia o de «publicado» (la marca de publicación es del CU-22).
- Una columna para distinguir «rechazada» de «no adjudicada» (se distingue por la fila en `adjudicacion`, decisión 8 del CU).
- Cambios en `finalista` o `postulacion_historial`.

## Resultado esperado

La tabla y la columna existen, con sus restricciones, y el diseño lógico coincide con la base.

## Validación

1. `./mvnw test` pasa (el test de la migración corre o se salta sin Docker).
2. Con `docker compose up`, `\d adjudicacion` y `\d convocatoria` muestran las columnas y restricciones descritas.
3. `./mvnw checkstyle:check` sin errores.
