# UT-CU07-BE-10 — Endpoints del expediente y de la auditoría

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-10` |
| **Nombre** | Endpoints del expediente y de la auditoría |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-06`, `UT-CU07-BE-07`, `UT-CU07-BE-08`, `UT-CU07-BE-09` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `B/features/finalistas/controller/FinalistaController.java`.

## Solicitud de negocio

La pantalla web consume estos endpoints.

## Objetivo

Existen los tres endpoints del CU con controllers delgados.

## Alcance

- `B/features/expedientes/controller/ExpedienteController.java` bajo `/api/v1/finalistas`: `GET /{codigo}/expediente` (`200` con el expediente), `POST /{codigo}/auditorias` (`201` con la auditoría creada, `200` si fue una repetición idempotente: misma `Idempotency-Key`, header opcional) y `GET /{codigo}/documentos/{documentoCodigo}/acceso` (`200` con `{ url, expiraEn }`).
- Todos exigen `ADMINISTRADOR_DUBSS` (el `403` lo escribe `ApiAccessDeniedHandler`) y usan `@CurrentUserId` para el actor; devuelven `ApiResponse` y no tienen lógica ni logging.
- Tests de contrato con `MockMvc`: forma del sobre y status de cada endpoint, `400` `ERR_SYS_01` por cuerpo vacío u observaciones de más de 500, los códigos `ERR_EXP_*` y `ERR_FIN_01`, `401` sin sesión y `403` con otro rol.

Archivos esperados (crear): `ExpedienteController` y su test. (Tocar): documentación de endpoints si la hay.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Los endpoints de finalistas del CU-21 (no cambian).
- Un `GET` de la lista de auditorías: ya viene dentro del expediente.

## Resultado esperado

Los tres endpoints responden según el contrato.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
