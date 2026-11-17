# UT-CU11-BE-02 — Consultas de la planilla, los registros digitales y los cotejos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-02` |
| **Nombre** | Consultas de la planilla, los registros digitales y los cotejos |
| **Caso de uso** | CU11-BE |
| **Depende de** | UT-CU11-BE-01 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación, `sort`) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Molde: `PlanillasConsultaRepository`.

## Solicitud de negocio

El Administrador necesita ver los registros digitales del periodo de la planilla para compararlos con la planilla firmada, y el historial de cotejos.

## Objetivo

`CotejosConsultaRepository` lee la planilla, cuenta y pagina sus registros digitales y pagina los cotejos.

## Alcance

- `B/features/cotejos/repository/CotejosConsultaRepository.java` (JDBC): `planilla(codigo)`, `resumenDigital(planilla)`, `registros(planilla, pageable)` y `cotejos(planilla, pageable)`.
- Registros digitales de una planilla: asistencias de su periodo de las becas de su convocatoria con `beca.creado_en <= planilla.generada_en`. Orden con lista blanca (`fecha`, `apellidos`; `creadoEn`, `fechaCotejo`); un campo no soportado, o más de uno, es `ERR_SYS_05`. No se selecciona el CI.
- DTOs de salida: `DatosCotejoResponse`, `RegistroDigitalResponse`, `CotejoResponse`.

Archivos esperados (crear/tocar): el repository y tres DTOs.

## Fuera de alcance (no tocar)

Los repositories de `becas`, `planillas` y `asistencias`; escritura de cotejos (UT-BE-03).

## Resultado esperado

Se obtienen los conteos y las páginas ordenadas, sin CI ni ids internos.

## Validación

1. `CotejoIntegrationTest` (datos, registros y listado) con base real (UT-BE-05).
