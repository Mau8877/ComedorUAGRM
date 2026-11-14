# UT-CU14-BE-12 — `UsuarioController` (`/api/v1/usuarios`)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-12` |
| **Nombre** | `UsuarioController` (`/api/v1/usuarios`) |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-02`, `UT-CU14-BE-07`, `UT-CU14-BE-08`, `UT-CU14-BE-09`, `UT-CU14-BE-10`, `UT-CU14-BE-11` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#obtener-el-usuario-autenticado-en-los-controllers)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El frontend necesita endpoints para listar, crear, editar y cambiar de estado o de rol las cuentas del personal, con el contrato de respuesta del proyecto.

## Objetivo

Los seis endpoints de `/api/v1/usuarios` exponen la gestión de cuentas del personal con controllers delgados.

## Alcance

- `B/features/usuarios/controller/UsuarioController.java` (`@RestController`, `@RequestMapping("/api/v1/usuarios")`): cada método valida con `@Valid`, llama a **un** método de `UsuarioPersonalService` y devuelve `ResponseEntity<ApiResponse<...>>`. El `actorCodigo` sale de `@CurrentUserId UUID`, nunca del body ni de la URL.
  - `GET /` → `200` con `ApiResponse.successList(...)` y su `meta`. `page` (1), `pageSize` (20), `search`, `filter[rol]`, `filter[activo]` y `sort`. Un `filter[...]` que no sea `rol` ni `activo`, o un `filter[activo]` que no sea `true` o `false`, responde `400` `ERR_US_11` (no se ignora).
  - `POST /` → `201` con `UsuarioCreadoResponse` (`message`: «Cuenta creada correctamente»), y la cabecera `Cache-Control: no-store` (lleva una contraseña).
  - `PATCH /{codigo}`, `POST /{codigo}/cambiar-rol`, `POST /{codigo}/desactivar` y `POST /{codigo}/reactivar` → `200` con `UsuarioPersonalResponse`, cada uno con su `message` en español.
  - Los métodos de mutación devuelven el `datos()` del `ResultadoAuditado` que entrega el `service`.
- Sin lógica de negocio, sin `try/catch` y sin logs en el controller. No se agrega ninguna ruta a `RutasPublicas`: los seis endpoints son privados.
- Tests (`MockMvc`, `UsuarioPersonalService` mockeado): el contrato del sobre y el código HTTP de cada endpoint; `Cache-Control: no-store` solo en el alta; la validación `400` del cuerpo; `filter[...]` desconocido → `ERR_US_11`; sin token → `401` `ERR_AUTH_01`. La falta de rol (`403`) se prueba en `UT-CU14-BE-13`, porque con el `service` mockeado `@PreAuthorize` no se ejecuta.

Archivos esperados (crear): `UsuarioController.java` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio y acceso a repositorios.
- Otros endpoints (por ejemplo, `GET /{codigo}`): ningún flujo del CU los necesita.
- `GET /api/v1/auth/usuario-actual` y el resto de `/auth` (CU01).

## Resultado esperado

El frontend gestiona cuentas del personal a través de los seis endpoints.

## Validación

1. `./mvnw test` pasa.
2. Un test comprueba el cuerpo completo (`status`, `data`, `message`, `meta` en el listado), no solo el código HTTP.
3. `./mvnw checkstyle:check` sin errores.
