# UT-CU16-MOB-05 — Pantalla «Revisar y adjuntar», rutas y acceso desde el detalle

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-05` |
| **Nombre** | Pantalla «Revisar y adjuntar», rutas y acceso desde el detalle |
| **Caso de uso** | CU16-MOB |
| **Depende de** | `UT-CU16-MOB-04` |

## Antes de implementar

- [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)

## Solicitud de negocio

Antes de subir un documento la persona debe poder verlo, recortarlo o repetirlo, y recibir un motivo claro si no sirve.

## Objetivo

Existen «Revisar y adjuntar», sus rutas y el botón «Gestionar documentos» del detalle de una postulación devuelta.

## Alcance

- `M/features/postulaciones/screens/revisar_documento_screen.dart`: vista previa de la imagen (o aviso para un PDF), «Recortar o girar» (solo imágenes), «Repetir foto» o «Elegir otro», el motivo local si el archivo no cumple las reglas (y el botón deshabilitado), «Adjuntar documento» o «Reemplazar documento»; si el servidor rechaza el archivo o falla la conexión se muestra el motivo y se conserva el archivo para reintentar; si la postulación cambió, avisa y vuelve a la lista.
- `M/core/routing/app_router.dart`: `/mis-postulaciones/:codigo/documentos` y `/mis-postulaciones/:codigo/documentos/revisar` (con `extra`; sin él redirige a la lista).
- `M/features/postulaciones/screens/detalle_postulacion_screen.dart`: sección «Tus documentos» con «Gestionar documentos», solo si `puedeCorregir`.
- Tests: los de «Revisar y adjuntar» en `documentos_pantallas_test.dart` y dos casos en `test/features/postulaciones/detalle_postulacion_test.dart`.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- El formulario de postulación (CU02) y el borrador (CU17).
- La pantalla de cámara propia.

## Resultado esperado

Desde el detalle de una postulación devuelta se llega a los documentos, se adjunta un archivo y se vuelve con el aviso.

## Validación

1. `flutter test` pasa completo.
2. `flutter analyze` sin problemas.
3. Manual en un teléfono con el backend y MinIO: adjuntar una foto recortada y un PDF, reemplazar y eliminar.
