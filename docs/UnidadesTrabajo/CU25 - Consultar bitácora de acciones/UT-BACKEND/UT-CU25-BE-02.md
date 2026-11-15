# UT-CU25-BE-02 — Endpoints, DTO y catálogo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-02` |
| **Nombre** | Endpoints, DTO y catálogo de errores |
| **Caso de uso** | CU25-BACKEND |
| **Depende de** | UT-CU25-BE-01 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

La pantalla web necesita la bitácora por HTTP, en el sobre estándar y sin exponer ids internos.

## Objetivo

Los dos endpoints de lectura, el DTO de respuesta y los errores `ERR_BIT_01` y `ERR_BIT_02` documentados.

## Alcance

- `BitacoraController`: `GET /api/v1/bitacora` y `GET /api/v1/bitacora/acciones`. Solo `GET`. Valida los
  `filter[...]` permitidos y el formato `AAAA-MM-DD` de las fechas (`ERR_SYS_05`).
- `BitacoraResponse` (`record`) con `codigo`, `fecha`, `usuario` (o `null`), `accion`, `entidad`, `detalle`, `ip`;
  sin `entidadId`.
- `BitacoraErrorCodes` y `docs/errors/bitacora/ERRORES_BITACORA.md` + su enlace en `docs/errors/README.md`.

Archivos esperados (crear/tocar): `BitacoraController.java`, `BitacoraResponse.java`, `BitacoraErrorCodes.java`,
`docs/errors/bitacora/ERRORES_BITACORA.md`, `docs/errors/README.md`.

## Fuera de alcance (no tocar)

- Rutas públicas (`RutasPublicas`): el endpoint es privado por defecto.
- Cualquier `POST`/`PUT`/`PATCH`/`DELETE` sobre la bitácora (RF-20).

## Resultado esperado

`GET /api/v1/bitacora` responde `200` con `data` array y `meta`; un `sort` o rango inválido responde `400`.

## Validación

1. `./mvnw compile` sin errores.
2. Con el backend en `dev`: `GET /api/v1/bitacora?filter[desde]=2026-09-30&filter[hasta]=2026-09-01` → `400` `ERR_BIT_02`.
3. `GET /api/v1/bitacora?sort=ip` → `400` `ERR_BIT_01`.
4. Sin rol de Administrador → `403` `ERR_AUTH_07`.
