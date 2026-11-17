# UT-CU23-BE-04 — Baja y suspensión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-04` |
| **Nombre** | Baja y suspensión |
| **Caso de uso** | CU23-BACKEND |
| **Depende de** | UT-CU23-BE-01, UT-CU23-BE-02 |

## Antes de implementar

[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y
[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Cuando un becado deja de cumplir, el Administrador debe sacarlo del padrón con su motivo.

## Objetivo

`POST /becas/{codigo}/dar-de-baja` y `/suspender`: cambian el estado de una beca vigente y registran el movimiento.

## Alcance

- `BecaEstadoService.darDeBaja(actor, codigo, motivo)` y `suspender(...)`, `@Transactional` y `@Auditar`
  (`BAJA_BECA`, `SUSPENSION_BECA`): bloquean la beca (`FOR UPDATE`), exigen que esté `VIGENTE` (`ERR_BEC_05`),
  cambian el estado, insertan el `MovimientoBeca` con el motivo recortado y anotan la bitácora.
- `MotivoRequest {motivo}` (`@NotBlank`, máx. 500; sin motivo → `400` `ERR_SYS_01`, A1).
- `BecaRepository`, `MovimientoBecaRepository` y los dos endpoints en un `BecaController`.
- El `detalle` de la bitácora lleva códigos y estados, nunca el motivo, nombres ni CI.

Archivos esperados (crear/tocar): `BecaEstadoService.java`, `BecaController.java`, `BecaRepository.java`,
`MovimientoBecaRepository.java`, `MotivoRequest.java`, `BitacoraAcciones.java`, `docs/errors/becas/ERRORES_BECAS.md`.

## Fuera de alcance (no tocar)

- El reemplazo (BE-05) y la reactivación.
- Cambiar la postulación o la adjudicación del becado.

## Resultado esperado

Una beca vigente pasa a baja o suspendida con su movimiento; una que no está vigente se rechaza con `409`.

## Validación

1. `./mvnw compile` sin errores.
2. Pruebas de BE-06: cambia el estado y deja movimiento y bitácora; sin motivo `400`; no vigente `409`; sin rol `403`.
