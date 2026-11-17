# UT-CU26-BE-01 — Migración, repositorios y pruebas de base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-01` |
| **Nombre** | Migración, repositorios y pruebas de base real |
| **Caso de uso** | CU26-BACKEND |
| **Depende de** | — |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y
[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Guardar las versiones del modelo con sus parámetros y llevar el estado y el avance de cada reprocesamiento, sin dejar que dos corran a la vez para la misma convocatoria.

## Objetivo

Las tablas `parametro_modelo` y `lote_reprocesamiento`, y los repositorios que las leen y escriben.

## Alcance

- Migración `V20261120100000__crear_parametro_modelo_y_lote_reprocesamiento.sql`: `parametro_modelo` (índice único parcial `(modelo_id, clave)`, `CHECK` de ponderación 0–100) y `lote_reprocesamiento` (con `procesadas` y `fallidas`, `CHECK` de estado y de cantidades, índice único parcial de un lote en curso por convocatoria).
- `ModeloPreseleccionConsultaRepository`: `vigente`, `parametros`, `versiones`, `listar`, `contar`, `archivarVigente`, `crear`, `insertarParametro`.
- `ReprocesamientoConsultaRepository`: `convocatoria`, `resultadosPublicados`, `calificadas`, `porReprocesar`, `crearLote`, `loteDe`, `lote`, `loteEnCurso`, `marcarEnProceso`, `sumarAvance`, `terminar`, `marcarHuerfanos`.
- `PrecalificacionConsultaRepository`: `datosCalificada`, `sigueCalificada`, `tieneVigenteDelModelo` (y `datosPendiente` / `sigueEnPendiente` pasan a compartir su consulta).
- Pruebas con base real: `ModeloPreseleccionConsultaRepositoryTest`, `ReprocesamientoConsultaRepositoryTest`, `PrecalificacionConsultaRepositoryTest`.
- `DiagramaConceptual.mermaid`: `procesadas` y `fallidas` en `LOTE_REPROCESAMIENTO`.

Archivos esperados (crear/tocar): la migración, los tres repositorios, los tres tests y `docs/DiagramaConceptual.mermaid`.

## Fuera de alcance (no tocar)

- Clases de modelo JPA para `parametro_modelo` y `lote_reprocesamiento`: se usa JDBC.
- Cualquier servicio o endpoint (BE-02 a BE-04).

## Resultado esperado

Las dos tablas existen con sus restricciones y los repositorios devuelven y modifican lo que describe cada método.

## Validación

1. `./mvnw -o -q compile` y `./mvnw -o -q checkstyle:check` sin errores.
2. Con un Postgres descartable: `./mvnw -o test -Dtest='ModeloPreseleccionConsultaRepositoryTest,ReprocesamientoConsultaRepositoryTest,PrecalificacionConsultaRepositoryTest' -Dtest.postgres.url=jdbc:postgresql://localhost:55432/<base_vacia>` → todos en verde (sin Docker quedan `SKIPPED`).
3. Un segundo `crearLote` para la misma convocatoria con otro en curso lanza `DataIntegrityViolationException`.
