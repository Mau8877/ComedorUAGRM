# UT-CU15-MOB-01 — Modelos y servicios del CU

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-01` |
| **Nombre** | Modelos y servicios del CU |
| **Caso de uso** | CU15-MOB |
| **Depende de** | — |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)
- Como referencia: `M/features/auth/models/` y `M/features/auth/services/auth_service.dart`

## Solicitud de negocio

La app necesita hablar con el backend para pedir el enlace, leer el perfil, actualizarlo y cambiar la contraseña.

## Objetivo

Existen los modelos Dart del perfil y los servicios que llaman a los cuatro endpoints.

## Alcance

- `M/features/perfil/models/perfil.dart`: `Perfil` y `PerfilPostulante` (con `Carrera` de `features/auth/models/carrera.dart` para la carrera), inmutables, con `fromJson` a mano. `PerfilPostulante` es `null` si el usuario no es Postulante.
- `M/features/perfil/models/actualizar_perfil_request.dart`: `ActualizarPerfilRequest` con `correo`, `telefono` y `contrasenaActual` opcionales; `toJson` **omite** los nulos.
- `M/features/auth/services/auth_service.dart`: `solicitarRecuperacion(String correo)` → `POST /auth/recuperar-contrasena`.
- `M/features/perfil/services/perfil_service.dart` (recibe el `Dio` compartido, no crea el suyo): `obtener()` (`GET /perfil`), `actualizar(request)` (`PATCH /perfil`, devuelve `Perfil`) y `cambiarContrasena({contrasenaActual, contrasenaNueva, confirmacion, refreshToken})` (`POST /auth/cambiar-contrasena` con `confirmacionContrasena` y `refreshToken` en el cuerpo).
- Tests: el `fromJson` con y sin `postulante`; `toJson` omite lo no enviado y nunca incluye el CI; cada servicio llama a la URL y al método correctos con el cuerpo esperado (con un `Dio` falso).

Archivos esperados (crear): los modelos, `perfil_service.dart` y sus tests. (Tocar): `auth_service.dart` y su test.

## Fuera de alcance (no tocar)

- Providers, pantallas y manejo de errores (UT-02 en adelante).
- `ApiClient` y `TokenStorage`.

## Resultado esperado

Las capas de arriba consumen el CU con llamadas tipadas.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
