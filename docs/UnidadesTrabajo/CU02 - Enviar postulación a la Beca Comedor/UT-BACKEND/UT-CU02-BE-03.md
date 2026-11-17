# UT-CU02-BE-03 — Precalificación: cliente de la IA, `PrecalificacionService` y reintento automático

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-03` |
| **Nombre** | Precalificación: cliente de la IA, `PrecalificacionService` y reintento automático |
| **Caso de uso** | CU02-BE |
| **Depende de** | UT-CU02-BE-01 |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (integraciones externas: el `service` las usa, nunca el `controller`) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

Cada postulación enviada tiene que recibir su puntaje de vulnerabilidad sin que el postulante espere y sin perderse si la IA no responde.

## Objetivo

Tras confirmar el envío se pide el puntaje a la IA; si responde, la postulación pasa a `calificada` con un puntaje vigente; si no, queda `pendiente` y se reintenta sola.

## Alcance

- `preseleccion/service/PrecalificacionClient.java` (interfaz) e `IaHttpPrecalificacionClient.java`: `POST {app.ia.base-url}/api/v1/puntaje`, 2 s de conexión y 5 s de lectura.
- `preseleccion/dto/SolicitudPuntaje`, `PuntajeCalculado` y las excepciones internas `IaNoDisponibleException` / `IaDatosInvalidosException`.
- `preseleccion/service/PrecalificacionService.java`: `solicitar` (asíncrono, nunca lanza) y `precalificar` (la llamada HTTP va **fuera** de la transacción; solo el registro va en una transacción corta: retira el puntaje vigente, inserta el nuevo `vigente` y califica).
- `postulaciones/service/CalificacionPostulacionService.java`: único punto por el que otro módulo pasa una postulación a `calificada`, con su fila de historial.
- `preseleccion/service/PrecalificacionReintentoJob.java` (cada 5 min) y `config/PreseleccionConfig.java` (ejecutor con cola).
- `PuntajePostulacion` (constructor), `ModeloPreseleccionRepository.findByVersion`, `PrecalificacionConsultaRepository` y las propiedades `app.ia.*` (con `IA_BASE_URL` en `docker-compose.yml`).
- `PrecalificacionServiceTest`.

Archivos esperados (crear/tocar): los listados arriba, `application.properties` y `docker-compose.yml`.

## Fuera de alcance (no tocar)

- El módulo de IA (`comedor-uagrm-ia`): su API y su modelo son las tareas B2 y B3. El contrato que esta UT consume está en `docs/Implementacion/CU02`.
- Parámetros del modelo y reprocesamiento (CU26, B4).
- Marcar «para revisión» una postulación que la IA rechaza (CU03 A1): hoy solo se loguea y se reintenta.

## Resultado esperado

Con una IA que cumpla el contrato, las postulaciones pendientes pasan a `calificada` solas; sin IA, siguen `pendiente`.

## Validación

1. `./mvnw test -Dtest=PrecalificacionServiceTest` en verde.
2. Con la IA caída: enviar una postulación la deja `pendiente` y el log muestra el aviso de la IA no disponible, sin error para el postulante.
3. Con una IA simulada que devuelva `{puntaje, modelo}` de un modelo registrado: el job la pasa a `calificada` con un solo puntaje vigente.
