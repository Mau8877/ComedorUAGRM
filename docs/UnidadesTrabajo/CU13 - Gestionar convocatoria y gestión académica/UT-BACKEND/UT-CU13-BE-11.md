# UT-CU13-BE-11 — Eliminar una convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-11` |
| **Nombre** | Eliminar una convocatoria |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-08` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#columnas-comunes-a-todas-las-tablas) (borrado lógico y sin `ON DELETE CASCADE`)
- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)

## Solicitud de negocio

Una convocatoria creada por error y todavía sin uso debe poder quitarse, pero una que ya recibió postulaciones nunca se borra: solo se cierra (A7).

## Objetivo

`ConvocatoriaService.eliminar(...)` elimina lógicamente una convocatoria en `BORRADOR` sin postulaciones, junto con su lista de documentos, y lo anota en la bitácora.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `ELIMINACION_CONVOCATORIA`.
- `ConvocatoriaService.eliminar(UUID actorCodigo, UUID codigo)`: `@Transactional`, `@PreAuthorize`, `@Auditar(accion = ELIMINACION_CONVOCATORIA, entidad = "convocatoria")`. Busca la convocatoria (`ERR_CV_01`); si tiene postulaciones, `ConflictException` `ERR_CV_10` (se comprueba **primero**: es el mensaje que A7 exige); si no está en `BORRADOR`, `ConflictException` `ERR_CV_08`. Marca `eliminadoEn` en la convocatoria y en cada uno de sus `ConvocatoriaDocumento` en la **misma transacción** (borrado lógico en cascada hecho por el `service`, sin `ON DELETE CASCADE`).
- Devuelve `ResultadoAuditado<Void>` con `detalle = {gestion: <codigo>}`. El controller responde `data: null`.
- `INFO` al eliminar (`module=convocatorias`, `convocatoriaId`).
- Tests unitarios: elimina una `BORRADOR` sin postulaciones y marca también sus documentos; con postulaciones → `ERR_CV_10` aunque esté en `BORRADOR`; `ABIERTA`, `CERRADA` o `PUBLICADA` sin postulaciones → `ERR_CV_08`; inexistente → `ERR_CV_01`; una convocatoria eliminada ya no aparece en `listar` ni en `obtener`.

Archivos esperados (crear/tocar): `ConvocatoriaService.java`, `BitacoraAcciones.java` y su test.

## Fuera de alcance (no tocar)

- El endpoint (`UT-CU13-BE-13`).
- Eliminar gestiones académicas o tipos de documento.
- Restaurar una convocatoria eliminada.

## Resultado esperado

El Administrador quita una convocatoria en borrador sin uso; las que ya tienen postulaciones se cierran, no se eliminan.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
