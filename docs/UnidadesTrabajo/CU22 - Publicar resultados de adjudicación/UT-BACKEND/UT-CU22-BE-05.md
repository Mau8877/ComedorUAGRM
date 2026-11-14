# UT-CU22-BE-05 — Resumen y decisiones para el Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-05` |
| **Nombre** | Resumen y decisiones para el Administrador |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-03`, `UT-CU22-BE-04` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Antes de publicar, el Administrador revisa cuántos adjudicados y no adjudicados hay, a quién se avisará y qué falta decidir.

## Objetivo

El `service` entrega el resumen de la publicación y la lista paginada de decisiones.

## Alcance

- `B/features/resultados/service/ResultadosAdminService.java` (`@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`):
  - `resumen(UUID convocatoriaCodigo)`: arma `ResumenPublicacionResponse` con los conteos, `aAvisar` (cantidad de `destinatariosDelAviso`), `yaPublicada`, `publicadoEn`/`publicadoPor` y `puedePublicar` (cerrada, no publicada y `calificadasPendientes = 0`; **no** exige que haya adjudicados). Convocatoria inexistente lanza `NotFoundException` `ERR_PRI_01`.
  - `decisiones(UUID convocatoriaCodigo, filtro, Pageable)`: devuelve la página de `DecisionResultadoResponse`. Un resultado de «no adjudicada» **sin** motivo; el motivo no se expone aquí.
- Sin logging de lecturas.
- Tests con el repository simulado: resumen con todo decidido, con pendientes (`puedePublicar = false`), ya publicada, convocatoria no cerrada, convocatoria inexistente, paginación y filtro, y que un rol distinto de Administrador recibe `403`.

Archivos esperados (crear): `ResultadosAdminService` y su test.

## Fuera de alcance (no tocar)

- Publicar (UT-06).
- Endpoints (UT-08).
- La lista pública del postulante (UT-07).

## Resultado esperado

El Administrador puede ver, sin riesgo, el estado exacto de la publicación.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
