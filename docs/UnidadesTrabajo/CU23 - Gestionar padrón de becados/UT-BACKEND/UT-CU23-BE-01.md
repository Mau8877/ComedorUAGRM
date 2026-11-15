# UT-CU23-BE-01 — Tablas `beca` y `movimiento_beca` y sus modelos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-01` |
| **Nombre** | Tablas `beca` y `movimiento_beca` y sus modelos |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | — (existen `adjudicacion`, `postulante` y `convocatoria`) |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (nombres, borrado lógico,
índices únicos parciales) y [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).

## Solicitud de negocio

El padrón necesita guardar el estado de cada beca y cada cambio que sufre.

## Objetivo

Las dos tablas, sus enums y los modelos JPA, con el diagrama de datos al día.

## Alcance

- Migración `V20261115100000__crear_beca_y_movimiento_beca.sql`: enums `estado_beca` y `tipo_movimiento_beca`;
  `beca` (única por postulante y convocatoria, y por adjudicación cuando existe) y `movimiento_beca` (un solo
  `REEMPLAZO` por beca; `beca_reemplazante_id` solo en un reemplazo).
- `EstadoBeca`, `TipoMovimientoBeca`, `Beca` (con `darDeBaja()` y `suspender()`) y `MovimientoBeca` (inmutable).
- `docs/DiagramaConceptual.mermaid`: `beca.adjudicacion_id` pasa a «nullable (NULL en un reemplazo), UK parcial».

Archivos esperados (crear/tocar): la migración, `features/becas/enums/{EstadoBeca,TipoMovimientoBeca}.java`,
`features/becas/model/{Beca,MovimientoBeca}.java`, `docs/DiagramaConceptual.mermaid`.

## Fuera de alcance (no tocar)

- Repositories, services y controllers (BE-02 en adelante).
- `adjudicacion`, `postulacion` y sus modelos.

## Resultado esperado

Flyway aplica la migración y Hibernate mapea las dos tablas sin errores.

## Validación

1. `./mvnw compile` sin errores.
2. Un test con base real arranca el contexto (Flyway aplica y Hibernate valida el esquema); lo cubre BE-06.
3. Insertar dos becas del mismo postulante y convocatoria viola `uq_beca_postulante_convocatoria`.
