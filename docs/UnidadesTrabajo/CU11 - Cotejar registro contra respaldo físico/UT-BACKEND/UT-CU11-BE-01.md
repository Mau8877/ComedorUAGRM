# UT-CU11-BE-01 — Migración y modelo de `cotejo_asistencia`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-01` |
| **Nombre** | Migración y modelo de `cotejo_asistencia` |
| **Caso de uso** | CU11-BE |
| **Depende de** | — (la tabla `planilla_asistencia` del CU09 ya existe) |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md). Molde: `V20261116100000__crear_planilla_asistencia.sql` y `PlanillaAsistencia.java`.

## Solicitud de negocio

Cada cotejo que el Administrador constata debe quedar guardado con sus cantidades, resultado, observaciones, responsable y fecha.

## Objetivo

Existen la tabla `cotejo_asistencia`, el enum `ResultadoCotejo`, el modelo JPA inmutable y su repository.

## Alcance

- Migración `V20261118100000__crear_cotejo_asistencia.sql`: `id`, `codigo_cotejo_asistencia` (UUID v7), `planilla_id` y `realizado_por` (FK), `fecha_cotejo`, `total_registros`, `coincidencias`, `discrepancias`, `resultado varchar(20)`, `observaciones text` y las columnas comunes; `CHECK` de cantidades (≥ 0 y suma = total) y de resultado (conforme ⇒ 0 discrepancias; con discrepancias ⇒ > 0 y observaciones no vacías); índices parciales por `(planilla_id, creado_en DESC)` y `realizado_por`. Sin unicidad.
- `B/features/cotejos/enums/ResultadoCotejo.java` (`CONFORME`, `CON_DISCREPANCIAS`).
- `B/features/cotejos/model/CotejoAsistencia.java`: extiende `BaseModel`, `@AttributeOverride` a `codigo_cotejo_asistencia`, `@Enumerated(STRING)`, solo getters.
- `B/features/cotejos/repository/CotejoAsistenciaRepository.java`.
- `DiagramaConceptual.mermaid` ya trae `COTEJO_ASISTENCIA` con estas columnas: se verifica que coincida.

Archivos esperados (crear/tocar): la migración, el enum, el modelo y el repository.

## Fuera de alcance (no tocar)

`cotejo_detalle` (opcional en el diseño), `planilla_asistencia` y `asistencia`.

## Resultado esperado

Flyway crea la tabla; la base rechaza un cotejo inconsistente aunque se inserte por fuera del servicio.

## Validación

1. Arrancar contra una base vacía: Flyway aplica la migración.
2. Insertar a mano un cotejo con `coincidencias + discrepancias <> total`, o `CONFORME` con discrepancias, o `CON_DISCREPANCIAS` sin observaciones: la base lo rechaza.
3. `CotejoIntegrationTest` pasa (UT-BE-05).
