# UT-CU01-BE-02 — Models y repositorios de los catálogos Rol y Carrera

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-02` |
| **Nombre** | Models y repositorios de los catálogos Rol y Carrera |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-01` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

El registro asigna siempre el rol Postulante y el formulario necesita las carreras; ambos son catálogos que el sistema debe poder leer.

## Objetivo

Existen los Models `Rol` y `Carrera`, las constantes de nombres de rol y sus repositorios, mapeados a las tablas de la migración.

## Alcance

- `B/features/usuarios/model/Rol.java` y `Carrera.java`: extienden `BaseModel`, `@Entity`, `@Table(name = "rol")` / `"carrera"`, `@AttributeOverride(name = "codigo", column = @Column(name = "codigo_rol"))` (y `codigo_carrera`). `@Getter` de clase; **sin setters** (son catálogos de solo lectura para la aplicación).
- `B/features/usuarios/RolNombres.java`: constantes de texto `POSTULANTE`, `ADMINISTRADOR_DUBSS`, `DIRECTIVO`, `PERSONAL_COMEDOR` (no un enum Java).
- `B/features/usuarios/repository/RolRepository.java` (`JpaRepository<Rol, Long>`, `Optional<Rol> findByNombre(String)`) y `CarreraRepository.java` (`findByCodigo(UUID)`, `Page<Carrera> findAll(Pageable)`).
- Test `T/features/usuarios/repository/…` solo si hay Docker (patrón de `TESTING_BACKEND.md`): `findByNombre("POSTULANTE")` existe tras la migración.

Archivos esperados (crear): los cinco de arriba más el test.

## Fuera de alcance (no tocar)

- Los Models `Usuario` y `Postulante` (`UT-CU01-BE-03`).
- Endpoint o service de carreras (`UT-CU01-BE-09`).
- Cargar carreras: ya lo hace la migración de `UT-CU01-BE-01`.

## Resultado esperado

Los catálogos son accesibles desde el código y `ddl-auto=validate` los acepta contra la migración.

## Validación

1. `./mvnw test` pasa y el contexto de Spring arranca (con la migración aplicada) sin errores de validación del esquema.
2. `./mvnw checkstyle:check` sin errores.
