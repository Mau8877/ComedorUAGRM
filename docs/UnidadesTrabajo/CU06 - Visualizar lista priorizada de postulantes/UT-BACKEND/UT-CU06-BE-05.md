# UT-CU06-BE-05 — Convocatorias disponibles y resumen de la lista

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-05` |
| **Nombre** | Convocatorias disponibles y resumen de la lista |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-02`, `UT-CU06-BE-04` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (regla de dependencia) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos).
- Como referencia: `UsuarioPersonalService.listar` (lecturas sin log, `@PreAuthorize` en el `service`).

## Solicitud de negocio

Antes de ver la lista, el administrador elige una convocatoria y quiere saber cuántas postulaciones tiene calificadas y cuántas siguen pendientes.

## Objetivo

El `service` entrega las convocatorias que se pueden consultar y el resumen de una de ellas.

## Alcance

- `B/features/postulaciones/service/ListaPriorizadaService.java` (`@Service`, `@RequiredArgsConstructor`), con el rol exigido por `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`:
  - `@Transactional(readOnly = true) List<ConvocatoriaPriorizableResponse> listarConvocatorias()`: convocatorias no eliminadas (cualquier estado), la de `fecha_apertura` más reciente primero, con la etiqueta de su gestión. Sin ninguna, devuelve lista vacía (el frontend muestra A9).
  - `@Transactional(readOnly = true) ResumenListaPriorizadaResponse resumir(UUID convocatoriaCodigo)`: `totalCalificadas` = postulaciones `calificada` con puntaje vigente; `totalPendientes` = postulaciones `pendiente`. Convocatoria inexistente o eliminada → `NotFoundException` `ERR_PRI_01`.
- Consultas de conteo en `PostulacionRepository` (conteos por `(convocatoria_id, estado)`, apoyados en el índice de UT-01).
- Sin logs ni bitácora (son lecturas).
- Tests de servicio (mocks): lista vacía, orden de las convocatorias, resumen con calificadas y pendientes, convocatoria inexistente (`ERR_PRI_01`), un usuario sin rol de administrador es rechazado.

Archivos esperados (crear): `ListaPriorizadaService.java` y su test. (Tocar): `PostulacionRepository.java`, `ConvocatoriaRepository.java`.

## Fuera de alcance (no tocar)

- La consulta de la lista (UT-06) y el PDF (UT-07).
- Los endpoints (UT-08).
- Cualquier endpoint de `/api/v1/convocatorias` (es del CU-13).

## Resultado esperado

El `service` devuelve las convocatorias consultables y el resumen correcto de cada una, y rechaza a quien no es Administrador DUBSS.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
