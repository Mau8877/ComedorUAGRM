# UT-CU06-BE-02 — Models, enums y repositories de las cinco tablas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-02` |
| **Nombre** | Models, enums y repositories de las cinco tablas |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-01` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (estructura de paquetes, `enums/`, Lombok por campo) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`BaseModel`, `@AttributeOverride`, enums con `@Enumerated(STRING)`).
- Como referencia: `B/features/usuarios/model/Usuario.java` y `Postulante.java`.

## Solicitud de negocio

El resto del CU necesita leer estas tablas desde Java.

## Objetivo

Existen los Models, enums y repositories de las cinco tablas, listos para consultar.

## Alcance

- `B/features/convocatorias/model/GestionAcademica.java` y `Convocatoria.java`; `B/features/convocatorias/enums/EstadoConvocatoria.java`; repositories `GestionAcademicaRepository` y `ConvocatoriaRepository` (`findByCodigo(UUID)`).
- `B/features/postulaciones/model/Postulacion.java`; `B/features/postulaciones/enums/EstadoPostulacion.java`; `PostulacionRepository` (solo `findByCodigo(UUID)` por ahora; la consulta de la lista llega en UT-06).
- `B/features/preseleccion/model/ModeloPreseleccion.java` y `PuntajePostulacion.java`; repositories `ModeloPreseleccionRepository` y `PuntajePostulacionRepository`.
- Cada Model extiende `BaseModel`, fija el nombre de la columna pública con `@AttributeOverride` (`codigo_convocatoria`, `codigo_postulacion`, …), lleva `@Getter` y **ningún** `@Setter` (nadie en este CU modifica estas filas; los CU que las editen agregan el setter de **su** campo). Enums como `EnumType.STRING` mapeados al tipo nativo de PostgreSQL.
- `JpaRepository<Model, Long>` (nunca `UUID`).
- Tests: `@DataJpaTest` con base real que guarda y vuelve a leer una fila de cada tabla, y que el `@SQLRestriction` oculta una fila con `eliminado_en`.

Archivos esperados (crear): los tres paquetes con los Models, enums y repositories de arriba y sus tests. (Tocar): ninguno.

## Fuera de alcance (no tocar)

- La consulta de la lista (UT-06).
- Setters, servicios o endpoints de convocatoria, postulación o puntaje (CU-13, CU-02, CU-03).
- Relaciones `@OneToMany` entre estos Models: la lista usa una consulta propia.

## Resultado esperado

Los cinco Models mapean sus tablas sin errores de arranque ni de validación.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
