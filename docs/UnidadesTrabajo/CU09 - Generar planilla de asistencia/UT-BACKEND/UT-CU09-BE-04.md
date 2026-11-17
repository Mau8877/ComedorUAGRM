# UT-CU09-BE-04 — Generar planilla, vista previa y resumen

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-04` |
| **Nombre** | Generar planilla, vista previa y resumen |
| **Caso de uso** | CU09-BE |
| **Depende de** | `UT-CU09-BE-01`, `UT-CU09-BE-02`, `UT-CU09-BE-03` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) (`@Auditar` con `ResultadoAuditado`), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (cada código nuevo se documenta en `docs/errors/` en el mismo cambio), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) (`@PreAuthorize` en el `service`).
- Como referencia: `B/features/actas/service/ActaAdjudicacionService.java`.

## Solicitud de negocio

Generar la planilla del periodo con el padrón vigente, guardarla y dejar constancia, sin que una falla deje una planilla a medias.

## Objetivo

`PlanillaAsistenciaService` genera la planilla, ofrece su vista previa y el resumen que necesita la pantalla.

## Alcance

- `B/features/planillas/exception/PlanillasErrorCodes.java`: `ERR_PLA_01` (final anterior a inicial, 400), `ERR_PLA_02` (fuera de la gestión, 400), `ERR_PLA_03` (más de 31 días, 400), `ERR_PLA_04` (sin becas vigentes, 409), `ERR_PLA_05` y `ERR_PLA_06` (se usan en BE-05). Documentados en `docs/errors/planillas/ERRORES_PLANILLAS.md` y enlazados desde `docs/errors/README.md`.
- `BitacoraAcciones.GENERACION_PLANILLA` y `DESCARGA_PLANILLA`.
- `B/features/planillas/service/PlanillaAsistenciaService.java`, solo `ADMINISTRADOR_DUBSS`:
  - `generar(actorCodigo, convocatoriaCodigo, desde, hasta)` con `@Transactional` y `@Auditar(GENERACION_PLANILLA)`: valida el periodo (`ERR_PLA_01`, `02`, `03` en ese orden), exige becados vigentes (`ERR_PLA_04`), genera el PDF, lo sube a `planillas/{convocatoria}/planilla-{desde}-{hasta}-{aleatorio}.pdf` y recién entonces guarda la fila; devuelve `ResultadoAuditado<PlanillaResponse>` con un detalle de códigos, periodo y cantidad (sin nombres, CI ni ruta).
  - `vistaPrevia(convocatoriaCodigo, desde, hasta)`: mismas validaciones, PDF en borrador, sin subir, guardar ni anotar.
  - `resumen(convocatoriaCodigo)` → `ResumenPlanillaResponse` (vigentes y límites de la gestión).
  - Convocatoria inexistente: `NotFoundException` con `ERR_PRI_01`.
- DTO `ResumenPlanillaResponse`.
- Tests con colaboradores simulados: camino feliz (fila, ruta, respuesta sin ruta, bitácora sin datos sensibles), orden de las operaciones, datos del PDF, periodo de un día y en los límites de la gestión, dos planillas del mismo periodo con archivos distintos, cada error de periodo y su precedencia, sin becas, convocatoria inexistente, falla de MinIO y del PDF sin registrar nada, vista previa sin huella, resumen, rol y transacción.

Archivos esperados (crear): `PlanillasErrorCodes.java`, `PlanillaAsistenciaService.java`, `ResumenPlanillaResponse.java`, `docs/errors/planillas/ERRORES_PLANILLAS.md`, `T/features/planillas/service/PlanillaAsistenciaServiceTest.java` y `T/features/planillas/exception/PlanillasErrorCodesTest.java`; tocar `BitacoraAcciones.java` y `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Listar y descargar (BE-05) y los endpoints (BE-06).
- Bloquear la convocatoria, claves de idempotencia o unicidad por periodo.
- Cambiar becas, avisar a alguien o registrar asistencia.

## Resultado esperado

Una llamada al `service` genera y registra la planilla, o no deja nada si algo falla.

## Validación

1. `./mvnw test -Dtest=PlanillaAsistenciaServiceTest,PlanillasErrorCodesTest` pasa (21 tests).
2. `./mvnw checkstyle:check` sin errores.
