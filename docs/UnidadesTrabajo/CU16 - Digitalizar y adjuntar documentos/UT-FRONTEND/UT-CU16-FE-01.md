# UT-CU16-FE-01 — Tipos, API, claves, hooks y mapeo de errores de documentos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-FE-01` |
| **Nombre** | Tipos, API, claves, hooks y mapeo de errores de documentos |
| **Caso de uso** | CU16-FE |
| **Depende de** | — |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (convención de `queryKey` e invalidación)

## Solicitud de negocio

La pantalla necesita hablar con los endpoints de documentos y traducir sus errores a mensajes claros.

## Objetivo

Existen las funciones de API, las claves de caché, los hooks y el mapeo de errores de documentos.

## Alcance

- `F/features/postulaciones/types/documentos.ts` (re-exportado desde `types/index.ts`): `ArchivoAdjunto`, `DocumentoRequerido`, `ReglasArchivo`, `DocumentosPostulacion`, `AccesoDocumento`.
- `F/features/postulaciones/api/postulacionesApi.ts`: `listarDocumentosApi`, `adjuntarDocumentoApi` y `reemplazarDocumentoApi` (`FormData`), `eliminarDocumentoApi` y `accesoDocumentoApi`.
- `api/keys.ts`: `postulacionesKeys.documentos(codigo)`. `api/hooks.ts`: `useDocumentos`, `useAdjuntarDocumento`, `useReemplazarDocumento`, `useEliminarDocumento` (invalida también al fallar) y `useAccesoDocumento`.
- `api/mapearErrorDocumentos.ts`: `ERR_DOC_04`/`09` → refrescar; `ERR_DOC_06`/`07` → no editable; conexión → reintentar sin perder los demás documentos; el resto deja pasar el mensaje en español del backend.
- Tests: `tests/documentosApi.test.tsx`.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Componentes y pantallas (`UT-CU16-FE-03` y `04`).
- Cambiar `apiClient` ni crear otra instancia de axios.

## Resultado esperado

Cualquier componente puede pedir, subir, reemplazar, eliminar y abrir documentos sin armar URLs ni claves a mano.

## Validación

1. `pnpm exec vitest run src/features/postulaciones/tests/documentosApi.test.tsx` pasa.
2. `pnpm lint` y `pnpm exec tsc -b` sin errores.
