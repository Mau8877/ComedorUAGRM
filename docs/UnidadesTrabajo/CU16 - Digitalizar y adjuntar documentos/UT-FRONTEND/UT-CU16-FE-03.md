# UT-CU16-FE-03 — Panel de documentos y modal de subida

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-FE-03` |
| **Nombre** | Panel de documentos y modal de subida |
| **Caso de uso** | CU16-FE |
| **Depende de** | `UT-CU16-FE-01`, `UT-CU16-FE-02` |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El postulante necesita ver qué documentos le faltan y adjuntarlos, reemplazarlos o eliminarlos.

## Objetivo

Existe un panel reutilizable de documentos y un modal para adjuntar o reemplazar un archivo.

## Alcance

- `F/features/postulaciones/components/DocumentosPostulacionPanel.tsx`: «Adjuntos (X de Y)», un bloque por documento con su estado (`Adjuntado`, `Falta`, `Opcional`), sus archivos (nombre, tamaño, fecha) y acciones abrir, reemplazar y eliminar (con confirmación) solo si `puedeEditar`; «Adjuntar» o «Agregar otro archivo» mientras no se llegue al máximo. Estados de carga y de error con reintento.
- `F/features/postulaciones/components/SubirDocumentoModal.tsx`: `ModalLayout` + TanStack Form; selector de archivo con `accept`, validación con el esquema, vista previa (imagen o aviso), el error del servidor dentro del modal sin perder el archivo, aviso y actualización si la postulación cambió.
- Tests: `tests/DocumentosPostulacionPanel.test.tsx` (cliente simulado): estados, no editable, máximo de archivos, adjuntar con `FormData`, formato y tamaño rechazados sin subir, error del servidor, reemplazar, eliminar con confirmación, abrir y error de carga.

Archivos esperados (crear/tocar): los dos componentes, `components/index.ts` y el test.

## Fuera de alcance (no tocar)

- La pantalla completa y la ruta (`UT-CU16-FE-04`).
- La cámara y el recorte (móvil).
- Colores o tamaños fuera de los tokens del tema.

## Resultado esperado

El panel se puede montar en cualquier pantalla pasándole el código de la postulación.

## Validación

1. `pnpm exec vitest run src/features/postulaciones/tests/DocumentosPostulacionPanel.test.tsx` pasa.
2. `pnpm lint` y `pnpm exec tsc -b` sin errores.
3. Manual: con MinIO y el backend corriendo, adjuntar un PDF, abrirlo, reemplazarlo y eliminarlo.
