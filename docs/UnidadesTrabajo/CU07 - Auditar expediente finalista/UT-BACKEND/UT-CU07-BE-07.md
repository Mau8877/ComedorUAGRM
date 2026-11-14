# UT-CU07-BE-07 — Registrar la auditoría y avisar al postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-07` |
| **Nombre** | Registrar la auditoría y avisar al postulante |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-03`, `UT-CU07-BE-04` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md).
- Como referencia: `FinalistaService.registrarEntrega` (CU-21).

## Solicitud de negocio

La DUBSS deja constancia del resultado del cotejo y el postulante se entera.

## Objetivo

`ExpedienteService.registrarAuditoria` valida, guarda la auditoría, la anota en la bitácora y avisa al postulante.

## Alcance

- `ExpedienteService.registrarAuditoria(actorCodigo, finalistaCodigo, RegistrarAuditoriaRequest)` con `@PreAuthorize`, `@Transactional` y `@Auditar(accion = AUDITORIA_EXPEDIENTE, entidad = "auditoria_expediente")`.
- Orden de las validaciones: finalista inexistente → `404` `ERR_FIN_01`; observaciones obligatorias si es «Observado» o «No válido» → `400` `ERR_EXP_03`; puntaje académico obligatorio si es «Validado» y entre 0 y 30 (hasta 2 decimales) → `400` `ERR_EXP_04`; la postulación no está «calificada» → `409` `ERR_EXP_02`; la entrega física no está registrada → `409` `ERR_EXP_01`.
- **Idempotencia (RNF-03):** el header opcional `Idempotency-Key` (UUID, generado por el cliente por intento) identifica la petición. Si ya hay una auditoría de ese finalista con esa clave y el mismo contenido, no crea otra: devuelve esa con `creada = false` y no anota la bitácora (`auditar() = false`) ni vuelve a avisar; se resuelve antes de validar el estado de la postulación. Con otro contenido responde `409` `ERR_EXP_06`; con una clave que no es UUID, `400` `ERR_EXP_07`. Sin clave siempre crea. Para que dos peticiones simultáneas no se pisen, el servicio bloquea la fila del finalista (`SELECT ... FOR UPDATE`) antes de buscar la clave. Requiere la columna `clave_idempotencia` (migración `V20261107140000`).
- Guarda la auditoría con el auditor del token (el controller pasa el `@CurrentUserId` como `actorCodigo`), nunca de un campo del cuerpo. No cambia el estado de la postulación ni su puntaje.
- Aviso: `NotificacionService.notificar` con `AUDITORIA_VALIDADA`, `AUDITORIA_OBSERVADA` o `AUDITORIA_NO_VALIDA` según el resultado, con la postulación como contexto. Un fallo del aviso o de su despacho **no** deshace la auditoría (solo `WARN`).
- Bitácora: código de la auditoría y del finalista, resultado y puntaje académico; **sin** observaciones, nombres ni CI.
- `LOG.info` con módulo, actor y resultado (sin datos personales).
- Tests unitarios: cada validación y su código, idempotencia dentro y fuera de la ventana, el evento elegido por resultado, aviso fallido sin deshacer, y que no toca el estado de la postulación.

Archivos esperados (tocar): `ExpedienteService` y su test.

## Fuera de alcance (no tocar)

- Devolver, archivar o rechazar la postulación (CU-19, CU-20).
- Pedir o guardar un puntaje distinto del académico.
- Endpoint (UT-10).
- Cualquier aviso por correo.

## Resultado esperado

La auditoría queda guardada, anotada y avisada; un envío repetido no crea otra.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
