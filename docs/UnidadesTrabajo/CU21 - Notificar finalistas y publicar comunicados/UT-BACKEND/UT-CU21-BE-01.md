# UT-CU21-BE-01 — Migración: `finalista`, `comunicado` y columnas de contexto en `notificacion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-01` |
| **Nombre** | Migración: `finalista`, `comunicado` y columnas de contexto en `notificacion` |
| **Caso de uso** | CU21-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) completo (id + `codigo_<tabla>`, columnas comunes, borrado lógico, índices únicos parciales, nombre de migración con timestamp).
- Diseño lógico en [`docs/DiagramaConceptual.mermaid`](../../../DiagramaConceptual.mermaid) (`FINALISTA`, `COMUNICADO`, `NOTIFICACION`).
- Como referencia: `R/db/migration/V20261106100000__crear_tablas_convocatoria_postulacion_y_puntaje.sql` y `V20261106100000__crear_tablas_notificaciones.sql`.

## Solicitud de negocio

El CU necesita guardar quién es finalista, cuándo se le avisó y cuándo entregó, y los comunicados publicados; hoy esas tablas no existen.

## Objetivo

La base tiene `finalista`, `comunicado` y el enum `destinatarios_comunicado` tal como el diseño lógico, y `notificacion` sabe a qué postulación o comunicado pertenece cada aviso.

## Alcance

- `R/db/migration/V20261107100000__crear_tablas_finalista_y_comunicado.sql`:
  - `CREATE TYPE destinatarios_comunicado AS ENUM ('TODOS','FINALISTAS','ADJUDICADOS')`.
  - `finalista`: `postulacion_id` (FK, único parcial), `posicion`, `plazo_entrega` (date), `notificado_en`, `documentacion_recibida_en`, más `id`, `codigo_finalista` y columnas comunes.
  - `comunicado`: `convocatoria_id` (nullable), `publicado_por`, `titulo` (150), `contenido`, `destinatarios`, `publicado_en`, más `id`, `codigo_comunicado` y columnas comunes.
  - `notificacion`: `postulacion_id` y `comunicado_id` (nullables) con sus índices parciales.
- No crea `auditoria_expediente`, `adjudicacion` ni nada de CU-05/07: CU-05 **amplía** `finalista` sin recrearla.

Archivos esperados (crear): la migración.

## Fuera de alcance (no tocar)

- Models y repositories (UT-02).
- Otras tablas de P4, ni tocar migraciones ya aplicadas.

## Resultado esperado

Flyway aplica la migración sobre una base vacía y sobre una con datos del CU06.

## Validación

1. Arrancar contra una base vacía: Flyway migra sin error y `spring.jpa.hibernate.ddl-auto=validate` no se queja.
2. Insertar dos `finalista` con la misma `postulacion_id` activa falla; con la primera eliminada lógicamente, funciona.
