# UT-CU22-BE-07 — Consulta de resultados para el postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-07` |
| **Nombre** | Consulta de resultados para el postulante |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-03`, `UT-CU22-BE-04` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@CurrentUserId`), [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).

## Solicitud de negocio

Una vez publicados los resultados, cada postulante debe poder ver su resultado y quiénes fueron adjudicados.

## Objetivo

El `service` entrega al postulante las convocatorias publicadas, la lista de adjudicados y su propio resultado.

## Alcance

- `B/features/resultados/service/ResultadosPostulanteService.java` (`@RequiredArgsConstructor`, `@PreAuthorize("isAuthenticated()")`):
  - `convocatoriasPublicadas(Pageable)`: página de `ConvocatoriaPublicadaResponse`.
  - `adjudicados(UUID convocatoriaCodigo, String search, Pageable)`: página de `AdjudicadoPublicoResponse` (nombre y apellido, nota, carrera, facultad). Convocatoria inexistente `ERR_PRI_01`; **sin publicación** `ERR_PUB_04` (`404`).
  - `miResultado(UUID convocatoriaCodigo, @CurrentUserId UUID usuario)`: `MiResultadoResponse`; `resultado = null` si el usuario no es finalista con decisión. Mismos errores.
- Nunca se devuelven no adjudicados en la lista, ni CI, correo, ingresos ni motivos.
- Sin logging de lecturas.
- Tests con el repository simulado: antes de publicar `ERR_PUB_04` en las dos consultas; después de publicar la lista solo trae adjudicados con los cinco campos; no adjudicado ve su resultado sin motivo; un usuario ajeno recibe `resultado = null`; paginación, `search` y orden; sin sesión `401`.

Archivos esperados (crear): `ResultadosPostulanteService` y su test.

## Fuera de alcance (no tocar)

- Los endpoints (UT-08).
- El CU-04 (estado de la postulación), que sigue siendo un follow-up.
- Mostrar al postulante el motivo de «no adjudicada».

## Resultado esperado

El postulante consulta solo lo público y lo propio, y solo después de publicar.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
