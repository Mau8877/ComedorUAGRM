# UT-CU21-BE-05 — Listado de finalistas con estados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-05` |
| **Nombre** | Listado de finalistas con estados |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-02`, `UT-CU21-BE-03` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize` en el `service`), [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación, `filter`) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `ListaPriorizadaRepository` y `ListaPriorizadaService` (CU06).

## Solicitud de negocio

El administrador ve los finalistas de una convocatoria con su aviso y su entrega.

## Objetivo

Existe `FinalistaService.listar`, que devuelve los finalistas paginados con el estado de su notificación y de su entrega.

## Alcance

- `B/features/finalistas/repository/FinalistaConsultaRepository.java` (JDBC, como la lista del CU06): finalistas de una convocatoria con `posicion`, nombre, CI, puntaje vigente, plazo, `notificado_en`, `documentacion_recibida_en` y el **estado de la notificación derivado del último aviso** de esa postulación sin comunicado: sin aviso → `SIN_ENVIAR`; `enviada_en` con valor → `ENVIADA`; nulo → `FALLIDA`. Orden por `posicion`; búsqueda por nombre, apellido o CI (comodines escapados, como en CU06); respeta el borrado lógico.
- `FinalistaService.listar(convocatoriaCodigo, search, page, pageSize)` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` y `readOnly`: `404` `ERR_FIN_02` si la convocatoria no existe; `400` `ERR_SYS_01` si `page` o `pageSize` < 1; devuelve la página y su `PageMeta`.
- Tests unitarios del service (Mockito) y de integración del repositorio (aviso enviado, aviso sin `enviada_en`, sin aviso, solo cuenta el último, el aviso de un comunicado no cuenta, búsqueda, paginación).

Archivos esperados (crear): `FinalistaConsultaRepository`, `FinalistaService` (solo `listar`) y sus tests.

## Fuera de alcance (no tocar)

- Notificar y registrar la entrega (UT-06 y 07).
- Endpoints (UT-10).
- Ordenar por otra columna: el orden es fijo.

## Resultado esperado

Se listan los finalistas con posición y estados correctos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
