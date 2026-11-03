# UT-CU01-MOB-01 — Almacenamiento seguro de la sesión (tokens)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-01` |
| **Nombre** | Almacenamiento seguro de la sesión (tokens) |
| **Caso de uso** | CU01-MOB |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La sesión usa los tokens que emite el backend: la app necesita guardarlos de forma segura para no pedir la contraseña cada vez que se abre.

## Objetivo

Existe `TokenStorage`: guarda el refresh token en `flutter_secure_storage` y el token de acceso en memoria.

## Alcance

- `pubspec.yaml`: agregar `flutter_secure_storage` (`flutter pub get`).
- `lib/core/storage/token_storage.dart`: `TokenStorage` (con una instancia compartida `TokenStorage.instance`, que usa `core/network` sin depender de `features/`) con `accessToken` (campo en memoria), `leerRefreshToken()`, `guardarSesion(accessToken, refreshToken)` y `borrarSesion()`. El refresh token se guarda en `flutter_secure_storage` (Keystore en Android); el token de acceso nunca se persiste. Se recibe el almacenamiento por constructor para poder simularlo en los tests.
- Test con un almacenamiento simulado: guardar, leer y borrar; el token de acceso no pasa por el almacenamiento seguro.

Archivos esperados (crear/tocar): `pubspec.yaml`, `pubspec.lock`, `token_storage.dart` y el test.

## Fuera de alcance (no tocar)

- Llamar a la API o renovar tokens (`UT-CU01-MOB-03`, `UT-CU01-MOB-05`).
- Login, registro y logout (`UT-CU01-MOB-06`).
- Firebase: no participa en la autenticación (queda solo para push, en otro CU).

## Resultado esperado

El resto de la feature guarda y lee los tokens solo a través de `TokenStorage`.

## Validación

1. `flutter pub get`, `flutter analyze` y `flutter test` pasan.
2. Ningún otro archivo de `lib/` usa `flutter_secure_storage` directamente.
