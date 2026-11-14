# UT-CU01-BE-09 — GET /api/v1/carreras (catálogo público de carreras)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-09` |
| **Nombre** | GET /api/v1/carreras (catálogo público de carreras) |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-02`, `UT-CU01-BE-05` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

El formulario de registro muestra la lista de carreras antes de que la persona tenga cuenta, así que ese catálogo debe poder leerse sin sesión.

## Objetivo

`GET /api/v1/carreras` devuelve las carreras paginadas, sin exigir token.

## Alcance

- `B/features/usuarios/dto/CarreraResponse.java` (`record`): `codigo`, `nombre`, `facultad`.
- `B/features/usuarios/service/CarreraService.java`: `listar(int page, int pageSize)` ordenado por `nombre`, devolviendo los datos y el `PageMeta`.
- `B/features/usuarios/controller/CarreraController.java`: `GET /api/v1/carreras?page&pageSize`, respuesta con `ApiResponse.successList(...)`. Sin lógica ni logging.
- `SecurityConfig`: `permitAll` **solo** para `GET /api/v1/carreras`.
- `SEGURIDAD_AUTH_BACKEND.md` (sección "Endpoints públicos"): documentar esta única excepción y su motivo.
- Tests: `200` sin token con `data` array y `meta`; `pageSize` aplicado; el resto de `/api/v1/**` sigue dando `401`.

Archivos esperados (crear/tocar): los tres de arriba, `SecurityConfig.java`, `SEGURIDAD_AUTH_BACKEND.md`, tests.

## Fuera de alcance (no tocar)

- Crear, editar o borrar carreras.
- Filtros o búsqueda (el selector usa `pageSize` grande, ver el README).
- Cargar datos en `carrera`.

## Resultado esperado

El formulario puede pedir las carreras sin sesión; cualquier otro recurso de negocio sigue protegido.

## Validación

1. `./mvnw test` pasa.
2. Con las carreras cargadas por la migración: `curl -i "localhost:8080/api/v1/carreras?pageSize=200"` devuelve `200`, `data` array y `meta`.
3. `curl -i localhost:8080/api/v1/otra-cosa` sin token devuelve `401`.
