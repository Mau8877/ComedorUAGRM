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

Quedan creadas las migraciones Flyway con las tablas `rol`, `usuario`, `carrera`, `postulante` y `bitacora`, sus índices únicos parciales, los 4 roles y las 71 carreras sembrados.

## Alcance

Dos archivos de migración. El primero, `V20261102100000__crear_tablas_cuentas_y_acceso.sql`, con:

- **Todas las tablas**: `id bigint GENERATED ALWAYS AS IDENTITY` (salvo `postulante`, cuya PK es `usuario_id`), `codigo_<tabla> uuid NOT NULL DEFAULT uuidv7() UNIQUE`, y `creado_en`, `actualizado_en` (`timestamptz NOT NULL DEFAULT now()`) y `eliminado_en` (`timestamptz NULL`).
- `rol`: `nombre varchar(50) NOT NULL`.
- `usuario`: `rol_id bigint NOT NULL` (FK a `rol`), `ci varchar(20)`, `correo varchar(150)`, `password_hash varchar(100)`, `nombres varchar(100)`, `apellidos varchar(100)` (todos `NOT NULL`), `activo boolean NOT NULL DEFAULT true` y el control de bloqueo por intentos fallidos: `intentos_fallidos integer NOT NULL DEFAULT 0`, `bloqueos_consecutivos integer NOT NULL DEFAULT 0` y `bloqueado_hasta timestamptz NULL`.
- `carrera`: `nombre varchar(150) NOT NULL`, `facultad varchar(150) NOT NULL`.
- `postulante`: `usuario_id bigint` (PK y FK a `usuario`), `carrera_id bigint NOT NULL` (FK a `carrera`), `registro_universitario varchar(20) NOT NULL`, `telefono varchar(20) NOT NULL`.
- `bitacora`: `usuario_id bigint NULL` (FK a `usuario`), `accion varchar(60) NOT NULL`, `entidad varchar(60)`, `entidad_id bigint`, `detalle jsonb`, `ip inet`.
- **Índices únicos parciales** (`WHERE eliminado_en IS NULL`): `rol(nombre)`, `carrera(nombre)`, `usuario(ci)`, `usuario(correo)`, `postulante(registro_universitario)`, `sesion(token_hash)`.
- `sesion` (un refresh token por dispositivo): `usuario_id bigint NOT NULL` (FK a `usuario`), `token_hash varchar(64) NOT NULL` (SHA-256 del refresh token, nunca el token), `expira_en timestamptz NOT NULL` y `revocada_en timestamptz NULL`.
- **Índices** de FK: `usuario(rol_id)`, `postulante(carrera_id)`, `bitacora(usuario_id)`, `sesion(usuario_id)`.
- **Dos migraciones.** `V20261102100000__crear_tablas_cuentas_y_acceso.sql` creó el esquema inicial (con `firebase_uid`, ya aplicada: no se edita) y `V20261103100000__autenticacion_con_jwt_propio.sql` pasa a la autenticación propia: quita `usuario.firebase_uid`, agrega `password_hash` y las columnas de bloqueo, y crea `sesion`. Las cuentas que ya existían quedan con un hash inválido hasta que se les asigne una contraseña (CU15).
- **Datos sembrados** en `rol`: `POSTULANTE`, `ADMINISTRADOR_DUBSS`, `DIRECTIVO`, `PERSONAL_COMEDOR`.

El segundo, `V20261102100100__cargar_carreras.sql`, inserta las 71 carreras de la UAGRM con su facultad en texto (por ejemplo, `Ingeniería Informática` / `Facultad de Ingeniería en Ciencias de la Computación y Telecomunicaciones`). La lista de origen repite algunas carreras con distintos códigos de plan (13 filas); como `carrera(nombre)` es único, se carga **una sola vez cada nombre** (sin distinguir mayúsculas ni tildes: `Ingenieria Civil` e `Ingeniería Civil` son la misma carrera).

Archivos esperados (crear): `projects/backend/src/main/resources/db/migration/V20261102100000__crear_tablas_cuentas_y_acceso.sql` y `.../V20261102100100__cargar_carreras.sql`.

## Fuera de alcance (no tocar)

- Cualquier otra tabla del diagrama (`postulacion`, `convocatoria`, `beca`, etc.): son de otros CU.
- Los Models JPA: son de `UT-CU01-BE-02`, `03` y `04`.
- No tocar `V1__init_schema.sql` (ya aplicada).

## Resultado esperado

Al arrancar con Flyway, la base tiene las 5 tablas con sus restricciones y los 4 roles; `ddl-auto=validate` aún no se contrasta con Models (eso llega en las UT siguientes).

## Validación

1. Levantar `docker compose up postgres` (Postgres 18) y arrancar el backend; Flyway debe aplicar la migración sin errores.
2. En `psql`, `\d usuario`, `\d postulante` y `\d bitacora` muestran las columnas, PK/FK e índices indicados; `SELECT nombre FROM rol` devuelve los 4 roles y `SELECT count(*) FROM carrera` devuelve 71.
3. Insertar dos `usuario` con el mismo `ci` falla; insertarlos con el mismo `ci` pero el primero con `eliminado_en` no nulo, funciona (índice parcial).
