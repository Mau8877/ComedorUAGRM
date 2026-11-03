# UT-CU01-MOB-03 — ApiClient: token de acceso en cada petición y renovación ante 401

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-03` |
| **Nombre** | ApiClient: token de acceso en cada petición y renovación ante 401 |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-01` |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Toda petición a la API debe llevar el token de acceso vigente, renovarse sola cuando vence y la app debe reaccionar si la sesión deja de ser válida.

## Objetivo

`ApiClient` agrega `Authorization: Bearer <token>`, renueva el token ante un `401` y avisa si la sesión ya no se puede recuperar.

## Alcance

- `lib/core/network/api_client.dart`: un `Interceptor` de request que toma el token de acceso de `TokenStorage` y lo pone en `Authorization`. En `onError`, ante un `401` de una ruta **que no sea** `/auth/login`, `/auth/registro` ni `/auth/refresh`, llama a un callback registrable (`ApiClient.onRenovar`) que intenta `POST /auth/refresh` (una sola renovación a la vez aunque fallen varias peticiones juntas) y, si lo logra, reintenta la petición original; si no, llama a `ApiClient.onSesionInvalida`. Un `401` de `login`, `registro` o `refresh` es un error del formulario y no dispara nada. El resto del flujo de errores se mantiene.
- Ambos callbacks se registran desde la feature de auth (`UT-CU01-MOB-05`) para no importar `features/` desde `core/`.
- Ni el token de acceso ni el refresh token se loguean nunca, ni con `ENABLE_API_LOGS=true` (excluir el header `Authorization` y los cuerpos de `/auth/*` del `LogInterceptor`).
- Tests con un adaptador de `dio` simulado: con token el header se agrega; sin token no; un `401` renueva una sola vez aunque fallen tres peticiones a la vez y reintenta; si la renovación falla se llama a `onSesionInvalida`; un `401` de `/auth/login` no renueva; el log no incluye el header.

Archivos esperados (tocar): `api_client.dart` y su test.

## Fuera de alcance (no tocar)

- Cerrar la sesión y navegar (es del provider de `UT-CU01-MOB-05`).
- Cambiar el desempaquetado del sobre existente.

## Resultado esperado

Las peticiones autenticadas llevan el token, el token vencido se renueva solo y una sesión irrecuperable se detecta en un solo lugar.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Con `ENABLE_API_LOGS=true` el log de peticiones no muestra el header `Authorization`.
