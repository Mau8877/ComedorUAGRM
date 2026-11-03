# UT-CU01-BE-03 — Models y repositorios de Usuario y Postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-03` |
| **Nombre** | Models y repositorios de Usuario y Postulante |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-01`, `UT-CU01-BE-02` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

La cuenta de un Postulante son dos filas relacionadas (`usuario` y `postulante`) que el sistema debe poder crear y consultar, incluyendo búsquedas por los datos únicos.

## Objetivo

Existen los Models `Usuario` y `Postulante` y sus repositorios, con las búsquedas que usa el registro y la sesión.

## Alcance

- `B/features/usuarios/model/Usuario.java`: extiende `BaseModel`, `@Table(name = "usuario")`, `@AttributeOverride(... "codigo_usuario")`. Campos: `rol` (`@ManyToOne(fetch = LAZY)` a `Rol`), `ci`, `correo`, `passwordHash`, `nombres`, `apellidos`, `activo`, `intentosFallidos`, `bloqueosConsecutivos`, `bloqueadoHasta`. `@Getter` de clase; `@Setter` solo en `activo` (lo usará CU14) y en los tres campos del bloqueo (los maneja el inicio de sesión).
- `B/features/usuarios/model/Postulante.java`: `@Table(name = "postulante")`, PK = FK a `usuario` con `@MapsId` (ver la nota de "tablas sin `id` propio" en `PERSISTENCIA_BD_BACKEND.md`: **no** hereda el `id` generado de `BaseModel`; en esta UT se resuelve cómo reutilizar `codigo`, `creadoEn`, `actualizadoEn` y `eliminadoEn`, por ejemplo con una segunda `@MappedSuperclass` sin `id`, y se deja documentado ahí). `@AttributeOverride` a `codigo_postulante`. Campos: `usuario`, `carrera` (`@ManyToOne` LAZY), `registroUniversitario`, `telefono`.
- `UsuarioRepository`: `findByCorreo(String)`, `findByCodigo(UUID)`, `existsByCi(String)`, `existsByCorreo(String)`.
- `PostulanteRepository`: `existsByRegistroUniversitario(String)`.
- Sin `@ToString`/`@EqualsAndHashCode` de Lombok.
- Tests (con Docker, patrón de `TESTING_BACKEND.md`): guardar un `Usuario` + `Postulante` y buscarlos; duplicar `ci` lanza `DataIntegrityViolationException`; un `Usuario` con `eliminadoEn` no se encuentra por `findByCorreo`.

Archivos esperados (crear): `Usuario.java`, `Postulante.java`, `UsuarioRepository.java`, `PostulanteRepository.java`, la superclase sin `id` y los tests.

## Fuera de alcance (no tocar)

- Lógica de registro (`UT-CU01-BE-11`).
- Los demás Models de P1 que no usa este CU.
- Cambiar `BaseModel` más allá de lo mínimo para la tabla sin `id`.

## Resultado esperado

Se pueden guardar y consultar usuarios y postulantes con borrado lógico y los nombres de columna del diseño.

## Validación

1. `./mvnw test` pasa; el contexto arranca con `ddl-auto=validate`.
2. Los tests con Testcontainers pasan donde haya Docker (en esta máquina quedan en SKIPPED).
3. `./mvnw checkstyle:check` sin errores.
