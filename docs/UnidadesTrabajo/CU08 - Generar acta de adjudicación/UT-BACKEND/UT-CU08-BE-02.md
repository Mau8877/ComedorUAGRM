# UT-CU08-BE-02 — Model y repository del acta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-02` |
| **Nombre** | Model y repository del acta |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok: `@Getter` y setters por campo; nunca `@Data`; carpetas del módulo) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`BaseModel`, `@AttributeOverride` del código público).
- Como referencia: `PublicacionResultado` y `PublicacionResultadoRepository` (CU-22).

## Solicitud de negocio

El acta se guarda una vez y no se edita: el modelo no debe dejar cambiarla.

## Objetivo

Existe el modelo JPA inmutable del acta y su repository.

## Alcance

- `B/features/actas/model/ActaAdjudicacion.java` (`@Entity`, `@Table(name = "acta_adjudicacion")`, `@AttributeOverride` de `codigo` → `codigo_acta_adjudicacion`, extiende `BaseModel`, `@Getter` sin ningún setter): `convocatoriaId`, `numero`, `storagePath`, `generadaPor` (id interno) y `generadaEn`. Todos `updatable = false`. Constructor público con los cinco valores.
- `B/features/actas/repository/ActaAdjudicacionRepository.java` (`JpaRepository<ActaAdjudicacion, Long>`): `findByConvocatoriaId(Long)`, `existsByConvocatoriaId(Long)` y `findByCodigo(UUID)`.
- Tests con `@DataJpaTest` y base real (Testcontainers, se saltan sin Docker): guarda y lee por convocatoria; `existsByConvocatoriaId` correcto; la clase no tiene setters (el campo no se actualiza); el borrado lógico excluye la fila de las consultas.

Archivos esperados (crear): `ActaAdjudicacion`, `ActaAdjudicacionRepository` y sus tests.

## Fuera de alcance (no tocar)

- Lógica de negocio, DTOs y consultas JDBC (UT-03 y UT-04).
- Relaciones `@ManyToOne` con `Convocatoria` o `Usuario`: se usan ids simples, como `Adjudicacion` y `PublicacionResultado` (este módulo no depende de entidades ajenas).

## Resultado esperado

El módulo puede guardar y leer actas con JPA sin poder editarlas.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
