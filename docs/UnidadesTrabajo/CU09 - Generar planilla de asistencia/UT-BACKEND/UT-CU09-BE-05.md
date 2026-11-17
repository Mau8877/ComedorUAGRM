# UT-CU09-BE-05 — Listar y descargar planillas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-05` |
| **Nombre** | Listar y descargar planillas |
| **Caso de uso** | CU09-BE |
| **Depende de** | `UT-CU09-BE-01`, `UT-CU09-BE-02` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) y [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear).
- Como referencia: `B/features/actas/service/ConsultaActasService.java`.

## Solicitud de negocio

El Administrador consulta las planillas generadas y descarga la que necesita imprimir, sin exponer el archivo ni el CI.

## Objetivo

`ConsultaPlanillasService` lista las planillas de una convocatoria y entrega una dirección de descarga temporal, anotada en la bitácora.

## Alcance

- `B/features/planillas/service/ConsultaPlanillasService.java`, solo `ADMINISTRADOR_DUBSS`:
  - `listar(convocatoriaCodigo, pageable)`: exige la convocatoria (`ERR_PRI_01`) y devuelve la página de `PlanillasConsultaRepository.planillas`. No se anota.
  - `descargar(actorCodigo, planillaCodigo)` con `@Transactional` y `@Auditar(DESCARGA_PLANILLA)`: `ERR_PLA_05` si la planilla no existe, `ERR_PLA_06` si su archivo no está en MinIO (y entonces no se anota), si no entrega una dirección firmada que vence en `AlmacenamientoService.VIGENCIA_ACCESO` (5 minutos). El detalle de la bitácora lleva solo el código de la planilla.
- DTO `AccesoPlanillaResponse(url, expiraEn)`.
- Tests con colaboradores simulados: página, convocatoria inexistente, dirección firmada sin ruta, planilla inexistente, archivo faltante sin firmar nada, rol y bitácora.

Archivos esperados (crear): `ConsultaPlanillasService.java`, `AccesoPlanillaResponse.java` y `T/features/planillas/service/ConsultaPlanillasServiceTest.java`.

## Fuera de alcance (no tocar)

- Generar planillas (BE-04) y el controller (BE-06).
- Devolver el binario del PDF o la ruta del archivo.
- Búsqueda de texto o filtros en el listado.

## Resultado esperado

El Administrador obtiene la lista y una dirección temporal para cada planilla.

## Validación

1. `./mvnw test -Dtest=ConsultaPlanillasServiceTest` pasa (6 tests).
2. `./mvnw checkstyle:check` sin errores.
