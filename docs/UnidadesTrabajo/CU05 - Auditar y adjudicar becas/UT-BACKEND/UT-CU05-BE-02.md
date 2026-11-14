# UT-CU05-BE-02 — Models: `Adjudicacion`, cantidad de finalistas y transiciones de la postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-02` |
| **Nombre** | Models: `Adjudicacion`, cantidad de finalistas y transiciones de la postulación |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `Postulacion.archivar()` / `rechazar()` (CU-20), `PostulacionHistorial` y `Finalista`.

## Solicitud de negocio

El sistema necesita representar la decisión final y que la postulación sepa pasar a «adjudicada» o a «no adjudicada».

## Objetivo

Existen el Model `Adjudicacion` y su repository, la `Convocatoria` expone la cantidad de finalistas y `Postulacion` sabe adjudicarse y no adjudicarse.

## Alcance

- `B/features/postulaciones/enums/ResultadoAdjudicacion.java` (`ADJUDICADA`, `NO_ADJUDICADA`), mapeado con `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` como `ResultadoAuditoria`.
- `B/features/postulaciones/model/Adjudicacion.java` (`extends BaseModel`, `@AttributeOverride(name = "codigo", column = @Column(name = "codigo_adjudicacion"))`): `postulacionId` (`Long`, `updatable = false`), `resultado`, `puntajeFinal` (`BigDecimal`, nulo permitido), `decididoPor` (`Long`, id interno del usuario) y `fechaAdjudicacion` (`Instant`). `@Getter` a nivel de clase, **sin setters** (inmutable una vez creada: decisión 6 y CA16) y constructor público con todos los campos. Sin `@Data`, `@ToString` ni `@EqualsAndHashCode`.
- `B/features/postulaciones/repository/AdjudicacionRepository.java` (`JpaRepository<Adjudicacion, Long>`): solo `findByPostulacionId(Long)`; las cuentas de cupos van por JDBC (UT-04).
- `Convocatoria` (CU-13 la hará editable): agregar `cantidadFinalistas` (`Integer`, columna `cantidad_finalistas`) **solo con `@Getter`**, sin setter ni constructor nuevo; quien la escribe es el CU-13.
- `Postulacion` gana `adjudicar()` (`calificada` → `adjudicada`) y `noAdjudicar()` (`calificada` → `no_adjudicada`); cada uno lanza `IllegalStateException` si el estado no es «calificada» (red de seguridad; la validación de negocio con `ERR_ADJ_01` es del `service`, UT-07). Verificar que `EstadoPostulacion` y el tipo `estado_postulacion` ya traen `adjudicada` y `no_adjudicada`; **si faltara alguno, parar y avisar** en vez de crear una migración aquí.
- Tests: cada transición cambia el estado desde «calificada» y falla desde cualquier otro (pendiente, devuelta, archivada, no adjudicada, adjudicada); una `Adjudicacion` se guarda y se lee con su enum y su puntaje (incluido `null`); una segunda adjudicación para la misma postulación viola el índice único; `Convocatoria` lee `cantidad_finalistas`.

Archivos esperados (crear): `ResultadoAdjudicacion`, `Adjudicacion`, `AdjudicacionRepository` y sus tests. (Tocar): `Convocatoria`, `Postulacion` y su test.

## Fuera de alcance (no tocar)

- Reglas de negocio, cupos, errores y bitácora (UT-04 y UT-07).
- Setters de `Adjudicacion`, o salir de «adjudicada» o «no adjudicada» (no hay reapertura).
- Escribir `cantidad_finalistas` (CU-13).
- Setters nuevos en `PostulacionHistorial` (sigue append-only) ni cambios en `Finalista`.

## Resultado esperado

Los Models de la decisión existen, son inmutables donde corresponde y la postulación cambia de estado de forma controlada.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
