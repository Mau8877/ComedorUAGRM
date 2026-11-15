# UT-CU18-BE-03 — `ValidacionPostulacionService` y sus pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-03` |
| **Nombre** | `ValidacionPostulacionService` y sus pruebas |
| **Caso de uso** | CU18-BE |
| **Depende de** | `UT-CU18-BE-01`, `UT-CU18-BE-02` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)
- El CU18 completo (flujos A1–A8 y CA01–CA12)

## Solicitud de negocio

Antes de aceptar una postulación hay que rechazar duplicados y no elegibles con un motivo claro, y reconocer el reenvío de una devuelta.

## Objetivo

`ValidacionPostulacionService.validar(usuarioCodigo, convocatoriaCodigo)` devuelve «válida» o lanza el motivo del rechazo.

## Alcance

- `B/features/postulaciones/service/ResultadoValidacion.java`: `record(postulanteId, convocatoriaId, postulacionAReenviar)`.
- `B/features/postulaciones/service/ValidacionPostulacionService.java`: `@Service`, `@PreAuthorize("hasRole('POSTULANTE')")`, `@Transactional(readOnly = true)`. Orden de las comprobaciones:
  1. convocatoria existente (`404` `ERR_CV_01`);
  2. la cuenta es de un postulante (`403` `ERR_VAL_02`);
  3. condiciones de elegibilidad: cuenta activa (`403` `ERR_VAL_02`);
  4. convocatoria `ABIERTA` y dentro de fechas, con el `Clock` del proyecto (`409` `ERR_VAL_03`);
  5. duplicados (`409` `ERR_VAL_01`, con el estado visible): `borrador` se ignora; `devuelta` de la misma convocatoria es el reenvío; cualquier otra postulación de la gestión es duplicada.
  Log `WARN` en cada rechazo, sin datos personales.
- `T/features/postulaciones/service/ValidacionPostulacionServiceTest.java` (Mockito, reloj fijo): un test por CA aplicable (válida, duplicada en la misma y en otra convocatoria de la gestión, decisión final sin revelar, borrador, devuelta, cuenta desactivada o sin postulante, convocatoria cerrada o fuera de fechas, inexistente, repetición).

Archivos esperados (crear/tocar): `ResultadoValidacion.java`, `ValidacionPostulacionService.java` y su test.

## Fuera de alcance (no tocar)

- Llamar a esta validación desde el CU02 y crear o reenviar postulaciones (B8).
- Un controller o ruta HTTP.
- Bitácora, notificaciones y condiciones de elegibilidad de la DUBSS (por definir).

## Resultado esperado

El CU02 podrá incluir la validación con una sola llamada.

## Validación

1. `./mvnw test -Dtest=ValidacionPostulacionServiceTest` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Ninguna prueba de la validación crea ni modifica filas.
