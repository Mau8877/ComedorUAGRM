# UT-CU16-MOB-01 — Dependencias, permisos nativos y reintento de subidas en `ApiClient`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-01` |
| **Nombre** | Dependencias, permisos nativos y reintento de subidas en `ApiClient` |
| **Caso de uso** | CU16-MOB |
| **Depende de** | — |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md)

## Solicitud de negocio

La app necesita cámara, galería, selector de PDF y recorte, y subir archivos aunque la sesión venza a mitad de la subida.

## Objetivo

Las dependencias y los permisos nativos están declarados y el `ApiClient` reintenta las subidas tras renovar la sesión.

## Alcance

- `pubspec.yaml`: `image_picker`, `image_cropper` y `file_picker` (los archivos generados de plugins de Linux, macOS y Windows se actualizan solos).
- `android/app/src/main/AndroidManifest.xml`: `UCropActivity` del recortador. `ios/Runner/Info.plist`: `NSCameraUsageDescription` y `NSPhotoLibraryUsageDescription`.
- `M/core/network/api_client.dart`: antes de enviar un `FormData` se guarda una copia sin enviar (`options.extra`); la petición reintentada tras renovar la sesión (401) usa esa copia.
- Tests: un caso en `test/core/api_client_test.dart` (una subida con `FormData` se reintenta con el token nuevo); sin el arreglo falla con «The FormData has already been finalized».

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Cambiar la renovación de sesión ni los endpoints públicos de `/auth`.
- Compilar para Android o iOS.

## Resultado esperado

Una subida de archivos sobrevive a una sesión vencida y los permisos de cámara y galería están declarados.

## Validación

1. `flutter test test/core/api_client_test.dart` pasa.
2. `flutter analyze` sin problemas.
3. Manual: `flutter run` en un teléfono sin errores de compilación por las dependencias nuevas.
