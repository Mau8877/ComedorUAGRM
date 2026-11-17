# UT-CU11-BE-03 — Servicios, errores y bitácora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-03` |
| **Nombre** | Servicios, errores y bitácora |
| **Caso de uso** | CU11-BE |
| **Depende de** | UT-CU11-BE-02 |

## Antes de implementar

[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Registrar el cotejo solo si lo declarado es consistente, dejar constancia en la bitácora y consultar los datos del cotejo.

## Objetivo

Los servicios validan y registran el cotejo y entregan los datos de consulta.

## Alcance

- `CotejosErrorCodes` (`ERR_COT_01..04`) y su documentación en `docs/errors/cotejos/ERRORES_COTEJOS.md` más el índice del `README.md`.
- `RegistrarCotejoRequest` (`@NotNull`, `@Min(0)`, `@Max`, observaciones ≤ 2000).
- `CotejoAsistenciaService.registrar` (`@PreAuthorize('ADMINISTRADOR_DUBSS')`, `@Transactional`, `@Auditar(COTEJO_ASISTENCIA)`): planilla existente (`ERR_COT_01`), suma = total (`ERR_COT_02`), resultado acorde a las discrepancias (`ERR_COT_03`), observaciones obligatorias con discrepancias (`ERR_COT_04`); observaciones sin espacios sobrantes; fecha de hoy en `America/La_Paz`.
- `ConsultaCotejosService`: `datos`, `registros` y `cotejos` (solo lectura, `ERR_COT_01` si la planilla no existe).
- `BitacoraAcciones.COTEJO_ASISTENCIA`.

Archivos esperados (crear/tocar): `exception/`, `dto/RegistrarCotejoRequest`, los dos servicios, `BitacoraAcciones.java` y `docs/errors/…`.

## Fuera de alcance (no tocar)

Cualquier cambio a asistencias, planillas o becas; notificaciones; el controller (UT-BE-04).

## Resultado esperado

Un cotejo inconsistente no se guarda; uno válido queda guardado y anotado sin observaciones en la bitácora.

## Validación

1. `CotejoAsistenciaServiceTest` y `CotejosErrorCodesTest` (UT-BE-05).
