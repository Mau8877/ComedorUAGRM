# UT-CU12-BE-07 — Endpoints del Directivo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-07` |
| **Nombre** | Endpoints del Directivo |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-04`, `UT-CU12-BE-05`, `UT-CU12-BE-06` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, rutas), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (el test de controller verifica el `ApiResponse`).
- Como referencia: `ListaPriorizadaController` (exportación a PDF, CU-06).

## Solicitud de negocio

El Directivo consulta y descarga los indicadores desde la web.

## Objetivo

Existen los tres endpoints del README, con el sobre estándar (el PDF, como archivo).

## Alcance

- `B/features/dashboard/controller/DashboardController.java` (`/api/v1/dashboard`):
  - `GET /filtros` → `200` con `FiltrosDashboardResponse`.
  - `GET /indicadores` con `filter[gestion]` y `filter[convocatoria]` opcionales (UUID; un valor que no es UUID responde `400` `ERR_SYS_05`) → `200` con `IndicadoresResponse`.
  - `GET /indicadores/pdf` con los mismos filtros → `200` `application/pdf` con `Content-Disposition: attachment`, usando `@CurrentUserId` para el usuario que genera.
- Cada método valida, llama a **un** método de `service` y envuelve en `ApiResponse` (el PDF devuelve los bytes). Sin lógica, sin repositories y sin logging.
- Todas las rutas exigen sesión; `SecurityConfig` no necesita reglas nuevas (el rol lo decide el `service`).
- Tests de controller (`MockMvc`): `200` en los tres, `400` con UUID inválido y con `ERR_DAS_02`, `404` con `ERR_DAS_01`, `403` para un Administrador DUBSS y para un Postulante, `401` sin sesión; verifican `status`, `data` y `error`, y que el PDF trae el tipo y el nombre de archivo correctos.

Archivos esperados (crear): `DashboardController` y su test.

## Fuera de alcance (no tocar)

- Lógica de negocio.
- Endpoints de otros módulos.
- Un endpoint para refrescar la vista.

## Resultado esperado

El frontend puede consultar y descargar el dashboard por HTTP con el contrato acordado.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Swagger muestra los tres endpoints.
