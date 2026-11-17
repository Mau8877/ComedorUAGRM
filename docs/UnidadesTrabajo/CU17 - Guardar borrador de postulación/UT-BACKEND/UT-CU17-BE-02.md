# UT-CU17-BE-02 — `BorradorPostulacionController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-02` |
| **Nombre** | `BorradorPostulacionController` |
| **Caso de uso** | CU17-BE |
| **Depende de** | UT-CU17-BE-01 |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, `PUT`) y [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)

## Solicitud de negocio

Las pantallas web y móvil necesitan guardar el borrador y mostrar «Mi borrador».

## Objetivo

Dos endpoints bajo `/api/v1/postulaciones` que delegan en `BorradorPostulacionService`.

## Alcance

- `controller/BorradorPostulacionController.java`: `PUT /{codigo}/borrador` (`200`, `ApiResponse<BorradorGuardadoResponse>`) y `GET /mi-borrador` (`200`, `ApiResponse<BorradorResumenResponse>`, con `data: null` si no hay). Solo delegan: el usuario sale de `@CurrentUserId`.
- La ruta literal `mi-borrador` convive con `GET /{codigo}` (CU04) porque Spring prefiere la ruta literal, igual que `mias`.
- `BorradorPostulacionControllerTest` (contrato HTTP con el service simulado: sobre, códigos `200`/`400`/`401` y que el postulante salga del token). Es un `@SpringBootTest` como los demás tests de controller del módulo: necesita Postgres, así que **no se pudo ejecutar** donde se escribió (compila).

Archivos esperados (crear/tocar): `BorradorPostulacionController.java` y `BorradorPostulacionControllerTest.java`.

## Fuera de alcance (no tocar)

- Lógica de negocio, acceso a datos o logging en el controller.
- `EnvioPostulacionController` (CU02): sigue con `POST /postulaciones` y `…/enviar`.
- Rutas `DELETE` del borrador.

## Resultado esperado

Un `PUT` con el formulario parcial responde `200` con la hora del guardado; `GET /mi-borrador` responde el resumen o `data: null`.

## Validación

1. Con el backend levantado y una sesión de postulante: `POST /api/v1/postulaciones`, luego `PUT /api/v1/postulaciones/{codigo}/borrador` con `{"dependientes":3}` → `200` y `camposPendientes: 5`.
2. `GET /api/v1/postulaciones/mi-borrador` → el resumen con esa fecha; un `PUT` con `{"promedioAcademico":150}` → `400` `ERR_ENV_05`.
3. Sin sesión → `401`; con un rol que no es `POSTULANTE` → `403` `ERR_AUTH_07`.
4. Con Postgres disponible: `./mvnw test -Dtest=BorradorPostulacionControllerTest` en verde.
