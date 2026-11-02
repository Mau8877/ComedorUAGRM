# UT-CU01-MOB-03 — ApiClient: token de Firebase en cada petición y manejo de 401

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-03` |
| **Nombre** | ApiClient: token de Firebase en cada petición y manejo de 401 |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-01` |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Toda petición a la API debe llevar el token vigente de Firebase y la app debe reaccionar si la sesión deja de ser válida.

## Objetivo

`ApiClient` agrega `Authorization: Bearer <token>` y avisa ante un `401` para cerrar la sesión.

## Alcance

- `lib/core/network/api_client.dart`: un `Interceptor` de request que obtiene el token con `FirebaseAuth.instance.currentUser?.getIdToken()` (si hay usuario) y lo pone en `Authorization`; en `onError`, ante un `401` llama a un callback registrable (`ApiClient.onUnauthorized`), manteniendo el resto del flujo de errores.
- El `onUnauthorized` se registra desde la feature de auth (`UT-CU01-MOB-05`) para no importar `features/` desde `core/`.
- El token no se loguea nunca, ni con `ENABLE_API_LOGS=true` (excluir el header `Authorization` del `LogInterceptor`).
- Tests con un adaptador de `dio` simulado: con usuario el header se agrega; sin usuario no; un `401` invoca el callback una vez; el log no incluye el header.

Archivos esperados (tocar): `api_client.dart` y su test.

## Fuera de alcance (no tocar)

- Cerrar la sesión y navegar (es del provider de `UT-CU01-MOB-05`).
- Cambiar el desempaquetado del sobre existente.

## Resultado esperado

Las peticiones autenticadas llevan el token y un `401` se detecta en un solo lugar.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Con `ENABLE_API_LOGS=true` el log de peticiones no muestra el header `Authorization`.
