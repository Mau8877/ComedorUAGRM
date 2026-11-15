# UT-CU13-FE-01 — Tipos y schemas de Zod

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-01` |
| **Nombre** | Tipos y schemas de Zod |
| **Caso de uso** | CU13-FE |
| **Depende de** | — |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature)
- `F/features/usuarios/schemas/usuarioPersonalSchema.ts` (referencia de estilo)

## Solicitud de negocio

Antes de pedir nada al backend, el formulario tiene que avisar al Administrador de los errores evidentes (fechas al revés, documentos sin ninguno obligatorio) con las mismas reglas que aplica el servidor.

## Objetivo

Existen los tipos de la feature y los schemas de Zod de la convocatoria y de la gestión, de los que se derivan los tipos de los formularios.

## Alcance

- `F/features/convocatorias/types/index.ts`: `EstadoConvocatoria` (`'BORRADOR' | 'ABIERTA' | 'CERRADA' | 'PUBLICADA'`), `GestionAcademica`, `TipoDocumento`, `Convocatoria` (resumen), `DocumentoRequerido`, `ConvocatoriaDetalle`, y los tipos de parámetros de los listados (`ConvocatoriasListParams` con `page`, `pageSize`, `search`, `estado`, `gestion`, `sort`; `GestionesListParams`; `TiposDocumentoListParams`). Los nombres de campo son **exactamente** los del backend (camelCase, `fechaApertura` como texto `yyyy-MM-dd`).
- `F/features/convocatorias/schemas/gestionSchema.ts`: `gestionSchema` (`etiqueta` obligatoria, hasta 50 caracteres, `fechaInicio` y `fechaFin` obligatorias, y un `refine` con el fin posterior al inicio, con el error sobre `fechaFin`) y el tipo `GestionRequest = z.infer<...>`. Mensajes en español.
- `F/features/convocatorias/schemas/convocatoriaSchema.ts`: `convocatoriaSchema` con `nombre` (obligatorio, hasta 150), `gestionCodigo` (obligatorio), `fechaApertura` y `fechaCierre` (obligatorias, el cierre posterior a la apertura, error sobre `fechaCierre`), `cupos` y `cantidadFinalistas` (entero positivo o vacío), `puntajeMinimo` (0 a 100, hasta dos decimales, o vacío) y `documentos` (`tipoDocumentoCodigo` y `obligatorio`; al menos uno, sin tipos repetidos y con al menos uno obligatorio, con el error sobre `documentos`). Un campo opcional vacío del formulario (`''`) se normaliza a `null` en el tipo de salida. `ConvocatoriaRequest = z.infer<...>`.
- `index.ts` de `types/` y de `schemas/`, y el barrel de la feature `F/features/convocatorias/index.ts` (solo lo que otras partes de la app pueden importar; hoy, nada más que lo que usen las rutas).
- Tests en `F/features/convocatorias/tests/schemas.test.ts` (Vitest): cada regla acepta lo válido y rechaza lo inválido con su mensaje y en el campo correcto; vacío → `null` en los opcionales; cierre igual a apertura rechazado; documentos vacíos, repetidos o sin obligatorio rechazados.

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

- Llamadas a la API y hooks (`UT-CU13-FE-02`) y componentes (UT 04 a 07).
- Las reglas que solo conoce el servidor (otra abierta en la gestión, postulaciones): las muestra el backend.
- Tipos de otras features.

## Resultado esperado

El formulario puede validar en el cliente con las mismas reglas del backend y los tipos del contrato están fijados.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila (los tipos cuadran).
