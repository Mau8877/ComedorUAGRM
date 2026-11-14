# UT-CU21-BE-09 — Bandeja de avisos del usuario (contrato de `NOTIFICACIONES_WEB.md`)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-09` |
| **Nombre** | Bandeja de avisos del usuario (contrato de `NOTIFICACIONES_WEB.md`) |
| **Caso de uso** | CU21-BE |
| **Depende de** | — |

## Antes de implementar

- [`docs/pendientes/NOTIFICACIONES_WEB.md`](../../../pendientes/NOTIFICACIONES_WEB.md): el contrato **no se redefine** acá.
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@CurrentUserId`).

## Solicitud de negocio

El Postulante (y el administrador) consulta sus avisos, los marca como leídos y los borra.

## Objetivo

Existen los endpoints de la bandeja del usuario autenticado.

## Alcance

- `BandejaNotificacionesService` y ampliación de `NotificacionController`: `GET /api/v1/notificaciones/me` (`page`, `pageSize`, `sort=fecha|-fecha`, default `-fecha`), `GET /me/resumen` (`{ noLeidas }`), `POST /{codigo}/leer` (idempotente; `404` `ERR_NOT_01` si no existe **o es de otro usuario**), `POST /leer-todo` y `DELETE /api/v1/notificaciones` (borrado físico de todas las del usuario).
- Respuesta de cada aviso: `id` (código público), `tipo`, `titulo`, `detalle` (el mensaje), `fecha` (`creado_en`), `leida`. Un `sort` distinto → `400` `ERR_NOT_02`.
- Exige sesión (sin rol específico) y opera **solo** sobre el usuario del token: leer-todo y eliminar no tocan avisos ajenos.
- `NotificacionRepository`: `findByCodigoAndUsuarioId`, `findByUsuarioId(Pageable)`, `countByUsuarioIdAndLeidaFalse`, y las actualizaciones masivas `marcarTodasLeidas` y `eliminarTodas`.
- Tests: del service (aislamiento, idempotencia, `sort`), del controller (`MockMvc`: sobre, `401`, `404`) y de integración del repositorio.

Archivos esperados (crear): `BandejaNotificacionesService` y los tests. (Tocar): `NotificacionController`, `NotificacionRepository`.

## Fuera de alcance (no tocar)

- Crear avisos (los crean los módulos de negocio).
- La bandeja y el push en la app móvil, `DELETE` de un solo aviso y `filter[leida]`.
- El aviso de prueba (`/prueba`) de CU-EXTRA-01.

## Resultado esperado

Un usuario lista, cuenta, lee y elimina sus avisos sin ver los de nadie más.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
