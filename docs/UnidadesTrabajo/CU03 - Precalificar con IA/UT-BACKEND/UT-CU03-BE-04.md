# UT-CU03-BE-04 — Core: datos del cálculo y modelo vigente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-04` |
| **Nombre** | Core: datos del cálculo y modelo vigente |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | — |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

Para calcular, el core necesita las respuestas completas de una postulación `pendiente` (incluidos los datos del formulario oficial) y las ponderaciones del modelo vigente.

## Objetivo

`PrecalificacionConsultaRepository.datosPendiente()` y la lectura del modelo vigente con sus parámetros.

## Alcance

- `DatosPuntaje` y `datosPendiente()` / `datosCalificada()`: unen `postulacion`, `respuesta_socioeconomica`, `postulante` y `carrera`, exigen las seis respuestas completas y leen de `datos_extra` (jsonb) la dependencia, la ocupación, los hijos y la discapacidad (nulos si no están).
- `bloquearPostulacion()`, `sigueEnPendiente()`, `sigueCalificada()`, `tieneVigenteDelModelo()` y `retirarVigentes()`: lo que usa el servicio para ser idempotente y para pasar el puntaje anterior a historial.
- `pendientes(limite)`: las `pendiente` con respuestas, las más antiguas primero (lo usa el reintento; desde la UT-09 excluye las marcadas para revisión).
- `ModeloPreseleccionConsultaRepository.vigente()` y `parametros()` (ya existen por el CU26): se usan, no se modifican.
- `PrecalificacionConsultaRepositoryTest` contra Postgres real, con los casos de `datos_extra` presente, ausente y con nulos.

Archivos esperados (crear/tocar): `PrecalificacionConsultaRepository.java`, `PrecalificacionConsultaRepositoryTest.java`.

## Fuera de alcance (no tocar)

- Cambiar el estado de la postulación (UT-CU03-BE-05).
- Escribir `respuesta_socioeconomica` (es del CU02).
- Crear tablas nuevas: `puntaje_postulacion` ya existe por una migración anterior.

## Resultado esperado

Dada una postulación `pendiente` con respuestas completas, el repositorio entrega todas las variables, y las del formulario oficial en nulo cuando no existen.

## Validación

1. `./mvnw -o test -Dtest=PrecalificacionConsultaRepositoryTest -Dtest.postgres.url=jdbc:postgresql://localhost:55432/<base_vacia>` en verde (6 casos; necesita Postgres, ver `TESTING_BACKEND.md`).
