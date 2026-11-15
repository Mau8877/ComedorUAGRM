# UT-CU13-BE-01 — Códigos de error `ERR_CV_01` a `ERR_CV_13` y su catálogo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-01` |
| **Nombre** | Códigos de error `ERR_CV_01` a `ERR_CV_13` y su catálogo |
| **Caso de uso** | CU13-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Cada rechazo de este CU (convocatoria inexistente, fechas incoherentes, otra convocatoria abierta, etc.) tiene que llegar al cliente con un código catalogado, y quien lo vea en un log debe poder entender qué pasó sin buscar en el código.

## Objetivo

Quedan definidas las 13 constantes `ERR_CV_*` del módulo y documentadas en `docs/errors/`, antes de que cualquier servicio las lance.

## Alcance

- `B/features/convocatorias/exception/ConvocatoriaErrorCodes.java` (clase `final` con constantes `String`, un comentario de una línea por código, igual que `UsuarioErrorCodes`): `ERR_CV_01` a `ERR_CV_13` con el significado y el HTTP de la tabla del [README](./README.md#contrato-de-datos--recurso-http).
- `docs/errors/convocatorias/ERRORES_CONVOCATORIAS.md` (archivo nuevo): una sección `## \`ERR_CV_NN\`` por código, con el formato obligatorio de `GUIA_ERRORES_BACKEND.md` (HTTP, Excepción, Significado, Dónde se lanza). «Dónde se lanza» apunta a la clase y el método previstos en las UT 06 a 12 (por ejemplo `ConvocatoriaEstadoService.abrir()` para `ERR_CV_07`).
- `docs/errors/README.md`: sección nueva `### Convocatorias (\`CV\`)` con un enlace por código a su ancla (`convocatorias/ERRORES_CONVOCATORIAS.md#err_cv_01`), con el mismo estilo que `Usuarios (US)`. Se actualiza también el árbol de carpetas del encabezado si lista los módulos.

Archivos esperados (crear/tocar): `ConvocatoriaErrorCodes.java`, `docs/errors/convocatorias/ERRORES_CONVOCATORIAS.md`, `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Lanzar las excepciones: lo hace cada servicio en su UT.
- Los `400` de Bean Validation (`@NotBlank`, `@Positive`, rangos de `puntajeMinimo`): caen en `ERR_SYS_01` y no se documentan acá.
- Los códigos de otros módulos (`ERR_US_*`, `ERR_PO_*`...).

## Resultado esperado

Los 13 códigos existen en Java y en el catálogo, con el índice del `README.md` apuntando a cada uno.

## Validación

1. Cada enlace del índice de `docs/errors/README.md` para `ERR_CV_*` abre su sección.
2. Hay 13 constantes en `ConvocatoriaErrorCodes` y 13 secciones en `ERRORES_CONVOCATORIAS.md`, con el mismo número y el mismo HTTP.
3. `./mvnw checkstyle:check` sin errores.
