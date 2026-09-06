# UT-HU01-BE-01 — Persistencia: `Usuario`, `Rol`, `UsuarioRol`, migraciones, repositorios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-01` |
| **Nombre** | Persistencia: `Usuario`, `Rol`, `UsuarioRol` + migraciones Flyway + repositorios |
| **Historia** | HU01-BE |
| **Depende de** | `common/model/BaseModel.java` (ya existe) |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md),
[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
completa (en particular la sección [Lombok](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok) —
`@Getter`/`@Setter` de Lombok, nunca a mano),
[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
completa (en particular el patrón `id`/`codigo` de `BaseModel` y la regla
de consultar el diagrama conceptual antes de crear un Model),
[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos), y
[`docs/DiagramaConceptual.mermaid`](../../../../docs/DiagramaConceptual.mermaid)
(entidades `USUARIO`, `ROL`, `USUARIO_ROL` — **fuente de verdad de los
campos**, no se inventa ningún campo fuera de ahí).

## Solicitud de negocio

El login necesita un usuario real persistido, con soporte para que un
mismo usuario tenga varios roles activos a la vez (esquema relacional del
diagrama conceptual, no un enum fijo de 2 valores).

## Objetivo

Crear las tablas `users`, `roles`, `user_roles` y las entidades JPA
correspondientes (todas en `features/usuarios/`, dueño del dato — ver
decisión de diseño del [README](./README.md)), con sus repositorios.

## Alcance

### Enum `EstadoUsuario` — `features/usuarios/enums/EstadoUsuario.java`

```java
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO
}
```

Set mínimo suficiente para esta HU (CA05 de HU01 solo distingue
activo/no-activo) — si el dominio real necesita más estados (ej.
`SUSPENDIDO`), se agregan cuando exista ese caso de uso concreto, no antes.

### Entidad `Usuario` — `features/usuarios/model/Usuario.java`

Extiende `BaseModel`. Campos según `docs/DiagramaConceptual.mermaid`
(entidad `USUARIO`) — **no** incluye `nombre` (vive en `Perfil`, entidad
separada fuera de alcance de esta HU) ni `role` como columna simple (los
roles son la relación `UsuarioRol` de abajo):

| Campo Java | Columna | Tipo/constraint |
|------------|---------|------------------|
| `username` | `username` | `VARCHAR(255) NOT NULL UNIQUE` |
| `correo` | `correo` | `VARCHAR(255) NOT NULL UNIQUE` |
| `passwordHash` | `password_hash` | `VARCHAR(255) NOT NULL` |
| `estado` | `estado` | `VARCHAR(20) NOT NULL`, `@Enumerated(EnumType.STRING)` |
| `correoVerificado` | `correo_verificado` | `BOOLEAN NOT NULL DEFAULT FALSE` |

```java
@Getter
@Setter
@Entity
@Table(name = "users")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
public class Usuario extends BaseModel {
    // username, correo, passwordHash, estado, correoVerificado
    // getters/setters generados por Lombok (@Getter/@Setter de clase) --
    // ver CONVENCIONES_JAVA_BACKEND.md#lombok. Sin @ToString/@EqualsAndHashCode
    // (passwordHash nunca debe poder imprimirse via toString()).
}
```

- `@Table(name = "users")` — nombre en inglés, ya fijado así en
  `SEGURIDAD_AUTH_BACKEND.md` (excepción deliberada dentro del dominio de
  identidad/auth, igual criterio se aplica a `roles`/`user_roles` abajo).
- `estado` inicializado en `ACTIVO` como valor de campo Java, además de
  fijarlo explícito en el `INSERT` de cualquier semilla de datos.
- **No** loguear ni exponer `passwordHash` en ningún `toString()`/log (ver
  [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear)).

### Entidad `Rol` — `features/usuarios/model/Rol.java`

| Campo Java | Columna | Tipo/constraint |
|------------|---------|------------------|
| `nombre` | `nombre` | `VARCHAR(50) NOT NULL UNIQUE` |
| `descripcion` | `descripcion` | `TEXT NULL` |
| `isActive` | `is_active` | `BOOLEAN NOT NULL DEFAULT TRUE` |

```java
@Getter
@Setter
@Entity
@Table(name = "roles")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_rol"))
public class Rol extends BaseModel { /* ... */ } // getters/setters via Lombok
```

### Entidad `UsuarioRol` — `features/usuarios/model/UsuarioRol.java`

Entidad de asociación propia (**no** un `@ManyToMany` simple de JPA — esta
fila tiene datos propios más allá de las dos FK: `codigo`, `isActive`,
timestamps de `BaseModel`):

| Campo Java | Columna | Tipo/constraint |
|------------|---------|------------------|
| `usuario` | `usuario_id` | `BIGINT NOT NULL`, `@ManyToOne`, FK a `users.id` |
| `rol` | `rol_id` | `BIGINT NOT NULL`, `@ManyToOne`, FK a `roles.id` |
| `isActive` | `is_active` | `BOOLEAN NOT NULL DEFAULT TRUE` |

```java
@Getter
@Setter
@Entity
@Table(name = "user_roles")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario_rol"))
public class UsuarioRol extends BaseModel {
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    private boolean isActive = true;
    // getters/setters via Lombok (@Getter/@Setter de clase)
}
```

### Migraciones Flyway

Tres archivos, `src/main/resources/db/migration/V{yyyyMMddHHmmss}__....sql`
(timestamps reales y **consecutivos** al momento de crearlos, ver
[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)):

```sql
-- V..._create_table_users.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    codigo_usuario UUID NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    correo VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- V..._create_table_roles.sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    codigo_rol UUID NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- V..._create_table_user_roles.sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    codigo_usuario_rol UUID NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES users(id),
    rol_id BIGINT NOT NULL REFERENCES roles(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_user_roles_usuario_id ON user_roles(usuario_id);
CREATE INDEX idx_user_roles_rol_id ON user_roles(rol_id);
```

- Índices en ambas FK de `user_roles` (se filtra por `usuario_id` en cada
  login) — ver [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#claves-foráneas-e-índices).

### Repositorios — `features/usuarios/repository/`

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCodigo(UUID codigo);

    @Query("SELECT u FROM Usuario u WHERE u.username = :identificador OR u.correo = :identificador")
    Optional<Usuario> findByUsernameOrCorreo(@Param("identificador") String identificador);
}

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    @Query("""
        SELECT ur FROM UsuarioRol ur JOIN FETCH ur.rol
        WHERE ur.usuario.id = :usuarioId AND ur.isActive = true AND ur.rol.isActive = true
        """)
    List<UsuarioRol> findRolesActivosDeUsuario(@Param("usuarioId") Long usuarioId);
}
```

- `RolRepository` **no** se crea en esta UT — nada de esta HU necesita
  listar/crear roles de forma independiente, solo leerlos ya asociados a
  un usuario (vía `UsuarioRolRepository`). Se agrega cuando exista una HU
  de administración de roles.

### Archivos esperados (crear)

- `features/usuarios/enums/EstadoUsuario.java`
- `features/usuarios/model/Usuario.java`, `Rol.java`, `UsuarioRol.java`
- `features/usuarios/repository/UsuarioRepository.java`, `UsuarioRolRepository.java`
- 3 migraciones Flyway

## Fuera de alcance (no tocar)

- `features/auth/**`, `security/**`, `config/SecurityConfig.java`
- `Perfil` (entidad separada del diagrama, fuera de esta HU)
- `RolRepository`, cualquier CRUD de roles o de usuarios
- Datos semilla (roles/usuarios de prueba) — no forma parte del alcance;
  se insertan a mano o vía script de desarrollo fuera de estas UT

## Resultado esperado

`ddl-auto=validate` no falla al arrancar. `UsuarioRepository.findByUsernameOrCorreo(...)`
y `UsuarioRolRepository.findRolesActivosDeUsuario(...)` disponibles para
las UT siguientes.

## Validación

1. Arranque de la app sin error de schema-validation contra Postgres.
2. Insertar un usuario y asociarle dos roles activos vía `UsuarioRol` →
   `findRolesActivosDeUsuario(usuarioId)` devuelve ambos.
3. Desactivar (`isActive=false`) una de las dos filas `UsuarioRol` →
   `findRolesActivosDeUsuario` devuelve solo la restante.
4. Desactivar (`isActive=false`) el `Rol` en sí (no la fila de
   asociación) → ese rol deja de aparecer en `findRolesActivosDeUsuario`
   aunque la fila `UsuarioRol` siga activa.
5. `findByUsernameOrCorreo("mismo-valor")` encuentra al usuario tanto si
   ese valor está en `username` como si está en `correo`.
6. Un `Usuario`/`Rol`/`UsuarioRol` soft-deleted no aparece en ninguna
   query (heredado del `@SQLRestriction` de `BaseModel`).
