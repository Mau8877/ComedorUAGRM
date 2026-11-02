# UT-CU01-BE-01 — Migración de tablas de cuentas y acceso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-01` |
| **Nombre** | Migración de tablas de cuentas y acceso |
| **Caso de uso** | CU01-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)

## Solicitud de negocio

El registro y el inicio de sesión necesitan dónde guardar usuarios, roles, postulantes, carreras y la bitácora de accesos, con la forma exacta que fija el diseño lógico del proyecto.

## Objetivo

Queda creada la migración Flyway con las tablas `rol`, `usuario`, `carrera`, `postulante` y `bitacora`, sus índices únicos parciales y los 4 roles sembrados.

## Alcance

Un único archivo `V20261102100000__crear_tablas_cuentas_y_acceso.sql` con:

- **Todas las tablas**: `id bigint GENERATED ALWAYS AS IDENTITY` (salvo `postulante`, cuya PK es `usuario_id`), `codigo_<tabla> uuid NOT NULL DEFAULT uuidv7() UNIQUE`, y `creado_en`, `actualizado_en` (`timestamptz NOT NULL DEFAULT now()`) y `eliminado_en` (`timestamptz NULL`).
- `rol`: `nombre varchar(50) NOT NULL`.
- `usuario`: `rol_id bigint NOT NULL` (FK a `rol`), `ci varchar(20)`, `correo varchar(150)`, `firebase_uid varchar(128)`, `nombres varchar(100)`, `apellidos varchar(100)` (todos `NOT NULL`) y `activo boolean NOT NULL DEFAULT true`.
- `carrera`: `nombre varchar(150) NOT NULL`, `facultad varchar(150) NOT NULL`.
- `postulante`: `usuario_id bigint` (PK y FK a `usuario`), `carrera_id bigint NOT NULL` (FK a `carrera`), `registro_universitario varchar(20) NOT NULL`, `telefono varchar(20) NOT NULL`.
- `bitacora`: `usuario_id bigint NULL` (FK a `usuario`), `accion varchar(60) NOT NULL`, `entidad varchar(60)`, `entidad_id bigint`, `detalle jsonb`, `ip inet`.
- **Índices únicos parciales** (`WHERE eliminado_en IS NULL`): `rol(nombre)`, `carrera(nombre)`, `usuario(ci)`, `usuario(correo)`, `usuario(firebase_uid)`, `postulante(registro_universitario)`.
- **Índices** de FK: `usuario(rol_id)`, `postulante(carrera_id)`, `bitacora(usuario_id)`.
- **Datos sembrados** en `rol`: `POSTULANTE`, `ADMINISTRADOR_DUBSS`, `DIRECTIVO`, `PERSONAL_COMEDOR`.

Archivos esperados (crear): `projects/backend/src/main/resources/db/migration/V20261102100000__crear_tablas_cuentas_y_acceso.sql`.

## Fuera de alcance (no tocar)

- Cualquier otra tabla del diagrama (`postulacion`, `convocatoria`, `beca`, etc.): son de otros CU.
- Carga de datos de `carrera`: queda como follow-up (origen por definir con la DUBSS).
- Los Models JPA: son de `UT-CU01-BE-02`, `03` y `04`.
- No tocar `V1__init_schema.sql` (ya aplicada).

## Resultado esperado

Al arrancar con Flyway, la base tiene las 5 tablas con sus restricciones y los 4 roles; `ddl-auto=validate` aún no se contrasta con Models (eso llega en las UT siguientes).

## Validación

1. Levantar `docker compose up postgres` (Postgres 18) y arrancar el backend; Flyway debe aplicar la migración sin errores.
2. En `psql`, `\d usuario`, `\d postulante` y `\d bitacora` muestran las columnas, PK/FK e índices indicados; `SELECT nombre FROM rol` devuelve los 4 roles.
3. Insertar dos `usuario` con el mismo `ci` falla; insertarlos con el mismo `ci` pero el primero con `eliminado_en` no nulo, funciona (índice parcial).
