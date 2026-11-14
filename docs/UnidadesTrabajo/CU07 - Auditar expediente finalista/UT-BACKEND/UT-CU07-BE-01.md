# UT-CU07-BE-01 — Migración: auditoría de expediente y tablas de apoyo del expediente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-01` |
| **Nombre** | Migración: auditoría de expediente y tablas de apoyo del expediente |
| **Caso de uso** | CU07-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y la migración de referencia `R/db/migration/V20261107100000__crear_tablas_finalista_y_comunicado.sql`.

## Solicitud de negocio

Para auditar un expediente hay que guardar la auditoría y leer lo que el postulante declaró y adjuntó; esas tablas las crean los CU-02 y CU-16 de otro integrante, que todavía no están.

## Objetivo

Existen en la base `auditoria_expediente` y lo mínimo de las tablas del expediente, tal como el diseño lógico.

## Alcance

- Una migración `R/db/migration/V20261107110000__crear_auditoria_expediente_y_tablas_del_expediente.sql`.
- Enum `resultado_auditoria` (`VALIDADO`, `OBSERVADO`, `NO_VALIDO`).
- `auditoria_expediente`: `id`, `codigo_auditoria_expediente` (UUID v7), `finalista_id` FK, `auditor_id` FK a `usuario`, `fecha_auditoria timestamptz NOT NULL DEFAULT now()`, `resultado`, `observaciones varchar(500)`, `puntaje_academico numeric(5,2)` con `CHECK (puntaje_academico BETWEEN 0 AND 30)`, y las columnas comunes. Es append-only (sin `UPDATE` ni `DELETE`). Índice `(finalista_id, fecha_auditoria DESC) WHERE eliminado_en IS NULL`.
- Tablas mínimas del diseño lógico que este CU lee: `tipo_documento`, `convocatoria_documento` (PK compuesta `convocatoria_id` + `tipo_documento_id`, `obligatorio`), `documento_adjunto` (con `storage_path`, que apunta a un objeto de MinIO) y `respuesta_socioeconomica` (PK = FK `postulacion_id`, con `promedio_academico` entre otras columnas). Mismas convenciones de código público y borrado lógico que el resto.
- **Coordinar antes de empezar** con quien hace los CU-02 y CU-16: si él ya integró alguna de esas cuatro tablas, esta migración **omite** la que ya existe (no se recrea ni se duplica).
- Los CU-02 y CU-16 amplían estas tablas después, sin recrearlas.

Archivos esperados (crear): la migración. Test de migración con Postgres real (existen las tablas, `CHECK` de 0 a 30, enum).

## Fuera de alcance (no tocar)

- Models, repositories y servicios (UT-03 en adelante).
- Cambiar la escala del puntaje de la IA (UT-02).
- Cualquier columna que el diseño lógico no tenga, salvo `puntaje_academico` y el largo de `observaciones`, que son decisión de este CU.

## Resultado esperado

La base tiene la auditoría y las tablas de apoyo, con la migración aplicada limpia.

## Validación

1. `./mvnw test` pasa (incluido el test de migración).
2. Arrancar sobre una base limpia: `\d auditoria_expediente` muestra las columnas y el `CHECK`.
