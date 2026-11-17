# UT-CU13-BE-10 — Abrir y cerrar una convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-10` |
| **Nombre** | Abrir y cerrar una convocatoria |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-08` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (índice único parcial `uq_convocatoria_abierta_por_gestion`)

## Solicitud de negocio

Solo se reciben postulaciones mientras la convocatoria está abierta, y solo puede haber una abierta por gestión. El Administrador abre y cierra la convocatoria (A3 y A4; CA05 a CA07).

## Objetivo

`ConvocatoriaEstadoService` pasa una convocatoria de `BORRADOR` a `ABIERTA` y de `ABIERTA` a `CERRADA`, con las protecciones de estado y la bitácora.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `APERTURA_CONVOCATORIA` y `CIERRE_CONVOCATORIA`.
- `B/features/convocatorias/service/ConvocatoriaEstadoService.java` (`@Service`; `@PreAuthorize` en los métodos públicos de Administrador):
  - `abrir(UUID actorCodigo, UUID codigo)`: `@Transactional`, `@Auditar(accion = APERTURA_CONVOCATORIA, entidad = "convocatoria")`. `NotFoundException` `ERR_CV_01` si no existe; si no está en `BORRADOR`, `ConflictException` `ERR_CV_08`; si ya hay **otra** `ABIERTA` en su gestión (`existsByGestionIdAndEstadoAndIdNot`), `ConflictException` `ERR_CV_07`; se pasa a `ABIERTA` y se hace `saveAndFlush`, traduciendo `DataIntegrityViolationException` del índice único parcial al mismo `ERR_CV_07` (dos aperturas simultáneas). `detalle = {estadoAnterior: "BORRADOR", estadoNuevo: "ABIERTA"}`.
  - `cerrar(UUID actorCodigo, UUID codigo)`: `@Auditar(accion = CIERRE_CONVOCATORIA, ...)`. Si no está `ABIERTA`, `ConflictException` `ERR_CV_08`; pasa a `CERRADA`. `detalle = {estadoAnterior: "ABIERTA", estadoNuevo: "CERRADA"}`.
  - Ambos devuelven `ResultadoAuditado<ConvocatoriaResponse>`.
- `INFO` en cada cambio de estado (`module=convocatorias`, `convocatoriaId`, `estado`).
- Cerrar no toca postulaciones ni borradores: quedan bloqueados porque el envío exige convocatoria `ABIERTA` (CU02 y CU17).
- Tests unitarios: abrir un `BORRADOR` sin otra abierta; abrir con otra abierta en la misma gestión → `ERR_CV_07` (y en otra gestión sí se puede); abrir una `ABIERTA`, `CERRADA` o `PUBLICADA` → `ERR_CV_08`; la violación del índice único se traduce a `ERR_CV_07`; cerrar una `ABIERTA`; cerrar una `BORRADOR`, `CERRADA` o `PUBLICADA` → `ERR_CV_08`; inexistente → `ERR_CV_01`.

Archivos esperados (crear/tocar): `ConvocatoriaEstadoService.java`, `BitacoraAcciones.java` y su test.

## Fuera de alcance (no tocar)

- El cierre automático al llegar la fecha (`UT-CU13-BE-12`).
- El endpoint (`UT-CU13-BE-13`).
- Reabrir una convocatoria cerrada.
- Validar fechas o cupos para poder abrir: la DUBSS aún no define cupos ni puntaje mínimo.
- Validar el envío de postulaciones (CU02 y CU17).

## Resultado esperado

El Administrador abre y cierra convocatorias respetando una sola abierta por gestión, y queda anotado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
