# UT-CU09-BE-02 — Consultas de planillas y becados vigentes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-02` |
| **Nombre** | Consultas de planillas y becados vigentes |
| **Caso de uso** | CU09-BE |
| **Depende de** | `UT-CU09-BE-01` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (regla de dependencia: no leer el repository de otro módulo) y [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort) (formato de `sort`, `ERR_SYS_05`).
- Como referencia: `B/features/actas/repository/ActasConsultaRepository.java`.

## Solicitud de negocio

La planilla se arma con los becados vigentes de la convocatoria y la pantalla lista las planillas ya generadas; ambas lecturas cruzan varias tablas.

## Objetivo

Existe `PlanillasConsultaRepository` con las lecturas del CU, todas con borrado lógico y en un número fijo de consultas.

## Alcance

- `B/features/planillas/repository/PlanillasConsultaRepository.java` (JDBC con `NamedParameterJdbcTemplate`):
  - `convocatoria(UUID)` → `DatosConvocatoria(id, codigo, nombre, gestion, gestionDesde, gestionHasta)` o vacío.
  - `contarVigentes(long)` y `becadosVigentes(long)` → `Becado(nombres, apellidos, ci, carrera)`, solo `beca.estado = 'VIGENTE'`, ordenados por apellidos, nombres e id, con `usuario` y `carrera` activos. El CI solo sale de aquí.
  - `planillas(UUID, Pageable)` → `Page<PlanillaResponse>`, la más reciente primero, con `sort` por `generadaEn` o `fechaDesde` (lista blanca de columnas, desempate por id); un campo no soportado o más de uno es `ERR_SYS_05`.
- `B/features/planillas/dto/PlanillaResponse.java`.
- Tests con Postgres real: convocatoria existente/inexistente/eliminada, solo vigentes y de la convocatoria, ordenados por apellido y nombre, ignora becas eliminadas, vacío, planillas ordenadas/paginadas/solo de la convocatoria y sin eliminadas, `sort` no soportado.

Archivos esperados (crear): `PlanillasConsultaRepository.java`, `PlanillaResponse.java` y `T/features/planillas/repository/PlanillasConsultaRepositoryTest.java`.

## Fuera de alcance (no tocar)

- Los `service`, el controller y el PDF.
- Leer `ActasConsultaRepository` o el repository del módulo `becas`.
- Seleccionar CI o correo fuera de `becadosVigentes`.

## Resultado esperado

El `service` obtiene todo lo que necesita sin armar SQL.

## Validación

1. `./mvnw test -Dtest=PlanillasConsultaRepositoryTest -Dtest.postgres.url=jdbc:postgresql://localhost:55432/<base_vacia>` pasa (12 tests).
2. `./mvnw checkstyle:check` sin errores.
