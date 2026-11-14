# UT-CU21-BE-10 — Endpoints de finalistas y comunicados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-10` |
| **Nombre** | Endpoints de finalistas y comunicados |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-05`, `UT-CU21-BE-06`, `UT-CU21-BE-07`, `UT-CU21-BE-08` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, `filter`), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `ListaPriorizadaController` y su test.

## Solicitud de negocio

La pantalla necesita endpoints para listar finalistas, notificarlos, registrar la entrega y publicar comunicados.

## Objetivo

Los endpoints de la tabla del README responden con el sobre estándar.

## Alcance

- `FinalistaController`: `GET /api/v1/finalistas` (`filter[convocatoriaId]` obligatorio, `search`, `page`, `pageSize`; un `filter[...]` o `sort` no soportado → `400` `ERR_FIN_05`), `POST /api/v1/finalistas/notificaciones` (`200`) y `PATCH /api/v1/finalistas/{codigo}/entrega` (`200`).
- `ComunicadoController`: `POST /api/v1/comunicados` (`201`) y `GET /api/v1/comunicados` (paginado).
- Cada método valida con `@Valid`, llama a **un** método del service y envuelve en `ApiResponse`; sin lógica, sin `try/catch`, sin logs. `@CurrentUserId` para el actor.
- Tests de contrato (`MockMvc` + seguridad): forma del sobre y códigos, `meta` en los listados, `400` por campos vacíos, `401` sin sesión y `403` para un Postulante.

Archivos esperados (crear): `FinalistaController`, `ComunicadoController` y sus tests.

## Fuera de alcance (no tocar)

- Lógica de negocio (UT-05 a 08) y la bandeja (UT-09).
- El endpoint de convocatorias: es el de CU06 (`GET /lista-priorizada/convocatorias`).

## Resultado esperado

Con sesión de administrador, los endpoints funcionan y los errores salen catalogados.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
