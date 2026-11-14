# UT-CUX01-BE-11 — Aviso de prueba

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-11` |
| **Nombre** | Aviso de prueba |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-10 |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) (`@PreAuthorize` en el `service`, `403` `ERR_AUTH_07`)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)

## Solicitud de negocio

Antes de que existan eventos reales, hay que poder comprobar de punta a punta que el teléfono y el correo reciben lo que el sistema envía.

## Objetivo

El Administrador DUBSS puede mandarse un aviso de prueba con un solo `POST`.

## Alcance

- `B/features/notificaciones/service/NotificacionService.java`: método `enviarPrueba(UUID codigoUsuario)` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` que obtiene al usuario (por el `service` de `usuarios`) y llama a `notificar(usuario, PRUEBA, {nombres})`.
- `B/features/notificaciones/controller/NotificacionController.java`: `POST /api/v1/notificaciones/prueba` → `200`, `data: null`, mensaje «La notificación de prueba se está enviando». Sin cuerpo; siempre a sí mismo (`@CurrentUserId`).
- Tests: `NotificacionControllerTest` (`200` como administrador, `403` como otro rol, `401` sin sesión, forma del sobre).

Archivos esperados (crear): `NotificacionController.java` y su test. (Tocar): `NotificacionService.java`.

## Fuera de alcance (no tocar)

- Enviar el aviso de prueba a otro usuario.
- Los endpoints de la bandeja (`/notificaciones/me`, leer, leer todo, eliminar).

## Resultado esperado

Con el JSON de Firebase puesto y un teléfono con sesión, el administrador recibe el push y el correo de prueba.

## Validación

1. `./mvnw test` pasa.
2. Como administrador, `POST /api/v1/notificaciones/prueba` responde `200` y el correo aparece en Mailpit; como postulante responde `403` `ERR_AUTH_07`.
3. `./mvnw checkstyle:check` sin errores.
