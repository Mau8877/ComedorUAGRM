# UT-CU19-BE-01 — Migración: historial de la postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-01` |
| **Nombre** | Migración: historial de la postulación |
| **Caso de uso** | CU19-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- La migración del CU-07 (`V20261107110000__crear_auditoria_expediente_y_tablas_del_expediente.sql`) como referencia de estilo, y el diseño lógico (`images/091`, transcrito en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid), entidad `POSTULACION_HISTORIAL`).

## Solicitud de negocio

Cada cambio de estado de una postulación debe quedar escrito en un historial que no se edita ni se borra.

## Objetivo

Existe la tabla `postulacion_historial` en la base, tal como la define el diseño lógico.

## Alcance

- Migración `R/db/migration/V{yyyyMMddHHmmss}__crear_postulacion_historial.sql` (timestamp posterior a `V20261107140000`).
- Tabla `postulacion_historial`: `id` (identity, PK), `codigo_postulacion_historial` (`uuid`, `DEFAULT uuidv7()`, único), `postulacion_id` (FK a `postulacion`, no nula), `usuario_id` (FK a `usuario`, **nula**: si el cambio lo hizo el sistema), `estado_anterior` y `estado_nuevo` (tipo `estado_postulacion` ya existente, no nulos), `motivo` (`text`, nulo) y `creado_en`, `actualizado_en`, `eliminado_en` como en las demás tablas.
- Índice `(postulacion_id, creado_en)` para leer el historial de una postulación en orden.
- **Sin ninguna columna fuera del diseño lógico** (no lleva clave de idempotencia).
- La migración **crea** la tabla; el CU-02, el CU-20 y el CU-05 la **amplían** sin recrearla.
- Test de migración con Testcontainers en el estilo de `ExpedienteMigracionTest` (se salta si Docker no está disponible): la tabla existe, la FK a `postulacion` y el índice.

Archivos esperados (crear): la migración y su test.

## Fuera de alcance (no tocar)

- Cualquier otra tabla o columna.
- Filas de ejemplo (UT-08).
- Modelo Java (UT-02).
- Cambiar el estado de ninguna postulación existente.

## Resultado esperado

La base tiene `postulacion_historial` lista para el Model.

## Validación

1. `./mvnw test` pasa (la migración corre sin error sobre una base limpia).
2. `./mvnw checkstyle:check` sin errores.
