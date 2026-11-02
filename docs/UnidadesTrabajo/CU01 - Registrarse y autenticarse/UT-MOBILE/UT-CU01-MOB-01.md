# UT-CU01-MOB-01 — Dependencias de Firebase y su inicialización

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-01` |
| **Nombre** | Dependencias de Firebase y su inicialización |
| **Caso de uso** | CU01-MOB |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)
- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

La app debe poder hablar con Firebase Auth desde que arranca.

## Objetivo

`firebase_core` y `firebase_auth` están instalados e inicializados con la configuración del `.env`.

## Alcance

- `pubspec.yaml`: agregar `firebase_core` y `firebase_auth` (`flutter pub get`).
- `lib/core/firebase/firebase_options.dart`: `FirebaseOptions` armado con `dotenv.env` (`FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_MESSAGING_SENDER_ID`, `FIREBASE_PROJECT_ID`); si falta una variable lanza un error en español que la nombra.
- `lib/main.dart`: `await Firebase.initializeApp(options: ...)` después de `dotenv.load()`.
- `.env.example`: las cuatro variables con un comentario de dónde se obtienen (consola de Firebase, app Android registrada).
- Test de la función que arma las opciones: con variables simuladas las devuelve y sin ellas falla con el mensaje esperado.

Archivos esperados (crear/tocar): `pubspec.yaml`, `pubspec.lock`, `firebase_options.dart`, `main.dart`, `.env.example` y el test.

## Fuera de alcance (no tocar)

- Login, registro y logout (`UT-CU01-MOB-06`).
- `google-services.json` / plugin de Gradle de Google: no hacen falta con opciones desde `.env`.
- Otros servicios de Firebase (FCM, Storage).

## Resultado esperado

La app arranca con Firebase inicializado.

## Validación

1. `flutter pub get`, `flutter analyze` y `flutter test` pasan.
2. `flutter run` en un emulador Android arranca sin errores de Firebase; sin una variable en `.env`, el error nombra cuál falta.
