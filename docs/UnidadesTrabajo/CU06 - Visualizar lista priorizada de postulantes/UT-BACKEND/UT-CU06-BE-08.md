# UT-CU06-BE-08 — Endpoints de la lista priorizada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-08` |
| **Nombre** | Endpoints de la lista priorizada |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-05`, `UT-CU06-BE-06`, `UT-CU06-BE-07` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados), [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `B/features/usuarios/controller/UsuarioController.java` (`validarFiltros`, `@CurrentUserId`).

## Solicitud de negocio

La pantalla del administrador necesita llamar a la lista por HTTP.

## Objetivo

Los cuatro endpoints están expuestos, delgados y protegidos para `ADMINISTRADOR_DUBSS`.

## Alcance

- `B/features/postulaciones/controller/ListaPriorizadaController.java` (`@RequestMapping("/api/v1/lista-priorizada")`):
  - `GET /convocatorias` → `ApiResponse.success(lista, ...)`.
  - `GET /resumen` con `filter[convocatoriaId]` obligatorio (falta o no es UUID → `400` `ERR_SYS_05`).
  - `GET /` con `filter[convocatoriaId]`, `filter[carreraId]`, `search`, `page` (1), `pageSize` (20) y `sort`; `validarFiltros(request)` rechaza cualquier otro `filter[...]` con `ERR_PRI_02`; responde `ApiResponse.successList(datos, mensaje, meta)`.
  - `POST /exportaciones` con `@Valid @RequestBody ExportarListaRequest` y `@CurrentUserId`; responde `ResponseEntity<byte[]>` con `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="..."` y `Cache-Control: no-store`. Es la **única** respuesta fuera del sobre; los errores salen por `GlobalExceptionHandler` como siempre.
- El controller no tiene lógica, no loguea y no accede a repositories.
- `SecurityConfig`/`RutasPublicas`: **no** se agrega ningún endpoint como público.
- Documentar en OpenAPI (anotaciones de springdoc ya usadas en el proyecto), incluido el `application/pdf`.
- Tests de controller (`MockMvc`, con `service` simulado): forma del sobre y de `meta`, valores por defecto, `filter` desconocido y `sort` → `400` `ERR_PRI_02`, falta `filter[convocatoriaId]` → `400` `ERR_SYS_05`, cabeceras del PDF, `404` `ERR_PRI_01`, `401` `ERR_AUTH_01` sin token, `403` `ERR_AUTH_07` con un usuario que no es administrador.

Archivos esperados (crear): `ListaPriorizadaController.java` y su test. (Tocar): configuración de OpenAPI si hace falta.

## Fuera de alcance (no tocar)

- Lógica de negocio (UT-05 a 07).
- Rutas `/api/v1/convocatorias`.
- Hacer públicos endpoints.

## Resultado esperado

La lista se consulta y se exporta por HTTP con el contrato del README.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con la aplicación levantada y los datos de prueba (UT-03), llamar a cada endpoint con un administrador devuelve lo esperado.
