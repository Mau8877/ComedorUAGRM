# UT-CU16-MOB-03 — Servicio de captura (cámara, galería, PDF y recorte)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-03` |
| **Nombre** | Servicio de captura (cámara, galería, PDF y recorte) |
| **Caso de uso** | CU16-MOB |
| **Depende de** | `UT-CU16-MOB-01` |

## Antes de implementar

- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

El postulante debe poder fotografiar un documento, elegirlo de su galería o de sus archivos y recortarlo.

## Objetivo

Existe `CapturaDocumentoService`, que envuelve los plugins para que las pantallas no dependan de ellos.

## Alcance

- `M/features/postulaciones/services/captura_documento_service.dart`: `tomarFoto()` (sin permiso lanza `PermisoCamaraDenegado`), `elegirDeGaleria()`, `elegirPdf()` y `recortar(imagen)`; las fotos se reducen (ancho 2200, calidad 85); cada método devuelve `null` si la persona cancela. Devuelve un `ArchivoLocal` (ruta, nombre, tipo MIME declarado y tamaño).
- El proveedor `capturaDocumentoServiceProvider` (en `UT-CU16-MOB-04`) permite sustituirlo en los tests.

Archivos esperados (crear/tocar): el servicio.

## Fuera de alcance (no tocar)

- Una pantalla de cámara propia con marco y flash.
- Probarlo en `flutter test`: los plugins son nativos; los tests de las pantallas usan una captura simulada.

## Resultado esperado

Las pantallas capturan y eligen archivos con una sola interfaz sustituible.

## Validación

1. `flutter analyze` sin problemas.
2. Manual en un teléfono: tomar una foto, elegir de la galería, elegir un PDF, recortar y girar, y negar el permiso de la cámara.
