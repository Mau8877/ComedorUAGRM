# UT-CUX01-BE-06 — Endpoints de dispositivos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-06` |
| **Nombre** | Endpoints de dispositivos |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-05 |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, rutas)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@CurrentUserId`; todo endpoint nuevo es privado por defecto)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La app móvil necesita avisar al backend qué teléfono es el suyo y cuándo ya no debe recibir avisos.

## Objetivo

Los dos endpoints de dispositivos funcionan con sesión y devuelven el sobre estándar.

## Alcance

- `B/features/notificaciones/dto/`: `RegistrarDispositivoRequest` (`token` `@NotBlank` `@Size(max = 512)`, `plataforma` `@NotNull`), `BajaDispositivoRequest` (`token` `@NotBlank`), `DispositivoResponse` (`codigo`, `plataforma`), todos `record`.
- `B/features/notificaciones/controller/DispositivoFcmController.java`:
  - `POST /api/v1/dispositivos-fcm` → `201` (o `200` si ya era de este usuario) con `DispositivoResponse`.
  - `POST /api/v1/dispositivos-fcm/baja` → `200` con `data: null`.
  - Usan `@CurrentUserId UUID` y delegan en un solo método del service. Sin lógica, sin logs en el controller.
- **No** se agregan a `RutasPublicas`: requieren sesión.
- Tests: `DispositivoFcmControllerTest` (`MockMvc`): `201`, `200` por repetición, `400` con token vacío o plataforma inválida, `401` sin sesión, forma del `ApiResponse` y baja idempotente.

Archivos esperados (crear): 3 DTOs, el controller y su test.

## Fuera de alcance (no tocar)

- Lógica de registro (`UT-CUX01-BE-05`).
- La app móvil que los llama.

## Resultado esperado

Con sesión se puede registrar y dar de baja un token; sin sesión, `401`.

## Validación

1. `./mvnw test` pasa.
2. `POST /api/v1/dispositivos-fcm` sin token de acceso responde `401` con `ERR_AUTH_01`.
3. `./mvnw checkstyle:check` sin errores.
