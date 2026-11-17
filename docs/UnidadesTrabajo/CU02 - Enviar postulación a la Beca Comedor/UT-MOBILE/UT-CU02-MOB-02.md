# UT-CU02-MOB-02 — Pantalla `PostularScreen` y ruta `/postular`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-02` |
| **Nombre** | Pantalla `PostularScreen` y ruta `/postular` |
| **Caso de uso** | CU02-MOB |
| **Depende de** | UT-CU02-MOB-01 |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)

## Solicitud de negocio

El Postulante tiene que poder enviar su postulación desde la app, con los documentos que ya puede adjuntar con la cámara (CU16).

## Objetivo

La ruta `/postular` muestra el asistente de tres pasos y el comprobante; «Postular» y «Corregir y reenviar» ya la usan.

## Alcance

- `screens/postular_screen.dart`: pasos 1 a 3, confirmación con `AlertDialog`, un envío a la vez y errores del backend.
- El paso 3 lista los documentos requeridos y manda a la pantalla del CU16 (`/mis-postulaciones/:codigo/documentos`) para adjuntarlos.
- `core/routing/app_router.dart`: la ruta `/postular`.
- `enlaces_cu02.dart`: `rutaPostular` y `rutaCorregir` pasan a `/postular`.
- Prueba: `postular_screen_test.dart` y los tests de `detalle_postulacion_test.dart` / `postulaciones_pantallas_test.dart` que dependían de los enlaces.

Archivos esperados (crear/tocar): los listados y `bloques_informacion.dart` / `mis_postulaciones_vista.dart` (ya no ocultan los botones).

## Fuera de alcance (no tocar)

- «Guardar borrador» (CU17).
- Rehacer la pantalla de documentos del CU16.

## Resultado esperado

El Postulante envía su postulación desde la app.

## Validación

1. `flutter analyze` y `flutter test` completos en verde.
2. Con el stack y un emulador: el mismo recorrido de la UT web.
