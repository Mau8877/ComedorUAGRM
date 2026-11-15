# UT-CU23-BE-03 — Alta de los adjudicados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-03` |
| **Nombre** | Alta de los adjudicados |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | UT-CU23-BE-01, UT-CU23-BE-02 |

## Antes de implementar

[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) (`@Auditar`,
`ResultadoAuditado`) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Los adjudicados deben figurar como becados para poder administrarlos.

## Objetivo

`POST .../padron/alta`: crea una beca vigente por cada adjudicada de la convocatoria que todavía no la tiene.

## Alcance

- `PadronConsultaRepository.crearBecasFaltantes(...)`: `INSERT ... SELECT` de las adjudicaciones `ADJUDICADA`, con
  `ON CONFLICT DO NOTHING`; la fecha de inicio es la de publicación en La Paz.
- `PadronService.darDeAlta(actor, codigo)` con `@Transactional` y `@Auditar(ALTA_BECAS)`: exige resultados publicados
  (`ERR_BEC_03`) y adjudicadas sin beca (`ERR_BEC_04`); anota la bitácora solo si creó becas.
- `BitacoraAcciones.ALTA_BECAS` y `DTO AltaPadronResponse {creadas}`; el endpoint en `PadronController`.
- Sin nombres, CI ni correos en el `detalle` de la bitácora (solo el código de la convocatoria y la cantidad).

Archivos esperados (crear/tocar): `PadronConsultaRepository.java`, `PadronService.java`, `PadronController.java`,
`AltaPadronResponse.java`, `BitacoraAcciones.java`, `docs/errors/becas/ERRORES_BECAS.md`.

## Fuera de alcance (no tocar)

- Adjudicar o publicar (CU05 y CU22).
- Notificar a los becados.

## Resultado esperado

Tras el alta, cada adjudicada tiene una beca vigente; repetir el alta responde `409` `ERR_BEC_04` y no duplica nada.

## Validación

1. `./mvnw compile` sin errores.
2. Pruebas de BE-06: crea una beca por adjudicada, no duplica, respeta que solo cuentan las `ADJUDICADA`, y deja una
   fila `ALTA_BECAS` en la bitácora.
