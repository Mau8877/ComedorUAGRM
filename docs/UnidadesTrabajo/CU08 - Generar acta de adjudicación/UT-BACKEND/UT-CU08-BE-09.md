# UT-CU08-BE-09 — Endpoints del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-09` |
| **Nombre** | Endpoints del Administrador |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-07`, `UT-CU08-BE-08` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, rutas, paginación), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `ResultadosAdminController`, `ParametrosListado` (CU-22) y `ListaPriorizadaController` (devolución de un PDF).

## Solicitud de negocio

La pantalla del Administrador necesita llegar a todo el CU por HTTP con el contrato acordado.

## Objetivo

Existen los endpoints de la tabla del README, con el sobre estándar.

## Alcance

- `B/features/actas/controller/ActaController.java`:
  - `GET /api/v1/actas` (`page`, `pageSize` por defecto 20, `search`, `sort`) y `GET /api/v1/actas/pendientes` (`page`, `pageSize`): `successList` con `PageMeta`. Cualquier `filter[...]` no soportado es `400` `ERR_SYS_05`.
  - `GET /api/v1/convocatorias/{codigo}/acta` y `GET .../acta/descarga` (con `@CurrentUserId`): `200`.
  - `POST /api/v1/convocatorias/{codigo}/acta` (con `@CurrentUserId`): `201`.
  - `GET /api/v1/convocatorias/{codigo}/acta/vista-previa`: devuelve el PDF (`application/pdf`, `Content-Disposition: inline`) sin sobre, como `ListaPriorizadaController`.
- Cada método valida, llama a **un** método de `service` y envuelve en `ApiResponse` (salvo la vista previa). Sin lógica, sin repositories y sin logging.
- **No** hay rutas para regenerar, reemplazar ni eliminar el acta (`PUT`, `PATCH` y `DELETE` no existen).
- Todas las rutas exigen sesión; `SecurityConfig` no necesita reglas nuevas (el rol lo decide el `service`).
- Tests de controller (`MockMvc`): `200`, `201`, `400` por `sort` o filtro inválido, `404` (`ERR_PRI_01`, `ERR_ACT_03`, `ERR_ACT_04`), `409` (`ERR_ACT_01`, `ERR_ACT_02`), `403` para un no administrador, `401` sin sesión; la vista previa responde `application/pdf` y, en error, con el sobre; verifican `status`, `data`, `error` y `meta`; `PUT`, `PATCH` y `DELETE` sobre el acta responden `405`.

Archivos esperados (crear): `ActaController` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Documentación de endpoints fuera de Swagger.
- Endpoints del CU-05, 19, 20, 21 y 22.

## Resultado esperado

El frontend puede generar, consultar y descargar actas por HTTP con el contrato acordado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
