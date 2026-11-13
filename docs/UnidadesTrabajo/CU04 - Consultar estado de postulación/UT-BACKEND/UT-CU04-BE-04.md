# UT-CU04-BE-04 — Endpoints del postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-04` |
| **Nombre** | Endpoints del postulante |
| **Caso de uso** | CU04-BE |
| **Depende de** | `UT-CU04-BE-02`, `UT-CU04-BE-03` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, `sort`, paginación), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `DevolucionController`.

## Solicitud de negocio

La web y la app necesitan dos rutas para consultar las postulaciones del usuario autenticado.

## Objetivo

Los dos endpoints del contrato responden con el sobre estándar.

## Alcance

- `B/features/postulaciones/controller/MisPostulacionesController.java`, `@RequestMapping("/api/v1/postulaciones")`:
  - `GET /mias` → `200` `ApiResponse` con la lista y `meta` (`ParametrosListado`: `page`, `pageSize`, `filter[gestionAcademica]`, `sort`).
  - `GET /{codigo}` → `200` con el detalle.
  Cada método solo valida, toma `@CurrentUserId UUID`, llama **un** método del service y envuelve. Sin lógica, sin `try/catch`, sin logging. `mias` se declara antes que `{codigo}`.
- Tests de controller (`MockMvc` + seguridad): forma del sobre (`status`, `data`, `meta`, `error`), `200`, `404` `ERR_POS_01`, `400` `ERR_SYS_05`, `401` sin token y `403` `ERR_AUTH_07` con otro rol.
- Confirmar que `/api/v1/postulaciones/**` no es público (queda privado por defecto; no se toca `RutasPublicas`).

Archivos esperados (crear): `MisPostulacionesController` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio (UT-02, UT-03).
- Endpoints de envío, borrador o corrección (CU-02, CU-17).
- `RutasPublicas` y `SecurityConfig`.

## Resultado esperado

Un postulante autenticado puede listar y consultar sus postulaciones por HTTP.

## Validación

1. `./mvnw test` pasa.
2. `GET /api/v1/postulaciones/mias` sin token da `401`; con rol `ADMINISTRADOR_DUBSS`, `403`.
3. `./mvnw checkstyle:check` sin errores.
