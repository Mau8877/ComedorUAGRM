# UT-CU18-BE-01 — Códigos de error `ERR_VAL_01` a `ERR_VAL_03` y su catálogo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-01` |
| **Nombre** | Códigos de error `ERR_VAL_01` a `ERR_VAL_03` y su catálogo |
| **Caso de uso** | CU18-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Cada rechazo de la validación debe tener un código catalogado que el CU02 pueda mostrar al postulante.

## Objetivo

Existen las constantes `ERR_VAL_01` a `ERR_VAL_03` y están documentadas en `docs/errors/`.

## Alcance

- `B/features/postulaciones/exception/ValidacionPostulacionErrorCodes.java`: las tres constantes con su HTTP en el Javadoc (prefijo `VAL`).
- `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`: una sección por código con el formato de la guía.
- `docs/errors/README.md`: sección «Validación de elegibilidad y duplicados (`VAL`)» con los tres enlaces.

Archivos esperados (crear/tocar): `ValidacionPostulacionErrorCodes.java`, `ERRORES_POSTULACIONES.md`, `README.md` de errores.

## Fuera de alcance (no tocar)

- Lanzar los errores (`UT-CU18-BE-03`).
- Un código nuevo para «convocatoria inexistente»: se reutiliza `ERR_CV_01`.

## Resultado esperado

Los tres códigos existen en el código y en el catálogo, con su enlace en el índice.

## Validación

1. Cada código del `.java` tiene su sección y su enlace en `docs/errors/README.md`.
2. `./mvnw checkstyle:check` sin errores.
