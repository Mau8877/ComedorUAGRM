# UT-CU16-MOB-04 — Proveedores y pantalla de documentos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-04` |
| **Nombre** | Proveedores y pantalla de documentos |
| **Caso de uso** | CU16-MOB |
| **Depende de** | `UT-CU16-MOB-02`, `UT-CU16-MOB-03` |

## Antes de implementar

- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (nombres de proveedores) y [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)

## Solicitud de negocio

El postulante necesita ver qué documentos le faltan y elegir cómo adjuntar cada uno.

## Objetivo

Existen los proveedores de documentos y la pantalla con la lista de documentos y sus acciones.

## Alcance

- `M/features/postulaciones/providers/documentos_postulacion_provider.dart`: `documentosPostulacionServiceProvider`, `capturaDocumentoServiceProvider`, `documentosPostulacionProvider` (familia por código, se descarta al salir) y `documentosPostulacionAccionesProvider` (adjuntar, reemplazar y eliminar; vuelve a pedir la lista después de cada cambio, también si falla).
- `M/features/postulaciones/screens/documentos_postulacion_screen.dart`: «Adjuntos (X de Y)», una tarjeta por documento con su estado, sus archivos (nombre, tamaño, fecha), reemplazar y eliminar (con confirmación) solo si `puedeEditar`, y «Adjuntar» o «Agregar otro archivo» mientras no se llegue al máximo; hoja inferior con las tres fuentes y el aviso si se niega la cámara.
- `M/features/postulaciones/widgets/alerta_documentos.dart`.
- Tests: `test/features/postulaciones/documentos_pantallas_test.dart` (servicio y captura simulados): estados, no editable, máximo, fuente, cámara denegada, eliminar con y sin confirmar.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- «Revisar y adjuntar» y las rutas (`UT-CU16-MOB-05`).
- Abrir un archivo ya subido.

## Resultado esperado

La lista de documentos se muestra y lleva a la revisión del archivo elegido.

## Validación

1. `flutter test test/features/postulaciones/documentos_pantallas_test.dart` pasa.
2. `flutter analyze` sin problemas.
