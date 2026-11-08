# UT-CU19-BE-03 — Códigos de error, bitácora, DTOs y evento de aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-03` |
| **Nombre** | Códigos de error, bitácora, DTOs y evento de aviso |
| **Caso de uso** | CU19-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- Como referencia: `ExpedienteErrorCodes`, los DTOs de `expedientes/dto` y `EventoNotificacion` (eventos `AUDITORIA_*` del CU-07).

## Solicitud de negocio

La devolución necesita sus errores catalogados, su acción en la bitácora, su contrato y su aviso al postulante.

## Objetivo

Quedan listos los códigos de error, la acción de bitácora, los DTOs y el evento de aviso de la devolución.

## Alcance

- `B/features/postulaciones/exception/DevolucionErrorCodes.java` (prefijo `DEV`): `ERR_DEV_01` (`409`, la postulación ya no está «calificada») y `ERR_DEV_02` (`409`, el finalista no tiene una auditoría posterior a su última devolución). El finalista inexistente reutiliza `ERR_FIN_01`; el motivo vacío o de más de 500 caracteres cae en `ERR_SYS_01`.
- **Documentar los dos códigos** en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` (con HTTP, excepción, significado y dónde se lanza) y agregar la sección «Devolución (`DEV`)» con sus links en `docs/errors/README.md`, como exige `GUIA_ERRORES_BACKEND.md`.
- `BitacoraAcciones.DEVOLUCION_POSTULACION` (comentario: el actor es el administrador; el detalle lleva códigos y estados, nunca el motivo, nombres ni CI).
- `B/features/postulaciones/dto/DevolverPostulacionRequest.java` (`record`): `motivo` con `@NotBlank` («El motivo de la devolución es obligatorio») y `@Size(max = 500)` («El motivo no puede pasar de 500 caracteres»).
- `B/features/postulaciones/dto/DevolucionResponse.java` (`record`): `postulacion` (código público de la postulación), `estado` (`devuelta`), `motivo`, `fecha` y `devueltaPor` (nombre del administrador).
- `EventoNotificacion.POSTULACION_DEVUELTA`: tipo `WARNING`, título «Tu postulación fue devuelta - Beca Comedor», mensaje «Hola {nombres}, la DUBSS devolvió tu postulación para que la corrijas. Observaciones: {observaciones}», **bandeja, push y correo** (`enviaCorreo = true`).
- Tests: validación del request (vacío, solo espacios, 501 caracteres, 500 exactos válidos) y que la plantilla del evento se arma con sus dos datos y no queda incompleta.

Archivos esperados (crear): `DevolucionErrorCodes`, `DevolverPostulacionRequest`, `DevolucionResponse` y sus tests. (Tocar): `BitacoraAcciones`, `EventoNotificacion`, `docs/errors/README.md` y `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`.

## Fuera de alcance (no tocar)

- La lógica que lanza los errores (UT-05).
- Códigos de idempotencia: la devolución **no** usa `Idempotency-Key`.
- Otros eventos de aviso.
- Cambiar las plantillas del CU-07 o CU-21.

## Resultado esperado

El módulo tiene su catálogo de errores documentado, su acción de bitácora, su contrato y su aviso.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
