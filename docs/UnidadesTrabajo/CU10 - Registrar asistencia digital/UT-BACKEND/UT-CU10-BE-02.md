# UT-CU10-BE-02 — Consultas y registro idempotente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-02` |
| **Nombre** | Consultas y registro idempotente |
| **Caso de uso** | CU10-BE |
| **Depende de** | UT-CU10-BE-01 |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (idempotencia, RNF-03) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Molde: `PlanillasConsultaRepository`.

## Solicitud de negocio

El personal necesita encontrar al becado por su CI y registrar su asistencia sin riesgo de duplicarla.

## Objetivo

Los repositorios buscan el becado por CI, leen la cabecera de una beca e insertan la asistencia solo si no existe.

## Alcance

- `AsistenciasConsultaRepository` (JDBC): `buscarPorCi(ci, fecha)` (une usuario, beca, postulante, carrera, convocatoria y la asistencia de esa fecha; prefiere la beca vigente, luego la más reciente; CI sin distinguir mayúsculas) y `cabeceraBeca(codigo)`.
- `AsistenciaRepository` (JPA): `findByBecaIdAndFecha` e `insertarSiNoExiste` (`INSERT … ON CONFLICT (beca_id, fecha) WHERE eliminado_en IS NULL DO NOTHING`, devuelve 1 o 0).

Archivos esperados (crear/tocar): los dos repositories.

## Fuera de alcance (no tocar)

Los repositories del módulo `becas`; lecturas para historial (CU-24) o cotejo (CU-11).

## Resultado esperado

Insertar dos veces la misma beca y fecha deja una fila y la segunda devuelve 0.

## Validación

1. `AsistenciaRepositoryTest` y `AsistenciasConsultaRepositoryTest` con base real (ver BE-04).
