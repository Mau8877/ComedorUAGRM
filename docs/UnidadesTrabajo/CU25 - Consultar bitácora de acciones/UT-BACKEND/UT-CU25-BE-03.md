# UT-CU25-BE-03 — Pruebas de la consulta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-03` |
| **Nombre** | Pruebas de la consulta |
| **Caso de uso** | CU25-BACKEND |
| **Depende de** | UT-CU25-BE-01, UT-CU25-BE-02 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Que el filtrado por fechas y los errores no se rompan sin que nadie lo note.

## Objetivo

Pruebas unitarias del servicio y una prueba de integración contra Postgres real que cubre el contrato HTTP completo.

## Alcance

- `BitacoraConsultaServiceTest` (Mockito): orden por defecto y `meta`; fechas como día completo en hora de
  Bolivia; `ERR_BIT_01`; `ERR_BIT_02`; página inválida.
- `BitacoraIntegrationTest` (Postgres real, JWT firmados de verdad): `401` sin sesión y `403` `ERR_AUTH_07` con rol
  de Postulante; listado de la más reciente a la más antigua, con la forma del `ApiResponse` y `meta`, sin
  `entidadId` ni `id`; paginación y `sort`; filtros por acción y por usuario (nombres, apellidos y correo, sin
  distinguir mayúsculas) combinados; fechas como día completo en hora de La Paz; lista vacía; `ERR_BIT_01`,
  `ERR_BIT_02`, `ERR_SYS_05` y `ERR_SYS_01`; sin escritura (POST, PUT, PATCH y DELETE no modifican nada); y que
  consultar no deja rastro en la bitácora.
- No hay un test de controller con servicio simulado aparte: la integración ya cubre el contrato HTTP y el rol,
  que es justo lo que un test simulado no puede probar (`@PreAuthorize` no corre con el servicio simulado).

Archivos esperados (crear/tocar): `BitacoraConsultaServiceTest.java`, `BitacoraIntegrationTest.java`.

## Fuera de alcance (no tocar)

- `AuditoriaAspectTest` y los tests de escritura de la bitácora.

## Resultado esperado

La consulta queda cubierta en su lógica (rango, orden, errores).

## Validación

1. `./mvnw test -Dtest=BitacoraConsultaServiceTest` → 5 pruebas en verde.
2. `./mvnw test -Dtest=BitacoraIntegrationTest` → 9 pruebas en verde (con Docker, o con
   `-Dtest.postgres.url=jdbc:postgresql://localhost:5432/<base_vacia>` si Testcontainers no conecta).
