# UT-CU13-BE-04 — Consultas de repositorio y consulta de postulaciones por convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-04` |
| **Nombre** | Consultas de repositorio y consulta de postulaciones por convocatoria |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-02`, `UT-CU13-BE-03` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (un módulo no usa el repositorio de otro)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- `B/features/usuarios/repository/UsuarioRepository.java` (`buscarPersonal`, como referencia de una búsqueda paginada con filtros)

## Solicitud de negocio

Las pantallas listan convocatorias, gestiones y tipos de documento con búsqueda y filtros; los servicios necesitan saber si ya hay otra convocatoria abierta, cuáles ya vencieron y si una convocatoria tiene postulaciones.

## Objetivo

Los repositorios del módulo tienen todas las consultas que usan los servicios de las UT siguientes, y el módulo `postulaciones` expone si una convocatoria tiene postulaciones sin que otro módulo toque su repositorio.

## Alcance

- `B/features/convocatorias/repository/ConvocatoriaRepository.java`:
  - `Page<Convocatoria> buscar(String patron, EstadoConvocatoria estado, UUID gestionCodigo, Pageable)` como `default` sobre una `Specification` (`ConvocatoriaSpecs`, con `JpaSpecificationExecutor` y `@EntityGraph` de la gestión) y no con `@Query`, porque un `UUID` o un enum nulo no tiene tipo para Postgres: `nombre` (sin mayúsculas, `patron` ya viene con `%`), `estado` y `gestion.codigo` opcionales, `join fetch` de la gestión y `countQuery` aparte.
  - `boolean existsByGestionIdAndEstadoAndIdNot(Long gestionId, EstadoConvocatoria estado, Long excluirId)` (comprobar si hay **otra** abierta en la gestión; el `Id` excluido evita compararla consigo misma).
  - `List<Convocatoria> findByEstadoAndFechaCierreLessThanEqual(EstadoConvocatoria estado, Instant ahora)`.
- `B/features/convocatorias/repository/GestionAcademicaRepository.java`: `Page<GestionAcademica> buscar(String patron, Pageable)` (por `etiqueta`), `boolean existsByEtiqueta(String etiqueta)` y `boolean existsByEtiquetaAndIdNot(String etiqueta, Long id)`.
- `B/features/convocatorias/repository/TipoDocumentoRepository.java` (nuevo, `JpaRepository<TipoDocumento, Long>`): `Optional<TipoDocumento> findByCodigo(UUID)`, `List<TipoDocumento> findByCodigoIn(Collection<UUID>)` y `Page<TipoDocumento> buscar(String patron, Pageable)` (por `nombre`).
- `B/features/convocatorias/repository/ConvocatoriaDocumentoRepository.java` (nuevo, `JpaRepository<ConvocatoriaDocumento, ConvocatoriaDocumentoId>`): `List<ConvocatoriaDocumento> findByConvocatoriaId(Long)` con `join fetch` del tipo de documento, y `@Modifying @Query(nativeQuery = true) int reactivar(Long convocatoriaId, Long tipoDocumentoId, boolean obligatorio)` que hace `UPDATE convocatoria_documento SET eliminado_en = NULL, obligatorio = :obligatorio, actualizado_en = now() WHERE convocatoria_id = :c AND tipo_documento_id = :t AND eliminado_en IS NOT NULL` y devuelve las filas afectadas (necesario porque `@SQLRestriction` oculta las eliminadas y la clave primaria incluye a las eliminadas).
- `B/features/postulaciones/repository/PostulacionRepository.java`: `boolean existsByConvocatoriaId(Long convocatoriaId)`.
- `B/features/postulaciones/service/PostulacionesDeConvocatoriaService.java` (nuevo): `boolean existenPara(Long convocatoriaId)`, sin `@PreAuthorize` (lo llaman otros servicios), que usa el repositorio anterior. Es la única vía por la que `convocatorias` pregunta por postulaciones.
- Tests (`@DataJpaTest` contra base real): `buscar` filtra por texto, estado y gestión y pagina; `existsByGestionIdAndEstadoAndIdNot` distingue la propia fila; `findByEstadoAndFechaCierreLessThanEqual` solo trae las vencidas; `reactivar` devuelve `1` para un par eliminado, `0` para uno que nunca existió o que está activo, y la fila vuelve a leerse; `existsByConvocatoriaId` ignora postulaciones eliminadas.

Archivos esperados (crear/tocar): `ConvocatoriaRepository.java`, `GestionAcademicaRepository.java`, `TipoDocumentoRepository.java`, `ConvocatoriaDocumentoRepository.java`, `PostulacionRepository.java`, `PostulacionesDeConvocatoriaService.java` y sus tests.

## Fuera de alcance (no tocar)

- La lógica de negocio de los servicios (UT 06 a 12).
- Cambiar las consultas existentes de `postulaciones` (lista priorizada, adjudicación, historial).
- Acceder a `PostulacionRepository` desde `features/convocatorias/`: se usa `PostulacionesDeConvocatoriaService`.

## Resultado esperado

Los servicios de las UT siguientes tienen todas las consultas listas y probadas contra una base real.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
