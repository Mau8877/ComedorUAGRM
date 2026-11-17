# UT-CU03-BE-10 — Core: consultar la precalificación de una postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-10` |
| **Nombre** | Core: consultar la precalificación de una postulación |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-05, UT-CU03-BE-09 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El mockup de grado (img 136) muestra al Administrador cuánto aportó cada variable al puntaje y la traza del proceso; el puntaje se guarda con su desglose, pero ningún endpoint lo devolvía.

## Objetivo

`GET /api/v1/postulaciones/{codigo}/precalificacion`, de solo lectura, para el Administrador DUBSS.

## Alcance

- `PrecalificacionController` (`features/preseleccion/controller/`) y `PrecalificacionDetalleService.obtener()` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`, `@Transactional(readOnly = true)`.
- DTO `PrecalificacionResponse` (con `AporteVariableResponse` y `PasoTrazaResponse`): postulante (nombres, apellidos, CI), convocatoria, estado, puntaje y su máximo, modelo, fecha de cálculo, posición en la lista priorizada, aporte de cada variable, advertencias, marca de revisión (`marcadaParaRevision`, `revisionDesde`, `intentos`) y traza. Sin respuestas socioeconómicas.
- `PrecalificacionConsultaRepository`: `cabecera()`, `posicion()` (mismo orden que el CU06) e `historial()`.
- El máximo de cada variable es su ponderación sobre la suma de las que tienen dato, a escala 0-70; un `detalle` ilegible da un desglose vacío, no un error.
- Una postulación sin calificar responde igual, con el puntaje, el modelo, la posición y las variables vacíos.
- `ERR_MOD_08` (`404`, postulación inexistente), documentado en `docs/errors/preseleccion/`.
- Tests: `PrecalificacionDetalleServiceTest`, `PrecalificacionControllerTest` y un caso en `PrecalificacionConsultaRepositoryTest` (Postgres real).

Archivos esperados (crear/tocar): `PrecalificacionController.java`, `PrecalificacionDetalleService.java`, los tres DTO, `PrecalificacionConsultaRepository.java`, `ModeloPreseleccionErrorCodes.java`, `docs/errors/` y los tests.

## Fuera de alcance (no tocar)

- Guardar o mostrar la duración del cálculo.
- Cambiar cómo se calcula o se guarda el puntaje.
- Un endpoint para desmarcar una postulación marcada para revisión.

## Resultado esperado

El Administrador obtiene el puntaje de una postulación con su desglose y su traza; cualquier otro rol recibe `403` y una postulación inexistente `404`.

## Validación

1. `./mvnw test -Dtest='PrecalificacionDetalleServiceTest,PrecalificacionControllerTest,PrecalificacionConsultaRepositoryTest'` en verde (Postgres real).
2. `./mvnw checkstyle:check` limpio.
