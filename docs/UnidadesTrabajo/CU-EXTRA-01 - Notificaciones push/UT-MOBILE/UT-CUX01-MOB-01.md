# UT-CUX01-MOB-01 — Firebase en la app Android

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-MOB-01` |
| **Nombre** | Firebase en la app Android |
| **Caso de uso** | CU-EXTRA-01-MOB |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- `M/android/app/google-services.json.example` y [`secrets/README.md`](../../../../projects/backend/comedor-uagrm-core/secrets/README.md) (qué archivos hay y de dónde salen)

## Solicitud de negocio

La app tiene que poder hablar con Firebase para recibir push, sin que a quien no tenga las credenciales se le rompa la compilación.

## Objetivo

La app Android incluye Firebase Messaging, compila con o sin `google-services.json` y arranca sin tumbarse si Firebase no está disponible.

## Alcance

- `M/pubspec.yaml`: `firebase_core` y `firebase_messaging` con las **versiones verificadas en pub.dev** al implementar (no de memoria).
- Plugin de Google Services de Gradle: declarado en `M/android/settings.gradle.kts` (versión verificada) y aplicado en `M/android/app/build.gradle.kts` **solo si existe `app/google-services.json`**, con un `logger.warn` que explique que sin el archivo no hay push.
- Confirmar `minSdk` compatible con las librerías elegidas; ajustar solo si lo exigen.
- `M/lib/main.dart`: `Firebase.initializeApp()` dentro de un `try/catch` (sin `google-services.json` o sin Play Services la app sigue sin push, con un `debugPrint`), antes de `runApp`. Ningún otro cambio en `main.dart` en esta UT.
- Confirmar que `google-services.json` está en el `.gitignore` y su plantilla `.example` versionada.

Archivos esperados (tocar): `pubspec.yaml`, `settings.gradle.kts`, `app/build.gradle.kts`, `main.dart`.

## Fuera de alcance (no tocar)

- Pedir permiso, obtener el token o llamar a la API (`UT-CUX01-MOB-02`).
- Configuración de iOS.

## Resultado esperado

`flutter run` arranca con el archivo de Firebase puesto, y `flutter build apk --debug` también funciona renombrándolo temporalmente.

## Validación

1. Con `google-services.json`: `flutter pub get`, `flutter analyze` y `flutter run` sin errores; el log no muestra el aviso de «sin push».
2. Sin el archivo: `flutter build apk --debug` compila y la app abre (CA16).
3. `git status` no muestra `google-services.json`.
