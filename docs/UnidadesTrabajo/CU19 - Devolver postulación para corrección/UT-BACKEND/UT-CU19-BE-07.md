# UT-CU19-BE-07 — Endpoint para devolver la postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-07` |
| **Nombre** | Endpoint para devolver la postulación |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-05`, `UT-CU19-BE-06` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `B/features/expedientes/controller/ExpedienteController.java`.

## Solicitud de negocio

La pantalla del expediente consume este endpoint.

## Objetivo

Existe el endpoint con un controller delgado.

## Alcance

- `B/features/postulaciones/controller/DevolucionController.java` con `@RequestMapping("/api/v1/finalistas/{codigo}")` (cuelga del finalista porque la devolución se hace desde su expediente) y `POST /devolver` (acción en infinitivo, ver `ENDPOINTS_BACKEND.md`): cuerpo `{ motivo }` validado con `@Valid`; responde `200` con `ApiResponse<DevolucionResponse>` y el mensaje «Postulación devuelta correctamente».
- Exige `ADMINISTRADOR_DUBSS` (el `403` lo escribe `ApiAccessDeniedHandler`), usa `@CurrentUserId` como actor, no tiene lógica ni logging y **no** recibe el header `Idempotency-Key`.
- Tests de contrato con `MockMvc`: forma del sobre y status `200`, `400` `ERR_SYS_01` por motivo vacío o de más de 500, `404` `ERR_FIN_01`, `409` `ERR_DEV_01` y `ERR_DEV_02`, `401` sin sesión y `403` con otro rol.

Archivos esperados (crear): `DevolucionController` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Un endpoint para listar el historial (lo leerá el CU-04).
- Endpoints de archivar o rechazar (CU-20).

## Resultado esperado

`POST /api/v1/finalistas/{codigo}/devolver` responde según el contrato.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
