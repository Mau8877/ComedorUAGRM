# UT-CU03-BE-09 — Core: marcar para revisión lo que la IA no puede calificar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-09` |
| **Nombre** | Core: marcar para revisión lo que la IA no puede calificar |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-05, UT-CU03-BE-06 |

## Antes de implementar

[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones y `DiagramaConceptual.mermaid`) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El diagrama de grado deja «marcada para revisión» a la postulación que la IA no puede calcular (A1). Sin esto el job reintentaba para siempre las mismas y, con más de 500, taparían a las demás.

## Objetivo

Contar los rechazos de la IA por postulación, marcarla al tercero y que el reintento automático deje de tomarla.

## Alcance

- Migración `V20261122100000__agregar_revision_precalificacion_a_postulacion.sql`: `intentos_precalificacion smallint NOT NULL DEFAULT 0` y `precalificacion_revision_en timestamptz NULL` en `postulacion`; `DiagramaConceptual.mermaid` actualizado.
- `Postulacion`: los dos campos; `enviar()` los reinicia (reenviar tras una devolución empieza de cero).
- `PrecalificacionConsultaRepository.registrarFallo(id, maximo)` (incrementa y marca al llegar al máximo; solo sobre una `pendiente`) y `pendientes()` excluye las marcadas.
- `PrecalificacionService`: `NO_CALCULADA` (la IA rechaza los datos o su versión de modelo no está registrada) cuenta un intento; `INTENTOS_MAXIMOS = 3`; la IA caída **no** cuenta; `WARN` al marcar.
- Tests: `PrecalificacionServiceTest`, `PrecalificacionConsultaRepositoryTest` (Postgres real), `PostulacionEnviarTest`.

Archivos esperados (crear/tocar): la migración, `DiagramaConceptual.mermaid`, `Postulacion.java`, `PrecalificacionConsultaRepository.java`, `PrecalificacionService.java` y los tres tests.

## Fuera de alcance (no tocar)

- Un valor nuevo del enum `estado_postulacion`: la postulación sigue `pendiente`.
- Una pantalla o aviso para ver las marcadas, y desmarcarlas sin reenviar.
- Contar los fallos del reprocesamiento (CU26): `recalcular()` no cambia.

## Resultado esperado

Una postulación que la IA rechaza tres veces deja de reintentarse y no tapa a las demás; reenviarla la reinicia.

## Validación

1. `./mvnw test -Dtest='PrecalificacionServiceTest,PrecalificacionConsultaRepositoryTest,PostulacionEnviarTest'` en verde (Postgres real).
2. En el repositorio: dos fallos siguen en `pendientes()`; el tercero la saca y sigue `pendiente`.
3. `./mvnw checkstyle:check` limpio.
