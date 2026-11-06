# UT-CUX01-BE-05 — Servicio de dispositivos (registro, baja y limpieza de tokens)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-05` |
| **Nombre** | Servicio de dispositivos (registro, baja y limpieza de tokens) |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-02 |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (el usuario se obtiene por el `service` de `usuarios`, no por su repository)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (borrado lógico)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

Cada usuario puede tener varios teléfonos recibiendo sus avisos, y el sistema debe mantener esa lista al día sin duplicados ni teléfonos «fantasma».

## Objetivo

La lógica para registrar un teléfono, darlo de baja y limpiar los tokens que Firebase declara inválidos.

## Alcance

- `B/features/notificaciones/service/DispositivoFcmService.java`:
  - `registrar(usuario, token, plataforma)`: si el token existe activo a nombre de este usuario, lo deja igual (no duplica); si está a nombre de otro usuario, lo **reasigna**; si no existe, lo crea. Devuelve un record con `codigo`, `plataforma` y si fue creado.
  - `darDeBaja(usuario, token)`: eliminación lógica solo si el token es de este usuario; si no existe o es de otro, no hace nada (idempotente).
  - `tokensDe(usuarioId)`: tokens activos del usuario.
  - `eliminarInvalidos(tokens)`: eliminación lógica de los tokens indicados (uso interno de `UT-CUX01-BE-10`).
- Para obtener el usuario a partir del `codigo` del token JWT se usa el `service` de `usuarios` que ya exista (verificar su método actual antes de crear uno nuevo).
- Logging INFO (registro, baja, limpieza) con `userId` y la cantidad; **nunca el token**.
- Tests: `DispositivoFcmServiceTest` con el repository simulado (nuevo, repetido, reasignado, baja de token ajeno, baja repetida, limpieza).

Archivos esperados (crear): `DispositivoFcmService.java`, el record de resultado y su test.

## Fuera de alcance (no tocar)

- Controller y rutas (`UT-CUX01-BE-06`).
- Enviar mensajes.

## Resultado esperado

El servicio mantiene un único registro por token y el usuario correcto como dueño.

## Validación

1. `./mvnw test` pasa.
2. Ninguna línea de log de los tests contiene el token.
3. `./mvnw checkstyle:check` sin errores.
