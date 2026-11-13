# UT-CU04-BE-02 — Listado «Mis postulaciones»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-02` |
| **Nombre** | Listado «Mis postulaciones» |
| **Caso de uso** | CU04-BE |
| **Depende de** | `UT-CU04-BE-01` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`page`, `pageSize`, `filter[...]`, `sort`), [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (regla de dependencia), [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize` en el service).
- Como referencia: `DevolucionConsultaRepository` y el uso de `ParametrosListado` de `common/util`.

## Solicitud de negocio

El postulante quiere ver todas sus postulaciones enviadas y su estado sin ir a la DUBSS.

## Objetivo

El backend lista, paginadas y de la más reciente a la más antigua, las postulaciones enviadas del usuario autenticado.

## Alcance

- `B/features/postulaciones/repository/MisPostulacionesConsultaRepository.java` (JDBC, `NamedParameterJdbcTemplate`): `listar(long usuarioId, UUID gestionAcademica, orden, page, pageSize)`. Una sola consulta que une `postulacion`, `convocatoria`, `gestion_academica` y `publicacion_resultado` (existencia, por convocatoria). Filtra `postulante_id` del usuario, `estado <> 'borrador'` y `eliminado_en IS NULL` en cada tabla. Cuenta el total con la misma condición.
- `B/features/postulaciones/service/MisPostulacionesService.java`: `listar(UUID usuarioCodigo, ParametrosListado)` con `@PreAuthorize("hasRole('POSTULANTE')")` y `@Transactional(readOnly = true)`. Traduce el código del usuario a su id, valida `sort` (solo `fechaEnvio`, por defecto `-fechaEnvio`) y `filter[gestionAcademica]` (código UUID; un valor no soportado o mal formado lanza `ERR_SYS_05`), calcula el `estado` con `EstadoVisible.calcular` y arma `MiPostulacionResumenResponse`. Sin logging de lecturas.
- Tests unitarios del service (mock del repository): mapeo del estado visible, rechazo de `sort` inválido, rol equivocado. Test del repository con Testcontainers (se salta sin Docker): excluye borradores y postulaciones ajenas, filtra por gestión, ordena y pagina, y no multiplica filas por la unión con `publicacion_resultado`.

Archivos esperados (crear): repository de consulta, `MisPostulacionesService` y sus tests.

## Fuera de alcance (no tocar)

- El detalle (UT-03) y el controller (UT-04).
- `features/resultados/`: la publicación se lee con SQL propio.
- Cualquier escritura, bitácora o marca de avisos como leídos.

## Resultado esperado

El service devuelve la página de resúmenes con el estado que corresponde ver al postulante.

## Validación

1. `./mvnw test` pasa.
2. Un postulante con una postulación `pendiente`, una `borrador` y otra ajena recibe solo la `pendiente`.
3. Una `adjudicada` sin publicación aparece como `calificada`; con publicación, como `adjudicada`.
4. `./mvnw checkstyle:check` sin errores.
