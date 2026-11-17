# UT-CU23-BE-02 — Consulta del padrón (resumen y listado)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-02` |
| **Nombre** | Consulta del padrón (resumen y listado) |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | UT-CU23-BE-01 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (listados, `sort`),
[`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

El Administrador necesita ver quiénes son los becados de una convocatoria y cuántos faltan por dar de alta.

## Objetivo

Los dos `GET` del padrón: el resumen y el listado paginado, con sus errores catalogados.

## Alcance

- `PadronConsultaRepository` (JDBC): datos de la convocatoria, fecha de publicación, conteos por estado, adjudicadas
  sin beca y la página del listado con nombre, CI y carrera.
- `PadronService.resumen(codigo)` y `listar(...)` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`.
- DTOs `BecaResponse`, `ResumenPadronResponse`; `BecaErrorCodes` (`ERR_BEC_01` a `ERR_BEC_10`) y
  `docs/errors/becas/ERRORES_BECAS.md` con su enlace en el índice.
- `PadronController` con los dos `GET`. El listado exige resultados publicados (`ERR_BEC_03`).

Archivos esperados (crear/tocar): `PadronConsultaRepository.java`, `PadronService.java`, `PadronController.java`,
`BecaResponse.java`, `ResumenPadronResponse.java`, `BecaErrorCodes.java`, `docs/errors/becas/ERRORES_BECAS.md`,
`docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Crear o cambiar becas (BE-03 a BE-05).
- Los repositories de `postulaciones`, `finalistas` y `resultados`.

## Resultado esperado

El Administrador obtiene el resumen y la página del padrón, ordenada y filtrada.

## Validación

1. `./mvnw compile` sin errores.
2. `GET .../padron` de una convocatoria sin publicar → `409` `ERR_BEC_03`; con `sort=ip` → `400` `ERR_BEC_10`.
3. Pruebas de BE-06 (unitarias e integración).
