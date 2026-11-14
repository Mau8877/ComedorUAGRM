# UT-CU22-BE-08 — Endpoints del Administrador y del postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-08` |
| **Nombre** | Endpoints del Administrador y del postulante |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-05`, `UT-CU22-BE-06`, `UT-CU22-BE-07` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, rutas, `PageMeta`), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (el test de controller verifica el `ApiResponse`).
- Como referencia: `CierrePostulacionController` (CU-20).

## Solicitud de negocio

El Administrador publica y consulta desde la web, y el postulante consulta sus resultados.

## Objetivo

Existen los endpoints de la tabla del README, con el sobre estándar.

## Alcance

- `B/features/resultados/controller/ResultadosAdminController.java`: `GET .../resultados/resumen`, `GET .../resultados/decisiones` (`page`, `pageSize` por defecto 20, `search`, `filter[resultado]`, `sort`) y `POST .../resultados/publicar` (`201`).
- `B/features/resultados/controller/ResultadosPostulanteController.java`: `GET /api/v1/resultados/convocatorias`, `GET .../convocatorias/{codigo}/resultados` y `GET .../convocatorias/{codigo}/resultados/mio` (con `@CurrentUserId`).
- Cada método valida, llama a **un** método de `service` y envuelve en `ApiResponse` (`successList` con `PageMeta` para las colecciones). Sin lógica, sin repositories y sin logging.
- Todas las rutas exigen sesión; `SecurityConfig` no necesita reglas nuevas (el rol lo decide el `service`).
- Tests de controller (`MockMvc`): `201`, `200`, `400` por `sort` inválido, `404`, `409` con `ERR_PUB_01` a `ERR_PUB_03`, `404` con `ERR_PUB_04`, `403` para un no administrador en las rutas de Administrador, `401` sin sesión; verifican `status`, `data`, `error` y `meta`.

Archivos esperados (crear): los dos controllers y sus tests.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Documentación de endpoints fuera de Swagger.
- Endpoints de CU-05, 19, 20 y 21.

## Resultado esperado

El frontend puede publicar y consultar resultados por HTTP con el contrato acordado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Swagger muestra los seis endpoints.
