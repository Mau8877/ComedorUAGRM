# Errores — Expedientes (`EXP`)

Errores de la auditoría del expediente de un finalista (CU07). Un finalista inexistente reutiliza [`ERR_FIN_01`](../finalistas/ERRORES_FINALISTAS.md#err_fin_01); los campos vacíos o con más de 500 caracteres en las observaciones caen en [`ERR_SYS_01`](../system/ERRORES_SISTEMA.md#err_sys_01).

## `ERR_EXP_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La entrega de la documentación física del finalista todavía no está registrada; la auditoría se hace contra los documentos físicos, así que primero hay que registrar la entrega (CU21).
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`

## `ERR_EXP_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La postulación del finalista ya no está «calificada» (otro administrador la devolvió, archivó o adjudicó mientras tanto).
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`

## `ERR_EXP_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El resultado es «Observado» o «No válido» y las observaciones están vacías.
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`

## `ERR_EXP_04`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El resultado es «Validado» y falta el puntaje académico, o el puntaje académico no está entre 0 y 30 (o tiene más de dos decimales).
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`

## `ERR_EXP_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El documento digital pedido no existe, está eliminado o no pertenece a la postulación del finalista.
- **Dónde se lanza:** `ExpedienteService.accesoDocumento()`

## `ERR_EXP_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La clave de idempotencia (`Idempotency-Key`) ya se usó para una auditoría de este finalista con otro resultado, observaciones o puntaje académico. Es un error del cliente: una clave nueva por cada auditoría distinta.
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`

## `ERR_EXP_07`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El header `Idempotency-Key` viene con un valor que no es un UUID.
- **Dónde se lanza:** `ExpedienteService.registrarAuditoria()`
