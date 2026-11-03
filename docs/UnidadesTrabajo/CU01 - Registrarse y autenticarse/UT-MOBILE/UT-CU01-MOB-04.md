# UT-CU01-MOB-04 — AuthService: llamadas a la API de cuentas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-04` |
| **Nombre** | AuthService: llamadas a la API de cuentas |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-02`, `UT-CU01-MOB-03` |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita hablar con el backend para listar carreras, registrar, anotar el acceso y recuperar el usuario.

## Objetivo

`AuthService` expone las cuatro llamadas del contrato HTTP con modelos tipados.

## Alcance

- `lib/features/auth/services/auth_service.dart`: recibe el `Dio` de `ApiClient.instance` (no crea el suyo). Métodos: `listarCarreras()` → `GET /carreras?pageSize=200`; `registrar(RegistroRequest)` → `POST /auth/registro` (devuelve la `Sesion`); `iniciarSesion(correo, contrasena)` → `POST /auth/login` (devuelve la `Sesion`); `renovar(refreshToken)` → `POST /auth/refresh`; `cerrarSesion(refreshToken)` → `POST /auth/logout`; `usuarioActual()` → `GET /auth/usuario-actual`. Los errores de negocio salen como `ApiException` (ya implementado en el interceptor).
- Tests con `Dio` simulado: cada método usa la ruta y el método HTTP correctos y devuelve el modelo; un error `ERR_US_01` llega como `ApiException` con su `code`.

Archivos esperados (crear): `auth_service.dart` y su test.

## Fuera de alcance (no tocar)

- Guardar los tokens y la lógica de los flujos (`UT-CU01-MOB-05` y `UT-CU01-MOB-06`).
- Estado, providers y pantallas.

## Resultado esperado

La capa de red del CU01 está lista y probada.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
