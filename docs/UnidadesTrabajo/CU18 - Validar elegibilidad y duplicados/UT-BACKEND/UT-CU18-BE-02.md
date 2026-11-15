# UT-CU18-BE-02 — Consultas de la validación (repositorio JDBC)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-02` |
| **Nombre** | Consultas de la validación (repositorio JDBC) |
| **Caso de uso** | CU18-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- `DevolucionConsultaRepository` como modelo de estilo

## Solicitud de negocio

La validación necesita leer postulante, convocatoria y postulaciones de la gestión sin depender de repositorios de otros módulos.

## Objetivo

Existe `ValidacionPostulacionConsultaRepository` con tres consultas de solo lectura y su prueba con base real.

## Alcance

- `B/features/postulaciones/repository/ValidacionPostulacionConsultaRepository.java` (`NamedParameterJdbcTemplate`, respeta `eliminado_en IS NULL`):
  - `postulanteDe(UUID usuarioCodigo)` → `Optional<DatosPostulante(id, activo)>`; vacío si la cuenta no es de un postulante.
  - `convocatoria(UUID codigo)` → `Optional<DatosConvocatoria(id, gestionId, gestionEtiqueta, estado, fechaApertura, fechaCierre)>`.
  - `postulacionesEnLaGestion(long postulanteId, long gestionId)` → `List<PostulacionExistente(id, codigo, convocatoriaId, estado, fechaEnvio)>` de todas las convocatorias de la gestión, en cualquier estado.
- `T/features/postulaciones/repository/ValidacionPostulacionConsultaRepositoryTest.java` (`@DataJpaTest` con `PostgresTestDatabase`): cuenta que no es de postulante, convocatoria eliminada, postulaciones de otra gestión, eliminadas y de otro postulante.

Archivos esperados (crear/tocar): el repositorio y su test.

## Fuera de alcance (no tocar)

- Reglas de negocio (qué es duplicado o elegible): son de `UT-CU18-BE-03`.
- Escribir o bloquear filas.
- Migraciones: no hacen falta.

## Resultado esperado

Las consultas devuelven lo necesario y nada eliminado ni ajeno.

## Validación

1. `./mvnw test -Dtest=ValidacionPostulacionConsultaRepositoryTest` pasa (con Docker; sin él queda `SKIPPED`).
2. `./mvnw checkstyle:check` sin errores.
