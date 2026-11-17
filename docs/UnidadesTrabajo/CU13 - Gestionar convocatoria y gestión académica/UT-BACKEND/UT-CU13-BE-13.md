# UT-CU13-BE-13 — Controllers de convocatorias, gestiones y tipos de documento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-13` |
| **Nombre** | Controllers de convocatorias, gestiones y tipos de documento |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-06`, `UT-CU13-BE-07`, `UT-CU13-BE-08`, `UT-CU13-BE-09`, `UT-CU13-BE-10`, `UT-CU13-BE-11` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (rutas, verbos, controllers delgados)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (tests de controller con la forma del `ApiResponse`)
- `B/features/usuarios/controller/UsuarioController.java` y `B/common/util/ParametrosListado.java`

## Solicitud de negocio

El panel web necesita endpoints REST para todo lo que el Administrador hace con convocatorias y gestiones.

## Objetivo

Los endpoints del [contrato del README](./README.md#contrato-de-datos--recurso-http) están expuestos, delgados y con el sobre estándar.

## Alcance

- `B/features/convocatorias/controller/ConvocatoriaController.java` (`/api/v1/convocatorias`): `GET` (lista; lee `page` y `pageSize` por defecto `1` y `20`, `search`, `filter[estado]`, `filter[gestion]` y `sort`, con `ParametrosListado.validarFiltros(request, Set.of("filter[estado]", "filter[gestion]"))`; un `filter[estado]` que no es del enum o un `filter[gestion]` que no es UUID es `ValidationException` `ERR_SYS_05`), `GET /{codigo}`, `POST` (`201`), `PUT /{codigo}`, `POST /{codigo}/abrir`, `POST /{codigo}/cerrar` y `DELETE /{codigo}` (`200`, `data: null`). Usan `ConvocatoriaService` y `ConvocatoriaEstadoService`; el actor sale de `@CurrentUserId UUID`; los métodos que escriben desempaquetan el `ResultadoAuditado` con `.datos()`.
- `B/features/convocatorias/controller/GestionAcademicaController.java` (`/api/v1/gestiones-academicas`): `GET` (lista, `sort`, sin filtros), `POST` (`201`) y `PUT /{codigo}`.
- `B/features/convocatorias/controller/TipoDocumentoController.java` (`/api/v1/tipos-documento`): `GET` (lista).
- Cada método: `@Valid` sobre el body, llama **un** método del service y envuelve en `ApiResponse` (`success` / `successList` con `PageMeta`). Sin lógica, sin `try/catch`, sin logging.
- Ninguna ruta nueva en `RutasPublicas`: todas son privadas por defecto.
- Tests de controller (`MockMvc`, servicios simulados, verificando `status`, `data`, `error` y `meta`): cada endpoint con su código de éxito; `400` por body inválido (`ERR_SYS_01`), por `filter` o `sort` no soportado; el listado trae `meta`; `DELETE` devuelve `data: null`; sin sesión `401` y sin el rol `403` (`ERR_AUTH_07`).

Archivos esperados (crear/tocar): `ConvocatoriaController.java`, `GestionAcademicaController.java`, `TipoDocumentoController.java` y sus tests.

## Fuera de alcance (no tocar)

- Lógica de negocio: está en los servicios.
- Endpoints para Postulantes o públicos (consultar convocatorias abiertas es del CU02).
- Cambiar `SecurityConfig` o `RutasPublicas`.

## Resultado esperado

El panel web puede consumir todo el CU por HTTP, con el sobre estándar y solo con rol de Administrador.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el backend arriba y una sesión de Administrador, `GET /api/v1/convocatorias` responde `200` con `data` y `meta`; sin sesión, `401`.
