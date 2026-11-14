# UT-CU20-BE-06 — Endpoints para archivar y rechazar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-06` |
| **Nombre** | Endpoints para archivar y rechazar |
| **Caso de uso** | CU20-BE |
| **Depende de** | `UT-CU20-BE-04` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `DevolucionController` (CU-19).

## Solicitud de negocio

La pantalla del expediente consume estos endpoints.

## Objetivo

Existen los dos endpoints con un controller delgado.

## Alcance

- `B/features/postulaciones/controller/CierrePostulacionController.java` con `@RequestMapping("/api/v1/finalistas/{codigo}")` y dos métodos: `POST /archivar` y `POST /rechazar` (acciones en infinitivo). Cuerpo `{ motivo }` validado con `@Valid`; responden `200` con `ApiResponse<CierrePostulacionResponse>` y los mensajes «Postulación archivada correctamente» / «Postulación rechazada correctamente».
- Exigen `ADMINISTRADOR_DUBSS` (el `403` lo escribe `ApiAccessDeniedHandler`), usan `@CurrentUserId` como actor, no tienen lógica ni logging y **no** reciben `Idempotency-Key`.
- Tests de contrato con `MockMvc`, para cada endpoint: forma del sobre y `200`, `400` `ERR_SYS_01` por motivo vacío, solo espacios o de más de 500, `404` `ERR_FIN_01`, `409` `ERR_ARC_01`, `401` sin sesión y `403` con otro rol.

Archivos esperados (crear): `CierrePostulacionController` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Un endpoint para listar el historial (lo leerá el CU-04).
- Reabrir una postulación.

## Resultado esperado

`POST /api/v1/finalistas/{codigo}/archivar` y `/rechazar` responden según el contrato.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
