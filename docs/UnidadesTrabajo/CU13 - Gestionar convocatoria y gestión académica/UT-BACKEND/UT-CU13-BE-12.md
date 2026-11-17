# UT-CU13-BE-12 — Cierre automático al llegar la fecha de cierre

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-12` |
| **Nombre** | Cierre automático al llegar la fecha de cierre |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-10` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) (`@Auditar` solo funciona entre beans, no dentro de la misma clase)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- `B/features/dashboard/service/RefrescoIndicadoresJob.java` y `B/config/SchedulingConfig.java` (patrón de job con `@Scheduled`)

## Solicitud de negocio

Una convocatoria se cierra sola cuando llega su fecha de cierre, sin que el Administrador tenga que acordarse (A5, CA08).

## Objetivo

Un job cierra las convocatorias `ABIERTA` cuya fecha de cierre ya pasó, y cada cierre queda en la bitácora como acción del sistema.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `CIERRE_AUTOMATICO_CONVOCATORIA` (con el comentario «el actor es nulo: lo hace el sistema»).
- `ConvocatoriaEstadoService.cerrarVencida(Long convocatoriaId)` (método nuevo): `@Transactional(propagation = REQUIRES_NEW)` y `@Auditar(accion = CIERRE_AUTOMATICO_CONVOCATORIA, entidad = "convocatoria")`. **Sin** `@PreAuthorize`: no lo llama ningún controller y nunca debe exponerse por HTTP. Vuelve a leer la convocatoria; si ya no está `ABIERTA` o su cierre todavía no pasó, no hace nada y devuelve `auditar = false` (otra instancia o el Administrador ya la cerró). Si corresponde, la pasa a `CERRADA`. Devuelve un `ResultadoAuditado<Void>` con `actorId = null`, `actorCodigo = null`, `entidadId` = la convocatoria y `detalle = {estadoAnterior: "ABIERTA", estadoNuevo: "CERRADA", motivo: "fecha_cierre"}`.
- Si `AuditoriaAspect` no admite `actorId` nulo, se ajusta **en esta UT** (con su test en `AuditoriaAspectTest`): `bitacora.usuario_id` ya es nullable.
- `B/features/convocatorias/service/CierreAutomaticoConvocatoriasJob.java` (`@Component`): `@Scheduled(cron = "${app.convocatorias.cierre-cron:0 */5 * * * *}", zone = "America/La_Paz")` y un `@EventListener(ApplicationReadyEvent.class)` (al arrancar recupera lo que venció con el backend apagado). Ambos llaman a `cerrarVencidas()`: busca con `findByEstadoAndFechaCierreLessThanEqual(ABIERTA, Instant.now())` y llama a `cerrarVencida(id)` **una por una**, atrapando y logueando (`ERROR`, sin datos personales) el fallo de una para que no detenga a las demás.
- `app.convocatorias.cierre-cron` se agrega a `application.properties` con su valor por defecto y a `.env.example` si el proyecto lista ahí las variables de cron (como `app.dashboard.refresco-cron`).
- `INFO` por cada convocatoria cerrada (`module=convocatorias`, `convocatoriaId`).
- Tests: cierra solo las `ABIERTA` vencidas y deja las vigentes, las `BORRADOR` y las `CERRADA`; el fallo de una no frena a las otras; `cerrarVencida` sobre una ya cerrada no anota nada; el evento de bitácora queda con actor nulo y acción `CIERRE_AUTOMATICO_CONVOCATORIA`.

Archivos esperados (crear/tocar): `CierreAutomaticoConvocatoriasJob.java`, `ConvocatoriaEstadoService.java`, `BitacoraAcciones.java`, `application.properties`, y `AuditoriaAspect` solo si hace falta, con sus tests.

## Fuera de alcance (no tocar)

- Apertura automática en la fecha de apertura: el CU no la pide.
- Avisar a los postulantes del cierre.
- Un botón o endpoint para «cerrar vencidas ahora».
- Aplicar el cierre sobre convocatorias `PUBLICADA`.

## Resultado esperado

Las convocatorias abiertas se cierran solas al pasar su fecha de cierre, y cada cierre queda en la bitácora.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con una convocatoria `ABIERTA` de cierre vencido, al arrancar el backend queda `CERRADA` y hay una fila de bitácora con `CIERRE_AUTOMATICO_CONVOCATORIA` y `usuario_id` nulo.
