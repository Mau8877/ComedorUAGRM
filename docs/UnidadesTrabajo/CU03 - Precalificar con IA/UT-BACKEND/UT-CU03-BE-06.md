# UT-CU03-BE-06 — Core: ejecución en segundo plano y reintento automático

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-06` |
| **Nombre** | Core: ejecución en segundo plano y reintento automático |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-05 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

El Postulante no debe esperar la precalificación ni perder su envío si la IA falla, y todas las postulaciones, aun en picos de envíos, deben terminar calificadas.

## Objetivo

`PrecalificacionService.solicitar()` asíncrono, el ejecutor con cola y `PrecalificacionReintentoJob`.

## Alcance

- `solicitar()` (`@Async` en `ejecutorPrecalificacion`): llama a `precalificar()` y nunca lanza; el CU02 lo invoca tras confirmar el envío (`afterCommit`).
- `PreseleccionConfig.ejecutorPrecalificacion()`: 2 a 4 hilos y cola de 5000; si se llena, la solicitud se descarta y el reintento recoge la postulación.
- `PrecalificacionReintentoJob`: cada 5 minutos (`app.ia.reintento-cron`, hora de La Paz), un lote de 500 `pendiente` de las más antiguas a las más recientes; corta la pasada en la primera `IA_NO_DISPONIBLE`.
- `PrecalificacionReintentoJobTest`: cuenta las calificadas, corta ante la IA caída, sigue ante `NO_CALCULADA` o una excepción, y sin pendientes no llama a la IA.

Archivos esperados (crear/tocar): `PreseleccionConfig.java`, `PrecalificacionReintentoJob.java`, `PrecalificacionReintentoJobTest.java`.

## Fuera de alcance (no tocar)

- Cambiar cómo el CU02 pide la precalificación (UT del CU02).
- Una cola externa (Redis, RabbitMQ): no hay más de una instancia del core.
- Dejar de reintentar lo que la IA rechaza (UT-CU03-BE-09).

## Resultado esperado

Si la IA está caída, el envío del Postulante se confirma igual y la postulación se califica sola, de las más antiguas a las más recientes, cuando la IA vuelve.

## Validación

1. `./mvnw -o test -Dtest=PrecalificacionReintentoJobTest` en verde (5 casos).
2. Con la IA apagada, enviar una postulación la deja `pendiente`; al levantar la IA y esperar la siguiente pasada del job, pasa a `calificada` (prueba manual).
