# UT-CU22-BE-02 — Model y repository de la publicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-02` |
| **Nombre** | Model y repository de la publicación |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (`@Getter`, `@Setter` por campo, nunca `@Data`), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`BaseModel`, `@AttributeOverride` del `codigo`).
- Como referencia: `Adjudicacion` y su repository (CU-05).

## Solicitud de negocio

El sistema necesita guardar y consultar la publicación de resultados como una entidad propia.

## Objetivo

Existe el Model `PublicacionResultado` y su repository.

## Alcance

- `B/features/resultados/model/PublicacionResultado.java`: extiende `BaseModel`, `@AttributeOverride(name = "codigo", column = @Column(name = "codigo_publicacion_resultado"))`, campos `convocatoriaId`, `publicadoPor` (id del usuario) y `publicadoEn`. `@Getter` a nivel de clase; sin setters públicos: se crea con un constructor que recibe los tres valores. Sin `@ToString`/`@EqualsAndHashCode`.
- `B/features/resultados/repository/PublicacionResultadoRepository.java`: `JpaRepository<PublicacionResultado, Long>` con `findByCodigo(UUID)` y `existsByConvocatoriaId(long)`.
- Tests con Testcontainers (se saltan sin Docker): guarda y recupera una publicación; una segunda para la misma convocatoria falla por la unicidad.

Archivos esperados (crear): Model, repository y su test.

## Fuera de alcance (no tocar)

- Reglas de negocio, bitácora y avisos (UT-06).
- Consultas JDBC de lectura (UT-04).
- Cualquier otro Model de `adjudicacion`, `finalista` o `notificacion`.

## Resultado esperado

El módulo puede persistir una publicación y consultarla por su código público.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
