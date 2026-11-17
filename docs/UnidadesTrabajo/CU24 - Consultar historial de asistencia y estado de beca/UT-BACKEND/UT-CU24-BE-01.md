# UT-CU24-BE-01 — Consultas de la beca y del historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-BE-01` |
| **Nombre** | Consultas de la beca y del historial |
| **Caso de uso** | CU24-BE |
| **Depende de** | — (las tablas `beca`, `movimiento_beca` y `asistencia` ya existen) |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (borrado lógico, `codigo_<tabla>`) y `AsistenciasConsultaRepository` como molde.

## Solicitud de negocio

El becado debe ver su beca y sus asistencias sin que el módulo lea repositories de otros módulos.

## Objetivo

Existe `MiBecaConsultaRepository` con las lecturas que necesitan los dos endpoints.

## Alcance

- `B/features/mibeca/repository/MiBecaConsultaRepository`: `buscarBeca(usuarioCodigo)` (une usuario, beca, convocatoria, gestión, adjudicación, último movimiento de baja o suspensión y el total de asistencias; prefiere la vigente y luego la más reciente), `contarAsistencias(becaId, desde, hasta)` y `listarAsistencias(becaId, desde, hasta, descendente, limite, desplazamiento)` (orden por `fecha`, fragmento fijo `ASC`/`DESC`).
- `B/features/mibeca/dto/MiBecaResponse` y `AsistenciaBecadoResponse` (sin ids internos).

Archivos esperados (crear/tocar): el repository y los dos DTO.

## Fuera de alcance (no tocar)

Migraciones, Models, `AsistenciasConsultaRepository`, `PadronConsultaRepository`, escrituras de cualquier tipo.

## Resultado esperado

Dado el código de un usuario, el repository devuelve su beca y páginas de sus asistencias, respetando el borrado lógico.

## Validación

1. `./mvnw -o compile` sin errores y `checkstyle:check` limpio.
2. `MiBecaIntegrationTest` (ver BE-03) contra una base real: ejercita estas consultas de punta a punta.
