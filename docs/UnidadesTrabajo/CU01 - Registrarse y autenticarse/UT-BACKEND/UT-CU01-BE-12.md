# UT-CU01-BE-12 — POST /api/v1/auth/registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-12` |
| **Nombre** | POST /api/v1/auth/registro |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-07`, `UT-CU01-BE-08`, `UT-CU01-BE-10`, `UT-CU01-BE-11` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app necesitan una forma de crear su cuenta de Postulante y quedar con la sesión abierta.

## Objetivo

`POST /api/v1/auth/registro` (público) crea la cuenta, abre la sesión y responde `201` (o `200` si la misma petición ya se había procesado).

## Alcance

- `B/features/auth/controller/AuthController.java`: `POST /api/v1/auth/registro`, `@Valid @RequestBody RegistroRequest`, llama a `AuthService.registrar(...)` con la IP (`request.getRemoteAddr()`) y devuelve `ApiResponse.success(sesion, "Registro exitoso")` con `201` o `200`; `sesion` es la `SesionResponse` (tokens y usuario). No exige token de acceso. Sin lógica ni `try/catch`.
- Tests (`@WebMvcTest` o `MockMvc`, `AuthService` simulado): `201` con el sobre correcto; `200` si ya existía; `400` `ERR_SYS_01` con body inválido; `409` con `ERR_US_01` y su mensaje; una confirmación de contraseña distinta da `400` `ERR_SYS_01`; un header `Authorization` vencido se ignora (ruta pública); y que un body con `rol` no lo usa.

Archivos esperados (crear): `AuthController.java` y su test.

## Fuera de alcance (no tocar)

- Login, refresh, logout y `GET /auth/usuario-actual` (`UT-CU01-BE-13`).
- Cambiar `GlobalExceptionHandler`: ya mapea `BusinessException` y la validación.

## Resultado esperado

El cliente puede registrar un Postulante por HTTP con el formato estándar de respuesta y errores.

## Validación

1. `./mvnw test` pasa con los escenarios indicados.
2. Los tests de controller verifican `status`, `data` y `error` (no solo el código HTTP), como pide `TESTING_BACKEND.md`.
3. `./mvnw checkstyle:check` sin errores.
