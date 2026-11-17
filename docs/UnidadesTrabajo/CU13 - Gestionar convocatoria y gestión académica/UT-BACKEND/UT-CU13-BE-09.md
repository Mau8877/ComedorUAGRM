# UT-CU13-BE-09 — Editar una convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-09` |
| **Nombre** | Editar una convocatoria |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-08` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#put-vs-patch) (por qué `PUT`)
- `B/features/usuarios/service/UsuarioPersonalService.java` (método `editar`: solo anota lo que de verdad cambia)

## Solicitud de negocio

Las fechas, los cupos o la lista de documentos de una convocatoria cambian antes y durante su vigencia, pero no pueden alterar lo que ya postularon los estudiantes (flujo principal, paso 4; A6).

## Objetivo

`ConvocatoriaService.editar(...)` reemplaza los datos de una convocatoria en `BORRADOR` o `ABIERTA`, con las restricciones por estado y por postulaciones, y anota la edición.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `EDICION_CONVOCATORIA`.
- `ConvocatoriaService.editar(UUID actorCodigo, UUID codigo, ConvocatoriaRequest)`: `@Transactional`, `@PreAuthorize`, `@Auditar(accion = EDICION_CONVOCATORIA, entidad = "convocatoria")`. Es un `PUT`: se reemplazan **todos** los campos; los opcionales omitidos quedan en `NULL`. Orden:
  1. Buscar la convocatoria (`NotFoundException` `ERR_CV_01`).
  2. Solo se edita en `BORRADOR` o `ABIERTA`; `CERRADA` o `PUBLICADA` → `ConflictException` `ERR_CV_08`.
  3. Mismas validaciones de fechas, gestión y documentos que al crear (`ERR_CV_04`, `ERR_CV_02`, `ERR_CV_03`, `ERR_CV_05`, `ERR_CV_06`).
  4. La gestión solo se cambia en `BORRADOR` (en `ABIERTA` sería mover una convocatoria abierta de gestión): si cambia en otro estado, `ERR_CV_08`.
  5. Si la convocatoria **tiene postulaciones** (`PostulacionesDeConvocatoriaService.existenPara`) y cambia la fecha de apertura o el conjunto de documentos (tipos u obligatoriedad), `ConflictException` `ERR_CV_09`. Los demás campos (nombre, fecha de cierre, cupos, puntaje mínimo, cantidad de finalistas) sí se pueden cambiar.
  6. Aplicar los campos, reemplazar los documentos con `DocumentosConvocatoriaService`: los tipos que dejan de estar se marcan con `eliminadoEn`; los que se agregan o se vuelven a agregar se guardan o reactivan.
- Devuelve `ResultadoAuditado<ConvocatoriaResponse>` con `detalle = {camposModificados: [...]}` (solo nombres de campo, `documentos` si cambió la lista). Si no cambia nada, `auditar = false` y devuelve igual la convocatoria.
- `INFO` al editar con cambios (`module=convocatorias`, `convocatoriaId`).
- Tests unitarios: cambia solo lo que difiere; vaciar `cupos` con `null` funciona; `CERRADA` y `PUBLICADA` → `ERR_CV_08`; cambiar gestión en `ABIERTA` → `ERR_CV_08`; con postulaciones, cambiar la fecha de apertura o los documentos → `ERR_CV_09` pero cambiar el nombre o el cierre sí; quitar y volver a agregar un documento lo reactiva; sin cambios → `auditar = false`; el `detalle` no lleva valores.

Archivos esperados (crear/tocar): `ConvocatoriaService.java`, `BitacoraAcciones.java` y su test.

## Fuera de alcance (no tocar)

- El estado (se cambia solo con abrir y cerrar, UT 10).
- El endpoint (`UT-CU13-BE-13`).
- Decidir con la DUBSS el alcance exacto de la edición con postulaciones: hoy solo se bloquean apertura y documentos.
- Revalidar las postulaciones ya enviadas por un cambio de cupos o de puntaje mínimo.

## Resultado esperado

El Administrador corrige una convocatoria sin dañar las postulaciones ya recibidas, y queda anotado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
