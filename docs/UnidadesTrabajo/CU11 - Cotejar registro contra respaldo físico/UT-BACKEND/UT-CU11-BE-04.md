# UT-CU11-BE-04 — Endpoints REST

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-04` |
| **Nombre** | Endpoints REST |
| **Caso de uso** | CU11-BE |
| **Depende de** | UT-CU11-BE-03 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados) y [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md).

## Solicitud de negocio

La pantalla web necesita consultar los datos del cotejo y registrarlo.

## Objetivo

`CotejoController` expone los cuatro endpoints del contrato.

## Alcance

- `B/features/cotejos/controller/CotejoController.java` en `/api/v1/planillas/{codigo}/cotejos`: `GET /datos`, `GET /registros`, `GET` (lista) y `POST` (`201`). Los listados rechazan `filter[...]` (`ERR_SYS_05`) y devuelven `data` + `meta`. Sin rutas `PUT`, `PATCH` ni `DELETE` (`405`).

Archivos esperados (crear/tocar): el controller.

## Fuera de alcance (no tocar)

`SecurityConfig` (el rol se protege en el `service`); lógica de negocio en el controller.

## Resultado esperado

Los endpoints responden con el sobre estándar y los códigos del contrato.

## Validación

1. `./mvnw -o compile` y `checkstyle:check` sin hallazgos.
2. `CotejoIntegrationTest` (UT-BE-05).
