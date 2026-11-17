# UT-CU09-BE-06 — Endpoints REST

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-06` |
| **Nombre** | Endpoints REST |
| **Caso de uso** | CU09-BE |
| **Depende de** | `UT-CU09-BE-04`, `UT-CU09-BE-05` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (rutas, paginación, controllers delgados), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#obtener-el-usuario-autenticado-en-los-controllers) (`@CurrentUserId`).
- Como referencia: `B/features/actas/controller/ActaController.java`.

## Solicitud de negocio

La pantalla necesita llamar al backend para ver el resumen, generar, previsualizar, listar y descargar.

## Objetivo

`PlanillaController` expone los cinco endpoints del CU con el sobre estándar.

## Alcance

- `B/features/planillas/controller/PlanillaController.java`: `GET /convocatorias/{codigo}/planillas/resumen`, `GET /convocatorias/{codigo}/planillas` (`page`, `pageSize`, `sort`; cualquier `filter[...]` es `ERR_SYS_05`), `POST /convocatorias/{codigo}/planillas` (`201`, cuerpo `GenerarPlanillaRequest` con `@Valid`), `GET /convocatorias/{codigo}/planillas/vista-previa?desde=&hasta=` (el único fuera del sobre: `application/pdf`, `inline`, `Cache-Control: no-store`) y `GET /planillas/{codigo}/descarga`. Cada método delega en un solo método de `service`; sin lógica, sin repositorios, sin `try/catch`, sin logging.
- DTO `GenerarPlanillaRequest(fechaDesde, fechaHasta)` con `@NotNull` en ambas (`ERR_SYS_01`).
- No hay `PUT`, `PATCH` ni `DELETE`: responden `405`.

Archivos esperados (crear): `PlanillaController.java` y `GenerarPlanillaRequest.java`.

## Fuera de alcance (no tocar)

- Lógica de negocio y validaciones del periodo (están en el `service`).
- Agregar rutas a `RutasPublicas`: todos los endpoints son privados.
- Una ruta para editar o eliminar planillas.

## Resultado esperado

Con sesión de administrador se puede recorrer todo el CU por HTTP.

## Validación

1. `./mvnw test -Dtest=PlanillaIntegrationTest -Dtest.postgres.url=...` pasa (ver BE-07).
2. En Swagger (`/swagger-ui`) aparecen los cinco endpoints.
