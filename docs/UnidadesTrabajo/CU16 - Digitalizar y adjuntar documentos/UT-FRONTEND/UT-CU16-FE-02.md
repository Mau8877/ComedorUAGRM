# UT-CU16-FE-02 — Esquema de validación del archivo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-FE-02` |
| **Nombre** | Esquema de validación del archivo |
| **Caso de uso** | CU16-FE |
| **Depende de** | `UT-CU16-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)

## Solicitud de negocio

El postulante debe saber enseguida que su archivo no sirve (formato o tamaño), sin esperar al servidor.

## Objetivo

Existe un esquema Zod del archivo armado con las reglas que manda el backend.

## Alcance

- `F/features/postulaciones/schemas/archivoDocumentoSchema.ts` (+ `schemas/index.ts`): `crearArchivoDocumentoSchema(reglas)` (`z.file()` con vacío, tamaño máximo y formatos permitidos, mensajes en español), `formatearTamano` y `nombresDeFormatos`.
- Tests: `tests/archivoDocumentoSchema.test.ts`.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Validar el contenido real del archivo: lo hace el backend.
- Valores fijos de formato o tamaño: salen de `reglas`.

## Resultado esperado

Un archivo grande, vacío o de formato no permitido se rechaza con un mensaje que dice el límite o los formatos aceptados.

## Validación

1. `pnpm exec vitest run src/features/postulaciones/tests/archivoDocumentoSchema.test.ts` pasa.
2. `pnpm lint` sin errores.
